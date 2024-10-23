# LSTPopView iOS universal pop-up window

[![Platform](https://img.shields.io/badge/platform-iOS-red.svg)](https://developer.apple.com/iphone/index.action) [![Language](http://img.shields.io/badge/language-OC-yellow.svg?style=flat )](https://en.wikipedia.org/wiki/Objective-C) [![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://mit-license.org) [![CocoaPods Compatible](https://img.shields.io/cocoapods/v/LSTPopView.svg)](https://img.shields.io/cocoapods/v/LSTPopView.svg)

### LSTPopView is an all-purpose pop-up window, powerful and easy to expand. Performance optimization and memory control make its operation more smooth and stable. The appearance of LSTPopView can make us focus more on the layout of pop-up pages. Save worry and effort! Improve development efficiency!

## Foreword
- Considering the author's energy problem, please check the API, imitate Demo, read README, and search Issues first. If it's BUG or Feature, it's better to mention Issue.
- Contact information: LoSenTrad@163.com, QQ group:1045568246, WeChat:a_LSTKit
- Development environment: Xcode12.3, iOS13.5, iPhone XS Max

## Blog address
- github: [https://github.com/LoSenTrad/LSTPopView](https://github.com/LoSenTrad/LSTPopView)
- CSDN: [https://blog.csdn.net/u012400600/article/details/106279654](https://blog.csdn.net/u012400600/article/details/106279654)
- jianshu: [https://www.jianshu.com/p/8023a85dc2a2](https://www.jianshu.com/p/8023a85dc2a2)

## Catalogue
* [Characteristics] (#Characteristics)  
* [Version Update History] (#Version Update History)  
* [Installation] (#Installation)
* [Basic Use] (#Basic Use) 
* [Notes for Use] (#Notes for Use) 
* [Demonstration Effect] (#Demonstration Effect)
* [Author] (#Author)  
* [Copyright] (#Copyright)  

## Characteristic
- Provide rich api, highly customized pop-up windows, easy to use
- Support pop-up animation, disappearing animation, active animation and other multiple animation matching
- Support click, long press, drag and drop, sweep gesture interaction
- Support multi-pop-up window management: formation, stacking, priority, etc.
- Support specifying pop-up parent classes, eg: UIWindow, self.view, etc.
- Safe and small memory occupation, pop-up memory is automatically recycled
- Provide life cycle api, custom animation control
- Automatically avoid the keyboard to prevent being blocked by the keyboard
- Support timer, multi-timer mechanism
- Support pure code/xib page
- Support horizontal and vertical screen switching
- Support multi-agent mechanism
- Support secondary packaging, such as components LSTHUD, LSTAlertView, etc.

## Version update history
- [Click me to check](https://github.com/LoSenTrad/LSTPopView/blob/master/UPDATE_HISTORY.md)

## Installation
- OC version installation
    - CocoaPods installation: Add the following description to the podfile file, and then `pod install` or `pod update`

      ```
      pod 'LSTPopView'
      ```  
    - Carthage installation: (not adapted for the time being)
- Swift version installation
    - (Planned to be developed)

- Manual import and integration
    - 1. Drag 5 files under the LSTPopView file to the project
    
     ```objective-c
     LSTPopView.h
     LSTPopView.m
     UIView+LSTPV.h
     UIView+LSTPV.m
     LSTPopViewProtocol.h
     ```  
    - 2.Add the dependency library LSTTimer to the podfile in the project: https://github.com/LoSenTrad/LSTTimer
     ```ruby
     pod 'LSTTimer'
     ```  
      
## Basic use
- Code example

    ```objective-c
    //Custom view
    LSTPopViewTVView *customView = [[LSTPopViewTVView alloc] initWithFrame:CGRectMake(0, 0, 300,400)];
   //Create a pop-up window PopViiew Specify the parent container self.view, do not specify the default is app window
    LSTPopView *popView = [LSTPopView initWithCustomView:customView
                                              parentView:self.view
                                                popStyle:LSTPopStyleSmoothFromBottom
                                            dismissStyle:LSTDismissStyleSmoothToBottom];
   //Pop-up window position: center, top, left, bottom, right
   popView.hemStyle = LSTHemStyleBottom;
   LSTPopViewWK(popView)
   //Click the background to trigger
   popView.bgClickBlock = ^{ [wk_popView dismiss]; };
   //Pop-up window display
  [popView pop];
  ```
  
- Debugging log
  ```objective-c
  /** Debugging log type */
  typedef NS_ENUM(NSInteger, LSTPopViewLogStyle) {
  LSTPopViewLogStyleNO = 0,          // Turn off debugging information (window and console log output)
  LSTPopViewLogStyleWindow,          // Open the small window in the upper left corner
  LSTPopViewLogStyleConsole,         // Turn on the console log output
  LSTPopViewLogStyleALL              // Open the small window and console log
  };
  ```
    - Debugging small windows: S represents the number of pop-ups that have been displayed, R represents the pop-ups in the removal queue, and S+R represents all the current number of pop-ups.
  
  

  

## Precautions for use
#### (Be sure to modify with weak)
- Analysis: LSTPopView has an automatic memory destruction mechanism for each pop-up window. External strong references to pop-up windows break this automatic memory destruction mechanism. For example, member variables are modified with strong, otherwise the pop-up window cannot be automatically destroyed, resulting in memory recovery.
- Specification for the use of class member variables:

  ```objective-c
  //Member variables can be modified with weak, but not with strong.
  @property (nonatomic,weak) LSTPopView *popView;
  ```
- The creation of member variables
  ```objective-c
  LSTPopViewTVView *customView = [[LSTPopViewTVView alloc] initWithFrame:CGRectMake(0, 0, 300,400)];
  //Pop-up instance creation
  LSTPopView *popView = [LSTPopView initWithCustomView:customView
                                                popStyle:LSTPopStyleSmoothFromBottom
                                            dismissStyle:LSTDismissStyleSmoothToBottom];
  //Here, the value is assigned to the member variable self.popView
  self.popView = popView;
  [popView pop];
  ```
  
- Wrong use:
  ```objective-c
  //Directly assigning values to member variables causes member variables to be empty. Please refer to the above usage specifications.
  self.popView = [LSTPopView initWithCustomView:customView
                                         popStyle:LSTPopStyleSmoothFromBottom
                                     dismissStyle:LSTDismissStyleSmoothToBottom];
  ```

    
## Demonstration effect

- Example scenarios commonly used in the application market

|QQ, WeChat, UC, Weibo, TikTok<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/base_demo.gif" width = "200" height = "424" alt="The name of the picture" align=center />|Drag to remove, swipe to remove<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/drag_sweep.gif" width = "200" height = "424" alt="The name of the picture" align=center /> |
|---|---|

- Rich entry and exit animation, drag and drop, sweep animation

|Pop-up animation, middle, up, left, down, left<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/pop.gif" width = "200" height = "424" alt="The name of the picture" align=center />|Remove animation, middle, top, left, bottom, left<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/dismiss.gif" width = "200" height = "424" alt="The name of the picture" align=center /> |Drag and drop, sweep animation<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/dragSweep.gif" width = "200" height = "424" alt="The name of the picture" align=center /> |
|---|---|---|

- Pop-up window position

|Pop-up window position, middle, top, left, bottom, right<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/hem_Style.gif" width = "200" height = "424" alt="The name of the picture" align=center />|X-axis, Y-axis modulation, width and height modulation<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/x_y_w_h.gif" width = "200" height = "424" alt="The name of the picture" align=center /> |
|---|---|

- Automatically avoid keyboard cover, specify container, timer

|Automatically avoid keyboard obstruction<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/keyboard.gif" width = "200" height = "424" alt="The name of the picture" align=center />|Specify the container to pop up<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/LSTPopView/%E6%8C%87%E5%AE%9A%E5%AE%B9%E5%99%A8.gif" width = "200" height = "424" alt="The name of the picture" align=center />|Pop-up window timing<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/LSTPopView/%E8%AE%A1%E6%97%B6%E5%99%A8.gif" width = "200" height = "424" alt="The name of the picture" align=center />|
|---|---|---|
     
- Multi-pop-up management (priority, formation)

|App startup multi-pop-up priority display<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/uPic/launch.gif" width = "200" height = "424" alt="The name of the picture" align=center />|Multi-window formation use<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/LSTPopView/%E7%AA%97%E5%8F%A3%E7%BC%96%E9%98%9F%E7%9A%84%E4%BD%BF%E7%94%A8.gif" width = "200" height = "424" alt="The name of the picture" align=center />|
|---|---|
     
 - Support horizontal and vertical screens
 
|Screen rotation<br><img src="https://raw.githubusercontent.com/5208171/LSTBlog/master/LSTPopView/%E5%B1%8F%E5%B9%95%E6%97%8B%E8%BD%AC.gif" width = "500" height = "500" alt="The name of the picture" align=center />|
|---|

       

## Author

LoSenTrad@163.com, QQ Group:1045568246, WeChat:a_LSTKit

## Copyright

 Everyone has a responsibility to respect the fruits of labor.
    
