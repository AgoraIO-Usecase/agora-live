/**
 * Tencent is pleased to support the open source community by making QMUI_iOS available.
 * Copyright (C) 2016-2021 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

//
//  QMUICommonDefines.h
//  qmui
//
//  Created by QMUI Team on 14-6-23.
//

#ifndef QMUICommonDefines_h
#define QMUICommonDefines_h

#import <UIKit/UIKit.h>
@import YYCategories;

#pragma mark - Variable - device dependent

/// Device type
#define IS_IPAD (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad)
//#define IS_IPOD [QMUIHelper isIPod]
#define IS_IPHONE ([[[UIDevice currentDevice] model] rangeOfString:@"iPhone"].location != NSNotFound)
#define IS_SIMULATOR NO //[QMUIHelper isSimulator]
//#define IS_MAC [QMUIHelper isMac]

/// The operating system version number is only the second-level version number. For example, 10.3.1 is only 10.3
#define IOS_VERSION ([[[UIDevice currentDevice] systemVersion] doubleValue])

/// The operating system version number in digital form, which can be used directly for size comparison; For example, 110205 represents version 11.2.5; According to the iOS specification, the version number may have a maximum of 3 digits
#define IOS_VERSION_NUMBER [QMUIHelper numbericOSVersion]

/// Horizontal or vertical screen
/// The user interface will return YES only when the screen is horizontal
#define IS_LANDSCAPE UIInterfaceOrientationIsLandscape(UIApplication.sharedApplication.statusBarOrientation)
/// Whether the device supports landscape or not, as long as the device is landscape, it will return YES
#define IS_DEVICE_LANDSCAPE UIDeviceOrientationIsLandscape([[UIDevice currentDevice] orientation])

/// The screen width will change according to the horizontal and vertical screen changes
#define SCREEN_WIDTH ([[UIScreen mainScreen] bounds].size.width)

/// The height of the screen will change according to the change of horizontal and vertical screen
#define SCREEN_HEIGHT ([[UIScreen mainScreen] bounds].size.height)

/// Device width has nothing to do with horizontal or vertical screen
#define DEVICE_WIDTH MIN([[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)

/// The height of the device has nothing to do with horizontal or vertical screens
#define DEVICE_HEIGHT MAX([[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)

/// Full screen device or not
#define IS_NOTCHED_SCREEN [QMUIHelper isNotchedScreen]
/// iPhone 12 Pro Max
#define IS_67INCH_SCREEN [QMUIHelper is67InchScreen]
/// iPhone XS Max
#define IS_65INCH_SCREEN [QMUIHelper is65InchScreen]
/// iPhone 12 / 12 Pro
#define IS_61INCH_SCREEN_AND_IPHONE12 [QMUIHelper is61InchScreenAndiPhone12Later]
/// iPhone XR
#define IS_61INCH_SCREEN [QMUIHelper is61InchScreen]
/// iPhone X/XS
#define IS_58INCH_SCREEN [QMUIHelper is58InchScreen]
/// iPhone 6/7/8 Plus
#define IS_55INCH_SCREEN [QMUIHelper is55InchScreen]
/// iPhone 12 mini
#define IS_54INCH_SCREEN [QMUIHelper is54InchScreen]
/// iPhone 6/7/8
#define IS_47INCH_SCREEN [QMUIHelper is47InchScreen]
/// iPhone 5/5S/SE
#define IS_40INCH_SCREEN [QMUIHelper is40InchScreen]
/// iPhone 4/4S
#define IS_35INCH_SCREEN [QMUIHelper is35InchScreen]
/// iPhone 4/4S/5/5S/SE
#define IS_320WIDTH_SCREEN (IS_35INCH_SCREEN || IS_40INCH_SCREEN)

#pragma mark - Variable - layout related

/// bounds && nativeBounds / scale && nativeScale
#define ScreenBoundsSize ([[UIScreen mainScreen] bounds].size)
#define ScreenNativeBoundsSize ([[UIScreen mainScreen] nativeBounds].size)
#define ScreenScale ([[UIScreen mainScreen] scale])

/// toolBar related frame
#define ToolBarHeight (IS_IPAD ? (IS_NOTCHED_SCREEN ? 70 : (IOS_VERSION >= 12.0 ? 50 : 44)) : (IS_LANDSCAPE ? 32 : 44) + SafeAreaInsetsConstantForDeviceWithNotch.bottom)

/// tabBar associated frame
#define TabBarHeight (IS_IPAD ? (IS_NOTCHED_SCREEN ? 65 : (IOS_VERSION >= 12.0 ? 50 : 49)) : (IS_LANDSCAPE ? 32 : 49) + SafeAreaInsetsConstantForDeviceWithNotch.bottom)

/// Status bar height (in the case of incoming calls, the height of the status bar will change, so it should be calculated in real time, iOS 13, the status bar height will not change in the case of incoming calls, etc.)
#define StatusBarHeight (UIApplication.sharedApplication.statusBarHidden ? 0 : UIApplication.sharedApplication.statusBarFrame.size.height)

/// Status bar height (If the status bar is not visible, it will also return a height that is visible in normal state)
#define StatusBarHeightConstant (UIApplication.sharedApplication.statusBarHidden ? (IS_IPAD ? (IS_NOTCHED_SCREEN ? 24 : 20) : PreferredValueForNotchedDevice(IS_LANDSCAPE ? 0 : ([[QMUIHelper deviceModel] isEqualToString:@"iPhone12,1"] ? 48 : (IS_61INCH_SCREEN_AND_IPHONE12 || IS_67INCH_SCREEN ? 47 : ((IS_54INCH_SCREEN && IOS_VERSION >= 15.0) ? 50 : 44))), 20)) : UIApplication.sharedApplication.statusBarFrame.size.height)

/// Static height of the navigationBar
#define NavigationBarHeight (IS_IPAD ? (IOS_VERSION >= 12.0 ? 50 : 44) : (IS_LANDSCAPE ? 32 : 44))

/// Static value of security zone for iPhoneX series full screen phones
#define SafeAreaInsetsConstantForDeviceWithNotch [QMUIHelper safeAreaInsetsForDeviceWithNotch]


#pragma mark - Method - Creator

#define UIImageMake(img) [UIImage imageNamed:img]

/// Font related macros for quickly creating a font object, more create macros can be found at UIFont+QMUI.h
#define UIFontMake(size) [UIFont systemFontOfSize:size]
#define UIFontItalicMake(size) [UIFont italicSystemFontOfSize:size] /// Italics are valid only for numbers and letters, not Chinese
#define UIFontBoldMake(size) [UIFont boldSystemFontOfSize:size]
#define UIFontBoldWithFont(_font) [UIFont boldSystemFontOfSize:_font.pointSize]

/// Uicolor-related macros for quickly creating a UIColor object. For more macros created, see UIColor+QMUI.h
#define UIColorMake(r, g, b) [UIColor colorWithRed:r/255.0 green:g/255.0 blue:b/255.0 alpha:1]
#define UIColorMakeWithRGBA(r, g, b, a) [UIColor colorWithRed:r/255.0 green:g/255.0 blue:b/255.0 alpha:a/1.0]
#define UIColorMakeWithHex(s) [UIColor colorWithHexString:s]

#endif /* QMUICommonDefines_h */
