//
//  UIDevice+Hardware.h
//  TestTable
//
//  Created by Inder Kumar Rathore on 19/01/13.
//  Copyright (c) 2013 Rathore. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface UIDevice (JKHardware)
+ (NSString *)jk_platform;
+ (NSString *)jk_platformString;


+ (NSString *)jk_macAddress;

//Return the current device CPU frequency
+ (NSUInteger)jk_cpuFrequency;
// Return the current device BUS frequency
+ (NSUInteger)jk_busFrequency;
//current device RAM size
+ (NSUInteger)jk_ramSize;
//Return the current device CPU number
+ (NSUInteger)jk_cpuNumber;
//Return the current device total memory

/// Obtain the iOS version number
+ (NSString *)jk_systemVersion;
/// Check whether the current system has a camera
+ (BOOL)jk_hasCamera;
/// Gets the total memory of the phone, and returns the number of bytes
+ (NSUInteger)jk_totalMemoryBytes;
/// Gets the phone's available memory, and returns the number of bytes
+ (NSUInteger)jk_freeMemoryBytes;

/// Gets the free space on the phone's hard drive and returns the number of bytes
+ (long long)jk_freeDiskSpaceBytes;
/// Gets the total space of the phone's hard drive, and returns the number of bytes
+ (long long)jk_totalDiskSpaceBytes;
@end
