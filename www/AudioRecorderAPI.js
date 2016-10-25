function AudioRecorderAPI() {
}

AudioRecorderAPI.prototype.record = function (successCallback, errorCallback, duration) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "record", duration ? [duration] : []);
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
