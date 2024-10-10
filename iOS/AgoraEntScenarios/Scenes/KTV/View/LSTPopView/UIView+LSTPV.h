//
//  UIView+LSTPV.h
//  LSTPopView
//
//  Created by LoSenTrad on 2020/11/30.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIView (LSTPV)

/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_X;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_Y;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_Width;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_Height;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_CenterX;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_CenterY;
/** Get/set the x coordinates of view */
@property (nonatomic, assign) CGFloat pv_Top;
/** Get/set the left coordinates of view */
@property (nonatomic, assign) CGFloat pv_Left;
/** Get/set the bottom coordinates of view Y */
@property (nonatomic, assign) CGFloat pv_Bottom;
/** Get/set the right coordinates of view */
@property (nonatomic, assign) CGFloat pv_Right;
/** Get/set the size of view */
@property (nonatomic, assign) CGSize pv_Size;


/** Is it Apple X series (Liu Haiping series) */
BOOL pv_IsIphoneX_ALL(void);
/** Screen size */
CGSize pv_ScreenSize(void);
/** Screen width */
CGFloat pv_ScreenWidth(void);
/** Screen height */
CGFloat pv_ScreenHeight(void);

@end

NS_ASSUME_NONNULL_END
