//
//  NSString+Emoji.h
//  Ulife_Service
//
//  Created by tcnj on 15/11/9.
//  Copyright © 2015年 UHouse. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (Emoji)
// ios Judge forbid entering emojis

// Convert the expression to a space
+ (NSString *)disable_emoji:(NSString *)text;

// Determines whether the string contains an expression
+ (BOOL)isContainsEmoji:(NSString *)string;
/// Determines whether a substring is contained
- (BOOL)qmui_includesString:(NSString *)string;

@end
