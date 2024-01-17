//
//  UIColor+Web.m
//  JKCategories (https://github.com/shaojiankui/JKCategories)
//
//  Created by Jakey on 14/12/22.
// Copyright (c) 2014 www.skyfox.org. All rights reserved
//

#import "UIColor+JKWeb.h"

@implementation UIColor (JKWeb)
/**
 *  @brief gets the color string for canvas
 *
 * @return canvas color
 */
- (NSString *)jk_canvasColorString
{
    CGFloat *arrRGBA = [self jk_getRGB];
    int r = arrRGBA[0] * 255;
    int g = arrRGBA[1] * 255;
    int b = arrRGBA[2] * 255;
    float a = arrRGBA[3];
    return [NSString stringWithFormat:@"rgba(%d,%d,%d,%f)", r, g, b, a];
}
/**
 * @brief gets the web color string
 *
 * @return page color
 */
- (NSString *)jk_webColorString
{
    CGFloat *arrRGBA = [self jk_getRGB];
    int r = arrRGBA[0] * 255;
    int g = arrRGBA[1] * 255;
    int b = arrRGBA[2] * 255;
    NSLog(@"%d,%d,%d", r, g, b);
    NSString *webColor = [NSString stringWithFormat:@"#%02X%02X%02X", r, g, b];
    return webColor;
}

- (CGFloat *) jk_getRGB{
    UIColor * uiColor = self;
    CGColorRef cgColor = [uiColor CGColor];
    int numComponents = (int)CGColorGetNumberOfComponents(cgColor);
    if (numComponents == 4){
        static CGFloat * components = Nil;
        components = (CGFloat *) CGColorGetComponents(cgColor);
        return (CGFloat *)components;
    } else { // Otherwise, black is returned by default
        static CGFloat components[4] = {0};
        CGFloat f = 0;
        // System colors for non-RGB Spaces are handled separately
        if ([uiColor isEqual:[UIColor whiteColor]]) {
            f = 1.0;
        } else if ([uiColor isEqual:[UIColor lightGrayColor]]) {
            f = 0.8;
        } else if ([uiColor isEqual:[UIColor grayColor]]) {
            f = 0.5;
        }
        components[0] = f;
        components[1] = f;
        components[2] = f;
        components[3] = 1.0;
        return (CGFloat *)components;
    }
}
@end
