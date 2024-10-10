//
//  VLKTVViewController.h
//  VoiceOnLine
//

#import "BaseViewController.h"

NS_ASSUME_NONNULL_BEGIN
@class SyncRoomInfo;
@class VLRoomSeatModel;

@interface VLKTVViewController : BaseViewController

@property (nonatomic, strong) SyncRoomInfo *roomModel;
@property (nonatomic, strong) NSArray <VLRoomSeatModel *> *seatsArray;

@end

NS_ASSUME_NONNULL_END
