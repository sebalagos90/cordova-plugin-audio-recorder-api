function AudioRecorderAPI() {
}
AudioRecorderAPI.prototype.PERMISSIONS_GRANTED = 'GRANTED';
AudioRecorderAPI.prototype.record = function (successCallback, errorCallback, duration) {
  if(device && device.platform === 'Android' && cordova && cordova.plugins && cordova.plugins.permissions){
    permissions = cordova.plugins.permissions;
    permissions.hasPermission(permissions.RECORD_AUDIO, function(status){
      if(!status.hasPermission){
        permissions.requestPermission(permissions.RECORD_AUDIO, function(){
          errorCallback('GRANTED');
        }, function(){
          errorCallback('NO_PERMISSIONS_GRANTED');
        });
      }
      else {
        cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "record", duration ? [duration] : []);
      }
    }, null);
  }
  else{
    cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "record", duration ? [duration] : []);
  }
  
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
