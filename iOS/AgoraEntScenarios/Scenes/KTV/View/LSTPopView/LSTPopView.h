//
//  LSTPopView.h
//  LoSenTrad
//
//  Created by LoSenTrad on 2020/2/22.
//

#import <UIKit/UIKit.h>
#import "LSTPopViewProtocol.h"
#import "UIView+LSTPV.h"


#define LSTPopViewWK(object)  __weak typeof(object) wk_##object = object;
#define LSTPopViewSS(object)  __strong typeof(object) object = weak##object;


NS_ASSUME_NONNULL_BEGIN

@interface LSTPopView : UIView


/** Agent Support multi-agent */
@property (nonatomic, weak) id<LSTPopViewProtocol> _Nullable delegate;
/** Logo is empty by default */
@property (nonatomic, copy) NSString *_Nullable identifier;
/** Pop-up window container */
@property (nonatomic, readonly) UIView *parentView;
/** custom view */
@property (nonatomic, readonly) UIView *currCustomView;
/** Pop-up window position Default LSTHemStyleCenter */
@property (nonatomic, assign) LSTHemStyle hemStyle;
/** Animation pop-up window style when displayed DefaultLSTPopStyleNO */
@property (nonatomic, assign) LSTPopStyle popStyle;
/** Animation pop-up window style when removed Default LSTDismissStyleNO */
@property (nonatomic, assign) LSTDismissStyle dismissStyle;
/** Animation duration when displaying, > 0. If it is not set, use the default animation duration and set it to LSTPopStyleNO, this attribute is invalid */
@property (nonatomic, assign) NSTimeInterval popDuration;
/** Animation duration when hidden, >0. If it is not set, use the default animation duration and set it to LSTDismissStyleNO. This attribute is invalid */
@property (nonatomic, assign) NSTimeInterval dismissDuration;
/** Pop-up horizontal direction (X) offset calibration Default 0 */
@property (nonatomic, assign) CGFloat adjustX;
/** Pop-up window vertical direction (Y) offset calibration Default 0 */
@property (nonatomic, assign) CGFloat adjustY;
/** Background color Default rgb(0,0,0) Transparency 0.3 */
@property (nullable,nonatomic,strong) UIColor *bgColor;
/** The transparency of the background when displaying, take the value (0.0~1.0), the default is 0.3 */
@property (nonatomic, assign) CGFloat bgAlpha;
/** Whether to hide the background Default NO */
@property (nonatomic, assign) BOOL isHideBg;
/** When displayed, click the background to remove the pop-up window, and the default is NO. */
@property (nonatomic, assign) BOOL isClickBgDismiss;
/** Whether to monitor the screen rotation, the default is YES */
@property (nonatomic, assign) BOOL isObserverScreenRotation;
/** Whether to support click feedback Default NO (temporarily closed) */
@property (nonatomic, assign) BOOL isClickFeedback;
/** Whether to avoid the keyboard Default YES */
@property (nonatomic, assign) BOOL isAvoidKeyboard;
/** The distance between the pop-up window and the keyboard Default 10 */
@property (nonatomic, assign) CGFloat avoidKeyboardSpace;
/** The custom view rounded corner direction setting Default UIRectCornerAllCorners takes effect when cornerRadius>0*/
@property (nonatomic, assign) UIRectCorner rectCorners;
/** Custom view rounded corner size */
@property (nonatomic, assign) CGFloat cornerRadius;
/** Pop-up vibration feedback Default NO iOS10+ system only has this effect */
@property (nonatomic, assign) BOOL isImpactFeedback;

//************ Group-related attributes ****************
/** Group identification, unified to pop-up window formation, convenient for independent management, default to nil, unified global processing */
@property (nullable,nonatomic,strong) NSString *groupId;
/** Whether to stack default NO If YES priority priority is not effective*/
@property (nonatomic,assign) BOOL isStack;
/** Single display default NO will only display the highest priority popView   */
@property (nonatomic, assign) BOOL isSingle;
/** Priority range 0~1000 (default 0, follow first-in-first-out) takes effect when isStack and isSingle are NO*/
@property (nonatomic, assign) CGFloat priority;
//****************************************

//************ Drag-and-drop gesture-related attributes ****************
/** Drag direction Default No drag  */
@property (nonatomic, assign) LSTDragStyle dragStyle;
/** X-axis or Y-axis drag to remove the critical distance range (0 ~ +∞) Default 0 does not drag to remove The base is used in dragStyle  */
@property (nonatomic, assign) CGFloat dragDistance;
/** Drag and drop to remove the animation type. The default is the same as dismissStyle.  */
@property (nonatomic, assign) LSTDismissStyle dragDismissStyle;
/** Drag and drop to disappear the animation time. The default is the same as dismissDuration. */
@property (nonatomic, assign) NSTimeInterval dragDismissDuration;
/** Drag and drop to restore animation time Default 0.25s */
@property (nonatomic, assign) NSTimeInterval dragReboundTime;

/** Swipe direction Default No swipe The premise is to turn on dragStyle */
@property (nonatomic, assign) LSTSweepStyle sweepStyle;
/** Swipe rate Control swipe removal Default 1000 based on the use of sweepStyle */
@property (nonatomic, assign) CGFloat swipeVelocity;
/** Swipe to remove the animation Default LSTSweepDismissStyleVelocity */
@property (nonatomic, assign) LSTSweepDismissStyle sweepDismissStyle;

//@property (nonatomic, strong) NSArray *xDragDistances;
//@property (nonatomic, strong) NSArray *yDragDistances;

//****************************************

/** Click on the background */
@property (nullable, nonatomic, copy) void(^bgClickBlock)(void);
/** Long press the background */
@property (nullable, nonatomic, copy) void(^bgLongPressBlock)(void);

/** Pop-up pan gesture offset  */
@property (nullable, nonatomic, copy) void(^panOffsetBlock)(CGPoint offset);


//The following keyboard pops up/closes. The third-party keyboard will call back many times (Baidu and Sogou tests), and the native keyboard will call back once.
/** The keyboard is about to pop up. */
@property (nullable, nonatomic, copy) void(^keyboardWillShowBlock)(void);
/** The keyboard has been popped up. */
@property (nullable, nonatomic, copy) void(^keyboardDidShowBlock)(void);
/** The keyboard frame will be changed. */
@property (nullable, nonatomic, copy) void(^keyboardFrameWillChangeBlock)(CGRect beginFrame,CGRect endFrame,CGFloat duration);
/** The keyboard frame has been changed. */
@property (nullable, nonatomic, copy) void(^keyboardFrameDidChangeBlock)(CGRect beginFrame,CGRect endFrame,CGFloat duration);
/** The keyboard will be put away. */
@property (nullable, nonatomic, copy) void(^keyboardWillHideBlock)(void);
/** The keyboard has been put away. */
@property (nullable, nonatomic, copy) void(^keyboardDidHideBlock)(void);

//************ Life cycle callback(Block) ************
/** The callback will be displayed. */
@property (nullable, nonatomic, copy) void(^popViewWillPopBlock)(void);
/** It has been displayed. Ca call back. */
@property (nullable, nonatomic, copy) void(^popViewDidPopBlock)(void);
/** will start removing the callback. */
@property (nullable, nonatomic, copy) void(^popViewWillDismissBlock)(void);
/** It has been removed. Ca call back. */
@property (nullable, nonatomic, copy) void(^popViewDidDismissBlock)(void);
/** Countdown callback */
@property (nullable, nonatomic, copy) void(^popViewCountDownBlock)(LSTPopView *popView,NSTimeInterval timeInterval);
//********************************************


/*
 The following is the construction method.
 customView: custom view
 popStyle: Pop-up animation
 dismissStyle: Remove the animation
 parentView: Pop-up window parent container
 */
+ (nullable instancetype)initWithCustomView:(UIView *_Nonnull)customView;

+ (nullable instancetype)initWithCustomView:(UIView *)customView
                                   popStyle:(LSTPopStyle)popStyle
                               dismissStyle:(LSTDismissStyle)dismissStyle;

+ (nullable instancetype)initWithCustomView:(UIView *)customView
                                 parentView:(UIView *_Nullable)parentView
                                   popStyle:(LSTPopStyle)popStyle
                               dismissStyle:(LSTDismissStyle)dismissStyle;
/*
 The following is the pop-up method.
  popStyle: Pop-up animation. The priority is higher than the global popStyle. It works locally.
  duration: Pop-up time, the priority is greater than the global popDuration, which plays a local role.
*/
- (void)pop;
- (void)popWithStyle:(LSTPopStyle)popStyle;
- (void)popWithDuration:(NSTimeInterval)duration;
- (void)popWithStyle:(LSTPopStyle)popStyle duration:(NSTimeInterval)duration;


/*
 The following is the pop-up method.
  dismissStyle: Pop-up animation, priority greater than global dismissStyle, partially works
  duration: Pop-up time, priority greater than global dismissDuration, local effect
*/
- (void)dismiss;
- (void)dismissWithStyle:(LSTDismissStyle)dismissStyle;
- (void)dismissWithDuration:(NSTimeInterval)duration;
- (void)dismissWithStyle:(LSTDismissStyle)dismissStyle duration:(NSTimeInterval)duration;


/** Delete the designated agent */
- (void)removeForDelegate:(id<LSTPopViewProtocol>)delegate;
/** Delete the proxy pool Delete all agents */
- (void)removeAllDelegate;



#pragma mark - ***** 以下是 窗口管理api *****

/** Get all popView globally (in the whole app) */
+ (NSArray *)getAllPopView;
/** Get all popViews on the current page*/
+ (NSArray *)getAllPopViewForParentView:(UIView *)parentView;
/** Get all popViews of the specified formation on the current page */
+ (NSArray *)getAllPopViewForPopView:(UIView *)popView;
/**
   Read popView (may read pop-ups across formations)
   It is recommended to use the getPopViewForGroupId:forkey: method for accurate reading.
 */
+ (LSTPopView *)getPopViewForKey:(NSString *)key;
/** Remove popView */
+ (void)removePopView:(LSTPopView *)popView;
/**
   Remove popView through the unique key (pop-up windows may be deleted by mistake across formations)
   It is recommended to use the removePopViewForGroupId:forkey: method for precise deletion.
*/
+ (void)removePopViewForKey:(NSString *)key;
/** Remove all popView */
+ (void)removeAllPopView;
/** Remove the last pop-up popView */
+ (void)removeLastPopView;


/** Open the debugging view. It is recommended to set it to hide online and open the test */
+ (void)setLogStyle:(LSTPopViewLogStyle)logStyle;


@end




NS_ASSUME_NONNULL_END
