//
//  LSTPopView+KTVModal.h
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/10.
//

#import "LSTPopView.h"
#import "VLPopMoreSelView.h"
#import "VLPopSelBgView.h"
#import "VLDropOnLineView.h"
#import "VLAudioEffectPicker.h"
#import "VLBadNetWorkView.h"
#import "VLPopSongList.h"
#import "VLEffectView.h"
#import "VLKTVSettingView.h"
#import "VLVoicePerShowView.h"
#import "VLEarSettingView.h"
#import "VLDebugView.h"
NS_ASSUME_NONNULL_BEGIN

@interface LSTPopView (KTVModal)

+ (LSTPopView*)getPopViewWithCustomView:(UIView*)customView;

//Change the background of MV
+ (LSTPopView*)popSelMVBgViewWithParentView:(UIView*)parentView
                                    bgModel:(VLKTVSelBgModel*)bgModel
                               withDelegate:(id<VLPopSelBgViewDelegate>)delegate;

//Pop up more
+ (LSTPopView*)popSelMoreViewWithParentView: (UIView*)parentView
                               withDelegate:(id<VLPopMoreSelViewDelegate>)delegate;


//Pop-up microphone view
+ (LSTPopView*)popDropLineViewWithParentView:(UIView*)parentView
                               withSeatModel:(VLRoomSeatModel *)seatModel
                                withDelegate:(id<VLDropOnLineViewDelegate>)delegate;

//Pop up the beautiful sound view
+ (LSTPopView*)popBelcantoViewWithParentView:(UIView*)parentView
                           withBelcantoModel:(VLBelcantoModel *)belcantoModel
                                withDelegate:(id<VLAudioEffectPickerDelegate>)delegate;

//Pop-up song order view
+ (LSTPopView*)popUpChooseSongViewWithParentView:(UIView*)parentView
                                         isOwner: (BOOL)isOwner
                                        isChorus:(BOOL)isChorus
                                 chooseSongArray: (NSArray*)chooseSongArray
                                      withRoomNo:(NSString*)roomNo
                                    withDelegate:(id<VLPopSongListDelegate>)delegate;

//Pop-up sound effect
+ (LSTPopView*)popSetSoundEffectViewWithParentView:(UIView*)parentView
                                         soundView:(VLEffectView*)soundView
                                      withDelegate:(id<VLEffectViewDelegate>)delegate;

//Network difference view
+ (LSTPopView*)popBadNetWrokTipViewWithParentView:(UIView*)parentView
                                     withDelegate:(id<VLBadNetWorkViewDelegate>)delegate;


//Console
+ (LSTPopView*)popSettingViewWithParentView:(UIView*)parentView
                                    setting:(VLKTVSettingModel*)settingModel
                               settingView:(VLKTVSettingView*)settingView
                               withDelegate:(id<VLKTVSettingViewDelegate>)delegate;

+ (LSTPopView*)popVoicePerViewWithParentView:(UIView*)parentView
                              isProfessional: (BOOL) isProfessional
                                    aecState:(BOOL)state
                                    aecLevel:(NSInteger)level
                                     isDelay: (BOOL) isDelay
                                    volGrade:(NSInteger)volGrade
                                       grade:(NSInteger)grade
                                    isRoomOwner: (BOOL) isRoomOwner
                                         perView:(VLVoicePerShowView*)perView
                                      withDelegate:(id<VLVoicePerShowViewDelegate>)delegate;

//Pop-up ear back view
+ (LSTPopView*)popEarSettingViewWithParentView:(UIView*)parentView
                                   isEarOn:(BOOL)isEarOn
                                           vol:(CGFloat)vol
                                  withDelegate:(id<VLEarSettingViewViewDelegate>)delegate;

//Pop-up virtual sound card view
+ (LSTPopView*)popSoundCardViewWithParentView:(UIView*)parentView
                                    soundOpen:(BOOL)isOpen
                                    gainValue:(double)gain
                                    typeValue:(NSInteger)type
                                   effectType:(NSInteger)effect;

+ (LSTPopView*)popSoundCardViewWithParentView:(UIView*)parentView
                                soundCardView:(UIView *)soundCardView;

//Pop-up debugging view
+ (LSTPopView*)popDebugViewWithParentView:(UIView*)parentView
                                    channelName:(NSString *)name
                                   sdkVer:(NSString *)ver
                                   isDebugMode:(BOOL)isDebugMode
                             withDelegate:(id<VLDebugViewDelegate>)delegate;

@end

NS_ASSUME_NONNULL_END
