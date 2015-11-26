#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Cordova/CDV.h>
#import <MQTTClient/MQTTSession.h>

@interface MqTTPlugin : CDVPlugin <MQTTSessionDelegate>
{

}
@property (nonatomic, retain) MQTTSession *session;
@property NSString* subCallbackId;

- (void)connect:(CDVInvokedUrlCommand*)command;
- (void)publish:(CDVInvokedUrlCommand*)command;
- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)disconnect:(CDVInvokedUrlCommand*)command;
@end
