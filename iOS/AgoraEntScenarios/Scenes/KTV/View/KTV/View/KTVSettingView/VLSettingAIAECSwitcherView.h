//
//  VLSRSwitcherView.h
//  VoiceOnLine
//

#import "VLKTVItemBaseView.h"
@class VLSettingAIAECSwitcherView;

NS_ASSUME_NONNULL_BEGIN

@protocol VLSettingAIAECSwitcherViewDelegate <NSObject>

- (void)aecSwitcherView:(VLSettingAIAECSwitcherView *)switcherView on:(BOOL)on;

- (void)aecSwitcherView:(VLSettingAIAECSwitcherView *)switcherView level:(NSInteger)level;

@end

@interface VLSettingAIAECSwitcherView : VLKTVItemBaseView

@property (nonatomic, weak) id <VLSettingAIAECSwitcherViewDelegate> delegate;

@property (nonatomic, assign) BOOL on;

- (instancetype)initWithMax:(CGFloat)max min:(CGFloat)min;

@property (nonatomic, assign) float value;

@end

NS_ASSUME_NONNULL_END
