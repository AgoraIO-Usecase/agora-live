//
//  CommerceGradualTableView.h
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/19.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

// 渐进方向
typedef NS_OPTIONS(NSInteger, CommerceTableViewGradualDirection) {
    CommerceTableViewGradualDirectionTop                                         = 1 << 0, // top
    CommerceTableViewGradualDirectionBottom                                   = 1 <<  1,    // bottom
};

@interface CommerceGradualTableView : UITableView

+ (instancetype)gradualTableViewWithFrame:(CGRect)frame direction:(CommerceTableViewGradualDirection)direction gradualValue:(id)gradualValue;

- (void)change;

@end

NS_ASSUME_NONNULL_END
