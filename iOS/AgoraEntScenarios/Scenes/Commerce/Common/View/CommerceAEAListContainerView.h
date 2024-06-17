//
//  AEAListContainerView.h
//  AgoraEditAvatar
//
//  Created by FanPengpeng on 2022/9/21.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class CommerceAEAListContainerView;

@protocol CommerceAEAListContainerViewDataSource <NSObject>

- (UIViewController *)listContainerView:(CommerceAEAListContainerView *)listContainerView viewControllerForIndex:(NSInteger) index;

@end

@interface CommerceAEAListContainerView : UIView

@property (weak, nonatomic, readonly) UIViewController *currentVC;

@property (weak, nonatomic) id<CommerceAEAListContainerViewDataSource> dataSource;

- (NSArray<UIViewController *> * __nullable)allLoadedViewControllers;

- (UIViewController *)viewControllerAtIndex:(NSInteger)index;


-(void)setSelectedIndex:(NSInteger)index;

@end

NS_ASSUME_NONNULL_END
