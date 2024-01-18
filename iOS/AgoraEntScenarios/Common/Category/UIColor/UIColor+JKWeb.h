//
//  UIColor+Web.h
//  JKCategories (https://github.com/shaojiankui/JKCategories)
//
//  Created by Jakey on 14/12/22.
// Copyright (c) 2014 www.skyfox.org. All rights reserved
//

#import <UIKit/UIKit.h>

@interface UIColor (JKWeb)
/**
* @brief gets the color string for canvas
*
* @return canvas color
*/
- (NSString *)jk_canvasColorString;
/**
 *  @brief gets the web color string
 *
 * @return page color
 */
- (NSString *)jk_webColorString;
@end
