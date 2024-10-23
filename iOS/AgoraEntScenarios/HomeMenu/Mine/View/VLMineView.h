//
//  VLMineView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>
@class VLLoginModel;

NS_ASSUME_NONNULL_BEGIN

typedef enum : NSUInteger {
    VLMineViewClickTypeUserProtocol = 0,   // User agreement
    VLMineViewClickTypeAboutUS,            // About us
    VLMineViewClickTypeDebug               //  Debug mode
} VLMineViewClickType;

typedef enum : NSUInteger {
    VLMineViewUserClickTypeNickName = 0, // Click to modify profile picture
    VLMineViewUserClickTypeAvatar        // Click avatar
} VLMineViewUserClickType;

@protocol VLMineViewDelegate <NSObject>

- (void)mineViewDidCick:(VLMineViewClickType)type;

- (void)mineViewDidCickUser:(VLMineViewUserClickType)type;

@optional

@end

@interface VLMineView : UIView

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLMineViewDelegate>)delegate;

- (void)refreseUserInfo:(VLLoginModel *)loginModel;
- (void)refreseAvatar:(UIImage *)avatar;
- (void)refreseNickName:(NSString *)nickName;
- (void)refreshTableView;

@end

NS_ASSUME_NONNULL_END
