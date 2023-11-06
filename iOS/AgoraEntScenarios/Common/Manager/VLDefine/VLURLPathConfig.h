//
//  VLURLPathConfig.h
//  VoiceOnLine
//

#ifndef VLURLPathConfig_h
#define VLURLPathConfig_h

//static NSString * const kExitRoomNotification = @"exitRoomNotification";
static NSString * const kChoosedSongListChangedNotification = @"choosedSongListChangedNotification";

#pragma mark - API
static NSString * const kURLPathUploadImage = @"/api-login/upload"; //upload image
static NSString * const kURLPathDestroyUser = @"/api-login/users/cancellation"; //Logout user
static NSString * const kURLPathGetUserInfo = @"/api-login/users/getUserInfo"; //Get user information
static NSString * const kURLPathUploadUserInfo = @"/api-login/users/update";  //Modifying User Information

#pragma mark - H5
static NSString * const kURLPathH5TermsOfService = @"https://www.agora.io/en/terms-of-service/";
static NSString * const kURLPathH5AboutUS = @"https://www.agora.io/cn/about-us/";
static NSString * const kURLPathH5PersonInfo = @"http://fullapp.oss-cn-beijing.aliyuncs.com/ent-scenarios/pages/manifest/index.html";
static NSString * const kURLPathH5ThirdInfoShared = @"https://fullapp.oss-cn-beijing.aliyuncs.com/scenarios/libraries.html";

#endif /* VLURLPathConfig_h */
