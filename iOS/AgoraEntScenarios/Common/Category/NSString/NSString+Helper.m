//
//  NSString+Helper.m
//  dajiaochong
//
//  Created by kidstone_test on 16/4/20.
// Copyright ©  2016 Wang Chunjing All rights reserved
//

#import "NSString+Helper.h"
#import <sys/sysctl.h>
#import <sys/utsname.h>
#import <CommonCrypto/CommonCryptor.h>
#import <CommonCrypto/CommonDigest.h> // Need to import for CC_MD5 access
#import "VLFontUtils.h"
#import "QMUICommonDefines.h"

@import UIKit;

#define     LocalStr_None           @""
static const char encodingTable[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

@implementation NSString(Helper)

- (NSString *)md5 {
    const char *cStr = [self UTF8String];
    unsigned char result[16];
    CC_MD5( cStr, (unsigned int)strlen(cStr), result ); // This is the md5 call
    return [NSString stringWithFormat:
            @"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
            result[0], result[1], result[2], result[3],
            result[4], result[5], result[6], result[7],
            result[8], result[9], result[10], result[11],
            result[12], result[13], result[14], result[15]
            ];
}
+ (NSString *)base64StringFromText:(NSString *)text {
    if (text && ![text isEqualToString:LocalStr_None]) {
        // Take the project's bundleIdentifier as the KEY
        
        NSString *key = [[NSBundle mainBundle] bundleIdentifier];
        NSData *data = [text dataUsingEncoding:NSUTF8StringEncoding];
        //IOS Comes with DES encryption Begin
        
        data = [self DESEncrypt:data WithKey:key];
        //IOS Comes with DES encryption End
        
        return [self base64EncodedStringFrom:data];
    }
    else {
        return LocalStr_None;
    }  
}
+ (NSData *)DESEncrypt:(NSData *)data WithKey:(NSString *)key {
    char keyPtr[kCCKeySizeAES256+1];
    bzero(keyPtr, sizeof(keyPtr));
    
    [key getCString:keyPtr maxLength:sizeof(keyPtr) encoding:NSUTF8StringEncoding];
    
    NSUInteger dataLength = [data length];
    
    size_t bufferSize = dataLength + kCCBlockSizeAES128;
    void *buffer = malloc(bufferSize);
    
    size_t numBytesEncrypted = 0;
    CCCryptorStatus cryptStatus = CCCrypt(kCCEncrypt, kCCAlgorithmDES,
                                          kCCOptionPKCS7Padding | kCCOptionECBMode,
                                          keyPtr, kCCBlockSizeDES,
                                          NULL,
                                          [data bytes], dataLength,
                                          buffer, bufferSize,
                                          &numBytesEncrypted);
    if (cryptStatus == kCCSuccess) {
        return [NSData dataWithBytesNoCopy:buffer length:numBytesEncrypted];
    }
    
    free(buffer);
    return nil;
}
+ (NSString *)base64EncodedStringFrom:(NSData *)data {
    if ([data length] == 0)
        return @"";
    
    char *characters = malloc((([data length] + 2) / 3) * 4);
    if (characters == NULL)
        return nil;
    NSUInteger length = 0;
    
    NSUInteger i = 0;
    while (i < [data length]) {
        char buffer[3] = {0,0,0};
        short bufferLength = 0;
        while (bufferLength < 3 && i < [data length])
            buffer[bufferLength++] = ((char *)[data bytes])[i++];
        
        //  Encode the bytes in the buffer to four characters, including padding "=" characters if necessary.
        
        characters[length++] = encodingTable[(buffer[0] & 0xFC) >> 2];
        characters[length++] = encodingTable[((buffer[0] & 0x03) << 4) | ((buffer[1] & 0xF0) >> 4)];
        if (bufferLength > 1)
            characters[length++] = encodingTable[((buffer[1] & 0x0F) << 2) | ((buffer[2] & 0xC0) >> 6)];
        else characters[length++] = '=';
        if (bufferLength > 2)
            characters[length++] = encodingTable[buffer[2] & 0x3F];
        else characters[length++] = '=';
    }
    
    return [[NSString alloc] initWithBytesNoCopy:characters length:length encoding:NSASCIIStringEncoding freeWhenDone:YES];
}


#pragma mark - Determines whether the string is empty
+ (BOOL)isBlankString:(NSString *)string {
    if (string == nil || string == NULL) {
        return YES;
    }
    
    if ([string isKindOfClass:[NSNull class]]) {
        return YES;
    }
    
    if ([[string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] length] == 0) {
        return YES;
    }
    
    if ([string isEqualToString:@"(null)"] || [string isEqualToString:@"null"]) {
        return YES;
    }
    return NO;
}
#pragma mark Account and password local detection
// No mobile phone number
+ (BOOL)isValidateTelNumber:(NSString *)number {
    // Mobile phone number regular expression
    // NSString *strRegex = @"^1+[3578]+\\d{9}";
    NSString *strRegex = @"^1+\\d{10}";
    BOOL rt = [self isValidateRegularExpression:number byExpression:strRegex];
    if (rt == YES) {
        
        if (number.length < 11 || number.length > 11) {
            rt = NO;
            return rt;
        } else {
            
            if ([number hasPrefix:@"0"]) {
                rt = NO;
                return rt;
            }
        }
    }
    
    return rt;
}

+ (BOOL)isValidateRegularExpression:(NSString *)strDestination byExpression:(NSString *)strExpression {
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"SELF MATCHES %@",strExpression];
    return [predicate evaluateWithObject:strDestination];
}

// The email format is correct
+ (BOOL)checkEmailIsValue:(NSString *)emailStr {
    NSString *emailRegex = @"[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
    NSPredicate *emailTest = [NSPredicate predicateWithFormat:@"SELF MATCHES%@",emailRegex];
    return [emailTest evaluateWithObject:emailStr];
}

// The password format is correct
+ (BOOL)checkPassWordIsValue:(NSString *)passWordStr {
    if (passWordStr.length >= 6 && passWordStr.length <= 18) {
        for (int i = 0; i < passWordStr.length; i++) {
            unichar charStr = [passWordStr characterAtIndex:i];
            if (charStr < 48 || (charStr > 57 && charStr < 65) || (charStr > 90 && charStr < 97) || charStr > 122) {
                return NO;
            }
            
        }
        return YES;
    }
    return NO;
}
+ (NSInteger)getNumberFromString:(NSString *)string {
    NSCharacterSet * set =[[NSCharacterSet decimalDigitCharacterSet] invertedSet];
    NSInteger second = [[string stringByTrimmingCharactersInSet:set] integerValue];
    return second;
}
+ (NSString *)intervalWithTimeString:(NSString *)timeStr {
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    [dateFormatter setLocale:[[NSLocale alloc]initWithLocaleIdentifier:@"zh_CN"]];
    NSDate *currentDate = [dateFormatter dateFromString:timeStr];
    return [NSString stringWithFormat:@"%.0f",[currentDate timeIntervalSince1970]*1000];
}

+ (NSString*)getCurrentTimes {

    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"YYYY-MM-dd HH:mm:ss"];
    NSDate *datenow = [NSDate date];
    NSString *currentTimeString = [formatter stringFromDate:datenow];
    return currentTimeString;
}
+ (NSString *)timeFormatted:(int)totalSeconds {

    int seconds = totalSeconds % 60;
    int minutes = (totalSeconds / 60) % 60;
    int hours = totalSeconds / 3600;
    return [NSString stringWithFormat:@"%02d:%02d:%02d",hours, minutes, seconds];
}

+ (NSString *)vj_timeInterval:(NSString *)start {
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"YYYY-MM-dd HH:mm:ss"];
    NSDate *startDate = [dateFormatter dateFromString:start];
    NSDate *endDate = [dateFormatter dateFromString:[self getCurrentTimes]];
    NSCalendar *calendar = [NSCalendar currentCalendar];
    /**
     * The time unit to be compared is commonly as follows and can be transmitted simultaneously:
     * NSCalendarUnitDay: day
     * NSCalendarUnitYear: year
     * NSCalendarUnitMonth: indicates the month
     * NSCalendarUnitHour: hour
     * NSCalendarUnitMinute: indicates the minute
     * NSCalendarUnitSecond: second
     */
    
    // Day interval
    NSCalendarUnit dayUnit = NSCalendarUnitDay;
    NSDateComponents *dayDelta = [calendar components:dayUnit fromDate:startDate toDate:endDate options:0];
    if (dayDelta.day >= 1) {
        return [NSString stringWithFormat:@"%ld days ago",dayDelta.day];
    }
    
    // Hour interval
    NSCalendarUnit hourUnit = NSCalendarUnitHour;
    NSDateComponents *hourDelta = [calendar components:hourUnit fromDate:startDate toDate:endDate options:0];
    if (hourDelta.hour >= 1) {
        return [NSString stringWithFormat:@"%ld Hours ago",hourDelta.hour];
    }
    
    // Minute interval
    NSCalendarUnit minUnit = NSCalendarUnitMinute;
    NSDateComponents *minDelta = [calendar components:minUnit fromDate:startDate toDate:endDate options:0];
    if (minDelta.minute >= 1) {
        return [NSString stringWithFormat:@"%ld minutes ago",minDelta.minute];
    }
    return @"Now";
}
+ (NSString *)getCurrentDeviceModel {
    struct utsname systemInfo;
    uname(&systemInfo);
    
    NSString *deviceModel = @"iPhone";
    deviceModel = [NSString stringWithCString:systemInfo.machine encoding:NSASCIIStringEncoding];
    
    if ([deviceModel isEqualToString:@"iPhone3,1"])    return @"iPhone 4";
    else if ([deviceModel isEqualToString:@"iPhone3,2"])    return @"iPhone4";
    else if ([deviceModel isEqualToString:@"iPhone3,3"])    return @"iPhone4";
    else if ([deviceModel isEqualToString:@"iPhone4,1"])    return @"iPhone4S";
    else if ([deviceModel isEqualToString:@"iPhone5,1"])    return @"iPhone5";
    else if ([deviceModel isEqualToString:@"iPhone5,2"])    return @"iPhone5 (GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPhone5,3"])    return @"iPhone5c (GSM)";
    else if ([deviceModel isEqualToString:@"iPhone5,4"])    return @"iPhone5c(GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPhone6,1"])    return @"iPhone5s(GSM)";
    else if ([deviceModel isEqualToString:@"iPhone6,2"])    return @"iPhone5s(GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPhone7,1"])    return @"iPhone6Plus";
    else if ([deviceModel isEqualToString:@"iPhone7,2"])    return @"iPhone6";
    else if ([deviceModel isEqualToString:@"iPhone8,1"])    return @"iPhone6s";
    else if ([deviceModel isEqualToString:@"iPhone8,2"])    return @"iPhone6sPlus";
    else if ([deviceModel isEqualToString:@"iPhone8,4"])    return @"iPhoneSE";
    else if ([deviceModel isEqualToString:@"iPhone9,1"])    return @"iPhone7";
    else if ([deviceModel isEqualToString:@"iPhone9,2"])    return @"iPhone7Plus";
    else if ([deviceModel isEqualToString:@"iPhone9,3"])    return @"iPhone7";
    else if ([deviceModel isEqualToString:@"iPhone9,4"])    return @"iPhone7Plus";
    else if ([deviceModel isEqualToString:@"iPhone10,1"])   return @"iPhone_8";
    else if ([deviceModel isEqualToString:@"iPhone10,4"])   return @"iPhone_8";
    else if ([deviceModel isEqualToString:@"iPhone10,2"])   return @"iPhone_8_Plus";
    else if ([deviceModel isEqualToString:@"iPhone10,5"])   return @"iPhone_8_Plus";
    else if ([deviceModel isEqualToString:@"iPhone10,3"])   return @"iPhoneX";
    else if ([deviceModel isEqualToString:@"iPhone10,6"])   return @"iPhoneX";
    else if ([deviceModel isEqualToString:@"iPhone11,8"])   return @"iPhoneXR";
    else if ([deviceModel isEqualToString:@"iPhone11,2"])   return @"iPhoneXS";
    else if ([deviceModel isEqualToString:@"iPhone11,6"])   return @"iPhoneXSMax";
    else if ([deviceModel isEqualToString:@"iPhone11,4"])   return @"iPhoneXSMax";
    else if ([deviceModel isEqualToString:@"iPhone12,1"])   return @"iPhone 11";
    else if ([deviceModel isEqualToString:@"iPhone12,3"])   return @"iPhone11Pro";
    else if ([deviceModel isEqualToString:@"iPhone12,5"])   return @"iPhone 11 Pro Max";
    else if ([deviceModel isEqualToString:@"iPhone12,8"])   return @"iPhoneSE(2nd generation)";
    else if ([deviceModel isEqualToString:@"iPhone13,1"])   return @"iPhone 12 mini";
    else if ([deviceModel isEqualToString:@"iPhone13,2"])   return @"iPhone 12";
    else if ([deviceModel isEqualToString:@"iPhone13,3"])   return @"iPhone 12 Pro";
    else if ([deviceModel isEqualToString:@"iPhone13,4"])   return @"iPhone 12 Pro Max";
    else if ([deviceModel isEqualToString:@"iPod1,1"])      return @"iPodTouch1G";
    else if ([deviceModel isEqualToString:@"iPod2,1"])      return @"iPodTouch2G";
    else if ([deviceModel isEqualToString:@"iPod3,1"])      return @"iPodTouch3G";
    else if ([deviceModel isEqualToString:@"iPod4,1"])      return @"iPodTouch4G";
    else if ([deviceModel isEqualToString:@"iPod5,1"])      return @"iPodTouch(5 Gen)";
    else if ([deviceModel isEqualToString:@"iPad1,1"])      return @"iPad";
    else if ([deviceModel isEqualToString:@"iPad1,2"])      return @"iPad3G";
    else if ([deviceModel isEqualToString:@"iPad2,1"])      return @"iPad2(WiFi)";
    else if ([deviceModel isEqualToString:@"iPad2,2"])      return @"iPad2";
    else if ([deviceModel isEqualToString:@"iPad2,3"])      return @"iPad2(CDMA)";
    else if ([deviceModel isEqualToString:@"iPad2,4"])      return @"iPad2";
    else if ([deviceModel isEqualToString:@"iPad2,5"])      return @"iPadMini(WiFi)";
    else if ([deviceModel isEqualToString:@"iPad2,6"])      return @"iPadMini";
    else if ([deviceModel isEqualToString:@"iPad2,7"])      return @"iPad Mini (GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPad3,1"])      return @"iPad 3 (WiFi)";
    else if ([deviceModel isEqualToString:@"iPad3,2"])      return @"iPad 3 (GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPad3,3"])      return @"iPad 3";
    else if ([deviceModel isEqualToString:@"iPad3,4"])      return @"iPad 4 (WiFi)";
    else if ([deviceModel isEqualToString:@"iPad3,5"])      return @"iPad 4";
    else if ([deviceModel isEqualToString:@"iPad3,6"])      return @"iPad 4 (GSM+CDMA)";
    else if ([deviceModel isEqualToString:@"iPad4,1"])      return @"iPad Air (WiFi)";
    else if ([deviceModel isEqualToString:@"iPad4,2"])      return @"iPad Air (Cellular)";
    else if ([deviceModel isEqualToString:@"iPad4,4"])      return @"iPad Mini 2 (WiFi)";
    else if ([deviceModel isEqualToString:@"iPad4,5"])      return @"iPad Mini 2 (Cellular)";
    else if ([deviceModel isEqualToString:@"iPad4,6"])      return @"iPad Mini 2";
    else if ([deviceModel isEqualToString:@"iPad4,7"])      return @"iPad Mini 3";
    else if ([deviceModel isEqualToString:@"iPad4,8"])      return @"iPad Mini 3";
    else if ([deviceModel isEqualToString:@"iPad4,9"])      return @"iPad Mini 3";
    else if ([deviceModel isEqualToString:@"iPad5,1"])      return @"iPad Mini 4 (WiFi)";
    else if ([deviceModel isEqualToString:@"iPad5,2"])      return @"iPad Mini 4 (LTE)";
    else if ([deviceModel isEqualToString:@"iPad5,3"])      return @"iPad Air 2";
    else if ([deviceModel isEqualToString:@"iPad5,4"])      return @"iPad Air 2";
    else if ([deviceModel isEqualToString:@"iPad6,3"])      return @"iPad Pro 9.7";
    else if ([deviceModel isEqualToString:@"iPad6,4"])      return @"iPad Pro 9.7";
    else if ([deviceModel isEqualToString:@"iPad6,7"])      return @"iPad Pro 12.9";
    else if ([deviceModel isEqualToString:@"iPad6,8"])      return @"iPad Pro 12.9";
    
    else if ([deviceModel isEqualToString:@"AppleTV2,1"])      return @"Apple TV 2";
    else if ([deviceModel isEqualToString:@"AppleTV3,1"])      return @"Apple TV 3";
    else if ([deviceModel isEqualToString:@"AppleTV3,2"])      return @"Apple TV 3";
    else if ([deviceModel isEqualToString:@"AppleTV5,3"])      return @"Apple TV 4";
    
    else if ([deviceModel isEqualToString:@"i386"])         return @"Simulator";
    else if ([deviceModel isEqualToString:@"x86_64"])       return @"Simulator";
    return deviceModel;
}

+ (NSMutableAttributedString*)changeLabelWithText:(NSString*)needText {
    NSMutableAttributedString *attrString = [[NSMutableAttributedString alloc] initWithString:needText];
    UIFont *font = VLUIFontMediumMake(24);
    [attrString addAttribute:NSFontAttributeName value:font range:NSMakeRange(0,needText.length-1)];
    [attrString addAttribute:NSFontAttributeName value:UIFontMake(12) range:NSMakeRange(needText.length-1,1)];
    return attrString;
}

+ (NSString *)convertToJsonData:(NSDictionary *)dict {
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:&error];
    NSString *jsonString;
    if (!jsonData) {
        NSLog(@"%@",error);
    }else{
        jsonString = [[NSString alloc]initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
    NSMutableString *mutStr = [NSMutableString stringWithString:jsonString];
    NSRange range = {0,jsonString.length};
    [mutStr replaceOccurrencesOfString:@" " withString:@"" options:NSLiteralSearch range:range];
    NSRange range2 = {0,mutStr.length};
    [mutStr replaceOccurrencesOfString:@"\n" withString:@"" options:NSLiteralSearch range:range2];
    return mutStr;
}

+ (BOOL)stringContainsEmoji:(NSString *)string {
    __block BOOL returnValue = NO;
    [string enumerateSubstringsInRange:NSMakeRange(0, [string length]) options:NSStringEnumerationByComposedCharacterSequences usingBlock:
     ^(NSString *substring, NSRange substringRange, NSRange enclosingRange, BOOL *stop) {
         
         const unichar hs = [substring characterAtIndex:0];
         // surrogate pair
         if (0xd800 <= hs && hs <= 0xdbff) {
             if (substring.length > 1) {
                 const unichar ls = [substring characterAtIndex:1];
                 const int uc = ((hs - 0xd800) * 0x400) + (ls - 0xdc00) + 0x10000;
                 if (0x1d000 <= uc && uc <= 0x1f77f) {
                     returnValue = YES;
                 }
             }
         } else if (substring.length > 1) {
             const unichar ls = [substring characterAtIndex:1];
             if (ls == 0x20e3) {
                 returnValue = YES;
             }
             
         } else {
             // non surrogate
             if (0x2100 <= hs && hs <= 0x27ff) {
                 returnValue = YES;
             } else if (0x2B05 <= hs && hs <= 0x2b07) {
                 returnValue = YES;
             } else if (0x2934 <= hs && hs <= 0x2935) {
                 returnValue = YES;
             } else if (0x3297 <= hs && hs <= 0x3299) {
                 returnValue = YES;
             } else if (hs == 0xa9 || hs == 0xae || hs == 0x303d || hs == 0x3030 || hs == 0x2b55 || hs == 0x2b1c || hs == 0x2b1b || hs == 0x2b50) {
                 returnValue = YES;
             }else if (hs == 0x200d){
                 returnValue = YES;
             }
         }
     }];
    
    return returnValue;
}
/**
 To determine whether it is a nine house
 @param string Specifies the entered character
 @return YES(it's a nine-gong pinyin keyboard)
*/
+ (BOOL)isNineKeyBoard:(NSString *)string
{
   NSString *other = @"➋➌➍➎➏➐➑➒";
   int len = (int)string.length;
   for(int i=0;i<len;i++)
   {
       if(!([other rangeOfString:string].location != NSNotFound))
           return NO;
   }
   return YES;
}

/**
 * Determines if an emoji is present in the string
 * @param string The character string
 * @return YES(with emojis)
*/
+ (BOOL)hasEmoji:(NSString*)string
{
   NSString *pattern = @"[^\\u0020-\\u007E\\u00A0-\\u00BE\\u2E80-\\uA4CF\\uF900-\\uFAFF\\uFE30-\\uFE4F\\uFF00-\\uFFEF\\u0080-\\u009F\\u2000-\\u201f\r\n]";
   NSPredicate *pred = [NSPredicate predicateWithFormat:@"SELF MATCHES %@", pattern];
   BOOL isMatch = [pred evaluateWithObject:string];
   return isMatch;
}
// Determines whether the string input is purely numeric
+ (BOOL)isPureInt:(NSString *)string {
    NSScanner* scan = [NSScanner scannerWithString:string];
    int val;
    return [scan scanInt:&val] && [scan isAtEnd];
}


+ (BOOL)isLegalCharacter:(NSString *)string {
    NSString *regex = @"[|➋➌➍➎➏➐➑➒|0-9|a-zA-Z|\u4e00-\u9fa5|#\\-]+";
    BOOL isLegal = ([string rangeOfString:regex options:NSRegularExpressionSearch].length > 0);
    return isLegal;
}
+ (NSString *)decodeString:(NSString*)encodedString {
    NSString *decodedString  = [encodedString stringByRemovingPercentEncoding];
    return decodedString;
}

+ (NSString *)getNormalStringFilterHTMLString:(NSString *)htmlStr {
    NSString *normalStr = htmlStr.copy;
    if (!normalStr || normalStr.length == 0 || [normalStr isEqual:[NSNull null]]) return nil;
    NSRegularExpression *regularExpression=[NSRegularExpression regularExpressionWithPattern:@"<[^>]*>" options:NSRegularExpressionCaseInsensitive error:nil];
    normalStr = [regularExpression stringByReplacingMatchesInString:normalStr options:NSMatchingReportProgress range:NSMakeRange(0, normalStr.length) withTemplate:@""];
    NSRegularExpression *plExpression=[NSRegularExpression regularExpressionWithPattern:@"&[^;]+;" options:NSRegularExpressionCaseInsensitive error:nil];
    normalStr = [plExpression stringByReplacingMatchesInString:normalStr options:NSMatchingReportProgress range:NSMakeRange(0, normalStr.length) withTemplate:@""];
    NSRegularExpression *spaceExpression=[NSRegularExpression regularExpressionWithPattern:@"^\\s*|\\s*$" options:NSRegularExpressionCaseInsensitive error:nil];
    normalStr = [spaceExpression stringByReplacingMatchesInString:normalStr options:NSMatchingReportProgress range:NSMakeRange(0, normalStr.length) withTemplate:@""];

    return normalStr;
}


+ (NSString *)formatFloat:(float)f {
    NSString * stringNumber = [NSString stringWithFormat:@"%.1f", f];
    NSNumber * inNumber = @(stringNumber.floatValue);
    NSString * outNumber = [NSString stringWithFormat:@"%@",inNumber];
    return outNumber;
}

@end
