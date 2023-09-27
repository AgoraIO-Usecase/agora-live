//
//  VLHotSpotBtn.m
//  VoiceOnLine
//

#import "VLHotSpotBtn.h"

@implementation VLHotSpotBtn

- (BOOL)pointInside:(CGPoint)point withEvent:(UIEvent*)event
{
    CGRect bounds = self.bounds;
    // If the original hot area is less than 44x44, enlarge the hot area, otherwise keep the original size unchanged
    CGFloat widthDelta = MAX(44.0 - bounds.size.width, 0);
    CGFloat heightDelta = MAX(44.0 - bounds.size.height, 0);
    bounds = CGRectInset(bounds, -0.5 * widthDelta, -0.5 * heightDelta);
    return CGRectContainsPoint(bounds, point);
}

@end
