//
//  VLNetworkConfig.h
//  VoiceOnLine
//

#import <Foundation/Foundation.h>

#define Request_Timeout 30.0f

#pragma mark--Network request type
typedef NS_ENUM(NSInteger , VLRequestType){
    VLRequestTypeGet            = 0,
    VLRequestTypePost           = 1,
    VLRequestTypePut            = 2,
    VLRequestTypeDelete         = 3,
};
