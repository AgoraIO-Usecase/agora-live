//
//  VLRoomPersonView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>
#import <AgoraRtcKit/AgoraRtcKit.h>

typedef enum : NSUInteger {
    VLRoomSeatDropTypeSelfly = 0,//Down the wheat by yourself
    VLRoomSeatDropTypeForceByRoomer //Forced to take the microphone by the landlord
    
} VLRoomSeatDropType;
#define viewTag 99999
@class VLRoomSeatModel, VLMicSeatList;
@protocol VLMicSeatListDelegate <NSObject>

- (void)onVLRoomPersonView:(VLMicSeatList*)view seatItemTappedWithModel:(VLRoomSeatModel *)model atIndex:(NSInteger)seatIndex;
- (void)onVLRoomPersonView:(VLMicSeatList*)view onRenderVideo:(VLRoomSeatModel*)model inView:(UIView*)videoView atIndex:(NSInteger)seatIndex;

@end

@interface VLMicSeatList : UIView

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLMicSeatListDelegate>)delegate withRTCkit:(AgoraRtcEngineKit *)RTCkit;

@property (nonatomic, copy) NSArray<VLRoomSeatModel *> *roomSeatsArray;


- (void)reloadSeatIndex:(NSUInteger)seatIndex;

- (void)updateVolumeForSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> *) speakers;

- (void)updateSingBtnWithChoosedSongArray:(NSArray *)choosedSongArray;
- (void)reloadData;
@end

