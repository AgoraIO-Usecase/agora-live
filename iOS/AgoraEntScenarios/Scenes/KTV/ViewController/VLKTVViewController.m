//
//  VLKTVViewController.m
//  VoiceOnLine
//

#import "VLKTVViewController.h"
#import "VLKTVTopView.h"
#import "VLKTVMVView.h"
#import "VLMicSeatList.h"
#import "VLKTVBottomToolbar.h"
#import "VLAudienceIndicator.h"
#import "VLKTVMVIdleView.h"
#import "VLOnLineListVC.h"
#import "KTVDebugInfo.h"
#import "VLKTVSettingView.h"
//model
#import "VLSongItmModel.h"
#import "VLKTVSelBgModel.h"
#import "UIViewController+VL.h"
#import "VLPopScoreView.h"
#import "VLUserCenter.h"
#import "VLMacroDefine.h"
#import "VLGlobalHelper.h"
#import "VLURLPathConfig.h"
#import "VLToast.h"
#import "VLKTVMVView.h"
#import "UIView+VL.h"
#import "AppContext+KTV.h"
#import "AESMacro.h"
#import "LSTPopView+KTVModal.h"
//#import "HWWeakTimer.h"
#import "VLAlert.h"
#import "VLKTVAlert.h"
#import "KTVDebugManager.h"
#import "VLVoicePerShowView.h"
#import "HeadSetManager.h"
#import "AgoraEntScenarios-swift.h"
@import AgoraRtcKit;
@import AgoraLyricsScore;
@import YYCategories;
@import SDWebImage;

NSInteger ktvApiStreamId = -1;
NSInteger ktvStreamId = -1;

NSString* const kSettingViewId = @"settingView";

@interface VLKTVViewController ()<
VLKTVTopViewDelegate,
VLKTVMVViewDelegate,
VLMicSeatListDelegate,
VLKTVBottomToolbarDelegate,
VLPopSelBgViewDelegate,
VLPopMoreSelViewDelegate,
VLDropOnLineViewDelegate,
VLAudienceIndicatorDelegate,
VLAudioEffectPickerDelegate,
VLPopSongListDelegate,
VLEffectViewDelegate,
VLKTVSettingViewDelegate,
VLBadNetWorkViewDelegate,
AgoraRtcMediaPlayerDelegate,
AgoraRtcEngineDelegate,
VLPopScoreViewDelegate,
KTVLrcControlDelegate,
KTVApiEventHandlerDelegate,
IMusicLoadStateListener,
VLVoicePerShowViewDelegate,
VLEarSettingViewViewDelegate,
VLDebugViewDelegate,
KTVServiceListenerProtocol,
VirtualSoundcardPresenterDelegate
>


typedef void (^CompletionBlock)(BOOL isSuccess, NSInteger songCode);
@property (nonatomic, assign) BOOL isEnterSeatNotFirst;
@property (nonatomic, strong) VLKTVMVView *MVView;
@property (nonatomic, strong) VLKTVSelBgModel *choosedBgModel;
@property (nonatomic, strong) VLKTVBottomToolbar *bottomView;
@property (nonatomic, strong) VLBelcantoModel *selBelcantoModel;
@property (nonatomic, strong) VLKTVMVIdleView *noBodyOnLineView; // mv Empty page
@property (nonatomic, strong) VLKTVTopView *topView;
@property (nonatomic, weak) VLKTVSettingView *settingView;
@property (nonatomic, strong) VLMicSeatList *roomPersonView; //Room microphone seat view
@property (nonatomic, strong) VLAudienceIndicator *requestOnLineView;//Empty space on the microphone seat
@property (nonatomic, strong) VLPopSongList *chooseSongView; //Song order view
@property (nonatomic, strong) VLVoicePerShowView *voicePerShowView; //Professional anchor
@property (nonatomic, strong) VLEffectView *effectView; // Sound effect view

@property (nonatomic, strong) VLSongItmModel *choosedSongModel; //The song ordered
@property (nonatomic, strong) AgoraRtcEngineKit *RTCkit;

@property (nonatomic, assign) BOOL isNowMicMuted;
@property (nonatomic, assign) BOOL isNowCameraMuted;
@property (nonatomic, assign) BOOL isOnMicSeat;
@property (nonatomic, assign) NSUInteger chorusNum;    //The number of people in the chorus
@property (nonatomic, assign) KTVSingRole singRole;    //Role
@property (nonatomic, assign) BOOL isEarOn;
@property (nonatomic, assign) int playoutVolume;
@property (nonatomic, assign) int soundVolume;
@property (nonatomic, assign) KTVPlayerTrackMode trackMode;  //Chorus/Accompaniment

@property (nonatomic, strong) NSArray <VLRoomSelSongModel*>* selSongsArray;
@property (nonatomic, strong) KTVApiImpl* ktvApi;
@property (nonatomic, assign) NSInteger selectedVoiceShowIndex;
@property (nonatomic, assign) BOOL isProfessional;
@property (nonatomic, assign) BOOL isDelay;
@property (nonatomic, strong) LyricModel *lyricModel;
@property (nonatomic, strong) KTVLrcControl *lrcControl;
@property (nonatomic, copy, nullable) CompletionBlock loadMusicCallBack;
@property (nonatomic, assign) NSInteger selectedEffectIndex;
@property (nonatomic, assign) BOOL isPause;
@property (nonatomic, assign) NSInteger retryCount;
@property (nonatomic, assign) BOOL isJoinChorus;
@property (nonatomic, assign) NSInteger coSingerDegree;
@property (nonatomic, assign) NSInteger currentSelectEffect;
@property (nonatomic, assign) NSInteger aecGrade;
@property (nonatomic, assign) NSInteger volGrade;
@property (nonatomic, assign) CGFloat earValue;
//@property (nonatomic, assign) checkAuthType checkType;
@property (nonatomic, assign) BOOL isDumpMode;
@property (nonatomic, assign) BOOL voiceShowHasSeted;
@property (nonatomic, assign) BOOL aecState; //AIAEC Switch
@property (nonatomic, assign) NSInteger aecLevel; //AEC Rank
@property (nonatomic, assign) NSString *selectUserNo;
@property (nonatomic, strong) SoundCardSettingView *soundSettingView;
@property (nonatomic, strong) LSTPopView *popSoundSettingView;
@property (nonatomic, strong) HeadSetManager *headeSet;
@property (nonatomic, assign) NSInteger roomPeopleCount;
@property (nonatomic, strong) VLKTVSettingModel *settingModel;
@property (nonatomic, assign) BOOL lazyLoadAndPlaySong;

@property (nonatomic, strong) VirtualSoundcardPresenter *soundcardPresenter;

@end

@implementation VLKTVViewController

- (VLAudienceIndicator *)requestOnLineView {
    if (!_requestOnLineView) {
        _requestOnLineView = [[VLAudienceIndicator alloc] initWithFrame:CGRectMake(0, SCREEN_HEIGHT-kSafeAreaBottomHeight-56-VLREALVALUE_WIDTH(30), SCREEN_WIDTH, 56)
                                                           withDelegate:self];
    }
    
    return _requestOnLineView;
}

#pragma mark view lifecycles
- (void)dealloc {
    KTVLogInfo(@"dealloc:%s",__FUNCTION__);
    [_soundcardPresenter removeDelegate:self];
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        [self subscribeServiceEvent];
    }
    
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = UIColor.blackColor;
    self.selectedVoiceShowIndex = -1;
    self.selectUserNo = @"";
    self.isDelay = true;
    _soundcardPresenter = [[VirtualSoundcardPresenter alloc] init];
    [_soundcardPresenter addDelegate:self];
  //  self.checkType = checkAuthTypeAll;
    [self subscribeServiceEvent];
    
    // setup view
    [self setBackgroundImage:@"bg-main" bundleName:@"KtvResource"];
    
//    UIView *bgView = [[UIView alloc]initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)];
//    bgView.backgroundColor = UIColorMakeWithRGBA(0, 0, 0, 0.6);
//    [self.view addSubview:bgView];
    //Head view
    VLKTVTopView *topView = [[VLKTVTopView alloc]initWithFrame:CGRectMake(0, kStatusBarHeight, SCREEN_WIDTH, 60) withDelegate:self];
    [self.view addSubview:topView];
    self.topView = topView;
    topView.listModel = self.roomModel;
    
    //Bottom button view
    VLKTVBottomToolbar *bottomView = [[VLKTVBottomToolbar alloc] initWithFrame:CGRectMake(0, SCREEN_HEIGHT-64-kSafeAreaBottomHeight, SCREEN_WIDTH, 64) withDelegate:self withRoomNo:self.roomModel.roomNo withData:self.seatsArray];
    bottomView.backgroundColor = [UIColor clearColor];
    self.bottomView = bottomView;
    [self.view addSubview:bottomView];
    [bottomView setHidden:!self.requestOnLineView.isHidden];
    
    //Remove the height of the beginning and end
    CGFloat musicHeight = SCREEN_HEIGHT -64 - kSafeAreaBottomHeight - kStatusBarHeight - 60 - 20;
    
    //MV view (show lyrics...)
    CGFloat mvViewTop = topView.bottom;
    self.MVView = [[VLKTVMVView alloc]initWithFrame:CGRectMake(0, mvViewTop, SCREEN_WIDTH, musicHeight * 0.5) withDelegate:self];
    [self.view addSubview:self.MVView];
    
    //Room microphone seat view
    VLMicSeatList *personView = [[VLMicSeatList alloc] initWithFrame:CGRectMake(0, self.MVView.bottom + 20, SCREEN_WIDTH, musicHeight * 0.5) withDelegate:self withRTCkit:self.RTCkit];
    self.roomPersonView = personView;
    self.roomPersonView.roomSeatsArray = self.seatsArray;
    [self.view addSubview:personView];

    //Empty space on microphone seat view
    [self.view addSubview:self.requestOnLineView];
    
    //start join
    [self joinRTCChannel];
    
//    self.isOnMicSeat = [self getCurrentUserSeatInfo] == nil ? NO : YES;
    
    //Process the background
    [self prepareBgImage];
    [[UIApplication sharedApplication] setIdleTimerDisabled: YES];
    
    //add debug
    //[self.topView addGestureRecognizer:[KTVDebugManager createStartGesture]];
    
    self.earValue = 100;
    
    if(AppContext.shared.isDebugMode){
        //If the debug mode is turned on
        UIButton *debugBtn = [[UIButton alloc]initWithFrame:CGRectMake(SCREEN_WIDTH - 100, SCREEN_HEIGHT - 200, 80, 80)];
        [debugBtn setBackgroundColor:[UIColor blueColor]];
        debugBtn.layer.cornerRadius = 40;
        debugBtn.layer.masksToBounds = true;
        [debugBtn setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
        [debugBtn setTitle:@"Debug" forState:UIControlStateNormal];
        [debugBtn addTarget:self action:@selector(showDebug) forControlEvents:UIControlEventTouchUpInside];
        [self.view addSubview:debugBtn];
    }
    kWeakSelf(self);
    self.headeSet = [HeadSetManager initHeadsetObserverWithCallback:^(BOOL inserted) {
        if(!inserted){
            //Unplug the headphones and turn off the ear return.
            if(weakself.isEarOn){
                [weakself onVLKTVEarSettingViewSwitchChanged:false];
                [VLToast toast:KTVLocalizedString(@"ktv_earback_micphone_pull")];
            }
        }
    }];
    
    if(self.lazyLoadAndPlaySong) {
        [self loadAndPlaySong];
    }
    
    [self reportMessage];
}

- (void)reportMessage {
    [ReportManager messageReportWithRtcEngine:self.RTCkit type:3];
}

-(void)showDebug {
    [LSTPopView popDebugViewWithParentView:self.view channelName:self.roomModel.roomNo sdkVer:[AgoraRtcEngineKit getSdkVersion]  isDebugMode:self.isDumpMode withDelegate:self];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [UIViewController popGestureClose:self];
    
    
    //Request the song that has been clicked
    [self refreshChoosedSongList:nil];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [UIViewController popGestureOpen:self];
    [self leaveRTCChannel];
    [[UIApplication sharedApplication] setIdleTimerDisabled: NO];
}

- (void)viewDidDisappear:(BOOL)animated
{
    KTVLogInfo(@"Agora - destroy RTCEngine");
    [AgoraRtcEngineKit destroy];
}

- (void)configNavigationBar:(UINavigationBar *)navigationBar {
    [super configNavigationBar:navigationBar];
}
- (BOOL)preferredNavigationBarHidden {
    return YES;
}
- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleLightContent;
}
// Is it allowed to manually slide back @return true yes, false no
- (BOOL)forceEnableInteractivePopGestuzreRecognizer {
    return NO;
}

#pragma mark service handler
- (void)subscribeServiceEvent {
    [[AppContext ktvServiceImp] subscribeWithListener:self];
}

-(void)setMVViewStateWith:(VLRoomSelSongModel *)song {
    if(!song){
        self.MVView.mvState = VLKTVMVViewStateNone;
    } else {
        KTVLogInfo(@"setMVViewState: singRole: %ld, isRoomOwner: %d", self.singRole, [self isRoomOwner]);
        switch (self.singRole) {
            case KTVSingRoleSoloSinger:
                self.MVView.mvState = VLKTVMVViewStateOwnerSing;
                [self.MVView setPlayState:_isPause];
                break;
            case KTVSingRoleLeadSinger:
                self.MVView.mvState = VLKTVMVViewStateOwnerSing;
                [self.MVView setPlayState:_isPause];
                break;
            case KTVSingRoleCoSinger:
                self.MVView.mvState = [self isRoomOwner] ? VLKTVMVViewStateOwnerChorus : VLKTVMVViewStateNotOwnerChorus;
                break;
            case KTVSingRoleAudience:
                self.MVView.mvState = [self isRoomOwner] ? VLKTVMVViewStateOwnerAudience : VLKTVMVViewStateAudience;
                break;
            default:
                break;
        }
        [self.MVView setSongNameWith:[NSString stringWithFormat:@"%@-%@", song.songName, song.singer]];
    }
}

#pragma mark view helpers
- (void)prepareBgImage {
    if (self.roomModel.bgOption) {
        VLKTVSelBgModel *selBgModel = [VLKTVSelBgModel new];
        selBgModel.imageName = [NSString stringWithFormat:@"ktv_mvbg%ld", self.roomModel.bgOption];
        selBgModel.isSelect = YES;
        self.choosedBgModel = selBgModel;
    }
}

//Change the background of MV
- (void)popSelMVBgView {
    [LSTPopView popSelMVBgViewWithParentView:self.view
                                     bgModel:self.choosedBgModel
                                withDelegate:self];
}

//Pop up more
- (void)popSelMoreView {
    [LSTPopView popSelMoreViewWithParentView:self.view
                                withDelegate:self];
}

//Pop-up microphone seat view
- (void)popDropLineViewWithSeatModel:(VLRoomSeatModel *)seatModel {
    [LSTPopView popDropLineViewWithParentView:self.view
                                withSeatModel:seatModel
                                 withDelegate:self];
}

//Pop up the beautiful sound view
- (void)popBelcantoView {
    [LSTPopView popBelcantoViewWithParentView:self.view
                            withBelcantoModel:self.selBelcantoModel
                                 withDelegate:self];
}

//Pop-up song order view
- (void)popUpChooseSongView:(BOOL)ifChorus {
    LSTPopView* popChooseSongView =
    [LSTPopView popUpChooseSongViewWithParentView:self.view
                                          isOwner:[self.roomModel.creatorNo isEqualToString:VLUserCenter.user.userNo]
                                         isChorus:ifChorus
                                  chooseSongArray:self.selSongsArray
                                       withRoomNo:self.roomModel.roomNo
                                     withDelegate:self];
    
    self.chooseSongView = (VLPopSongList*)popChooseSongView.currCustomView;
}

//Professional anchor
- (void)popVoicePerView {
    LSTPopView* popView =
    [LSTPopView popVoicePerViewWithParentView:self.view isProfessional:self.isProfessional aecState: self.aecState aecLevel:self.aecLevel isDelay:self.isDelay volGrade: self.volGrade grade: self.aecGrade isRoomOwner:[self isRoomOwner] perView:self.voicePerShowView withDelegate:self];
    self.voicePerShowView = (VLVoicePerShowView*)popView.currCustomView;
}


//Pop-up sound effect
- (void)popSetSoundEffectView {
    LSTPopView* popView =
    [LSTPopView popSetSoundEffectViewWithParentView:self.view
                                          soundView:self.effectView
                                       withDelegate:self];
    self.effectView = (VLEffectView*)popView.currCustomView;
    [self.effectView setSelectedIndex:self.selectedEffectIndex];
}

//Network difference view
- (void)popBadNetWrokTipView {
    [LSTPopView popBadNetWrokTipViewWithParentView:self.view
                                      withDelegate:self];
}

//The user pops up to leave the room
- (void)popForceLeaveRoom {
    VL(weakSelf);
    [[VLKTVAlert shared]showKTVToastWithFrame: UIScreen.mainScreen.bounds
                                        image: [UIImage ktv_sceneImageWithName:@"empty" ]
                                      message: KTVLocalizedString(@"ktv_owner_leave")
                                  buttonTitle:KTVLocalizedString(KTVLocalizedString(@"ktv_gotit"))
                                   completion:^(bool flag, NSString * _Nullable text) {
        [weakSelf leaveRoom];
        [[VLKTVAlert shared] dismiss];
    }];
}

- (void)showSettingView {
    LSTPopView* popView = [LSTPopView popSettingViewWithParentView:self.view
                                                           setting: self.settingModel
                                                       settingView:self.settingView
                                                      withDelegate:self];
    popView.identifier = kSettingViewId;
    
    self.settingView = (VLKTVSettingView*)popView.currCustomView;
    BOOL flag = self.selSongsArray.count > 0;
    if (flag == false) {
        [self.settingView setIspause:true];
    } else {
        [self.settingView setIspause:self.isPause];
    }
    [self.settingView setSelectEffect:self.selectedEffectIndex];
    [self.settingView setAEClevel:self.aecLevel];
    [self.settingView setChorusStatus: flag];
}

- (void)showScoreViewWithScore:(NSInteger)score {
                        //  song:(VLRoomSelSongModel *)song {
}

- (void)popScoreViewDidClickConfirm
{
    KTVLogInfo(@"Using as score view hidding");
}

#define VLEarSettingView
-(void)onVLKTVEarSettingViewValueChanged:(double)Value{
    if(self.earValue == Value){
        return;
    }
    self.earValue = Value;
    KTVLogInfo(@"ear vol:%f", Value);
    [self.RTCkit setInEarMonitoringVolume:Value];
}

- (void)onVLKTVEarSettingViewSwitchChanged:(BOOL)flag{
    self.isEarOn = flag;
    [self.RTCkit enableInEarMonitoring:flag];
}

#pragma mark - VLDebugViewDelegate
- (void)didExportLogWith:(NSString *)path {
    UIActivityViewController *activityController = [[UIActivityViewController alloc] initWithActivityItems:@[[NSURL fileURLWithPath:path isDirectory:YES]] applicationActivities:nil];
    activityController.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:activityController animated:YES completion:nil];
}

- (void)didDumpModeChanged:(BOOL)enable {
    self.isDumpMode = enable;
    NSString* key = @"dump enable";
    [KTVDebugInfo setSelectedStatus:enable forKey:key];
    [KTVDebugManager reLoadParamAll];
}

-(void)didParamsSetWith:(NSString *)key value:(NSString *)value{
    if([value.lowercaseString isEqualToString:@"true"] || [value.lowercaseString isEqualToString:@"false"] || [value.lowercaseString isEqualToString:@"yes"] || [value.lowercaseString isEqualToString:@"no"]){
        BOOL flag = [value.lowercaseString isEqualToString:@"true"] || [value.lowercaseString isEqualToString:@"yes"];
        NSString *params = @"";
        if(flag){
            params = [NSString stringWithFormat:@"{\"%@\":true", key];
        } else {
            params = [NSString stringWithFormat:@"{\"%@\":false", key];
        }
        [self.RTCkit setParameters:params];
    }else if([self isPureNumberString:value]){
        NSInteger num = [value integerValue];
        NSString *params = [NSString stringWithFormat:@"{\"%@\":%li", key, (long)num];
        [self.RTCkit setParameters:params];
    } else {
        NSString *params = [NSString stringWithFormat:@"{\"%@\":\"%@\"", key, value];
        [self.RTCkit setParameters:params];
    }
}

- (BOOL)isPureNumberString:(NSString *)string {
    NSCharacterSet *numberSet = [NSCharacterSet characterSetWithCharactersInString:@"0123456789"];
    NSRange range = [string rangeOfCharacterFromSet:numberSet.invertedSet];
    return (range.location == NSNotFound);
}
#pragma mark - rtc callbacks
- (void)rtcEngine:(AgoraRtcEngineKit *)engine didJoinedOfUid:(NSUInteger)uid elapsed:(NSInteger)elapsed
{
    [KTVLog infoWithText:[NSString stringWithFormat:@"didJoinedOfUid: %ld", uid]];
//    [self.ktvApi mainRtcEngine:engine didJoinedOfUid:uid elapsed:elapsed];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didAudioRouteChanged:(AgoraAudioOutputRouting)routing {
    
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didAudioPublishStateChange:(NSString *)channelId oldState:(AgoraStreamPublishState)oldState newState:(AgoraStreamPublishState)newState elapseSinceLastState:(int)elapseSinceLastState {
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine
reportAudioVolumeIndicationOfSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> *)speakers
      totalVolume:(NSInteger)totalVolume {
    [self.roomPersonView updateVolumeForSpeakers:speakers];
}

- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine
receiveStreamMessageFromUid:(NSUInteger)uid
         streamId:(NSInteger)streamId
             data:(NSData * _Nonnull)data {    //Receive the RTC message of the other party
    
    NSDictionary *dict = [VLGlobalHelper dictionaryForJsonData:data];
//    KTVLogInfo(@"receiveStreamMessageFromUid:%@,streamID:%d,uid:%d",dict,(int)streamId,(int)uid);
    if ([dict[@"cmd"] isEqualToString:@"SingingScore"]) {
        //The accompant shows his own score, and the audience shows the lead singer's score.
        int score = [dict[@"score"] intValue];
        dispatch_async(dispatch_get_main_queue(), ^{
            if(self.singRole == KTVSingRoleCoSinger){
                [self showScoreViewWithScore:[self.lrcControl getAvgScore]];
                return;
            }

            [self showScoreViewWithScore:score];
        });
        
        KTVLogInfo(@"score: %ds",score);
        return;
    } else if([dict[@"cmd"] isEqualToString:@"singleLineScore"]) {//The audience receives the score of the lead singer.
        KTVLogInfo(@"index: %li", [dict[@"index"] integerValue]);
        if(self.singRole != KTVSingRoleAudience){
            return;
        }
        //The audience uses the lead singer's score to show
        NSInteger index = [dict[@"index"] integerValue];
        NSInteger score = [dict[@"score"] integerValue];
        NSInteger cumulativeScore = [dict[@"cumulativeScore"] integerValue];
        NSInteger total = [dict[@"total"] integerValue];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.MVView.lineScoreView showScoreViewWithScore:score];
            [self.MVView.incentiveView showWithScore:score];
        });
        KTVLogInfo(@"index: %li, score: %li, cumulativeScore: %li, total: %li", index, score, cumulativeScore, total);
    } else if([dict[@"cmd"] isEqualToString:@"sendVoiceHighlight"]) {
        if(self.singRole == KTVSingRoleAudience){
            return;
        };

        NSInteger audioEffectPreset = [dict[@"preset"] integerValue];
        switch (audioEffectPreset) {
            case AgoraAudioEffectPresetOff:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetOff];
                KTVLogInfo(@"effect:Off");
                break;
            case AgoraAudioEffectPresetRoomAcousticsKTV:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetRoomAcousticsKTV];
                KTVLogInfo(@"effect:KTV");
                break;
            case AgoraAudioEffectPresetRoomAcousVocalConcer:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetRoomAcousVocalConcer];
                KTVLogInfo(@"effect:Concer");
                break;
            case AgoraAudioEffectPresetRoomAcousStudio:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetRoomAcousStudio];
                KTVLogInfo(@"effect:Studio");
                break;
            case AgoraAudioEffectPresetRoomAcousPhonograph:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetRoomAcousPhonograph];
                KTVLogInfo(@"effect:graph");
                break;
            default:
                [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetOff];
                KTVLogInfo(@"effect:Off");
                break;
        }
    } else if([dict[@"cmd"] isEqualToString:@"cancelVoiceHighlight"]) {
        //Life highlights practical effects.
        [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetOff];
    }
}

// Network quality callbacks
- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine
   networkQuality:(NSUInteger)uid
        txQuality:(AgoraNetworkQuality)txQuality
        rxQuality:(AgoraNetworkQuality)rxQuality
{
    //    VLLog(@"Agora - network quality : %lu", txQuality);
    if(uid == [VLUserCenter.user.id intValue]) {
        if(txQuality == AgoraNetworkQualityExcellent || txQuality == AgoraNetworkQualityGood) {
            // Good quality
            [self.topView setNetworkQuality:0];
        } else if(txQuality == AgoraNetworkQualityPoor || txQuality == AgoraNetworkQualityBad) {
            // Bad quality
            [self.topView setNetworkQuality:1];
        } else if(txQuality == AgoraNetworkQualityVBad || txQuality == AgoraNetworkQualityDown) {
            // Barely usable
            [self.topView setNetworkQuality:2];
        } else {
            // Unknown or detecting
            [self.topView setNetworkQuality:3];
        }
    }
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine tokenPrivilegeWillExpire:(NSString *)token {
    KTVLogInfo(@"tokenPrivilegeWillExpire: %@", token);
    [[NetworkManager shared] generateTokenWithChannelName:self.roomModel.roomNo
                                                      uid:VLUserCenter.user.id
                                                    types: @[@(AgoraTokenTypeRtc), @(AgoraTokenTypeRtm)]
                                                   expire:1500
                                                  success:^(NSString * token) {
        KTVLogInfo(@"tokenPrivilegeWillExpire rtc renewToken: %@", token);
        [self.RTCkit renewToken:token];
        //TODO: mcc missing
//        [self.AgoraMcc renewToken:token];
    }];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine contentInspectResult:(AgoraContentInspectResult)result {
    KTVLogInfo(@"contentInspectResult: %ld", result);
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine localAudioStats:(AgoraRtcLocalAudioStats *)stats {
}

#pragma mark - action utils / business
- (void)stopPlaySong {
    self.isPause = false;
   // [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetOff];
   // self.MVView.joinCoSi ngerState = KTVJoinCoSingerStateWaitingForJoin;
    [self.MVView setOriginBtnState: VLKTVMVViewActionTypeSingAcc];
    [self.MVView setMvState:[self isRoomOwner] ? VLKTVMVViewStateOwnerAudience : VLKTVMVViewStateAudience];
    [self.ktvApi switchSingerRoleWithNewRole:KTVSingRoleAudience
                           onSwitchRoleState:^(KTVSwitchRoleState state, KTVSwitchRoleFailReason reason) {
    }];
    self.aecLevel = 1;
    self.aecState = true;
    // Turn off the ear return state after the song is broadcast.
//    if(self.isEarOn){
//        self.isEarOn = false;
//        [self.RTCkit enableInEarMonitoring:_isEarOn includeAudioFilters:AgoraEarMonitoringFilterNone];
//    }
}

- (void)loadAndPlaySong{
    if (self.ktvApi == NULL) {
        self.lazyLoadAndPlaySong = YES;
        KTVLogInfo(@"loadAndPlaySong before viewDidLoad");
        return;
    }
    
    self.voiceShowHasSeted = false;
    self.selectedVoiceShowIndex = -1;
    self.selectUserNo = @"";
    self.currentSelectEffect = 0;
  //  [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetOff];
    
//    if([self isRoomOwner]){
//        [self.MVView setPerViewAvatar:@""];
//    }
    
    VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
    [self.MVView setMvState: VLKTVMVViewStateMusicLoading];
    if(!model){
        return;
    }
    [self markSongPlaying:model];
    
    //TODO: will remove ktv api adjust playout volume method
    [self setPlayoutVolume:50];

    KTVSingRole role1 = [self getUserSingRole];
    KTVLogInfo(@"loadAndPlaySong[%@][%@]: role: %ld", model.songNo, model.songName, role1);
    KTVSongConfiguration* songConfig = [[KTVSongConfiguration alloc] init];
  //  songConfig.autoPlay = (role == KTVSingRoleAudience || role == KTVSingRoleCoSinger) ? NO : YES ;
    songConfig.mode = (role1 == KTVSingRoleAudience || role1 == KTVSingRoleCoSinger) ? KTVLoadMusicModeLoadLrcOnly : KTVLoadMusicModeLoadMusicAndLrc;
    songConfig.mainSingerUid = [model.owner.userId integerValue];
    songConfig.songIdentifier = model.songNo;

    VL(weakSelf);
    self.loadMusicCallBack = ^(BOOL isSuccess, NSInteger songCode) {
        KTVSingRole role = [weakSelf getUserSingRole];
        if(role != role1) {
            KTVLogInfo(@"load music succuss, but role did change %ld -> %ld", role1, role);
        }
        KTVLogInfo(@"load music[%ld] completion, isSuccess: %d, role: %ld", songCode, isSuccess, role);
        if (!isSuccess) {
            return;
        }
        
        if(role == KTVSingRoleCoSinger){
            weakSelf.singRole = KTVSingRoleCoSinger;
        }
        
        if(role == KTVSingRoleCoSinger){
            [weakSelf.ktvApi startSingWithSongCode:songCode startPos:0];
        }
        
        if(self.singRole == role) {
            if(role == KTVSingRoleSoloSinger || role == KTVSingRoleLeadSinger){
                [weakSelf.ktvApi startSingWithSongCode:songCode startPos:0];
                weakSelf.aecLevel = 0;
                weakSelf.aecState = false;
            }
            [weakSelf setMVViewStateWith:model];
        } else {
            [weakSelf.ktvApi switchSingerRoleWithNewRole:role
                                       onSwitchRoleState:^( KTVSwitchRoleState state, KTVSwitchRoleFailReason reason) {
                if(state != KTVSwitchRoleStateSuccess && role != KTVSingRoleAudience) {
                    //TODO: error toast and retry?
                    KTVLogError(@"switchSingerRole error: %ld", reason);
                    return;
                } else {
                    if(role == KTVSingRoleSoloSinger || role == KTVSingRoleLeadSinger){
                        [weakSelf.ktvApi startSingWithSongCode:songCode startPos:0];
                        weakSelf.aecLevel = 0;
                        weakSelf.aecState = false;
                    }
                    [weakSelf setMVViewStateWith:model];
                }
            }];
        }
    };
    
    [self.lrcControl resetShowOnce];
    [self.ktvApi loadMusicWithSongCode:[model.songNo integerValue] config:songConfig onMusicLoadStateListener:self];
}

- (void)enterSeatWithIndex:(NSNumber*)index completion:(void(^)(NSError*))completion {
    [[AppContext ktvServiceImp] enterSeatWithSeatIndex:index completion:completion];
    [self _checkEnterSeatAudioAuthorized];
}

- (void)_checkEnterSeatAudioAuthorized {
    if (self.isEnterSeatNotFirst) {
        return;
    }
    
    self.isEnterSeatNotFirst = YES;
    [AgoraEntAuthorizedManager checkAudioAuthorizedWithParent:self completion:nil];
}

- (void)leaveSeatWithSeatModel:(VLRoomSeatModel * __nonnull)seatModel
                 withCompletion:(void(^ __nullable)(NSError*))completion {
//    if([seatModel.owner.userId isEqualToString:VLUserCenter.user.id]) {
//        if(seatModel.isVideoMuted == 1) {
//            [self.RTCkit stopPreview];
//        }
//    }
    if ([seatModel.owner.userId isEqualToString:VLUserCenter.user.id]) {
        [[AppContext ktvServiceImp] leaveSeatWithCompletion:completion];
    } else {
        [[AppContext ktvServiceImp] kickSeatWithSeatIndex:seatModel.seatIndex completion:completion];
    }
}

- (void)refreshChoosedSongList:(void (^ _Nullable)(void))block{
    VL(weakSelf);
    [[AppContext ktvServiceImp] getChoosedSongsListWithCompletion:^(NSError * error, NSArray<VLRoomSelSongModel *> * songArray) {
        if (error != nil) {
            return;
        }
        if(songArray.count == 0){
            return;
        }
        weakSelf.selSongsArray = songArray;
        if(block) {
            block();
        }
    }];
}

- (void)markSongPlaying:(VLRoomSelSongModel *)model {
    if (model.status == VLSongPlayStatusPlaying) {
        return;
    }
    [[AppContext ktvServiceImp] markSongDidPlayWithSongCode:model.songNo completion:^(NSError * error) {
    }];
}

- (void)syncChoruScore:(NSInteger)score {
    NSDictionary *dict = @{
        @"cmd":@"SingingScore",
        @"score":@(score)
    };
    [self sendStreamMessageWithDict:dict success:nil];
}

- (void)joinChorus {
    self.isJoinChorus = YES;
    [self.MVView setMvState:VLKTVMVViewStateJoinChorus];
    if(![self enableShowJoinChorusButton]){
        KTVLogInfo(@"joinChorus fail! enableShowJoinChorusButton false");
        [self _rollbackAfterChorusJoinFailure];
        [VLToast toast:KTVLocalizedString(@"ktv_mic_full")];
        return;
    }
    
    if(self.RTCkit.getConnectionState != AgoraConnectionStateConnected){
        KTVLogInfo(@"joinChorus fail! rtc connected(%ld) is not connected", (long)self.RTCkit.getConnectionState);
        [self _rollbackAfterChorusJoinFailure];
        [VLToast toast:KTVLocalizedString(@"ktv_join_chorus_failed")];
        return;
    }
    
    if (![self getJoinChorusEnable]) {
        KTVLogInfo(@"joinChorus fail! getJoinChorusEnable false");
        [self _rollbackAfterChorusJoinFailure];
        [VLToast toast:KTVLocalizedString(@"ktv_join_chorus_failed")];
        return;
    }
    
    //If you don't have a microphone seat, you need to have a microphone seat first.
    if ([self getCurrentUserSeatInfo] == nil) {
        VL(weakSelf);
        [self enterSeatWithIndex:nil completion:^(NSError *error) {
            if(error){
                KTVLogError(@"joinChorus fail! enterSeat error:%@", error.description);
                [weakSelf _rollbackAfterChorusJoinFailure];
                [VLToast toast:KTVLocalizedString(@"ktv_join_chorus_failed")];
                return;
            }
            [weakSelf _joinChorus];
        }];
        
//        //TODO(chenpan): show error
//        [VLToast toast:KTVLocalizedString(@"ktv_mic_full")];
        return;
    }
    
    [self _joinChorus];
}

-(void)joinChorusFailedAndUIUpadte {
}

- (void)_rollbackAfterChorusJoinFailure {
    [self.MVView setMvState: [self isRoomOwner] ? VLKTVMVViewStateOwnerAudience : VLKTVMVViewStateAudience];
    self.isJoinChorus = NO;
    KTVLogInfo(@"join chorus fail");
}

- (void)_joinChorus {
    [self.MVView.incentiveView reset];
    
    VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
    KTVSingRole role = KTVSingRoleCoSinger;
    KTVSongConfiguration* songConfig = [[KTVSongConfiguration alloc] init];
    songConfig.mode = KTVLoadMusicModeLoadMusicOnly;
    songConfig.mainSingerUid = [model.owner.userId integerValue];
    songConfig.songIdentifier = model.songNo;
    
    VL(weakSelf);
    self.loadMusicCallBack = ^(BOOL isSuccess, NSInteger songCode) {
        if (!isSuccess) {
            [weakSelf _rollbackAfterChorusJoinFailure];
            [VLToast toast:KTVLocalizedString(@"ktv_join_chorus_failed")];
            return;
        }
        
        [weakSelf.ktvApi startSingWithSongCode:songCode startPos:0];
        KTVLogInfo(@"before switch role, load music success");
        [weakSelf.ktvApi switchSingerRoleWithNewRole:role
                                   onSwitchRoleState:^( KTVSwitchRoleState state, KTVSwitchRoleFailReason reason) {
            if (state == KTVSwitchRoleStateFail && reason != KTVSwitchRoleFailReasonNoPermission) {
                [weakSelf _rollbackAfterChorusJoinFailure];
                [VLToast toast:KTVLocalizedString(@"ktv_join_chorus_failed")];
                return;
            }

            [weakSelf.MVView setMvState: [weakSelf isRoomOwner] ? VLKTVMVViewStateOwnerChorus : VLKTVMVViewStateNotOwnerChorus];
            weakSelf.isJoinChorus = NO;
            
            weakSelf.isNowMicMuted = role == KTVSingRoleAudience;

            VLRoomSelSongModel *selSongModel = weakSelf.selSongsArray.firstObject;
            [[AppContext ktvServiceImp] joinChorusWithSongCode:selSongModel.songNo completion:^(NSError * error) {
            }];
            
            [[AppContext ktvServiceImp] updateSeatAudioMuteStatusWithMuted:NO completion:^(NSError * error) {
            }];
            weakSelf.aecLevel = 0;
            weakSelf.aecState = false;
        }];
    };
    KTVLogInfo(@"before songCode:%li", [model.songNo integerValue]);
    [self.ktvApi loadMusicWithSongCode:[model.songNo integerValue] config:songConfig onMusicLoadStateListener:self];
}

- (void)removeCurrentSong {
    VLRoomSelSongModel* top = [self.selSongsArray firstObject];
    if(top == NULL) {return;}
    [[AppContext ktvServiceImp] removeSongWithSongCode:top.songNo completion:^(NSError * error) {
        if (error) {
            KTVLogInfo(@"deleteSongEvent fail: %@ %ld", top.songName, error.code);
        }
    }];
}

- (void)leaveRoom {
    VL(weakSelf);
    [[AppContext ktvServiceImp] leaveRoomWithCompletion:^(NSError * error) {
        if (error != nil) {
            return;
        }
        
        for (BaseViewController *vc in weakSelf.navigationController.childViewControllers) {
            if ([vc isKindOfClass:[VLOnLineListVC class]]) {
                [weakSelf.navigationController popToViewController:vc animated:YES];
                [AgoraEntLog autoUploadLogWithScene:KTVLog.kLogKey];
            }
        }
    }];
}

#pragma mark - rtc utils
- (void)setupContentInspectConfig {
    AgoraContentInspectConfig* config = [AgoraContentInspectConfig new];
    NSDictionary* dic = @{
        @"userNo": [VLUserCenter user].id ? : @"unknown",
        @"sceneName": @"ktv"
    };
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:dic options:0 error:nil];
    NSString* jsonStr = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    config.extraInfo = jsonStr;
    AgoraContentInspectModule* module = [AgoraContentInspectModule new];
    module.interval = 30;
    module.type = AgoraContentInspectTypeModeration;
    config.modules = @[module];
    [self.RTCkit enableContentInspect:YES config:config];
    
    //Add audio identification interface
    [[NetworkManager shared] voiceIdentifyWithChannelName:self.roomModel.roomNo
                                              channelType:1
                                                sceneType:SceneTypeKtv
                                                  success:^(NSString * msg) {
        KTVLogInfo(@"voiceIdentify success: %@", msg);
    }];
}

- (void)joinRTCChannel {
    KTVLogInfo(@"joinRTCChannel");
    AgoraRtcEngineConfig* rtcConfig = [AgoraRtcEngineConfig new];
    rtcConfig.appId = [AppContext.shared appId];
    rtcConfig.channelProfile = AgoraChannelProfileLiveBroadcasting;
    AgoraLogConfig* logConfig = [AgoraLogConfig new];
    logConfig.filePath = [AgoraEntLog sdkLogPath];
    rtcConfig.logConfig = logConfig;
    self.RTCkit = [AgoraRtcEngineKit sharedEngineWithConfig:rtcConfig delegate:self];
    
    //use game streaming in so mode, chrous profile in chrous mode
    [self.RTCkit setAudioScenario:AgoraAudioScenarioGameStreaming];
    [self.RTCkit setAudioProfile:AgoraAudioProfileMusicHighQualityStereo];
    if(AppContext.shared.isDebugMode){
        [self.RTCkit setParameters: @"{\"che.audio.neteq.dump_level\": 1}"];
    }
    [self.RTCkit setParameters: @"{\"che.audio.input_sample_rate\": 48000}"];
    [_soundcardPresenter setupEngine:self.RTCkit];
    /// Turn on the singing scoring function
    int code = [self.RTCkit enableAudioVolumeIndication:50 smooth:10 reportVad:true];
    
    if (code == 0) {
        KTVLogInfo(@"Score callback turned on successfully\n");
    } else {
        KTVLogInfo(@"Failed to turn on the scoring callback：%d\n",code);
    }
    
    [self.RTCkit enableVideo];
    [self.RTCkit enableAudio];
    
    [self setupContentInspectConfig];
    
    VLRoomSeatModel* myseat = [self.seatsArray objectAtIndex:0];
    
    self.isNowMicMuted = myseat.isAudioMuted;
    self.isNowCameraMuted = myseat.isVideoMuted;
    self.trackMode = KTVPlayerTrackModeAcc;
    self.singRole = KTVSingRoleAudience;
    
    AgoraVideoEncoderConfiguration *encoderConfiguration =
    [[AgoraVideoEncoderConfiguration alloc] initWithSize:CGSizeMake(100, 100)
                                               frameRate:AgoraVideoFrameRateFps7
                                                 bitrate:20
                                         orientationMode:AgoraVideoOutputOrientationModeFixedLandscape
                                              mirrorMode:AgoraVideoMirrorModeAuto];
    [self.RTCkit setVideoEncoderConfiguration:encoderConfiguration];
    self.aecLevel = 1;
    self.aecState = true;
    [self.RTCkit setEnableSpeakerphone:YES];
    
    AgoraDataStreamConfig *config = [AgoraDataStreamConfig new];
    config.ordered = NO;
    config.syncWithAudio = NO;
    [self.RTCkit createDataStream:&ktvStreamId
                           config:config];
    
    NSString* exChannelToken = AppContext.shared.agoraRTCToken;
    KTVApiConfig* apiConfig = [[KTVApiConfig alloc] initWithAppId: [[AppContext shared] appId]
                                                         rtmToken: AppContext.shared.agoraRTMToken
                                                           engine: self.RTCkit
                                                      channelName: self.roomModel.roomNo
                                                         localUid: [VLUserCenter.user.id integerValue]
                                                chorusChannelName: [NSString stringWithFormat:@"%@_rtc_ex", self.roomModel.roomNo]
                                               chorusChannelToken: exChannelToken
                                                             type: KTVTypeNormal
                                                        musicType: loadMusicTypeMcc
                                                        maxCacheSize: 10
                                                        mccDomain: AppContext.shared.isDebugMode ? @"api-test.agora.io" : nil];
    self.ktvApi = [[KTVApiImpl alloc] init];
    [self.ktvApi createKtvApiWithConfig:apiConfig];
    [self.ktvApi renewInnerDataStreamId];
    KTVLrcControl* lrcControl = [[KTVLrcControl alloc] initWithLrcView:self.MVView.karaokeView];
    [self.ktvApi setLrcViewWithView:lrcControl];
    self.lrcControl = lrcControl;
    self.lrcControl.delegate = self;
    VL(weakSelf);
    lrcControl.skipCallBack = ^(NSInteger time, BOOL flag) {
        NSInteger seekTime = flag ? [weakSelf.ktvApi getMusicPlayer].getDuration - 800 : time;
        [weakSelf.ktvApi seekSingWithTime:seekTime];
    };
    [self.ktvApi muteMicWithMuteStatus:self.isNowMicMuted];
    [self.ktvApi addEventHandlerWithKtvApiEventHandler:self];
    
    [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": -1}"];
    
    [self.RTCkit setAudioEffectPreset:AgoraAudioEffectPresetRoomAcousticsKTV];
    
//    VL(weakSelf);
    KTVLogInfo(@"Agora - joining RTC channel for roomNo: %@, with uid: %@", self.roomModel.roomNo, VLUserCenter.user.id);
    int ret =
    [self.RTCkit joinChannelByToken:AppContext.shared.agoraRTCToken
                          channelId:self.roomModel.roomNo
                                uid:[VLUserCenter.user.id integerValue]
                       mediaOptions:[self channelMediaOptions]
                        joinSuccess:nil];
    if (ret != 0) {
        KTVLogError(@"joinChannelByToken fail: %d, uid: %ld, token: %@", ret, [VLUserCenter.user.id integerValue], AppContext.shared.agoraRTCToken);
    }
    
    VLRoomSeatModel* info = [self getCurrentUserSeatInfo];
    if (info) {
        [self _checkEnterSeatAudioAuthorized];
//
//        if (!info.isVideoMuted) {
//            [AgoraEntAuthorizedManager checkCameraAuthorizedWithParent:self completion:nil];
//        }
        self.isNowMicMuted = info.isAudioMuted;
        self.isNowCameraMuted = info.isVideoMuted;
    } else {
        self.isNowMicMuted = YES;
        self.isNowCameraMuted = YES;
    }
}

- (void)leaveRTCChannel {
    [self.ktvApi removeEventHandlerWithKtvApiEventHandler:self];
    [self.ktvApi cleanCache];
    self.ktvApi = nil;
    self.loadMusicCallBack = nil;
    [self.RTCkit leaveChannel:^(AgoraChannelStats * _Nonnull stat) {
        KTVLogInfo(@"Agora - Leave RTC channel");
    }];
}

- (AgoraRtcChannelMediaOptions*)channelMediaOptions {
    AgoraRtcChannelMediaOptions *option = [AgoraRtcChannelMediaOptions new];
    [option setClientRoleType:[self isBroadcaster] ? AgoraClientRoleBroadcaster : AgoraClientRoleAudience];
    [option setPublishCameraTrack:!self.isNowCameraMuted];
    // use audio volume to control mic on/off, so that mic is always on when broadcaster
    [option setPublishMicrophoneTrack:[self isBroadcaster]];
    [option setPublishCustomAudioTrack:NO];
    [option setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    [option setAutoSubscribeAudio:YES];
    [option setAutoSubscribeVideo:YES];
    [option setPublishMediaPlayerId:[[self.ktvApi getMusicPlayer] getMediaPlayerId]];
    [option setEnableAudioRecordingOrPlayout:YES];
    return option;
}

- (void)sendStreamMessageWithDict:(NSDictionary *)dict
                         success:(void (^ _Nullable)(BOOL))success {
//    VLLog(@"sendStremMessageWithDict:::%@",dict);
    NSData *messageData = [VLGlobalHelper compactDictionaryToData:dict];
    
    int code = [self.RTCkit sendStreamMessage:ktvStreamId
                                         data:messageData];
    if (code == 0 && success) {
        success(YES);
    } else{
    };
}

#pragma mark -- VLKTVAPIDelegate

- (void)didLrcViewDragedToPos:(NSInteger)pos score:(NSInteger)score totalScore:(NSInteger)totalScore{
    [self.ktvApi.getMusicPlayer seekToPosition:pos];
}

- (void)didLrcViewScorllFinishedWith:(NSInteger)score totalScore:(NSInteger)totalScore lineScore:(NSInteger)lineScore lineIndex:(NSInteger)lineIndex{
    if(self.singRole == KTVSingRoleAudience){
        return;
    }
    
    NSInteger realScore = self.singRole == KTVSingRoleCoSinger ? self.coSingerDegree + lineScore : score;
    [self.MVView.lineScoreView showScoreViewWithScore:lineScore];
    [self.MVView.incentiveView showWithScore:lineScore];
    //Synchronize the scores of the lead singer to the audience
    if(self.singRole == KTVSingRoleSoloSinger || self.singRole == KTVSingRoleLeadSinger){
        [self sendMainSingerLineScoreToAudienceWith:score totalScore:totalScore lineScore:lineScore lineIndex:lineIndex];
    } else {
        self.coSingerDegree += lineScore;
    }
}

-(void)sendMainSingerLineScoreToAudienceWith:(NSInteger)cumulativeScore totalScore:(NSInteger)totalScore lineScore:(NSInteger)lineScore lineIndex:(NSInteger)lineIndex{
    NSDictionary *dict = @{
        @"cmd":@"singleLineScore",
        @"score":@(lineScore),
        @"index":@(lineIndex),
        @"cumulativeScore":@(cumulativeScore),
        @"total":@(totalScore),
        
    };
    [self sendStreamMessageWithDict:dict success:nil];
    KTVLogInfo(@"index: %li, score: %li, cumulativeScore: %li, total: %li", lineIndex, lineScore, cumulativeScore, totalScore);
}

- (void)didSongLoadedWith:(LyricModel *)model{
    self.lyricModel = model;
}

- (void)onChorusChannelAudioVolumeIndicationWithSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> *)speakers totalVolume:(NSInteger)totalVolume{
    
}

- (void)didJoinChours {
    //Join the chorus
    [self.MVView setMvState:VLKTVMVViewStateJoinChorus];
    [self joinChorus];
}

-(void)didLeaveChours {
    //Quit the chorus
    [[AppContext ktvServiceImp] leaveChorusWithSongCode:self.selSongsArray.firstObject.songNo
                                             completion:^(NSError * error) {
        if (error == nil) {
            return;
        }
        [VLToast toast:error.localizedDescription];
    }];
}

- (BOOL)enableShowJoinChorusButton {
    if(_isOnMicSeat) {
        return YES;
    }
    NSUInteger count = [self getOnMicUserCount];
    KTVLogInfo(@"getOnMicUserCount = %ld", count);
    return count < 8;
}

#pragma mark -- VLKTVTopViewDelegate
- (void)onVLKTVTopView:(VLKTVTopView *)view closeBtnTapped:(id)sender {
    VL(weakSelf);
    BOOL isOwner = [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
    NSString *title = isOwner ? KTVLocalizedString(@"ktv_disband_room") : KTVLocalizedString(@"ktv_exit_room");
    NSString *message = isOwner ? KTVLocalizedString(@"ktv_confirm_disband_room") : KTVLocalizedString(@"ktv_confirm_exit_room");
    NSArray *array = [[NSArray alloc]initWithObjects:KTVLocalizedString(@"ktv_cancel"),KTVLocalizedString(@"ktv_gotit"), nil];
    [[VLAlert shared] showAlertWithFrame:UIScreen.mainScreen.bounds title:title message:message placeHolder:@"" type:ALERTYPENORMAL buttonTitles:array completion:^(bool flag, NSString * _Nullable text) {
        if(flag == YES){
            [weakSelf leaveRoom];
        }
        [[VLAlert shared] dismiss];
    }];
}

- (void)onVLKTVTopView:(VLKTVTopView *)view moreBtnTapped:(id)sender {
    AUiMoreDialog* dialog = [[AUiMoreDialog alloc] initWithFrame:self.view.bounds];
    [self.view addSubview:dialog];
    [dialog show];
}

#pragma mark - VLPopMoreSelViewDelegate
- (void)onVLKTVMoreSelView:(VLPopMoreSelView *)view
                 btnTapped:(id)sender
                 withValue:(VLKTVMoreBtnClickType)typeValue {
    [[LSTPopView getPopViewWithCustomView:view] dismiss];
    switch (typeValue) {
//        case VLKTVMoreBtnClickTypeBelcanto:
//            [self popBelcantoView];
//            break;
//        case VLKTVMoreBtnClickTypeSound:
//            [self popSetSoundEffectView];
//            break;
        case VLKTVMoreBtnClickTypeSetting:
            [self popVoicePerView];
            break;
        case VLKTVMoreBtnClickTypeMV:
            [self popSelMVBgView];
            break;
        default:
            break;
    }
}

#pragma mark - VLKTVBottomViewDelegate
- (void)onVLKTVBottomView:(VLKTVBottomToolbar *)view
                btnTapped:(id)sender
               withValues:(VLKTVBottomBtnClickType)typeValue {
    switch (typeValue) {
        case VLKTVBottomBtnClickTypeMore:  //More
//            [self popSelMVBgView];
//            [self popSelMoreView];
            [self showSettingView];
            break;
        case VLKTVBottomBtnClickTypeJoinChorus:
            [self popUpChooseSongView:YES];
            break;
        case VLKTVBottomBtnClickTypeChoose:
            [self popUpChooseSongView:NO];
            break;
        case VLKTVBottomBtnClickTypeAudio:
            [[AppContext ktvServiceImp] updateSeatAudioMuteStatusWithMuted:!self.isNowMicMuted
                                                                completion:^(NSError * error) {
            }];
            break;
        case VLKTVBottomBtnClickTypeVideo:
            [[AppContext ktvServiceImp] updateSeatVideoMuteStatusWithMuted:!self.isNowCameraMuted
                                                                completion:^(NSError * error) {
            }];
            break;
        default:
            break;
    }
}

#pragma mark - VLRoomPersonViewDelegate
- (void)onVLRoomPersonView:(VLMicSeatList *)view
   seatItemTappedWithModel:(VLRoomSeatModel *)model
                   atIndex:(NSInteger)seatIndex {
    
    BOOL isOwner = [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
    if(isOwner) {
        //is owner
        if ([model.owner.userId isEqualToString:VLUserCenter.user.id]) {
            //self, return
            return;
        }
        if (model.owner.userId.length > 0) {
            return [self popDropLineViewWithSeatModel:model];
        }
    } else {
        if (model.owner.userId.length > 0) {
            //occupied
            if ([model.owner.userId isEqualToString:VLUserCenter.user.id]) {//I clicked on myself.
                return [self popDropLineViewWithSeatModel:model];
            }
        } else{
            //empty
            BOOL isOnSeat = [self getCurrentUserSeatInfo] == nil ? NO : YES;
            if (!isOnSeat) {
                //not yet seated
                [self enterSeatWithIndex:@(seatIndex) completion:^(NSError *error) {
                    if (error == nil) {return;}
                    [VLToast toast: error.localizedDescription];
                }];
            }
        }
    }
}

- (void)onVLRoomPersonView:(VLMicSeatList *)view onRenderVideo:(VLRoomSeatModel *)model inView:(UIView *)videoView atIndex:(NSInteger)seatIndex
{
    AgoraRtcVideoCanvas *videoCanvas = [[AgoraRtcVideoCanvas alloc] init];
    videoCanvas.uid = [model.owner.userId unsignedIntegerValue];
    videoCanvas.view = videoView;
    videoCanvas.renderMode = AgoraVideoRenderModeHidden;
    if([model.owner.userId isEqual:VLUserCenter.user.id]) {
        //is self
        [self.RTCkit setupLocalVideo:videoCanvas];
    } else {
        [self.RTCkit setupRemoteVideo:videoCanvas];
    }
}

#pragma mark - VLPopSelBgViewDelegate
-(void)onVLPopSelBgView:(VLPopSelBgView *)view
       tappedWithAction:(VLKTVSelBgModel *)selBgModel
                atIndex:(NSInteger)index {
    NSAssert(true, @"need remove");
//    KTVChangeMVCoverInputModel* inputModel = [KTVChangeMVCoverInputModel new];
////    inputModel.roomNo = self.roomModel.roomNo;
//    inputModel.mvIndex = index;
////    inputModel.userNo = VLUserCenter.user.id;
//    VL(weakSelf);
//    [[AppContext ktvServiceImp] changeMVCoverWithInputModel:inputModel
//                                                 completion:^(NSError * error) {
//        if (error != nil) {
//            [VLToast toast:error.description];
//            return;
//        }
//
//        [[LSTPopView getPopViewWithCustomView:view] dismiss];
//        weakSelf.choosedBgModel = selBgModel;
//    }];
}

#pragma mark VLPopChooseSongViewDelegate
- (void)chooseSongView:(VLPopSongList*)view tabbarDidClick:(NSUInteger)tabIndex {
    if (tabIndex != 1) {
        return;
    }
    
    [self refreshChoosedSongList:nil];
}

#pragma mark - VLChooseBelcantoViewDelegate
- (void)onVLChooseBelcantoView:(VLAudioEffectPicker *)view
                    itemTapped:(VLBelcantoModel *)model
                     withIndex:(NSInteger)index {
    self.selBelcantoModel = model;
    if (index == 0) {
        [self.RTCkit setVoiceBeautifierPreset:AgoraVoiceBeautifierPresetOff];
    }else if (index == 1){
        [self.RTCkit setVoiceBeautifierPreset:AgoraVoiceBeautifierPresetChatBeautifierMagnetic];
    }else if (index == 2){
        [self.RTCkit setVoiceBeautifierPreset:AgoraVoiceBeautifierPresetChatBeautifierFresh];
    }else if (index == 3){
        [self.RTCkit setVoiceBeautifierPreset:AgoraVoiceBeautifierPresetChatBeautifierVitality];
    }else if (index == 4){
        [self.RTCkit setVoiceBeautifierPreset:AgoraVoiceBeautifierPresetChatBeautifierVitality];
    }
}

#pragma mark VLDropOnLineViewDelegate
- (void)onVLDropOnLineView:(VLDropOnLineView *)view action:(VLRoomSeatModel *)seatModel {
    [self leaveSeatWithSeatModel:seatModel withCompletion:^(NSError *error) {
        [[LSTPopView getPopViewWithCustomView:view] dismiss];
        if (error == nil) {return;}
        [VLToast toast:error.localizedDescription];
    }];
}

#pragma mark VLTouristOnLineViewDelegate
//The way to go to the microphone seat
- (void)requestOnlineAction {
}
#pragma mark - MVViewDelegate
// Score real-time callback
- (void)onKTVMVView:(VLKTVMVView *)view scoreDidUpdate:(int)score {
}

- (void)onKTVMVView:(VLKTVMVView *)view btnTappedWithActionType:(VLKTVMVViewActionType)type {
    if (type == VLKTVMVViewActionTypeSetParam) {
        [self showSettingView];
    } else if (type == VLKTVMVViewActionTypeMVPlay) { //Play
        [self.ktvApi resumeSing];
        self.isPause = false;
    } else if (type == VLKTVMVViewActionTypeMVPause) { //Suspend
        [self.ktvApi pauseSing];
        self.isPause = true;
    } else if (type == VLKTVMVViewActionTypeMVNext) { //Cut
        
        if(self.RTCkit.getConnectionState != AgoraConnectionStateConnected){
            [VLToast toast:KTVLocalizedString(@"ktv_change_failed")];
            return;
        }
        
        VL(weakSelf);

        NSString *title = KTVLocalizedString(@"ktv_change_song");
        NSString *message = KTVLocalizedString(@"ktv_change_next_song");
        NSArray *array = [[NSArray alloc]initWithObjects:KTVLocalizedString(@"ktv_cancel"),KTVLocalizedString(@"ktv_gotit"), nil];
        [[VLAlert shared] showAlertWithFrame:UIScreen.mainScreen.bounds title:title message:message placeHolder:@"" type:ALERTYPENORMAL buttonTitles:array completion:^(bool flag, NSString * _Nullable text) {
            if(flag == YES){
                [weakSelf removeCurrentSong];
            }
            [[VLAlert shared] dismiss];
        }];
    } else if (type == VLKTVMVViewActionTypeSingOrigin) { // Original singer
        self.trackMode = KTVPlayerTrackModeOrigin;
    } else if (type == VLKTVMVViewActionTypeSingAcc) { // Accompany
        self.trackMode = KTVPlayerTrackModeAcc;
    } else if (type == VLKTVMVViewActionTypeRetryLrc) {  //Try the lyrics again
        [self reloadMusic];
    } else if (type == VLKTVMVViewActionTypeSingLead){
        self.trackMode = KTVPlayerTrackModeLead; //Director and singer
    }
}

- (void)onKTVMView:(VLKTVMVView *)view lrcViewDidScrolled:(NSInteger)position {
    [[self.ktvApi getMusicPlayer] seekToPosition:position];
}

- (void)reloadMusic{
    VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
    KTVSongConfiguration* songConfig = [[KTVSongConfiguration alloc] init];
//    KTVSingRole role = [self getUserSingRole];
    songConfig.mode = KTVLoadMusicModeLoadLrcOnly;
    songConfig.mainSingerUid = [model.owner.userId integerValue];
    songConfig.songIdentifier = model.songNo;

    [self.MVView setMvState:VLKTVMVViewStateMusicLoading];
    VL(weakSelf);
    self.loadMusicCallBack = ^(BOOL isSuccess, NSInteger songCode) {
        if (!isSuccess) {
            return;
        }
        [weakSelf setMVViewStateWith:model];
    };
    
    [self.ktvApi loadMusicWithSongCode:[model.songNo integerValue] config:songConfig onMusicLoadStateListener:self];
}

#pragma mark - VLKTVSettingViewDelegate

- (void)settingViewBackAction {
    [[[LSTPopView getAllPopView] lastObject] dismiss];
}

- (void)settingViewSettingChanged:(VLKTVSettingModel *)setting
              valueDidChangedType:(VLKTVValueDidChangedType)type {
    self.settingModel = setting;
    if (type == VLKTVValueDidChangedTypeEar) { // Ear return settings
        [self showEarSettingView];
    } else if (type == VLKTVValueDidChangedTypeMV) { // MV
        
    } else if (type == VLKTVValueDidChangedTypeSoundCard) { // Virtual sound card
        [self showSoundCardView];
    } else if (type == VLKTVValueDidChangedRiseFall) { // Up and down tune
        // Adjust the tone of the currently playing media resources
        // Adjust the tone of the locally played music file according to the semitone scale. The default value is 0, that is, the tone is not adjusted. The value range is [-12,12], and the pitch distance of each adjacent two values is half a tone different. The higher the absolute value of the value, the more the tone rises or decreases.
        NSInteger value = setting.toneValue * 2 - 12;
        [[self.ktvApi getMusicPlayer] setAudioPitch:value];
    } else if (type == VLKTVValueDidChangedTypeSound) { // Volume
        // Adjust the volume of the audio acquisition signal, and the value range is [0,400]
        // 0. Mute 100, default original volume 400, 4 times the original volume, with overflow protection
        if(self.soundVolume != setting.soundValue){
            [self.RTCkit adjustRecordingSignalVolume:setting.soundValue];
            self.soundVolume = setting.soundValue;
        }
    } else if (type == VLKTVValueDidChangedTypeAcc) { // Accompany
        int value = setting.accValue;
        if(self.playoutVolume != value){
            self.playoutVolume = value;
        }
    } else if (type == VLKTVValueDidChangedTypeListItem) {
        AgoraAudioEffectPreset preset = [self audioEffectPreset:setting.kindIndex];
        [self.RTCkit setAudioEffectPreset:preset];
    } else if (type == VLKTVValueDidChangedTypeRemoteValue) {
        [self.RTCkit adjustPlaybackSignalVolume:setting.remoteVolume];
    } else if (type == VLKTVValueDidChangedTypeLrc) {//Lyrics level
        LRCLEVEL level = LRCLEVELMID;
        if(setting.lrcLevel == 0){
            level = LRCLEVELOW;
        } else if(setting.lrcLevel == 1){
            level = LRCLEVELMID;
        } else if(setting.lrcLevel == 2){
            level = LRCLEVELHIGH;
        }
        [self.MVView setLrcLevelWith:level];
    } else if (type == VLKTVValueDidChangedTypeVqs) {//Acoustic fidelity
        [self didVolQualityGradeChangedWithIndex:setting.vqs];
    } else if (type == VLKTVValueDidChangedTypeAns) {//Reduce the noise
        [self didAIAECGradeChangedWithIndex:setting.ans];
    } else if (type == VLKTVValueDidChangedTypeaiaec) {//AIAEC
        [self didAECStateChange:setting.enableAec];
    } else if (type == VLKTVValueDidChangedTypebro) {//Professional anchor
        [self voicePerItemSelectedAction:setting.isPerBro];
    } else if (type == VLKTVValueDidChangedTypeAecLevel){
        [self didAECLevelSetWith:setting.aecLevel];
    } else if (type == VLKTVValueDidChangedTypeenableMultipath){
        [self.ktvApi enableMutipathWithEnable:setting.enableMultipath];
    }
}

- (void)settingViewSettingChanged:(VLKTVSettingModel *)setting effectChoosed:(NSInteger)effectIndex {
    [self settingViewEffectChoosed:effectIndex];
}


-(void)showEarSettingView {
    [LSTPopView popEarSettingViewWithParentView:self.view isEarOn:_isEarOn vol:self.earValue withDelegate:self];
}

-(void)showSoundCardView {
    self.soundSettingView = [[SoundCardSettingView alloc] init];
    [self.soundSettingView setupWithEnable:[_soundcardPresenter getSoundCardEnable]
                                 typeValue:[_soundcardPresenter getPresetValue]
                                 gainValue:[_soundcardPresenter getGainValue]
                                effectType:[_soundcardPresenter getPresetSoundEffectType]];
    kWeakSelf(self);
    self.soundSettingView.clicKBlock = ^(NSInteger index) {
        if(index == 2){
            //Sound effect settings
            [weakself showSoundEffectView];
        } else if (index == 4) {
            //Microphone settings
            [weakself showSoundMicTypeView];
        }
    };
    self.soundSettingView.gainBlock = ^(float gain) {
        [weakself.soundcardPresenter setGainValue:(NSInteger)gain];
    };
    
    self.soundSettingView.typeBlock = ^(NSInteger index) {
        [weakself.soundcardPresenter setPresetValue:index];
    };
    
    self.soundSettingView.soundCardBlock = ^(BOOL flag) {
        [weakself.soundcardPresenter setSoundCardEnable:flag];
    };
    self.soundSettingView.clickBackBlock = ^ {
        [weakself settingViewBackAction];
    };
    self.popSoundSettingView = [LSTPopView popSoundCardViewWithParentView:self.view soundCardView:self.soundSettingView];
}

-(void)showSoundEffectView {
    SoundCardEffectView *effectView = [[SoundCardEffectView alloc]init];
    effectView.effectType = [_soundcardPresenter getPresetSoundEffectType];
    LSTPopView* popEffectView = [LSTPopView popSoundCardViewWithParentView:self.view soundCardView:effectView];
    kWeakSelf(self);
    effectView.clickBlock = ^(NSInteger index) {
        if (index != -1) {
            [weakself.soundcardPresenter setPresetSoundEffectType:index];
        }
        [LSTPopView removePopView:popEffectView];
        [LSTPopView removePopView:self.popSoundSettingView];
        [weakself showSoundCardView];
    };
}

-(void)showSoundMicTypeView {
    SoundCardMicTypeView *micTypeView = [[SoundCardMicTypeView alloc]init];
    micTypeView.micType = [_soundcardPresenter getPresetValue];
    LSTPopView* popMicView = [LSTPopView popSoundCardViewWithParentView:self.view soundCardView:micTypeView];
    kWeakSelf(self);
    micTypeView.clickBlock = ^(NSInteger index) {
        [LSTPopView removePopView:popMicView];
        [LSTPopView removePopView:self.popSoundSettingView];
        [weakself showSoundCardView];
    };
}

#pragma mark - VirtualSoundcardPresenterDelegate

- (void)onSoundcardPresenterValueChangedWithIsEnabled:(BOOL)isEnabled presetValue:(NSInteger)presetValue gainValue:(NSInteger)gainValue presetSoundType:(NSInteger)presetSoundType {
    [self.soundSettingView setupWithEnable:isEnabled
                                 typeValue:presetValue
                                 gainValue:gainValue
                                effectType:presetSoundType];
    [self.settingView setUseSoundCard:isEnabled];
}

- (void)settingViewEffectChoosed:(NSInteger)effectIndex {
    self.selectedEffectIndex = effectIndex;
    NSArray *effects = @[@(AgoraAudioEffectPresetRoomAcousticsKTV),
                         @(AgoraAudioEffectPresetOff),
                         @(AgoraAudioEffectPresetRoomAcousVocalConcer),
                         @(AgoraAudioEffectPresetRoomAcousStudio),
                         @(AgoraAudioEffectPresetRoomAcousPhonograph),
                         @(AgoraAudioEffectPresetRoomAcousSpatial),
                         @(AgoraAudioEffectPresetRoomAcousEthereal),
                         @(AgoraAudioEffectPresetStyleTransformationPopular),
                         @(AgoraAudioEffectPresetStyleTransformationRnb)];
    self.currentSelectEffect = [effects[effectIndex] integerValue];
    KTVLogInfo(@"setAudioEffectPreset: %ld", effectIndex);
    [self.RTCkit setAudioEffectPreset: [effects[effectIndex] integerValue]];
}

- (AgoraAudioEffectPreset)audioEffectPreset:(NSInteger)index {
    NSArray* audioEffectPresets = @[
        @(AgoraAudioEffectPresetOff),
        @(AgoraAudioEffectPresetRoomAcousticsKTV),
        @(AgoraAudioEffectPresetRoomAcousVocalConcer),
        @(AgoraAudioEffectPresetRoomAcousStudio),
        @(AgoraAudioEffectPresetRoomAcousPhonograph),
        @(AgoraAudioEffectPresetRoomAcousSpatial),
        @(AgoraAudioEffectPresetRoomAcousEthereal),
        @(AgoraAudioEffectPresetStyleTransformationPopular),
        @(AgoraAudioEffectPresetStyleTransformationRnb),
    ];
    if (audioEffectPresets.count <= index) {
        return AgoraAudioEffectPresetOff;
    }
    
    return [[audioEffectPresets objectAtIndex:index] integerValue];
}

//Sound effect settings
- (void)effectItemClickAction:(NSInteger)effect {
    self.selectedEffectIndex = effect;
    NSArray *effects = @[@(AgoraAudioEffectPresetOff),
                         @(AgoraAudioEffectPresetRoomAcousticsKTV),
                         @(AgoraAudioEffectPresetRoomAcousVocalConcer),
                         @(AgoraAudioEffectPresetRoomAcousStudio),
                         @(AgoraAudioEffectPresetRoomAcousPhonograph),
                         @(AgoraAudioEffectPresetRoomAcousSpatial),
                         @(AgoraAudioEffectPresetRoomAcousEthereal),
                         @(AgoraAudioEffectPresetStyleTransformationPopular),
                         @(AgoraAudioEffectPresetStyleTransformationRnb)];
    self.currentSelectEffect = [effects[effect] integerValue];
    [self.RTCkit setAudioEffectPreset: [effects[effect] integerValue]];
}

//Professional anchor setting
- (void)voicePerItemSelectedAction:(BOOL)isSelected {
    self.isProfessional = isSelected;
    [self.ktvApi enableProfessionalStreamerMode:isSelected];
}

- (void)didAIAECGradeChangedWithIndex:(NSInteger)index{
    self.aecGrade = index;
    [self onAINSModeChangedWithMode:index];
}

-(void)didVolQualityGradeChangedWithIndex:(NSInteger)index {
    self.volGrade = index;
    [self onAECLevelChangedWithLevel:index];
}

- (void)voiceDelaySelectedAction:(BOOL)isSelected{
    self.isDelay = isSelected;
    [self enableLowLatencyMode:isSelected];
}

- (void)onAECLevelChangedWithLevel:(NSInteger)level {
    if (level == 0) {
        [self.RTCkit setParameters:@"{\"che.audio.aec.split_srate_for_48k\": 16000}"];
    } else if (level == 1) {
        [self.RTCkit setParameters:@"{\"che.audio.aec.split_srate_for_48k\": 24000}"];
    } else if (level == 2) {
        [self.RTCkit setParameters:@"{\"che.audio.aec.split_srate_for_48k\": 48000}"];
    }
}

- (void)onAINSModeChangedWithMode:(NSInteger)mode {
    if (mode == 0) {
        // close
        [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": 0}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerBound\": 80}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerMask\": 50}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.statisticalbound\": 5}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.finallowermask\": 30}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.enhfactorstastical\": 200}"];
    } else if (mode == 1) {
        // middle
        [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": 2}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerBound\": 80}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerMask\": 50}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.statisticalbound\": 5}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.finallowermask\": 30}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.enhfactorstastical\": 200}"];
    } else if (mode == 2) {
        // high
        [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": 2}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerBound\": 10}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.lowerMask\": 10}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.statisticalbound\": 0}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.finallowermask\": 8}"];
        [self.RTCkit setParameters:@"{\"che.audio.nsng.enhfactorstastical\": 200}"];
    }
}

- (void)enableLowLatencyMode:(BOOL)enable {
    if (enable) {
        //  [self.RTCkit setParameters:@"{\"che.audio.aiaec.working_mode\": 0}"];
        [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": -1}"];
        //        [self.RTCkit setParameters:@"{\"che.audio.aec.nlp_size\": 128}"];
        //        [self.RTCkit setParameters:@"{\"che.audio.aec.nlp_size\": 64}"];
    } else {
        //   [self.RTCkit setParameters:@"{\"che.audio.aiaec.working_mode\": 0}"];
        [self.RTCkit setParameters:@"{\"che.audio.ains_mode\": 0}"];
        //        [self.RTCkit setParameters:@"{\"che.audio.aiaec.working_mode\": 512}"];
        //        [self.RTCkit setParameters:@"{\"che.audio.aec.nlp_hop_size\": 64}"];
    }
}

-(void)didAECStateChange:(BOOL)enable{
    self.aecState = enable;
    if(enable){
        [self.RTCkit setParameters:@"{\"che.audio.aiaec.working_mode\": 1}"];
    } else {
        [self.RTCkit setParameters:@"{\"che.audio.aiaec.working_mode\": 0}"];
    }
}

-(void)didAECLevelSetWith:(NSInteger)level{
    self.aecLevel = level;
    [self.RTCkit setParameters:[NSString stringWithFormat:@"{\"che.audio.aiaec.postprocessing_strategy\":%li}", (long)level]];
}

#pragma mark --
- (void)_fetchServiceAllData {
    //Request the song that has been clicked
    VL(weakSelf);
    [self refreshChoosedSongList:^{
        //Request lyrics and songs
        if(weakSelf.selSongsArray.count == 0){
            return;
        }
        [weakSelf loadAndPlaySong];
    }];
}

#pragma mark - getter/handy utils
- (BOOL)isCurrentSongMainSinger:(NSString *)userNo {
    VLRoomSelSongModel *selSongModel = self.selSongsArray.firstObject;
    return [selSongModel.owner.userId isEqualToString:userNo];
}

- (BOOL)isRoomOwner {
    return [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
}

- (BOOL)isBroadcaster {
    return [self isRoomOwner] || self.isOnMicSeat;
}

- (VLRoomSelSongModel*)selSongWithSongNo:(NSString*)songNo {
    __block VLRoomSelSongModel* song = nil;
    [self.selSongsArray enumerateObjectsUsingBlock:^(VLRoomSelSongModel * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([obj.songNo isEqualToString:songNo]) {
            song = obj;
            *stop = YES;
        }
    }];
    
    return song;
}

/// Get the microphone of the current user
- (VLRoomSeatModel*)getCurrentUserSeatInfo {
    return [self getUserSeatInfoWithUserId:VLUserCenter.user.id];
}

- (VLRoomSeatModel*)getUserSeatInfoWithUserId:(NSString*)userId {
    for (VLRoomSeatModel *model in self.seatsArray) {
        if ([model.owner.userId isEqualToString:userId]) {
            return model;
        }
    }
    
    return nil;
}

-(BOOL)checkIfCosingerWith:(NSInteger)index{
    VLRoomSeatModel *model = self.seatsArray[index];
    return [AppContext isKtvChorusingWithSeat:model];
}

/// Obtain the microphone according to the microphone index
/// @param seatIndex <#seatIndex description#>
- (VLRoomSeatModel*)getUserSeatInfoWithIndex:(NSUInteger)seatIndex {
    for (VLRoomSeatModel *model in self.seatsArray) {
        if (model.seatIndex == seatIndex) {
            return model;
        }
    }
    
    return nil;
}

/// Calculate the singing role of the current song user
- (KTVSingRole)getUserSingRole {
    BOOL currentSongIsJoinSing = [AppContext isKtvChorusingWithUserId:VLUserCenter.user.id];
    BOOL currentSongIsSongOwner = [AppContext isKtvSongOwnerWithUserId:VLUserCenter.user.id];
    BOOL currentSongIsChorus = [self getChorusNumWithSeatArray:self.seatsArray] > 0;
    if (currentSongIsSongOwner) {
        return currentSongIsChorus ? KTVSingRoleLeadSinger : KTVSingRoleSoloSinger;
    } else if (currentSongIsJoinSing) {
        return KTVSingRoleCoSinger;
    } else {
        return KTVSingRoleAudience;
    }
}


/// Calculate the number of chorusists
/// @param seatArray <#seatArray description#>
- (NSUInteger)getChorusNumWithSeatArray:(NSArray*)seatArray {
    NSUInteger chorusNum = 0;
    for(VLRoomSeatModel* seat in seatArray) {
        //TODO: validate songCode
        if([AppContext isKtvChorusingWithSeat:seat]) {
            chorusNum += 1;
        }
    }
    
    return chorusNum;
}

- (BOOL)getJoinChorusEnable {
    //It's not that the audience is not allowed to join.
    if ([self getUserSingRole] != KTVSingRoleAudience) {
        KTVLogInfo(@"getJoinChorusEnable fail role = %ld", [self getUserSingRole]);
        return NO;
    }
    
    VLRoomSelSongModel* topSong = [[self selSongsArray] firstObject];
    //TODO: Not allowed to join if it is not playing.
    if (topSong.status != VLSongPlayStatusPlaying) {
        KTVLogInfo(@"getJoinChorusEnable fail status = %ld", topSong.status);
        return NO;
    }
    
    return YES;
}

//Get the total number of viewers who have not been under the microphone seat
-(NSInteger)getOnMicUserCount{
    NSInteger num = 0;
    if(self.seatsArray){
        for(VLRoomSeatModel *model in self.seatsArray){
            if(model.owner.userId.length > 0){
                num++;
            }
        }
    }
    return num;
}

#pragma mark - setter
- (void)setKtvApi:(KTVApiImpl *)ktvApi {
    _ktvApi = ktvApi;
    [[AppContext shared] setKtvAPI:ktvApi];
}

- (void)setRoomUsersCount:(NSUInteger)userCount {
    self.roomModel.roomPeopleNum = userCount;
    self.topView.listModel = self.roomModel;
}

- (void)setChoosedBgModel:(VLKTVSelBgModel *)choosedBgModel {
    _choosedBgModel = choosedBgModel;
    [self.MVView changeBgViewByModel:choosedBgModel];
}

-(void)setVoiceShowHasSeted:(BOOL)voiceShowHasSeted {
    _voiceShowHasSeted = voiceShowHasSeted;
}

-(void)setSelectedVoiceShowIndex:(NSInteger)selectedVoiceShowIndex {
    _selectedVoiceShowIndex = selectedVoiceShowIndex;
}

- (void)setSeatsArray:(NSArray<VLRoomSeatModel *> *)seatsArray {
    _seatsArray = seatsArray;
    
    //update booleans
    self.isOnMicSeat = [self getCurrentUserSeatInfo] == nil ? NO : YES;
    
    self.roomPersonView.roomSeatsArray = self.seatsArray;
    [self.roomPersonView updateSingBtnWithChoosedSongArray:_selSongsArray];
    self.chorusNum = [self getChorusNumWithSeatArray:seatsArray];
    [self onSeatFull];
}

-(void)onSeatFull{
    if(self.singRole != KTVSingRoleAudience){
        return;
    }
    NSInteger count = [self getOnMicUserCount];
    if(!_isOnMicSeat && count >= 8){
        VLRoomSelSongModel *topSong = self.selSongsArray.firstObject;
        if(topSong){
            [self.MVView setMvState:[self isRoomOwner] ? VLKTVMVViewStateOwnerAudience : VLKTVMVViewStateAudience];
        } else {
            [self.MVView setMvState:VLKTVMVViewStateNone];
        }
    } else {
        if(!self.isJoinChorus){
            VLRoomSelSongModel *topSong = self.selSongsArray.firstObject;
            if(topSong){
                [self.MVView setMvState:[self isRoomOwner] ? VLKTVMVViewStateOwnerAudience : VLKTVMVViewStateAudience];
            } else {
                [self.MVView setMvState:VLKTVMVViewStateNone];
            }
        }
    }
}

- (void)setChorusNum:(NSUInteger)chorusNum {
    NSUInteger origChorusNum = _chorusNum;
    _chorusNum = chorusNum;
    if (origChorusNum != chorusNum) {
        //Lead singer <-> solo switch, non-song owner does not need to be called
        if(![self isCurrentSongMainSinger:VLUserCenter.user.id]) {
            return;
        }
        KTVLogInfo(@"seat array update chorusNum %ld->%ld, currentRole: %ld", origChorusNum, chorusNum, self.singRole);
        if(self.singRole == KTVSingRoleAudience) { return; }
        //lead singer <-> solo
        KTVSingRole role = [self getUserSingRole];
        [self.ktvApi switchSingerRoleWithNewRole:role
                               onSwitchRoleState:^(KTVSwitchRoleState state, KTVSwitchRoleFailReason reason) {
        }];
    }
}

- (void)setIsOnMicSeat:(BOOL)isOnMicSeat {
    BOOL onMicSeatStatusDidChanged = _isOnMicSeat != isOnMicSeat;
    _isOnMicSeat = isOnMicSeat;
    
    if (onMicSeatStatusDidChanged) {
        if (!isOnMicSeat) {
            [self stopPlaySong];
        }
        
        //start mic once enter seat
        AgoraRtcChannelMediaOptions *option = [AgoraRtcChannelMediaOptions new];
        [option setClientRoleType:[self isBroadcaster] ? AgoraClientRoleBroadcaster : AgoraClientRoleAudience];
        // use audio volume to control mic on/off, so that mic is always on when broadcaster
        [option setPublishMicrophoneTrack:[self isBroadcaster]];
        [self.RTCkit updateChannelWithMediaOptions:option];
    }
    
    //    [self.RTCkit enableLocalAudio:isOnMicSeat];
    //    [self.RTCkit muteLocalAudioStream:!isOnMicSeat];
    
    self.bottomView.hidden = !isOnMicSeat;
    self.requestOnLineView.hidden = isOnMicSeat;
    
//    VLRoomSeatModel* info = [self getCurrentUserSeatInfo];
//    if(onMicSeatStatusDidChanged){
//        if(info == nil){
//            self.isNowMicMuted = true;
//            self.isNowCameraMuted = true;
//        } else {
//            self.isNowMicMuted = info.isAudioMuted;
//            self.isNowCameraMuted = info.isVideoMuted;
//        }
//    }
}

- (void)setIsNowMicMuted:(BOOL)isNowMicMuted {
    BOOL oldValue = _isNowMicMuted;
    _isNowMicMuted = isNowMicMuted;
    
    [self.ktvApi muteMicWithMuteStatus:isNowMicMuted];
    [self.RTCkit adjustRecordingSignalVolume:isNowMicMuted ? 0 : 100];
    if(oldValue != isNowMicMuted) {
        [self.bottomView updateAudioBtn:isNowMicMuted];
    }
}

- (void)setIsNowCameraMuted:(BOOL)isNowCameraMuted {
    BOOL oldValue = _isNowCameraMuted;
    _isNowCameraMuted = isNowCameraMuted;
    
    [self.RTCkit enableLocalVideo:!isNowCameraMuted];
    if (isNowCameraMuted) {
        [self.RTCkit stopPreview];
    }
    AgoraRtcChannelMediaOptions *option = [AgoraRtcChannelMediaOptions new];
    [option setPublishCameraTrack:!self.isNowCameraMuted];
    [self.RTCkit updateChannelWithMediaOptions:option];
    if(oldValue != isNowCameraMuted) {
        [self.bottomView updateVideoBtn:isNowCameraMuted];
    }
}

- (void)setIsEarOn:(BOOL)isEarOn {
    _isEarOn = isEarOn;
    [self _checkInEarMonitoring];
    NSAssert(self.settingView != nil, @"self.settingView == nil");
    [self.settingView setIsEarOn:isEarOn];
}

- (void)setPlayoutVolume:(int)playoutVolume {
    _playoutVolume = playoutVolume;
    
    // The official document is 100? SDK is 400????
    // Adjust the local playback volume. The value range is [0,100]
    // 0, silent. 100. (Default) Original playback volume of media files
    //    [self.ktvApi adjustPlayoutVolume:playoutVolume];
    [[self.ktvApi getMusicPlayer] adjustPlayoutVolume:playoutVolume];
    
    // Adjust the volume heard by remote users and take the value range [0, 400]
    // 100: (default) the original volume of the media file. 400: Four times the original volume (with overflow protection)
    [[self.ktvApi getMusicPlayer] adjustPublishSignalVolume:playoutVolume];
    
    //update ui
    [self.settingView setAccValue: (float)playoutVolume / 100.0];
}

- (void)_checkInEarMonitoring {
    //    if([self isCurrentSongMainSinger:VLUserCenter.user.id]) {
    //        [self.RTCkit enableInEarMonitoring:_isEarOn includeAudioFilters:AgoraEarMonitoringFilterBuiltInAudioFilters];
    //    } else {
    //        [self.RTCkit enableInEarMonitoring:NO includeAudioFilters:AgoraEarMonitoringFilterBuiltInAudioFilters];
    //    }
    if(self.singRole != KTVSingRoleAudience){//The lead singer and the backing singer can open the ear back.
        [self.RTCkit enableInEarMonitoring:_isEarOn includeAudioFilters:AgoraEarMonitoringFilterNone];
    }
}

- (void)setSelSongsArray:(NSArray<VLRoomSelSongModel *> *)selSongsArray {
    NSArray<VLRoomSelSongModel*> *oldSongsArray = _selSongsArray;
    _selSongsArray = [NSMutableArray arrayWithArray:selSongsArray];
    self.chorusNum = [self getChorusNumWithSeatArray:self.seatsArray];
    if (self.chooseSongView) {
        BOOL isOwner = [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
        [self.chooseSongView setSelSongsArray:_selSongsArray isOwner:isOwner]; //Refresh the UI of the ordered song
    }
    
    [self.roomPersonView updateSingBtnWithChoosedSongArray:_selSongsArray];
    VLRoomSelSongModel* originalTopSong = [oldSongsArray firstObject];
    VLRoomSelSongModel* updatedTopSong = [selSongsArray firstObject];
    KTVLogInfo(@"setSelSongsArray current top[%@] songName: %@, status: %ld",
               updatedTopSong.songNo, updatedTopSong.songName, updatedTopSong.status);
    KTVLogInfo(@"setSelSongsArray orig top[%@] songName: %@, status: %ld",
               originalTopSong.songNo, originalTopSong.songName, originalTopSong.status);
    if(![updatedTopSong.songNo isEqualToString:originalTopSong.songNo]){
        [self.MVView reset];
        [self.lrcControl resetLrc];
        //song changes
        [self stopPlaySong];
        
        if(self.selSongsArray.count == 0){
            [self.MVView setMvState:VLKTVMVViewStateNone];
            return;
        }
        
        [self loadAndPlaySong];
    }
}

- (void)setTrackMode:(KTVPlayerTrackMode)trackMode {
    KTVLogInfo(@"setTrackMode: %ld", trackMode);
    _trackMode = trackMode;
    VLKTVMVViewActionType type = VLKTVMVViewActionTypeSingAcc;
    if(self.singRole == KTVSingRoleCoSinger){
        [[self.ktvApi getMusicPlayer] selectAudioTrack:self.trackMode == KTVPlayerTrackModeOrigin ? 0 : 1];
        type = trackMode == KTVPlayerTrackModeOrigin ? VLKTVMVViewActionTypeSingOrigin : VLKTVMVViewActionTypeSingAcc;
        [self.MVView setOriginBtnState: type];
        return;
    } else if(self.singRole == KTVSingRoleSoloSinger || self.singRole == KTVSingRoleLeadSinger) {
        [self.MVView setOriginBtnState: trackMode == KTVPlayerTrackModeOrigin ? VLKTVMVViewActionTypeSingOrigin : VLKTVMVViewActionTypeSingAcc];
        int ret = [[self.ktvApi getMusicPlayer] selectMultiAudioTrack:trackMode == KTVPlayerTrackModeAcc ? 1 : 0 publishTrackIndex:trackMode == KTVPlayerTrackModeOrigin ? 0 : 1 ];
        
        switch (trackMode) {
            case KTVPlayerTrackModeOrigin:
                type = VLKTVMVViewActionTypeSingOrigin;
                break;
            case KTVPlayerTrackModeAcc:
                type = VLKTVMVViewActionTypeSingAcc;
                break;
            case KTVPlayerTrackModeLead:
                type = VLKTVMVViewActionTypeSingLead;
                break;
            default:
                break;
        }
        [self.MVView setOriginBtnState: type];
    }
}

- (void)setSingRole:(KTVSingRole)singRole {
    KTVLogInfo(@"setSingRole: %ld", singRole);
    _singRole = singRole;
    self.lrcControl.lrcView.lyricsView.draggable = false;
    self.lrcControl.isMainSinger = (_singRole == KTVSingRoleSoloSinger || _singRole == KTVSingRoleLeadSinger);
    
    self.MVView.isOriginLeader = (_singRole == KTVSingRoleSoloSinger || _singRole == KTVSingRoleLeadSinger);
}

#pragma mark KTVApiEventHandlerDelegate
- (void)onMusicPlayerStateChangedWithState:(AgoraMediaPlayerState)state reason:(AgoraMediaPlayerReason)reason isLocal:(BOOL)isLocal {
    dispatch_async(dispatch_get_main_queue(), ^{
        if(state == AgoraMediaPlayerStatePlaying) {
            //Display skipping prelude
            if(self.singRole == KTVSingRoleSoloSinger || self.singRole == KTVSingRoleLeadSinger){
                [self.lrcControl showPreludeEnd];
            }
        } else if(state == AgoraMediaPlayerStatePaused) {
            //    [self.MVView updateMVPlayerState:VLKTVMVViewActionTypeMVPause];
            [self.lrcControl hideSkipViewWithFlag:true];
        } else if(state == AgoraMediaPlayerStateStopped) {
            
        } else if(state == AgoraMediaPlayerStatePlayBackAllLoopsCompleted || state == AgoraMediaPlayerStatePlayBackCompleted) {
            if(isLocal) {
                KTVLogInfo(@"Playback all loop completed");
                // if(self.singRole != KTVSingRoleAudience){
                //Both the singer and the landlord use their own scores.
                if(self.singRole == KTVSingRoleLeadSinger || self.singRole == KTVSingRoleSoloSinger){
                    [self syncChoruScore:[self.lrcControl getAvgScore]];
                    [self showScoreViewWithScore: [self.lrcControl getAvgScore]];
                    [self removeCurrentSong];
                }
            }
        }
        
        //Judge whether the accompaniment is in a suspended state
        if(self.singRole == KTVSingRoleCoSinger){
            self.isPause = (isLocal && state == AgoraMediaPlayerStatePaused);
        }
    });
}

- (void)onSingerRoleChangedWithOldRole:(enum KTVSingRole)oldRole newRole:(enum KTVSingRole)newRole {
    if(oldRole == newRole){
        KTVLogInfo(@"old role:%li is equal to new role", oldRole);
    }
    
    KTVLogInfo(@"onSingerRoleChangedWithOldRole oldRole: %ld, newRole: %ld", oldRole, newRole);
    self.singRole = newRole;
}

- (void)onSingingScoreResultWithScore:(float)score {
}

- (void)onTokenPrivilegeWillExpire {
    
}

- (void)onMusicPlayerProgressChangedWith:(NSInteger)progress {
    
}


#pragma mark KTVMusicLoadStateListener

- (void)onMusicLoadProgressWithSongCode:(NSInteger)songCode
                                percent:(NSInteger)percent
                                  state:(AgoraMusicContentCenterPreloadState)state
                                    msg:(NSString *)msg
                               lyricUrl:(NSString *)lyricUrl {
    dispatch_async_on_main_queue(^{
        VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
        if (![model.songNo isEqualToString:[NSString stringWithFormat:@"%ld", songCode]]) {
            KTVLogInfo(@"onMusicLoadProgressWithSongCode break songCode missmatch %@/%ld percent: %ld", model.songNo, songCode, percent);
            return;
        }
//        KTVLogInfo(@"onMusicLoadProgressWithSongCode songCode %@/%ld percent: %ld", model.songNo, songCode, percent);
        if(state == AgoraMusicContentCenterPreloadStateError){
            [VLToast toast:KTVLocalizedString(@"ktv_load_failed_and_change")];
            if(self.loadMusicCallBack) {
                self.loadMusicCallBack(NO, songCode);
                self.loadMusicCallBack = nil;
            }
            return;
        }
        
        if (state == AgoraMusicContentCenterPreloadStateOK){
        }
        
        VLRoomSelSongModel *topSong = self.selSongsArray.firstObject;
        
        if(self.singRole == KTVSingRoleSoloSinger
           || self.singRole == KTVSingRoleLeadSinger
           || self.isJoinChorus
           || [topSong.owner.userId isEqualToString:VLUserCenter.user.id]){
            self.MVView.loadingProgress = percent;
        }
    });
}

- (void)onMusicLoadFailWithSongCode:(NSInteger)songCode reason:(enum KTVLoadSongFailReason)reason{
    dispatch_async_on_main_queue(^{
        KTVLogError(@"onMusicLoadFail songCode: %ld reason: %ld", songCode, reason);
        VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
        if (![model.songNo isEqualToString:[NSString stringWithFormat:@"%ld", songCode]]) {
            KTVLogInfo(@"onMusicLoadFail break songCode missmatch %@/%ld", model.songNo, songCode);
            return;
        }
        self.MVView.loadingProgress = 100;
        if (reason == KTVLoadSongFailReasonNoLyricUrl) {
            [self.MVView setMvState:[self isRoomOwner] ? VLKTVMVViewStateMusicOwnerLoadLrcFailed : VLKTVMVViewStateMusicLoadLrcFailed];
        } else {
            BOOL isOwner = [self isRoomOwner] || [AppContext isKtvSongOwnerWithUserId:VLUserCenter.user.id];
            [self.MVView setMvState:isOwner ? VLKTVMVViewStateMusicOwnerLoadFailed : VLKTVMVViewStateMusicLoadFailed];
        }
        if(self.loadMusicCallBack) {
            self.loadMusicCallBack(NO, songCode);
            self.loadMusicCallBack = nil;
        }
    });
}

- (void)onMusicLoadSuccessWithSongCode:(NSInteger)songCode lyricUrl:(NSString * _Nonnull)lyricUrl {
    dispatch_async_on_main_queue(^{
        KTVLogInfo(@"onMusicLoadSuccess songCode: %ld, lyricUrl: %@", songCode, lyricUrl);
        VLRoomSelSongModel* model = [[self selSongsArray] firstObject];
        if (![model.songNo isEqualToString:[NSString stringWithFormat:@"%ld", songCode]]) {
            KTVLogInfo(@"onMusicLoadSuccess break songCode missmatch %@/%ld", model.songNo, songCode);
            return;
        }
        if(self.loadMusicCallBack){
            self.loadMusicCallBack(YES, songCode);
            self.loadMusicCallBack = nil;
            [self.MVView.incentiveView reset];
        }
        
        self.MVView.loadingProgress = 100;
        if(lyricUrl.length > 0){
            KTVLogInfo(@"onMusicLoadSuccessWithSongCode: %ld role:%ld", songCode, self.singRole);
        }
        self.retryCount = 0;
    });
}


#pragma mark KTVServiceListenerProtocol
    
- (void)onRoomDidDestroy {
    KTVLogInfo(@"onRoomDidDestroy");
    [self leaveRTCChannel];
    BOOL isOwner = [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
    //The owner closes the room.
    if (isOwner) {
        kWeakSelf(self);
        NSString *mes = KTVLocalizedString(@"ktv_room_exit");
        [[VLKTVAlert shared]showKTVToastWithFrame: UIScreen.mainScreen.bounds
                                            image: [UIImage ktv_sceneImageWithName:@"empty" ]
                                          message: mes
                                      buttonTitle: KTVLocalizedString(@"ktv_gotit")
                                       completion: ^(bool flag, NSString * _Nullable text) {
            [[VLKTVAlert shared]dismiss];
            [weakself leaveRoom];
        }];
        return;
    }
    
    [self popForceLeaveRoom];
}

- (void)onRoomDidExpire {
    KTVLogInfo(@"onRoomDidExpire");
    [self leaveRTCChannel];
    BOOL isOwner = [self.roomModel.creatorNo isEqualToString:VLUserCenter.user.id];
    NSString *mes = isOwner ? KTVLocalizedString(@"ktv_room_timeout") : KTVLocalizedString(@"ktv_room_offline");
    kWeakSelf(self);
    [[VLKTVAlert shared]showKTVToastWithFrame: UIScreen.mainScreen.bounds
                                        image: [UIImage ktv_sceneImageWithName:@"empty" ]
                                      message: mes buttonTitle:KTVLocalizedString(@"ktv_confirm")
                                   completion: ^(bool flag, NSString * _Nullable text) {
        [[VLKTVAlert shared]dismiss];
        [weakself leaveRoom];
    }];
}
    
    
- (void)onUserCountUpdateWithUserCount:(NSUInteger)userCount {
    [self setRoomUsersCount:userCount];
}
    
- (void)onMicSeatSnapshotWithSeat:(NSDictionary<NSString *,VLRoomSeatModel *> *)seat {
    NSMutableArray<VLRoomSeatModel*>* setsArray = [NSMutableArray array];
    for (int i = 0; i < seat.count; i++) {
        VLRoomSeatModel* model = [seat objectForKey:[NSString stringWithFormat:@"%d", i]];
        if ([model.owner.userId isEqualToString:VLUserCenter.user.id]) {
            [self updateNowMicMuted:model.isAudioMuted];
            [self updateNowCameraMuted:model.isVideoMuted];
        }
        [setsArray addObject:model];
    }
    [self setSeatsArray:setsArray];
    [self.roomPersonView reloadData];
}
    
- (void)onUserEnterSeatWithSeatIndex:(NSInteger)seatIndex user:(SyncUserThumbnailInfo *)user {
    KTVLogInfo(@"onUserEnterSeatWithSeatIndex: seatIndex:%ld userId: %@", seatIndex, user.userId);
    VLRoomSeatModel* model = [self getUserSeatInfoWithIndex:seatIndex];
    if (model == nil) {
        return;
    }
    
    //Message on the microphone / Whether to turn on the video / Whether to mute
    model.owner = user;
    [self setSeatsArray:self.seatsArray];
    
    VLRoomSelSongModel *song = self.selSongsArray.firstObject;
    if(!self.isJoinChorus){
        [self setMVViewStateWith:song];
    }
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
    
    [self onSeatFull];
}

- (void)onUserLeaveSeatWithSeatIndex:(NSInteger)seatIndex user:(SyncUserThumbnailInfo *)user {
    KTVLogInfo(@"onUserLeaveSeatWithSeatIndex: seatIndex:%ld userId: %@", seatIndex, user.userId);
    VLRoomSeatModel* model = [self getUserSeatInfoWithIndex:seatIndex];
    if (model == nil) {
        return;
    }
    
    // Messages from the microphone
    // Lower the microphone to reset the placement model
    model.owner = [SyncUserThumbnailInfo new];
    [self setSeatsArray:self.seatsArray];
    
    VLRoomSelSongModel *song = self.selSongsArray.firstObject;
    if(!self.isJoinChorus){
        [self setMVViewStateWith:song];
    }
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
    [self onSeatFull];
}

- (void)updateNowMicMuted:(BOOL)isMute {
    self.isNowMicMuted = isMute;
    // The switch microphone will check the ear return status and turn it off temporarily.
    if(self.isEarOn){
        [self.RTCkit enableInEarMonitoring:!self.isNowMicMuted includeAudioFilters:AgoraEarMonitoringFilterNone];
    }
    if (!isMute) {
        [AgoraEntAuthorizedManager checkAudioAuthorizedWithParent:self completion:nil];
    }
}
  
- (void)onSeatAudioMuteWithSeatIndex:(NSInteger)seatIndex isMute:(BOOL)isMute {
    KTVLogInfo(@"onSeatAudioMuteWithSeatIndex: seatIndex:%ld isMute: %d", seatIndex, isMute);
    VLRoomSeatModel* model = [self getUserSeatInfoWithIndex:seatIndex];
    if (model == nil) {
        return;
    }
    
    if ([model.owner.userId isEqualToString:VLUserCenter.user.id]) {
        [self updateNowMicMuted:isMute];
    }
    
    model.isAudioMuted = isMute;
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
}

- (void)onUserSeatUpdateWithSeat:(VLRoomSeatModel *)seat {
    
}


- (void)updateNowCameraMuted: (BOOL)isMute {
    self.isNowCameraMuted = isMute;
    if (!isMute) {
        [AgoraEntAuthorizedManager checkCameraAuthorizedWithParent:self completion:nil];
    }
}
    
- (void)onSeatVideoMuteWithSeatIndex:(NSInteger)seatIndex isMute:(BOOL)isMute {
    KTVLogInfo(@"onSeatVideoMuteWithSeatIndex: seatIndex:%ld isMute: %d", seatIndex, isMute);
    VLRoomSeatModel* model = [self getUserSeatInfoWithIndex:seatIndex];
    if (model == nil) {
        return;
    }
    if ([model.owner.userId isEqualToString:VLUserCenter.user.id]) {
        [self updateNowCameraMuted:isMute];
    }
    
    model.isVideoMuted = isMute;
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
}

- (void)onChosenSongListDidChangedWithSongs:(NSArray<VLRoomSelSongModel *> *)songs {
    [self _checkInEarMonitoring];
    NSString* origTopSongNo = NullToString(self.selSongsArray.firstObject.songNo);
    NSString* currentTopSongNo = NullToString(songs.firstObject.songNo);
    if (![origTopSongNo isEqualToString:currentTopSongNo]) {
        KTVLogInfo(@"clean old song: %@", origTopSongNo);
        if ([self.ktvApi isSongLoadingWithSongCode:origTopSongNo]) {
            [self.ktvApi removeMusicWithSongCode:[origTopSongNo integerValue]];
        }
        [self stopPlaySong];
        self.coSingerDegree = 0;
        [LSTPopView removeAllPopView];
    }
    self.selSongsArray = [NSMutableArray arrayWithArray:songs];
}

- (void)onChoristerDidEnterWithChorister:(KTVChoristerModel *)chorister {
    KTVLogInfo(@"onChoristerDidEnterWithChorister: %@", chorister.userId);
    VLRoomSeatModel* model = [self getUserSeatInfoWithUserId:chorister.userId];
    if (model == nil) {
        return;
    }
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
    self.chorusNum = [self getChorusNumWithSeatArray:self.seatsArray];
}

- (void)onChoristerDidLeaveWithChorister:(KTVChoristerModel *)chorister {
    KTVLogInfo(@"onChoristerDidLeaveWithChorister: %@", chorister.userId);
    VLRoomSeatModel* model = [self getUserSeatInfoWithUserId:chorister.userId];
    if (model == nil) {
        return;
    }
    [self.roomPersonView reloadSeatIndex:model.seatIndex];
    self.chorusNum = [self getChorusNumWithSeatArray:self.seatsArray];
    
    if ([chorister.userId isEqualToString:VLUserCenter.user.id]) {
        [self stopPlaySong];
        self.isNowMicMuted = true;
        [self.MVView.incentiveView reset];
        [self.MVView setOriginBtnState: VLKTVMVViewActionTypeSingAcc];
        [[AppContext ktvServiceImp] updateSeatAudioMuteStatusWithMuted:YES
                                                            completion:^(NSError * error) {
        }];
    }
}

@end




