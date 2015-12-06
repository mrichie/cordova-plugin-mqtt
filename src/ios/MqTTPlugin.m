#import "MqTTPlugin.h"

@implementation MqTTPlugin
@synthesize session;

- (void) connect:(CDVInvokedUrlCommand *)command{
    NSString* url = [command.arguments objectAtIndex:0];
    NSString* clientId = [command.arguments objectAtIndex:1];
    bool cleanSession = [[command.arguments objectAtIndex:2] boolValue];
    NSString* userName = [command.arguments objectAtIndex:3];
    NSString* password = [command.arguments objectAtIndex:4];
    session = [[MQTTSession alloc]initWithClientId:clientId];

    [session setDelegate:self];
    session.cleanSessionFlag = cleanSession;
    if(!userName){
	session.userName = userName;
    }
    if(!password){
	session.password = password;
    }
    [session connectAndWaitToHost:url port:1883 usingSSL:NO];
    
}

- (void) publish:(CDVInvokedUrlCommand *)command{
    NSString* topicName = [command.arguments objectAtIndex:0];
    int qos = [[command.arguments objectAtIndex:1] intValue];
    NSString* message = [command.arguments objectAtIndex:2];
    [session publishData:[message dataUsingEncoding:NSUTF8StringEncoding] onTopic:topicName retain:NO qos:qos publishHandler:^(NSError *error){
        CDVPluginResult *result;
        if(error != NULL){
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }else{
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }
    }];
}

- (void) subscribe:(CDVInvokedUrlCommand *)command{
    self.subCallbackId = command.callbackId;
    NSString* topicName = [command.arguments objectAtIndex:0];
    int qos = [[command.arguments objectAtIndex:1] intValue];
    [session subscribeToTopic:topicName atLevel:qos subscribeHandler:^(NSError *error, NSArray *gQoss) {
        CDVPluginResult *result;
        if(error != NULL){
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }
    }];
}

- (void) unsubscribe:(CDVInvokedUrlCommand *)command{
    NSString* topicName = [command.arguments objectAtIndex:0];
    [session unsubscribeTopic:topicName];
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) disconnect:(CDVInvokedUrlCommand *)command{
    [session close];
}

// MqTTSession delegate //
- (void)newMessage:(MQTTSession *)session
              data:(NSData *)data
           onTopic:(NSString *)topic
               qos:(MQTTQosLevel)qos
          retained:(BOOL)retained
               mid:(unsigned int)mid {
    NSDateFormatter *dateFormatter=[[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSString * timestamp = [dateFormatter stringFromDate:[NSDate date]];
    NSDictionary *info = @{
                            @"time": timestamp,
                            @"topic": topic,
                            @"content": [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding],
                            @"qos": [NSNumber numberWithInt:qos]
                        };

    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:info options:0 error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    [self.commandDelegate evalJs:[NSString stringWithFormat:@"cordova.plugins.mqtt.onMessage(%@);", jsonString]];
}
@end
