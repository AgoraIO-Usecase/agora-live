//
//  AppContext+KTV.m
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/21.
//

#import "AppContext+KTV.h"
#import "AgoraEntScenarios-Swift.h"

NSString* kServiceImpKey = @"ServiceImpKey";
NSString* kAgoraKTVAPIKey = @"kAgoraKTVAPIKey";
@implementation AppContext (KTV)

#pragma mark mcc
+ (void)setupKtvConfig {
    [AppContext shared].sceneImageBundleName = @"KtvResource";
    [AppContext shared].sceneLocalizeBundleName = @"KtvResource";
}

- (void)setKtvAPI:(KTVApiImpl *)ktvAPI {
    [[AppContext shared].extDic setValue:ktvAPI forKey:kAgoraKTVAPIKey];
}

- (KTVApiImpl*)ktvAPI {
    return [[AppContext shared].extDic valueForKey:kAgoraKTVAPIKey];
}

#pragma mark service
+ (id<KTVServiceProtocol>)ktvServiceImp {
    id<KTVServiceProtocol> ktvServiceImp = [[AppContext shared].extDic valueForKey:kServiceImpKey];
    if (ktvServiceImp == nil) {
        ktvServiceImp = [[KTVRTMManagerServiceImpl alloc] initWithAppId:KeyCenter.AppId
                                                                   host:KeyCenter.RTMHostUrl
                                                         appCertificate:KeyCenter.Certificate
                                                                   user:VLUserCenter.user];
      //  ktvServiceImp = [KTVSyncManagerServiceImp new];
        [[AppContext shared].extDic setValue:ktvServiceImp forKey:kServiceImpKey];
    }
    
    return ktvServiceImp;
}

+ (void)unloadKtvServiceImp {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if ([ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        [ktvServiceImp destroy];
    }
    [[AppContext shared].extDic removeAllObjects];
}

+ (NSDictionary<NSString*, VLRoomSeatModel*>* __nullable)ktvSeatMap {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return nil;
    }
    return [ktvServiceImp seatMap];
}

+ (NSArray<VLRoomSelSongModel*>* __nullable)ktvSongList {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return nil;
    }
    return [ktvServiceImp songList];
}

+ (NSArray<KTVChoristerModel*>* __nullable)ktvChoristerList {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return nil;
    }
    return [ktvServiceImp choristerList];
}


+ (BOOL)isKtvRoomOwnerWithSeat:(VLRoomSeatModel*)seat {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return NO;
    }
    
    if ([ktvServiceImp.room.owner.userId length] == 0 && [seat.owner.userId length] == 0) {
        return NO;
    }
    
    return [ktvServiceImp.room.owner.userId isEqualToString:seat.owner.userId];
}


+ (BOOL)isKtvChorusingWithSeat:(VLRoomSeatModel*)seat {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return NO;
    }
    
    NSArray<KTVChoristerModel*>* choristerList = [self ktvChoristerList];
    for (KTVChoristerModel* chorister in choristerList) {
        if ([seat.owner.userId isEqualToString:chorister.userId]) {
            return YES;
        }
    }
    
    return NO;
}

+ (BOOL)isKtvChorusingWithUserId:(NSString*)userId {
    KTVRTMManagerServiceImpl* ktvServiceImp = (KTVRTMManagerServiceImpl*)[self ktvServiceImp];
    if (![ktvServiceImp isKindOfClass:[KTVRTMManagerServiceImpl class]]) {
        return NO;
    }
    
    NSArray<KTVChoristerModel*>* choristerList = [self ktvChoristerList];
    for (KTVChoristerModel* chorister in choristerList) {
        if ([userId isEqualToString:chorister.userId]) {
            return YES;
        }
    }
    
    return NO;
}

+ (BOOL)isKtvSongOwnerWithSeat:(VLRoomSeatModel*)seat {
    VLRoomSelSongModel* song = [[self ktvSongList] firstObject];
    
    return [song.owner.userId isEqualToString:VLUserCenter.user.id] && [song status] == VLSongPlayStatusPlaying;
}
@end
