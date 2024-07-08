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

@interface VLSettingAIAECSwitcherView : UIView

@property (nonatomic, weak) id <VLSettingAIAECSwitcherViewDelegate> delegate;

- (instancetype)initWithMax:(NSInteger)max min:(NSInteger)min title:(NSString *)title;

@property (nonatomic, assign) NSInteger value;

@end

NS_ASSUME_NONNULL_END
