package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.agreeya.audiodetectionapp.PcmAudioHelper;
import com.agreeya.audiodetectionapp.WavAudioFormat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorderAPI extends CordovaPlugin {

  private String pcmOutputURL, waveOutputURL;
  private CountDownTimer countDowntimer;
  final int SAMPLE_RATE = 24000;
  final String TAG = "AUDIO_RECORDER";
  boolean isRecording;
  AudioRecord audioRecorder;
  byte[] audioBuffer;
  int bufferSize;
  Thread recordingThread;
  File rawOutputFile, waveOutputFile;
  CallbackContext recordCallbackContext;


  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    final Context context = cordova.getActivity().getApplicationContext();
    Integer seconds;
    //URL MEMORIA EXTERNA
    //pcmOutputURL = context.getExternalFilesDir(null)+ "/tempAudio" + ".pcm";
    //waveOutputURL = context.getExternalFilesDir(null) + "/tempAudio" + ".wav";
    //URL MEMORIA INTERNA (NO ACCESIBLE)
    pcmOutputURL = context.getFilesDir().getAbsoluteFile() + "/tempAudio" + ".pcm";
    waveOutputURL = context.getFilesDir().getAbsoluteFile() + "/tempAudio" + ".wav";

    if (action.equals("record")) {
      if (args.length() >= 1) {
        seconds = args.getInt(0);
      } else {
        seconds = 7;
      }
      recordCallbackContext = callbackContext;
      bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_8BIT);

      if(bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE){
        bufferSize = SAMPLE_RATE * 2;
      }

      audioBuffer = new byte[bufferSize];
      audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
              SAMPLE_RATE,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_8BIT,
              bufferSize);

      if(audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
        Log.e(TAG, "Audio audioRecorder can not been initialized D:");
        recordCallbackContext.error("Audio audioRecorder can not been initialized D:");
        return false;
      }

      Log.i(TAG, "Start recording");
      audioRecorder.startRecording();
      isRecording = true;

      recordingThread = new Thread(new Runnable() {

        @Override
        public void run() {
          FileOutputStream os = null;
          try{
            os = new FileOutputStream(pcmOutputURL);
          } catch(FileNotFoundException e) {
            audioRecorder.stop();
            audioRecorder.release();
            isRecording = false;
            Log.e(TAG, e.toString());
          }


          while(isRecording) {
            audioRecorder.read(audioBuffer, 0, audioBuffer.length);
            try{
              os.write(audioBuffer, 0, bufferSize);
            } catch(IOException e) {
              audioRecorder.stop();
              audioRecorder.release();
              isRecording = false;
              Log.e(TAG, e.toString());
            }
          }
          try {
            os.close();
          } catch (IOException e) {
            Log.e(TAG, e.toString());
          }
          audioRecorder.stop();
          audioRecorder.release();
          rawOutputFile = new File(pcmOutputURL);
          waveOutputFile = new File(waveOutputURL);
          WavAudioFormat af = new WavAudioFormat(SAMPLE_RATE, 8, 1, true);
          try{
            /*
            Thank you akhilesh2491 and aalap-shah
            https://github.com/aalap-shah/Drive
            */
            PcmAudioHelper.convertRawToWav(af, rawOutputFile, waveOutputFile);
            recordCallbackContext.success(pcmOutputURL);
          } catch (IOException e) {
            Log.e(TAG, e.toString());
          }
        }
      });
      recordingThread.start();
      countDowntimer = new CountDownTimer(seconds * 1000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
          stopRecord();
        }
      };
      countDowntimer.start();
      return true;
    }

    if (action.equals("stop")) {
      countDowntimer.cancel();
      stopRecord();
      return true;
    }

    if (action.equals("playFromBase64")) {
      String base64Data = args.getString(0);
      decodeAudioAndPlay(base64Data, context, callbackContext);
      return true;
    }

    return false;
  }

  private void stopRecord() {
    if(audioRecorder != null){
      isRecording = false;
    }
    else {
      recordCallbackContext.error("Stop record error");
    }
  }
  private void decodeAudioAndPlay(String base64Audio, Context context, final CallbackContext callbackContext) {
    try{
      File audioFile = new File(pcmOutputURL);
      FileOutputStream fos = new FileOutputStream(audioFile);
      fos.write(Base64.decode(base64Audio.getBytes(), Base64.DEFAULT));
      fos.close();
      try {
        MediaPlayer mp = new MediaPlayer();
        mp.setDataSource(context, Uri.parse(pcmOutputURL));
        mp.prepare();
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
          @Override
          public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();
            callbackContext.success();
          }
        });

      } catch(Exception e) {
        Log.e("decodeAudioAndPlay", e.toString());
        callbackContext.error(e.toString());
      }
    } catch(Exception e) {
      Log.e("decodeAudioAndPlay", e.toString());
      callbackContext.error(e.toString());
    }
  }

}
