//
//  UIColor+Gradient.h
//  JKCategories (https://github.com/shaojiankui/JKCategories)
//
//  Created by Jakey on 14/12/15.
// Copyright (c) 2014 www.skyfox.org. All rights reserved
//

#import <UIKit/UIKit.h>

@interface UIColor (JKGradient)
/**
 * @brief gradient color
 *
 * @param c1 Start color
 * @param c2 end color
 * @param height Gradient height
 *
 * @return gradient color
 */
+ (UIColor*)jk_gradientFromColor:(UIColor*)c1 toColor:(UIColor*)c2 withHeight:(int)height;
@end
