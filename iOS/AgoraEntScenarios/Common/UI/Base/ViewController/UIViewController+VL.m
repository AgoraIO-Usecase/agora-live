//
//  UIViewController+VL.m
//  VoiceOnLine
//

#import "UIViewController+VL.h"

@implementation UIViewController (VL)

+ (void)popGestureClose:(UIViewController *)VC
{
    // Disable the sideslip back gesture
    if ([VC.navigationController respondsToSelector:@selector(interactivePopGestureRecognizer)]) {
        for (UIGestureRecognizer *popGesture in VC.navigationController.interactivePopGestureRecognizer.view.gestureRecognizers) {
            popGesture.enabled = NO;
        }
    }
}

+ (void)popGestureOpen:(UIViewController *)VC
{
    // Enable the Slide-back gesture
    if ([VC.navigationController respondsToSelector:@selector(interactivePopGestureRecognizer)]) {
        for (UIGestureRecognizer *popGesture in VC.navigationController.interactivePopGestureRecognizer.view.gestureRecognizers) {
            popGesture.enabled = YES;
        }
    }
}


@end
