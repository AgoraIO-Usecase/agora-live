//
//  LSTPopViewProtocol.h
//  LSTCategory
//
//  Created by LoSenTrad on 2020/5/30.
//

#import <Foundation/Foundation.h>
@class LSTPopView;


#ifdef DEBUG
#define LSTPVLog(format, ...) printf("class: <%p %s:(row of %d) > method: %s \n%s\n", self, [[[NSString stringWithUTF8String:__FILE__] lastPathComponent] UTF8String], __LINE__, __PRETTY_FUNCTION__, [[NSString stringWithFormat:(format), ##__VA_ARGS__] UTF8String] )
#else
#define LSTPVLog(format, ...)
#endif

/** Debugging log type */
typedef NS_ENUM(NSInteger, LSTPopViewLogStyle) {
    LSTPopViewLogStyleNO = 0,          // Turn off debugging information (window and console log output)
    LSTPopViewLogStyleWindow,          // Open the small window in the upper left corner
    LSTPopViewLogStyleConsole,         // Turn on the console log output
    LSTPopViewLogStyleALL              // Open the small window and console log
};

/** Display animation style */
typedef NS_ENUM(NSInteger, LSTPopStyle) {
    LSTPopStyleFade = 0,               // Default gradient appearance
    LSTPopStyleNO,                     // No animation
    LSTPopStyleScale,                  // Zoom in, enlarge first, and then restore to the original size
    LSTPopStyleSmoothFromTop,          // Top Smooth fade-in animation
    LSTPopStyleSmoothFromLeft,         // Smooth fade-in animation on the left
    LSTPopStyleSmoothFromBottom,       // Bottom Smooth fade-in animation
    LSTPopStyleSmoothFromRight,        // Smooth fade-in animation on the right side
    LSTPopStyleSpringFromTop,          // Top Smooth fade-in animation with spring
    LSTPopStyleSpringFromLeft,         // Left smooth fade-in animation with spring
    LSTPopStyleSpringFromBottom,       // Bottom Smooth fade-in animation with spring
    LSTPopStyleSpringFromRight,        // Right side smooth fade-in animation with spring
    LSTPopStyleCardDropFromLeft,       // Drop animation on the left side of the top
    LSTPopStyleCardDropFromRight,      // Drop animation on the right side of the top
};
/** Disappearing animation style */
typedef NS_ENUM(NSInteger, LSTDismissStyle) {
    LSTDismissStyleFade = 0,             // Default gradient disappears
    LSTDismissStyleNO,                   // No animation
    LSTDismissStyleScale,                // Zoom
    LSTDismissStyleSmoothToTop,          // Top Smooth fade-out animation
    LSTDismissStyleSmoothToLeft,         // Left smooth fade-out animation
    LSTDismissStyleSmoothToBottom,       // Bottom Smooth fade-out animation
    LSTDismissStyleSmoothToRight,        // Smooth fade-out animation on the right
    LSTDismissStyleCardDropToLeft,       // The card fell from the middle to the left.
    LSTDismissStyleCardDropToRight,      // The card fell from the middle to the right.
    LSTDismissStyleCardDropToTop,        // The card disappears from the middle to the top.
};
/** Active animation style (in development) */
typedef NS_ENUM(NSInteger, LSTActivityStyle) {
    LSTActivityStyleNO = 0,               /// No animation
    LSTActivityStyleScale,                /// Zoom
    LSTActivityStyleShake,                /// Shake
};
/** Pop-up window position */
typedef NS_ENUM(NSInteger, LSTHemStyle) {
    LSTHemStyleCenter = 0,   //Between two parties
    LSTHemStyleTop,          //Stick to the top
    LSTHemStyleLeft,         //Stick to the left
    LSTHemStyleBottom,       //Stick to the bottom
    LSTHemStyleRight,        //Stick to the right
    LSTHemStyleTopLeft,      //Stick to the top and left
    LSTHemStyleBottomLeft,   //Stick to the bottom and left
    LSTHemStyleBottomRight,  //Stick to the bottom and right
    LSTHemStyleTopRight      //Stick to the top and right
};
/** Drag direction */
typedef NS_ENUM(NSInteger, LSTDragStyle) {
    LSTDragStyleNO = 0,  //By default, the window cannot be dragged.
    LSTDragStyleX_Positive = 1<<0,   //Drag in the positive direction of the X axis
    LSTDragStyleX_Negative = 1<<1,   //Drag in the negative direction of the X axis
    LSTDragStyleY_Positive = 1<<2,   //Y axis positive direction drag
    LSTDragStyleY_Negative = 1<<3,   //Y axis negative direction drag
    LSTDragStyleX = (LSTDragStyleX_Positive|LSTDragStyleX_Negative),   //Drag in the direction of the X axis
    LSTDragStyleY = (LSTDragStyleY_Positive|LSTDragStyleY_Negative),   //Drag in the direction of the Y axis
    LSTDragStyleAll = (LSTDragStyleX|LSTDragStyleY)   //All-round drag
};
///** You can swipe in the direction that disappears. */
typedef NS_ENUM(NSInteger, LSTSweepStyle) {
    LSTSweepStyleNO = 0,  //By default, the window cannot be dragged.
    LSTSweepStyleX_Positive = 1<<0,   //Drag in the positive direction of the X axis
    LSTSweepStyleX_Negative = 1<<1,   //Drag in the negative direction of the X axis
    LSTSweepStyleY_Positive = 1<<2,   //Y axis positive direction drag
    LSTSweepStyleY_Negative = 1<<3,   //Y axis negative direction drag
    LSTSweepStyleX = (LSTSweepStyleX_Positive|LSTSweepStyleX_Negative),   //Drag in the direction of the X axis
    LSTSweepStyleY = (LSTSweepStyleY_Positive|LSTSweepStyleY_Negative),   //Drag in the direction of the Y axis
    LSTSweepStyleALL = (LSTSweepStyleX|LSTSweepStyleY)   //Swipe in all directions
};

/**
 The animation type can be swiped to disappear. The setting is effective for one-way sweeping.
   LSTSweepDismissStyleSmooth: Automatic adaptation chooses one of the following
   LSTDismissStyleSmoothToTop,
   LSTDismissStyleSmoothToLeft,
   LSTDismissStyleSmoothToBottom ,
   LSTDismissStyleSmoothToRight
 */
typedef NS_ENUM(NSInteger, LSTSweepDismissStyle) {
    LSTSweepDismissStyleVelocity = 0,  //Default acceleration Remove
    LSTSweepDismissStyleSmooth = 1     //Smooth removal
};


NS_ASSUME_NONNULL_BEGIN

@protocol LSTPopViewProtocol <NSObject>


/** Click the pop-up window to call back */
- (void)lst_PopViewBgClickForPopView:(LSTPopView *)popView;
/** Long press the pop-up window to call back */
- (void)lst_PopViewBgLongPressForPopView:(LSTPopView *)popView;




// ****** Life cycle ******
/** will be displayed */
- (void)lst_PopViewWillPopForPopView:(LSTPopView *)popView;
/** has been displayed */
- (void)lst_PopViewDidPopForPopView:(LSTPopView *)popView;
/** Countdown in progress timeInterval: Duration */
- (void)lst_PopViewCountDownForPopView:(LSTPopView *)popView forCountDown:(NSTimeInterval)timeInterval;
/** Countdown Countdown Completed */
- (void)lst_PopViewCountDownFinishForPopView:(LSTPopView *)popView;
/** will start to remove */
- (void)lst_PopViewWillDismissForPopView:(LSTPopView *)popView;
/** It has been removed */
- (void)lst_PopViewDidDismissForPopView:(LSTPopView *)popView;
//***********************




@end

NS_ASSUME_NONNULL_END
