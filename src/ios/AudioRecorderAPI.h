#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioServices.h>

@interface AudioRecorderAPI : CDVPlugin {
  NSString *recorderFilePath;
  NSNumber *duration;
  float sampleRate;
  int linearPCMBits;
  int numberOfChannels;
  AVAudioRecorder *recorder;
  AVAudioPlayer *player;
  CDVPluginResult *pluginResult;
  CDVInvokedUrlCommand *_command;
}

- (void)checkRecordPermissions:(CDVInvokedUrlCommand*)command;
- (void)record:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;
- (void)playFromBase64:(CDVInvokedUrlCommand*)command;

@end
