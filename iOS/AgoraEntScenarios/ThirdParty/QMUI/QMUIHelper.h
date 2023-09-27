/**
 * Tencent is pleased to support the open source community by making QMUI_iOS available.
 * Copyright (C) 2016-2021 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  QMUIHelper.h
//  qmui
//
//  Created by QMUI Team on 14/10/25.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "QMUICommonDefines.h"

NS_ASSUME_NONNULL_BEGIN

// TODO: molice Delete the qmui_badgeCenterOffset series interface after it is discarded
extern const CGPoint QMUIBadgeInvalidateOffset;

@interface QMUIHelper : NSObject

@end

@interface QMUIHelper (Device)

/// iPhone12,5、iPad6,8
/// @NEW_DEVICE_CHECKER
@property(class, nonatomic, readonly) NSString *deviceModel;

@property(class, nonatomic, readonly) BOOL isIPad;
@property(class, nonatomic, readonly) BOOL isIPod;
@property(class, nonatomic, readonly) BOOL isIPhone;
@property(class, nonatomic, readonly) BOOL isSimulator;
@property(class, nonatomic, readonly) BOOL isMac;

/// Bangs with physical grooves or use a device like the Home Indicator
/// @NEW_DEVICE_CHECKER
@property(class, nonatomic, readonly) BOOL isNotchedScreen;

/// iPhone 12 Pro Max
@property(class, nonatomic, readonly) BOOL is67InchScreen;

/// iPhone XS Max / 11 Pro Max
@property(class, nonatomic, readonly) BOOL is65InchScreen;

/// iPhone 12 / 12 Pro
@property(class, nonatomic, readonly) BOOL is61InchScreenAndiPhone12Later;

/// iPhone XR / 11
@property(class, nonatomic, readonly) BOOL is61InchScreen;

/// iPhone X / XS / 11Pro
@property(class, nonatomic, readonly) BOOL is58InchScreen;

/// iPhone 8 Plus
@property(class, nonatomic, readonly) BOOL is55InchScreen;

/// iPhone 12 mini
@property(class, nonatomic, readonly) BOOL is54InchScreen;

/// iPhone 8
@property(class, nonatomic, readonly) BOOL is47InchScreen;

/// iPhone 5
@property(class, nonatomic, readonly) BOOL is40InchScreen;

/// iPhone 4
@property(class, nonatomic, readonly) BOOL is35InchScreen;

@property(class, nonatomic, readonly) CGSize screenSizeFor67Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor65Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor61InchAndiPhone12Later;
@property(class, nonatomic, readonly) CGSize screenSizeFor61Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor58Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor55Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor54Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor47Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor40Inch;
@property(class, nonatomic, readonly) CGSize screenSizeFor35Inch;

@property(class, nonatomic, readonly) CGFloat preferredLayoutAsSimilarScreenWidthForIPad;

/// insets for the isNotchedScreen device. Note that the new iPad does not necessarily have physical notches for the Home button-less device, but because it uses the Home Indicator, its safeAreaInsets are also non-0.
/// @NEW_DEVICE_CHECKER
@property(class, nonatomic, readonly) UIEdgeInsets safeAreaInsetsForDeviceWithNotch;

/// If the current device is a high-performance device, the device is judged only once, and the result is directly read from the future. Therefore, there is no performance problem
//@property(class, nonatomic, readonly) BOOL isHighPerformanceDevice;

/// System Settings whether to open the "zoom - to - amplification", support the iPhone device can magnify the query in the official document at https://support.apple.com/zh-cn/guide/iphone/iphd6804774e/ios
/// @NEW_DEVICE_CHECKER
@property(class, nonatomic, readonly) BOOL isZoomedMode;

@end


NS_ASSUME_NONNULL_END
