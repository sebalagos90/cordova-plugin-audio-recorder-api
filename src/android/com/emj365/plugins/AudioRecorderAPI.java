package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;

public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;


  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    Integer seconds;
    outputFile = context.getFilesDir().getAbsoluteFile() + "/tempAudio" + ".m4a";

    if (action.equals("record")) {
      if (args.length() >= 1) {
        seconds = args.getInt(0);
      } else {
        seconds = 7;
      }
      myRecorder = new MediaRecorder();
      myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
      myRecorder.setAudioSamplingRate(44100);
      myRecorder.setAudioChannels(1);
      myRecorder.setAudioEncodingBitRate(32000);
      myRecorder.setOutputFile(outputFile);

      try {
        myRecorder.prepare();
        myRecorder.start();
      } catch (final Exception e) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            callbackContext.error(e.getMessage());
          }
        });
        return false;
      }

      countDowntimer = new CountDownTimer(seconds * 1000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
          stopRecord(callbackContext);
        }
      };
      countDowntimer.start();
      return true;
    }

    if (action.equals("stop")) {
      countDowntimer.cancel();
      stopRecord(callbackContext);
      return true;
    }

    if (action.equals("playFromBase64")) {
      String base64Data = args.getString(0);
      decodeAudioAndPlay(base64Data, context, callbackContext);
      return true;
    }

    return false;
  }

  private void stopRecord(final CallbackContext callbackContext) {
    myRecorder.stop();
    myRecorder.release();
    final String audioData = audioToBase64(outputFile);

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        callbackContext.success(audioData);
      }
    });
  }

  private String audioToBase64(String urlAudio) {
    byte[] audioBytes;
    String _audioBase64 = "";
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      File audioFile = new File(urlAudio);
      FileInputStream fis = new FileInputStream(audioFile);
      byte[] buf = new byte[1024];
      int n;
      while (-1 != (n = fis.read(buf)))
        baos.write(buf, 0, n);
      audioBytes = baos.toByteArray();

      // Here goes the Base64 string
      _audioBase64 = Base64.encodeToString(audioBytes, Base64.DEFAULT);
    } catch(Exception e){
      _audioBase64 = "";
    }
    return _audioBase64;
  }

  private void decodeAudioAndPlay(String base64Audio, Context context, CallbackContext callbackContext) {
    try{
      File audioFile = new File(outputFile);
      FileOutputStream fos = new FileOutputStream(audioFile);
      fos.write(Base64.decode(base64Audio.getBytes(), Base64.DEFAULT));
      fos.close();
      try {
        MediaPlayer mp = new MediaPlayer();
        mp.setDataSource(context, Uri.parse(outputFile));
        mp.prepare();
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
          @Override
          public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();
          }
        });
        callbackContext.success();
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
