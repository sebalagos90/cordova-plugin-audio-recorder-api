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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder myRecorder;
  private String pcmOutputURL, waveOutputURL;
  private CountDownTimer countDowntimer;
  final int SAMPLE_RATE = 44100;
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
    pcmOutputURL = context.getExternalFilesDir(null)+ "/tempAudio" + ".pcm";
    waveOutputURL = context.getExternalFilesDir(null) + "/tempAudio" + ".wav";
    //URL MEMORIA INTERNA (NO ACCESIBLE)
    //pcmOutputURL = context.getFilesDir().getAbsoluteFile() + "/tempAudio" + ".pcm";
    //waveOutputURL = context.getFilesDir().getAbsoluteFile() + "/tempAudio" + ".wav";

    if (action.equals("record")) {
      if (args.length() >= 1) {
        seconds = args.getInt(0);
      } else {
        seconds = 7;
      }
      recordCallbackContext = callbackContext;
      bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT);

      if(bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE){
        bufferSize = SAMPLE_RATE * 2;
      }

      audioBuffer = new byte[bufferSize];

      audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
              SAMPLE_RATE,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
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
          try{
            rawToWave(rawOutputFile, waveOutputFile);
            recordCallbackContext.success(waveOutputURL);
          } catch (IOException e) {
            Log.e(TAG, e.toString());
          }
          //playAudio(pcmOutputURL, context);
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

  private void rawToWave(final File rawFile, final File waveFile) throws IOException {

    byte[] rawData = new byte[(int) rawFile.length()];
    DataInputStream input = null;
    try {
      input = new DataInputStream(new FileInputStream(rawFile));
      input.read(rawData);
    } finally {
      if (input != null) {
        input.close();
      }
    }

    DataOutputStream output = null;
    try {
      output = new DataOutputStream(new FileOutputStream(waveFile));
      // WAVE header
      // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
      writeString(output, "RIFF"); // chunk id
      writeInt(output, 36 + rawData.length); // chunk size
      writeString(output, "WAVE"); // format
      writeString(output, "fmt "); // subchunk 1 id
      writeInt(output, 16); // subchunk 1 size
      writeShort(output, (short) 1); // audio format (1 = PCM)
      writeShort(output, (short) 2); // number of channels
      writeInt(output, SAMPLE_RATE/2); // sample rate
      writeInt(output, 16); // byte rate
      writeShort(output, (short) 2); // block alignx
      writeShort(output, (short) 16); // bits per sample
      writeString(output, "data"); // subchunk 2 id
      writeInt(output, rawData.length); // subchunk 2 size
      // Audio data (conversion big endian -> little endian)
      short[] shorts = new short[rawData.length / 2];
      ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
      ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
      for (short s : shorts) {
        bytes.putShort(s);
      }

      output.write(fullyReadFileToBytes(rawFile));
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }
  byte[] fullyReadFileToBytes(File f) throws IOException {
    int size = (int) f.length();
    byte bytes[] = new byte[size];
    byte tmpBuff[] = new byte[size];
    FileInputStream fis= new FileInputStream(f);
    try {

      int read = fis.read(bytes, 0, size);
      if (read < size) {
        int remain = size - read;
        while (remain > 0) {
          read = fis.read(tmpBuff, 0, remain);
          System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
          remain -= read;
        }
      }
    }  catch (IOException e){
      throw e;
    } finally {
      fis.close();
    }

    return bytes;
  }
  private void writeInt(final DataOutputStream output, final int value) throws IOException {
    output.write(value >> 0);
    output.write(value >> 8);
    output.write(value >> 16);
    output.write(value >> 24);
  }

  private void writeShort(final DataOutputStream output, final short value) throws IOException {
    output.write(value >> 0);
    output.write(value >> 8);
  }

  private void writeString(final DataOutputStream output, final String value) throws IOException {
    for (int i = 0; i < value.length(); i++) {
      output.write(value.charAt(i));
    }
  }

}
