//
//  UIView+VL.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIView (VL)

- (void)vl_whenTouches:(NSUInteger)numberOfTouches tapped:(NSUInteger)numberOfTaps handler:(void (^)(void))block;

- (void)vl_whenTapped:(void (^)(void))block;

- (void)vl_whenDoubleTapped:(void (^)(void))block;

- (void)vl_eachSubview:(void (^)(UIView *subview))block;

/**
 Fillet corner
 To use automatic layouts, you need to use them in layoutsubviews
 @param radius Rounded dimension
 @param corner Position of rounded corners
 */
- (void)vl_radius:(CGFloat)radius corner:(UIRectCorner)corner;


@end

NS_ASSUME_NONNULL_END
