#import "AudioRecorderAPI.h"
#import <Cordova/CDV.h>

@implementation AudioRecorderAPI

#define RECORDINGS_FOLDER [NSHomeDirectory() stringByAppendingPathComponent:@"Library/NoCloud"]

- (void)checkRecordPermissions:(CDVInvokedUrlCommand*)command {
    _command = command; 
    [self.commandDelegate runInBackground:^{

        AVAudioSessionRecordPermission permissionStatus = [[AVAudioSession sharedInstance] recordPermission];
        switch (permissionStatus) {
            case AVAudioSessionRecordPermissionUndetermined:{
                [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
                    if (granted) {
                        // Microphone enabled code
                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"FT_GRANTED"];
                    }
                    else {
                        // Microphone disabled code
                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"FT_NOT_GRANTED"];
                    }
                    //We have to resolve here, because the plugin doesn't resolve with the pluginResult at the bottom of the method
                    //when the app shows the alertview to grand mic permissions for the first time. 
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
                }];
                break;
            }
            case AVAudioSessionRecordPermissionDenied:
                // direct to settings...
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"NOT_GRANTED"];
                break;
            case AVAudioSessionRecordPermissionGranted:
                // mic access ok...
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"GRANTED"];
                break;
            default:
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"NOT_GRANTED"];
                // this should not happen.. maybe throw an exception.
                break;
        }
        //Verify if the pluginResult exist for the first time.
        if (pluginResult) {
          [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
        }
    }];
}

- (void)record:(CDVInvokedUrlCommand*)command {
    _command = command;
    duration = [_command.arguments objectAtIndex:0];
    sampleRate = [[_command.arguments objectAtIndex:1] floatValue];
    linearPCMBits = [[_command.arguments objectAtIndex:2] intValue];
    numberOfChannels = [[_command.arguments objectAtIndex:3] intValue];

    [self.commandDelegate runInBackground:^{
        
        AVAudioSession *audioSession = [AVAudioSession sharedInstance];
        
        NSError *err;
        [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:&err];
        if (err)
        {
            NSLog(@"%@ %ld %@", [err domain], [err code], [[err userInfo] description]);
        }
        err = nil;
        [audioSession setActive:YES error:&err];
        UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker; AudioSessionSetProperty(kAudioSessionProperty_OverrideAudioRoute, sizeof (audioRouteOverride),&audioRouteOverride);
        if (err)
        {
            NSLog(@"%@ %ld %@", [err domain], [err code], [[err userInfo] description]);
        }

        NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                  [NSNumber numberWithFloat: sampleRate],AVSampleRateKey,
                                  [NSNumber numberWithInt: kAudioFormatLinearPCM],AVFormatIDKey,
                                  [NSNumber numberWithInt:linearPCMBits],AVLinearPCMBitDepthKey,
                                  [NSNumber numberWithInt: numberOfChannels], AVNumberOfChannelsKey,
                                  [NSNumber numberWithBool:NO],AVLinearPCMIsBigEndianKey,
                                  [NSNumber numberWithBool:NO],AVLinearPCMIsFloatKey,
                                  [NSNumber numberWithInt: AVAudioQualityMin],AVEncoderAudioQualityKey,nil];
        
        // Create a new dated file
        recorderFilePath = [NSString stringWithFormat:@"%@/%@.wav", RECORDINGS_FOLDER, @"tempAudio"];
        NSLog(@"recording file path: %@", recorderFilePath);
        
        NSURL *url = [NSURL fileURLWithPath:recorderFilePath];
        err = nil;
        recorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&err];
        if(!recorder){
            NSLog(@"recorder: %@ %ld %@", [err domain], [err code], [[err userInfo] description]);
            return;
        }
        
        [recorder setDelegate:self];
        
        if (![recorder prepareToRecord]) {
            NSLog(@"prepareToRecord failed");
            return;
        }
        
        if (![recorder recordForDuration:(NSTimeInterval)[duration intValue]]) {
            NSLog(@"recordForDuration failed");
            return;
        }

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"recording"];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
    }];
}

- (void)stop:(CDVInvokedUrlCommand*)command {
    _command = command;
    NSLog(@"stopRecording");
    [recorder stop];
    NSLog(@"stopped");
}

- (void)playFromBase64:(CDVInvokedUrlCommand*)command {
    _command = command;
    NSString *base64Audio = [_command.arguments objectAtIndex:0];
    [self.commandDelegate runInBackground:^{
        NSLog(@"playing from base64 string");
        NSLog(base64Audio);
        NSData *audioData = [[NSData alloc] initWithBase64EncodedString:base64Audio options:0];
        
        NSError *err;
        player = [[AVAudioPlayer alloc] initWithData:audioData error:&err];
        //player = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&err];
        player.numberOfLoops = 0;
        player.delegate = self;
        [player prepareToPlay];
        [player play];
        if (err) {
            NSLog(@"%@ %ld %@", [err domain], [err code], [[err userInfo] description]);
        }
        NSLog(@"playing");
    }];
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    NSLog(@"audioPlayerDidFinishPlaying");
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"playbackComplete"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
}

- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)recorder successfully:(BOOL)flag {
    NSURL *url = [NSURL fileURLWithPath: recorderFilePath];
    NSError *err = nil;
    NSData *audioData = [NSData dataWithContentsOfFile:[url path] options: 0 error:&err];
    NSString *base64Audio = [audioData base64Encoding];
    if(!base64Audio) {
        NSLog(@"audio data: %@ %ld %@", [err domain], [err code], [[err userInfo] description]);
    } else {
        NSLog(@"recording saved: %@", recorderFilePath);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:base64Audio];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:_command.callbackId];
    }
}

@end
