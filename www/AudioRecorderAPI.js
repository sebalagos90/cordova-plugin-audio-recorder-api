/*globals cordova, device*/
'use strict';
function AudioRecorderAPI() {
}
AudioRecorderAPI.prototype.PERMISSIONS_GRANTED = 'GRANTED';
AudioRecorderAPI.prototype.checkRecordPermissions = function (callback) {
  if(device && device.platform === 'Android' && cordova && cordova.plugins && cordova.plugins.permissions){
    var permissions = cordova.plugins.permissions;
    permissions.hasPermission(permissions.RECORD_AUDIO, function(status){
      if(!status.hasPermission){
        permissions.requestPermission(permissions.RECORD_AUDIO, function(response){
          if (response.hasPermission) {
            callback('FT_GRANTED');
          } else {
            callback('NOT_GRANTED');
          }
        }, function(){
          callback('NOT_GRANTED');
        });
      }
      else {
        callback('GRANTED');
      }
    }, null);
  } else{
    cordova.exec(function (iosPermissions){
      if (iosPermissions === 'GRANTED') {
        callback('GRANTED');
      } else {
        callback(iosPermissions);
      }
    }, null, "AudioRecorderAPI", "checkRecordPermissions", []);
  }
};

AudioRecorderAPI.prototype.record = function (successCallback, errorCallback, duration, sampleRate, bitsEncode, numberOfChannels) {
  var params = [duration ? duration : 7, sampleRate ? sampleRate : 44100, bitsEncode ? bitsEncode : 16, numberOfChannels ? numberOfChannels : 2];
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "record", params);
};

AudioRecorderAPI.prototype.stop = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "stop", []);
};

AudioRecorderAPI.prototype.playFromBase64 = function (base64String, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "playFromBase64", [base64String]);
};

AudioRecorderAPI.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.audioRecorderAPI = new AudioRecorderAPI();
  return window.plugins.audioRecorderAPI;
};

cordova.addConstructor(AudioRecorderAPI.install);
