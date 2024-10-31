//
//  VLKTVMVView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>
#import "AgoraEntScenarios-swift.h"
@import ScoreEffectUI;
@import AgoraLyricsScore;

NS_ASSUME_NONNULL_BEGIN
@class VLKTVSelBgModel;

typedef NS_ENUM(NSUInteger, LRCLEVEL) {
    LRCLEVELOW = 0,
    LRCLEVELMID = 1,
    LRCLEVELHIGH = 2,
};

//Join the state of accompaniment
typedef enum : NSUInteger {
    KTVJoinCoSingerStateIdle = 0,         //No button
    KTVJoinCoSingerStateWaitingForJoin,   //Button display: Join the chorus
    KTVJoinCoSingerStateJoinNow,          //Button display: joining
    KTVJoinCoSingerStateWaitingForLeave,   //Button display: exit the chorus
} KTVJoinCoSingerState;

typedef enum : NSUInteger {
    VLKTVMVViewStateIdle = 0,
    VLKTVMVViewStateLoading,
    VLKTVMVViewStateLoadFail,
} VLKTVMVLoadingState;

typedef enum : NSUInteger {
    VLKTVMVViewActionTypeSetParam = 0,  // Set parameters
    VLKTVMVViewActionTypeMVPlay,     // play
    VLKTVMVViewActionTypeMVPause,    // parse
    VLKTVMVViewActionTypeMVNext,     // Play the next song
    VLKTVMVViewActionTypeSingOrigin, // Original singer
    VLKTVMVViewActionTypeSingAcc,    // Accompany
    VLKTVMVViewActionTypeSingLead,   //Director and singer
    VLKTVMVViewActionTypeRetryLrc    // Retry the song
} VLKTVMVViewActionType;

typedef enum : NSUInteger {
    VLKTVMVViewStateNone = 0,  // No one is ordering songs at present.
    VLKTVMVViewStateMusicLoading = 1,  // The current song is loading
    VLKTVMVViewStateAudience = 2, //Audience
    VLKTVMVViewStateOwnerSing = 3, //The owner of the house ordered a song to sing.
    VLKTVMVViewStateOwnerAudience = 4, //The landlord did not join the chorus.
    VLKTVMVViewStateJoinChorus = 5,//Join the chorus
    VLKTVMVViewStateOwnerChorus = 6, //Homeowner's chorus
    VLKTVMVViewStateNotOwnerChorus = 7, //Non-homeowner singing
    VLKTVMVViewStateMusicOwnerLoadFailed = 8, //The song of the singer failed to load (the same as the landlord or the singer)
    VLKTVMVViewStateMusicLoadFailed = 9, //The audience's song failed to load
    VLKTVMVViewStateMusicOwnerLoadLrcFailed = 10, //The song of the singer failed to load (homeowner)
    VLKTVMVViewStateMusicLoadLrcFailed = 11, //Audience lyrics failed to load
} VLKTVMVViewState; //It is mainly used to record the display status in various situations.

@class VLKTVMVView;
@protocol VLKTVMVViewDelegate <NSObject>

- (void)onKTVMVView:(VLKTVMVView*)view btnTappedWithActionType:(VLKTVMVViewActionType)type;

/// Score real-time callback
- (void)onKTVMVView:(VLKTVMVView*)view scoreDidUpdate:(int)score;

- (void)didJoinChours;

- (void)didLeaveChours;

- (BOOL)enableShowJoinChorusButton;

@end

@interface VLKTVMVView : UIView
@property (nonatomic, assign) NSInteger loadingProgress;
@property (nonatomic, strong) KaraokeView *karaokeView;
@property (nonatomic, strong) IncentiveView *incentiveView;
@property (nonatomic, strong) LineScoreView *lineScoreView;
@property (nonatomic, strong) UIButton *joinChorusBtn;
@property (nonatomic, assign) BOOL isOriginLeader;
- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLKTVMVViewDelegate>)delegate;

//Change the background
- (void)changeBgViewByModel:(VLKTVSelBgModel *)selBgModel;
@property (nonatomic, strong) UIImageView *bgImgView;

@property (nonatomic, assign) VLKTVMVViewState mvState;//Record the status of buttons in various situations

//- (void)cleanMusicText;
- (int)getSongScore;
- (void)setSongScore:(int)score;
- (int)getAvgSongScore;

- (void)setOriginBtnState:(VLKTVMVViewActionType)type;
- (void)setJoinChorusFailedLoadingWith:(NSString *)msg;

#pragma mark - Lyrics related

/// Reset the score
- (void)reset;

-(void)setPerViewAvatar:(NSString *)url;

-(void)setSongNameWith:(NSString *)text;

-(void)setPlayState:(BOOL)isPlaying;

-(void)setLrcLevelWith:(LRCLEVEL)level;

@end

NS_ASSUME_NONNULL_END
