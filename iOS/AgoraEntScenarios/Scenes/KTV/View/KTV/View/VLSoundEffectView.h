//
//  VLsoundEffectView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>

typedef enum : NSUInteger {
    VLKTVSoundEffectTypeHeFeng = 0,
    VLKTVSoundEffectTypeXiaoDiao,
    VLKTVSoundEffectTypeDaDiao,
    VLKTVSoundEffectTypeNone
} VLKTVSoundEffectType;

NS_ASSUME_NONNULL_BEGIN
@class VLSoundEffectView;
@protocol VLsoundEffectViewDelegate <NSObject>

@optional
- (void)soundEffectViewBackBtnActionWithView:(VLSoundEffectView*)view;
- (void)soundEffectItemClickAction:(VLKTVSoundEffectType)effectType;

@end

@interface VLSoundEffectView : UIView

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLsoundEffectViewDelegate>)delegate;

@end

NS_ASSUME_NONNULL_END
