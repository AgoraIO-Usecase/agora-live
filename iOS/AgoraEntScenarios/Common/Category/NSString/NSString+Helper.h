//
//  NSString+Helper.h
//  dajiaochong
//
//  Created by kidstone_test on 16/4/20.
//  Copyright © 2016年 王春景. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString(Helper)
+ (NSString *)base64StringFromText:(NSString *)text;
+ (BOOL)isBlankString:(NSString *)string;

// Format the time to time stamp
+ (NSString *)intervalWithTimeString:(NSString *)timeStr;

- (NSString *)md5;

//Gets the current time
+ (NSString *)getCurrentTimes;

+ (NSString *)vj_timeInterval:(NSString *)start;

+ (NSInteger)getNumberFromString:(NSString *)string;

+ (NSString *)timeFormatted:(int)totalSeconds;

#pragma mark Account and password local detection
// No mobile phone number
+ (BOOL)isValidateTelNumber:(NSString *)number;

// The email format is correct
+ (BOOL)checkEmailIsValue:(NSString *)emailStr;

// The password format is correct
+ (BOOL)checkPassWordIsValue:(NSString *)passWordStr;

+ (NSString *)getCurrentDeviceModel;

+ (NSMutableAttributedString*)changeLabelWithText:(NSString*)needText;

+ (NSString *)convertToJsonData:(NSDictionary*)dict;

+ (BOOL)stringContainsEmoji:(NSString *)string;
+ (BOOL)isNineKeyBoard:(NSString *)string;
+ (BOOL)hasEmoji:(NSString*)string;

+ (BOOL)isPureInt:(NSString *)string;
+ (BOOL)isLegalCharacter:(NSString *)string;
+ (NSString *)decodeString:(NSString*)encodedString;
+ (NSString *)getNormalStringFilterHTMLString:(NSString *)htmlStr;
+ (NSString *)formatFloat:(float)f;
@end
