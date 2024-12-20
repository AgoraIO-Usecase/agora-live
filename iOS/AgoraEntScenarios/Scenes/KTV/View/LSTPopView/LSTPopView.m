//
//  LSTPopView.m
//  LoSenTrad
//
//  Created by LoSenTrad on 2020/2/22.
//

#import "LSTPopView.h"
#import <objc/runtime.h>


static NSString *const LSTPopViewLogTitle = @"LSTPopView Logger ---> :";
static LSTPopViewLogStyle _logStyle;


@interface LSTPopViewManager : NSObject

LSTPopViewManager *LSTPopViewM(void);

@end

@interface LSTPopViewManager ()

/** Storage has displayed popView*/
@property (nonatomic,strong) NSMutableArray *popViewMarr;
/** Store the displayed popview in order of pop-ups */
@property (nonatomic,strong) NSPointerArray *showList;
/** Store popView to be removed */
@property (nonatomic,strong) NSHashTable <LSTPopView *> *removeList;
/** Memory Information View */
@property (nonatomic,strong) UILabel *infoView;

@end

@implementation LSTPopViewManager

static LSTPopViewManager *_instance;

LSTPopViewManager *LSTPopViewM() {
    return [LSTPopViewManager sharedInstance];
}

+ (instancetype)sharedInstance {
    if (!_instance) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            _instance = [[self alloc] init];
        });
    }
    return _instance;
}

+ (id)allocWithZone:(struct _NSZone *)zone {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _instance = [super allocWithZone:zone];
    });
    return _instance;
}

+ (NSArray *)getAllPopView {
    return LSTPopViewM().popViewMarr;
}
/** Get all popView on the current page */
+ (NSArray *)getAllPopViewForParentView:(UIView *)parentView {
    NSMutableArray *mArr = LSTPopViewM().popViewMarr;
    NSMutableArray *resMarr = [NSMutableArray array];
    for (id obj in mArr) {
        LSTPopView *popView = (LSTPopView *)obj;
        if ([popView.parentView isEqual:parentView]) {
            [resMarr addObject:obj];
        }
    }
    return [NSArray arrayWithArray:resMarr];
}

/** Get all popViews of the specified formation on the current page */
+ (NSArray *)getAllPopViewForPopView:(LSTPopView *)popView {
    NSArray *mArr = [self getAllPopViewForParentView:popView.parentView];
    NSMutableArray *resMarr = [NSMutableArray array];
    for (id obj in mArr) {
        LSTPopView *tPopView = (LSTPopView *)obj;
        
        if (popView.groupId == nil && tPopView.groupId == nil) {
            [resMarr addObject:obj];
            continue;
        }
        if ([tPopView.groupId isEqual:popView.groupId]) {
            [resMarr addObject:obj];
            continue;
        }
    }
    return [NSArray arrayWithArray:resMarr];
}

/** Read popView */
+ (LSTPopView *)getPopViewForKey:(NSString *)key {
    if (key.length<=0) { return nil; }
    NSMutableArray *mArr = LSTPopViewM().popViewMarr;
    for (id obj in mArr) {
        LSTPopView *popView = (LSTPopView *)obj;
        if ([popView.identifier isEqualToString:key]) {
            return popView;
        }
    }
    return nil;
}

+ (void)savePopView:(LSTPopView *)popView {
    [LSTPopViewM().popViewMarr addObject:popView];
    
    //Prioritize
    [self sortingArr];
    
    if (_logStyle & LSTPopViewLogStyleWindow) {
        [self setInfoData];
    }
    if (_logStyle & LSTPopViewLogStyleConsole) {
        [self setConsoleLog];
    }
}

/** Remove popView */
+ (void)removePopView:(LSTPopView *)popView {
    if (!popView) { return; }
    NSArray *arr = LSTPopViewM().popViewMarr;
    for (id obj in arr) {
        LSTPopView *tPopView = (LSTPopView *)obj;
        
        if ([tPopView isEqual:popView]) {
//            [LSTPopViewM().popViewMarr removeObject:obj];
            [tPopView dismissWithStyle:LSTDismissStyleNO];
            break;
        }
    }
    
    if (_logStyle & LSTPopViewLogStyleWindow) {
        [self setInfoData];
    }
    if (_logStyle & LSTPopViewLogStyleConsole) {
        [self setConsoleLog];
    }
}

/** Remove popView */
+ (void)removePopViewForKey:(NSString *)key {
    if (key.length<=0) { return; }
    NSArray *arr = LSTPopViewM().popViewMarr;
    for (id obj in arr) {
        LSTPopView *tPopView = (LSTPopView *)obj;
        
        if ([tPopView.identifier isEqualToString:key]) {
//            [LSTPopViewM().popViewMarr removeObject:obj];
            [tPopView dismissWithStyle:LSTDismissStyleNO];
            break;
        }
    }
    if (_logStyle & LSTPopViewLogStyleWindow) {
        [self setInfoData];
    }
    if (_logStyle & LSTPopViewLogStyleConsole) {
        [self setConsoleLog];
    }
}
/** Remove all popView */
+ (void)removeAllPopView {
    NSMutableArray *arr = LSTPopViewM().popViewMarr;
    if (arr.count<=0) { return;  }
    
    NSArray *popViewMarr = [NSArray arrayWithArray:LSTPopViewM().popViewMarr];
    
    [popViewMarr enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        LSTPopView *tPopView = (LSTPopView *)obj;
        
        [tPopView dismissWithStyle:LSTDismissStyleNO];
    }];
    
    if (_logStyle &  LSTPopViewLogStyleWindow) {
        [self setInfoData];
    }
    if (_logStyle & LSTPopViewLogStyleConsole) {
        [self setConsoleLog];
    }
}

+ (void)removeLastPopView {
    NSPointerArray *showList =  LSTPopViewM().showList;
    for (NSInteger i = showList.count - 1; i >= 0; i --) {
        LSTPopView *popView = [showList pointerAtIndex:i];
        if (popView) {
            [self removePopView:popView];
            [showList addPointer:NULL];
            [showList compact];
            break;
        }
    }
}

#pragma mark - ***** Other  *****

+ (void)setInfoData {
    LSTPopViewM().infoView.text = [NSString stringWithFormat:@"S:%zd R:%zd",LSTPopViewM().popViewMarr.count,LSTPopViewM().removeList.allObjects.count];
}

+ (void)setConsoleLog {
    LSTPVLog(@"%@ count of S:%zd count of R:%zd",LSTPopViewLogTitle,LSTPopViewM().popViewMarr.count,LSTPopViewM().removeList.allObjects.count);
}

//Bubbling sorting
+ (void)sortingArr {
    NSMutableArray *arr = LSTPopViewM().popViewMarr;
    for (int i = 0; i < arr.count; i++) {
        for (int j = i+1; j < arr.count; j++) {
            LSTPopView *iPopView = (LSTPopView *)arr[i];
            
            LSTPopView *jPopView = (LSTPopView *)arr[j];
            
            if (iPopView.priority > jPopView.priority) {
                [arr exchangeObjectAtIndex:i withObjectAtIndex:j];
            }
        }
    }
}

/** Transfer popView to the queue to be removed */
+ (void)transferredToRemoveQueueWithPopView:(LSTPopView *)popView {
    NSArray *popViewMarr = LSTPopViewM().popViewMarr;
    
    [popViewMarr enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        LSTPopView *tPopView = (LSTPopView *)obj;
        
        if ([popView isEqual:tPopView]) {
            [LSTPopViewM().removeList addObject:obj];
            [LSTPopViewM().popViewMarr removeObject:obj];
        }
    }];
}

+ (void)destroyInRemoveQueue:(LSTPopView *)popView {
    if (LSTPopViewM().removeList.allObjects.count <= 0) {
        [self transferredToRemoveQueueWithPopView:popView];
    }
   
    if (_logStyle&LSTPopViewLogStyleWindow) {
        [self setInfoData];
    }
    if (_logStyle&LSTPopViewLogStyleConsole) {
        [self setConsoleLog];
    }
}

#pragma mark - ***** Lazy loading *****

- (UILabel *)infoView {
    if(_infoView) return _infoView;
    _infoView = [[UILabel alloc] init];
    _infoView.backgroundColor = UIColor.orangeColor;
    _infoView.textAlignment = NSTextAlignmentCenter;
    _infoView.font = [UIFont systemFontOfSize:11];
    _infoView.frame = CGRectMake(pv_ScreenWidth()-70, pv_IsIphoneX_ALL()? 40:20, 60, 10);
    _infoView.layer.cornerRadius = 1;
    _infoView.layer.masksToBounds = YES;
    UIView *superView = [UIApplication sharedApplication].keyWindow;
    [superView addSubview:_infoView];
    return _infoView;
}

- (NSMutableArray *)popViewMarr {
    if(_popViewMarr) return _popViewMarr;
    _popViewMarr = [NSMutableArray array];
    return _popViewMarr;
}

- (NSHashTable<LSTPopView *> *)removeList {
    if (_removeList) return _removeList;
    _removeList = [NSHashTable hashTableWithOptions:NSPointerFunctionsWeakMemory];
    return _removeList;
}

- (NSPointerArray *)showList {
    if (_showList) return _showList;
    _showList = [NSPointerArray pointerArrayWithOptions:(NSPointerFunctionsWeakMemory)];
    return _showList;
}

@end


static const NSTimeInterval LSTPopViewDefaultDuration = -1.0f;
// Angle radians
#define LST_DEGREES_TO_RADIANS(angle) ((angle) / 180.0 * M_PI)


@interface LSTPopViewBgView : UIView

/** Whether to hide the background Default NO */
@property (nonatomic, assign) BOOL isHideBg;

@end

@implementation LSTPopViewBgView

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event{
    UIView *hitView = [super hitTest:point withEvent:event];
    if(hitView == self && self.isHideBg){
        return nil;
    }
    return hitView;
}

@end

@interface LSTPopView () <UIGestureRecognizerDelegate>

/** Pop-up container The default is app UIWindow. View can be specified as the container */
@property (nonatomic, weak) UIView *container;
/** Background layer */
@property (nonatomic, strong) LSTPopViewBgView *backgroundView;
/** Custom view */
@property (nonatomic,strong) UIView *customView;
/** Avoid keyboard offset */
@property (nonatomic, assign) CGFloat avoidKeyboardOffset;
/** Delegate Pool */
@property (nonatomic, strong) NSHashTable <id<LSTPopViewProtocol>> *delegates;
/** Background click gesture */
@property (nonatomic, strong) UITapGestureRecognizer *tapGesture;
/** Custom View Sliding Gestures */
@property (nonatomic, strong) UIPanGestureRecognizer *panGesture;
//Is it that is being dragged at present? tableView
@property (nonatomic, assign) BOOL isDragScrollView;
/** Mark has a UIScrollView, UITableView, UICollectionView in popView*/
@property (nonatomic, weak) UIScrollView *scrollerView;
/** Record custom view original Frame */
@property (nonatomic, assign) CGRect originFrame;
/** Mark whether dragDismissStyle is copied */
@property (nonatomic, assign) BOOL isDragDismissStyle;
/** Whether to pop up the keyboard */
@property (nonatomic,assign,readonly) BOOL isShowKeyboard;
/** The height of the pop-up keyboard */
@property (nonatomic, assign) CGFloat keyboardY;

@end

@implementation LSTPopView


#pragma mark - ***** Initialize *****

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self initSubViews];
    }
    return self;
}

- (void)initSubViews {
    //Initialize the configuration
    _isClickBgDismiss = NO;
    _isObserverScreenRotation = YES;
    _bgAlpha = 0.25;
    _popStyle = LSTPopStyleFade;
    _dismissStyle = LSTDismissStyleFade;
    _popDuration = LSTPopViewDefaultDuration;
    _dismissDuration = LSTPopViewDefaultDuration;
    _hemStyle = LSTHemStyleCenter;
    _adjustX = 0;
    _adjustY = 0;
    _isClickFeedback = NO;
    _isAvoidKeyboard = YES;
    _avoidKeyboardSpace = 10;
    _bgColor = [UIColor blackColor];
    _identifier = @"";
    _isShowKeyboard = NO;
    _priority = 0;
    _isHideBg = NO;
    _isShowKeyboard = NO;
    _isImpactFeedback = NO;
    _rectCorners = UIRectCornerAllCorners;
    
    _isSingle = NO;
    
    //Drag and drop related attributes to initialize
    _dragStyle = LSTDragStyleNO;
    _dragDismissStyle = LSTDismissStyleNO;
    _dragDistance = 0.0f;
    _dragReboundTime = 0.25;
    _dragDismissDuration = LSTPopViewDefaultDuration;
    _swipeVelocity = 1000.0f;
    
    _isDragDismissStyle = NO;
}

+ (nullable instancetype)initWithCustomView:(UIView *_Nonnull)customView {
    return [self initWithCustomView:customView popStyle:LSTPopStyleNO dismissStyle:LSTDismissStyleNO];
}


+ (nullable instancetype)initWithCustomView:(UIView *_Nonnull)customView
                                   popStyle:(LSTPopStyle)popStyle
                               dismissStyle:(LSTDismissStyle)dismissStyle {
    
    return [self initWithCustomView:customView
                         parentView:nil
                           popStyle:popStyle
                       dismissStyle:dismissStyle];
}

+ (nullable instancetype)initWithCustomView:(UIView *_Nonnull)customView
                                 parentView:(UIView *)parentView
                                   popStyle:(LSTPopStyle)popStyle
                               dismissStyle:(LSTDismissStyle)dismissStyle {
    // Check customView is exist (check customView is exist)
    if (!customView) { return nil; }
    if (![parentView isKindOfClass:[UIView class]] && parentView != nil) {
        LSTPVLog(@"parentView is error !!!");
        return nil;
    }
    
    CGRect popViewFrame = CGRectZero;
    if (parentView) {
        popViewFrame =  parentView.bounds;
    } else {
        popViewFrame =  CGRectMake(0, 0, pv_ScreenWidth(), pv_ScreenHeight());
    }
    
    LSTPopView *popView = [[LSTPopView alloc] initWithFrame:popViewFrame];
    popView.backgroundColor = [UIColor clearColor];

    popView.container = parentView? parentView : [UIApplication sharedApplication].keyWindow;

    popView.customView = customView;
    popView.backgroundView = [[LSTPopViewBgView alloc] initWithFrame:popView.bounds];
    popView.backgroundView.backgroundColor = [UIColor clearColor];
    popView.popStyle = popStyle;
    popView.dismissStyle = dismissStyle;
    
    [popView addSubview:popView.backgroundView];
    [popView.backgroundView addSubview:customView];
    
    //Add gestures to the background
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:popView action:@selector(popViewBgViewTap:)];
    tap.delegate = popView;
    popView.tapGesture = tap;
    [popView.backgroundView addGestureRecognizer:tap];
    
    //Add drag-and-drop gestures
    popView.panGesture = [[UIPanGestureRecognizer alloc] initWithTarget:popView action:@selector(pan:)];
    popView.panGesture.delegate = popView;
    [popView.customView addGestureRecognizer:popView.panGesture];
    
    //    [popView.backgroundView addTarget:popView action:@selector(popViewBgViewTap:) forControlEvents:UIControlEventTouchUpInside];;
    
    
    //    UILongPressGestureRecognizer *customViewLP = [[UILongPressGestureRecognizer alloc] initWithTarget:popView action:@selector(bgLongPressEvent:)];
    //    [popView.backgroundView addGestureRecognizer:customViewLP];
    
    //    UITapGestureRecognizer *customViewTap = [[UITapGestureRecognizer alloc] initWithTarget:popView action:@selector(customViewClickEvent:)];
    //    [popView.customView addGestureRecognizer:customViewTap];
    
    //The keyboard will display
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardWillShow:)
                                                 name:UIKeyboardWillShowNotification
                                               object:nil];
    //The keyboard display is finished.
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardDidShow:)
                                                 name:UIKeyboardDidShowNotification
                                               object:nil];
    //The keyboard frame will be changed.
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardWillChangeFrame:)
                                                 name:UIKeyboardWillChangeFrameNotification
                                               object:nil];
    //The keyboard frame has been changed.
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardDidChangeFrame:)
                                                 name:UIKeyboardDidChangeFrameNotification
                                               object:nil];
    //The keyboard will be put away.
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardWillHide:)
                                                 name:UIKeyboardWillHideNotification
                                               object:nil];
    //The keyboard has been put away.
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(keyboardDidHide:)
                                                 name:UIKeyboardDidHideNotification
                                               object:nil];
    //Screen rotation
    [[NSNotificationCenter defaultCenter] addObserver:popView
                                             selector:@selector(statusBarOrientationChange:)
                                                 name:UIApplicationDidChangeStatusBarOrientationNotification
                                               object:nil];
    //Monitor customView frame
    [popView.customView addObserver:popView
                         forKeyPath:@"frame"
                            options:NSKeyValueObservingOptionOld
                            context:NULL];
    return popView;
}

- (void)dealloc {
    [self.customView removeObserver:self forKeyPath:@"frame"];
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    //Destroy from the queue to be removed
    [LSTPopViewManager destroyInRemoveQueue:self];
    [self lst_PopViewDidDismissForPopView:self];
    self.popViewDidDismissBlock? self.popViewDidDismissBlock():nil;
}

- (void)removeFromSuperview {

    [super removeFromSuperview];
    
    [LSTPopViewManager transferredToRemoveQueueWithPopView:self];
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *hitView = [super hitTest:point withEvent:event];
    if (hitView != self.customView) {
        if(hitView == self && self.isHideBg){ return nil; }
    }
    return hitView;
}

#pragma mark - ***** Delegate method*****
- (void)lst_PopViewBgClickForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewBgClickForPopView:popView];
        }
    }
}

- (void)lst_PopViewBgLongPressForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewBgLongPressForPopView:popView];
        }
    }
}

- (void)lst_PopViewWillPopForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewWillPopForPopView:popView];
        }
    }
}

- (void)lst_PopViewDidPopForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewDidPopForPopView:popView];
        }
    }
}

- (void)lst_PopViewCountDownForPopView:(LSTPopView *)popView forCountDown:(NSTimeInterval)timeInterval {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewCountDownForPopView:popView forCountDown:timeInterval];
        }
    }
}

- (void)lst_PopViewCountDownFinishForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewCountDownFinishForPopView:popView];
        }
    }
}

- (void)lst_PopViewWillDismissForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewWillDismissForPopView:popView];
        }
    }
}

- (void)lst_PopViewDidDismissForPopView:(LSTPopView *)popView {
    for (id<LSTPopViewProtocol> delegate in _delegates.copy) {
        if ([delegate respondsToSelector:_cmd]) {
            [delegate lst_PopViewDidDismissForPopView:popView];
        }
    }
}

#pragma mark - ***** Interface layout *****

- (void)setCustomViewFrameWithHeight:(CGFloat)height {
    
    CGFloat changeY = 0;
    switch (self.hemStyle) {
        case LSTHemStyleTop ://Stick to the top
        {
            self.customView.pv_X = _backgroundView.pv_CenterX - _customView.pv_Size.width*0.5 + _adjustX;
            self.customView.pv_Y = _adjustY;
            changeY = _adjustY;
        }
            break;
        case LSTHemStyleLeft ://Stick to the left
        {
            self.customView.pv_X = _adjustX;
            self.customView.pv_Y = _backgroundView.pv_CenterY - _customView.pv_Size.height*0.5 + _adjustY;
            changeY = height*0.5;
        }
            break;
        case LSTHemStyleBottom ://Stick to the bottom
        {
            self.customView.pv_X = _backgroundView.pv_CenterX - _customView.pv_Size.width*0.5 + _adjustX;
            [self.customView layoutIfNeeded];
            self.customView.pv_Y = _backgroundView.pv_Height - _customView.pv_Height + _adjustY;
            changeY = height;
        }
            break;
        case LSTHemStyleRight ://Stick to the right
        {
            self.customView.pv_X = _backgroundView.pv_Width - _customView.pv_Width + _adjustX;
            self.customView.pv_Y = _backgroundView.pv_CenterY - _customView.pv_Size.height*0.5 + _adjustY;
            changeY = height*0.5;
        }
            break;
        case LSTHemStyleTopLeft :///Stick to the top and left
        {
            self.customView.pv_X = _adjustX;
            self.customView.pv_Y = _adjustY;
            changeY = _adjustY;
        }
            break;
        case LSTHemStyleBottomLeft ://Bottom and left
        {
            self.customView.pv_X = _adjustX;
            [self.customView layoutIfNeeded];
            self.customView.pv_Y = _backgroundView.pv_Height - _customView.pv_Height + _adjustY;
            changeY = height;
        }
            break;
        case LSTHemStyleBottomRight ://Stick to the bottom and right
        {
            self.customView.pv_X = _backgroundView.pv_Width - _customView.pv_Width + _adjustX;
            [self.customView layoutIfNeeded];
            self.customView.pv_Y = _backgroundView.pv_Height - _customView.pv_Height + _adjustY;
            changeY = height;
        }
            break;
        case LSTHemStyleTopRight ://Stick to the top and right
        {
            self.customView.pv_X = _backgroundView.pv_Width - _customView.pv_Width + _adjustX;
            self.customView.pv_Y = _adjustY;
            changeY = _adjustY;
        }
            break;
        default://Center
        {
            self.customView.pv_X = _backgroundView.pv_CenterX - _customView.pv_Size.width*0.5 + _adjustX;
            self.customView.pv_Y = _backgroundView.pv_CenterY - _customView.pv_Size.height*0.5 + _adjustY;
            changeY = height*0.5;
        }
            break;
    }
    
    CGFloat originBottom = _originFrame.origin.y + _originFrame.size.height + _avoidKeyboardSpace;
    if (_isShowKeyboard && (originBottom > _keyboardY)) {//The keyboard has been displayed.
        CGFloat Y = _keyboardY - _customView.pv_Height - _avoidKeyboardSpace;
        _customView.pv_Y = Y;
        CGRect newFrame = _originFrame;
        newFrame.origin.y = newFrame.origin.y - changeY;
        newFrame.size = CGSizeMake(_originFrame.size.width, _customView.pv_Height);
        self.originFrame = newFrame;
    }else {//There is no keyboard display.
        self.originFrame = _customView.frame;
    }
    
    [self setCustomViewCorners];
}

- (void)setCustomViewCorners {
    
    BOOL isSetCorner = NO;
    
    if (self.rectCorners & UIRectCornerTopLeft) {
        isSetCorner = YES;
    }
    if (self.rectCorners & UIRectCornerTopRight) {
        isSetCorner = YES;
    }
    if (self.rectCorners & UIRectCornerBottomLeft) {
        isSetCorner = YES;
    }
    if (self.rectCorners & UIRectCornerBottomRight) {
        isSetCorner = YES;
    }
    if (self.rectCorners & UIRectCornerAllCorners) {
        isSetCorner = YES;
    }
    
    if (isSetCorner && self.cornerRadius>0) {
        UIBezierPath * path = [UIBezierPath bezierPathWithRoundedRect:self.customView.bounds byRoundingCorners:self.rectCorners cornerRadii:CGSizeMake(self.cornerRadius, self.cornerRadius)];
        
        CAShapeLayer * layer = [[CAShapeLayer alloc]init];
        layer.frame = self.customView.bounds;
        layer.path = path.CGPath;
        self.customView.layer.mask = layer;
    }
    
}

#pragma mark - ***** setter Setter/Data Processing *****

- (void)setPopDuration:(NSTimeInterval)popDuration {
    if (popDuration <= 0.0) { return; }
    _popDuration = popDuration;
}

- (void)setDismissDuration:(NSTimeInterval)dismissDuration {
    if (dismissDuration <= 0.0) { return; }
    _dismissDuration = dismissDuration;
}

- (void)setDragDismissDuration:(NSTimeInterval)dragDismissDuration {
    if (dragDismissDuration <= 0.0) { return; }
    _dragDismissDuration = dragDismissDuration;
}

- (void)setHemStyle:(LSTHemStyle)hemStyle {
    _hemStyle = hemStyle;
}

- (void)setAdjustX:(CGFloat)adjustX {
    _adjustX = adjustX;
//    self.customView.x = _customView.x + adjustX;
//    self.originFrame = _customView.frame;
    [self setCustomViewFrameWithHeight:0];
}

- (void)setAdjustY:(CGFloat)adjustY {
    _adjustY = adjustY;
//    self.customView.y = _customView.y + adjustY;
//    self.originFrame = _customView.frame;
    [self setCustomViewFrameWithHeight:0];
}

- (void)setIsObserverScreenRotation:(BOOL)isObserverScreenRotation {
    _isObserverScreenRotation = isObserverScreenRotation;
}

- (void)setBgAlpha:(CGFloat)bgAlpha {
    _bgAlpha = (bgAlpha <= 0.0f) ? 0.0f : ((bgAlpha > 1.0) ? 1.0 : bgAlpha);
    }

- (void)setIsClickFeedback:(BOOL)isClickFeedback {
    _isClickFeedback = isClickFeedback;
}

- (void)setIsHideBg:(BOOL)isHideBg {
    _isHideBg = isHideBg;
    self.backgroundView.isHideBg = isHideBg;
    self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
}

- (void)setDelegate:(id<LSTPopViewProtocol>)delegate {
    if ([self.delegates containsObject:delegate]) return;
    [self.delegates addObject:delegate];
}

- (void)setDragDismissStyle:(LSTDismissStyle)dragDismissStyle {
    _dragDismissStyle = dragDismissStyle;
    self.isDragDismissStyle = YES;
}

#pragma mark - ***** pop *****
- (void)pop {
    [self popWithStyle:self.popStyle duration:self.popDuration];
}

- (void)popWithStyle:(LSTPopStyle)popStyle {
    [self popWithStyle:popStyle duration:self.popDuration];
}

- (void)popWithDuration:(NSTimeInterval)duration {
    [self popWithStyle:self.popStyle duration:duration];
}

- (void)popWithStyle:(LSTPopStyle)popStyle duration:(NSTimeInterval)duration {
    [self popWithPopStyle:popStyle duration:duration isOutStack:NO];
}

- (void)popWithPopStyle:(LSTPopStyle)popStyle duration:(NSTimeInterval)duration isOutStack:(BOOL)isOutStack {
    
    NSTimeInterval resDuration = [self getPopDuration:duration];
    
    [self setCustomViewFrameWithHeight:0];
    
    self.originFrame = self.customView.frame;
    
    if (self.isSingle) {
        NSArray *popViewArr = [LSTPopViewManager getAllPopViewForPopView:self];
        for (id obj  in popViewArr) {//Remove all popView and remove fixed a timer
            LSTPopView *lastPopView = (LSTPopView *)obj;
            
            [lastPopView dismissWithDismissStyle:LSTDismissStyleNO duration:0.2 isRemove:YES];
        }
    }else {
        if (!isOutStack) {//Dealing with hiding the penultimate popView
            NSArray *popViewArr = [LSTPopViewManager getAllPopViewForPopView:self];
            if (popViewArr.count >= 1) {
                id obj = popViewArr[popViewArr.count - 1];
                LSTPopView *lastPopView = (LSTPopView *)obj;
                
                if (self.isStack) {//Stacked display
                }else {
                    if (self.priority >= lastPopView.priority) {//Pin the top display
                        if (lastPopView.isShowKeyboard) {
                            [lastPopView endEditing:YES];
                        }
                        [lastPopView dismissWithDismissStyle:LSTDismissStyleFade duration:0.2 isRemove:NO];
                    } else {//Hide the display
                        if (!self.parentView) {
                            self.container = [UIApplication sharedApplication].keyWindow;
                        }
                        [LSTPopViewManager savePopView:self];
                        return;
                    }
                }
            } else {
            }
        }
    }
    
//    if (!isOutStack){
//        [self.parentView addSubview:self];
//    }
    if (!self.superview) {
        [self.container addSubview:self];
        [LSTPopViewM().showList addPointer:(__bridge void * _Nullable)self];
    }
    
    if (!isOutStack){
        [self lst_PopViewWillPopForPopView:self];
        
        self.popViewWillPopBlock? self.popViewWillPopBlock():nil;
    }
    //Animation processing
    [self popAnimationWithPopStyle:popStyle duration:resDuration];
    //Vibrating feedback
    [self beginImpactFeedback];
    
    LSTPopViewWK(self);;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(resDuration * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        
        if (!isOutStack){
            [self lst_PopViewDidPopForPopView:self];
            wk_self.popViewDidPopBlock? wk_self.popViewDidPopBlock():nil;
        }
    });
    
    //save popView
    if (!isOutStack) { [LSTPopViewManager savePopView:self]; }
}

- (void)popAnimationWithPopStyle:(LSTPopStyle)popStyle duration:(NSTimeInterval)duration {
    
    LSTPopViewWK(self);
    if (popStyle == LSTPopStyleFade) {
        self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
        self.customView.alpha = 0.0f;
        [UIView animateWithDuration:0.2 animations:^{
            self.backgroundView.backgroundColor =[self getNewColorWith:self.bgColor alpha:self.bgAlpha];
            self.customView.alpha = 1.0f;
        }];
    } else if (popStyle == LSTPopStyleNO){
        self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
        [UIView animateWithDuration:0.1 animations:^{
            self.backgroundView.backgroundColor =[self getNewColorWith:self.bgColor alpha:self.bgAlpha];
        }];
        self.customView.alpha = 1.0f;
    } else {
        self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
        [UIView animateWithDuration:duration animations:^{
            wk_self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:self.bgAlpha];
        }];
        //Custom view transition animation
        [self hanlePopAnimationWithDuration:duration popStyle:popStyle];
    }
}

- (void)hanlePopAnimationWithDuration:(NSTimeInterval)duration
                             popStyle:(LSTPopStyle)popStyle {
    
    self.alpha = 0;
    [UIView animateWithDuration:duration*0.2 animations:^{
        self.alpha = 1;
    }];
    
    LSTPopViewWK(self);
    switch (popStyle) {
        case LSTPopStyleScale:// < Zoom animation, zoom in first, and then restore to the original size
        {
            [self animationWithLayer:_customView.layer duration:((0.3*duration)/0.7) values:@[@0.0, @1.2, @1.0]]; // Another set of animation values (the other animation values) @[@0.0, @1.2, @0.9, @1.0]
        }
            break;
        case LSTPopStyleSmoothFromTop:
        case LSTPopStyleSmoothFromBottom:
        case LSTPopStyleSmoothFromLeft:
        case LSTPopStyleSmoothFromRight:
        case LSTPopStyleSpringFromTop:
        case LSTPopStyleSpringFromLeft:
        case LSTPopStyleSpringFromBottom:
        case LSTPopStyleSpringFromRight:
        {
            CGPoint startPosition = self.customView.layer.position;
            if (popStyle == LSTPopStyleSmoothFromTop || popStyle == LSTPopStyleSpringFromTop) {
                self.customView.layer.position = CGPointMake(startPosition.x, -_customView.pv_Height*0.5);
                
            } else if (popStyle == LSTPopStyleSmoothFromLeft || popStyle == LSTPopStyleSpringFromLeft) {
                self.customView.layer.position = CGPointMake(-_customView.pv_Width*0.5, startPosition.y);
                
            } else if (popStyle == LSTPopStyleSmoothFromBottom || popStyle == LSTPopStyleSpringFromBottom) {
                self.customView.layer.position = CGPointMake(startPosition.x, CGRectGetMaxY(self.frame) + _customView.pv_Height*0.5);
                
            } else {
                self.customView.layer.position = CGPointMake(CGRectGetMaxX(self.frame) + _customView.pv_Width*0.5 , startPosition.y);
            }
            
            CGFloat damping = 1.0;
            if (popStyle == LSTPopStyleSpringFromTop||
                popStyle == LSTPopStyleSpringFromLeft||
                popStyle == LSTPopStyleSpringFromBottom||
                popStyle == LSTPopStyleSpringFromRight) {
                damping = 0.65;
            }
            
            [UIView animateWithDuration:duration
                                  delay:0
                 usingSpringWithDamping:damping
                  initialSpringVelocity:1.0
                                options:UIViewAnimationOptionCurveEaseOut
                             animations:^{
                
                wk_self.customView.layer.position = startPosition;
            } completion:nil];
        }
            break;
        case LSTPopStyleCardDropFromLeft:
        case LSTPopStyleCardDropFromRight:
        {
            CGPoint startPosition = self.customView.layer.position;
            if (popStyle == LSTPopStyleCardDropFromLeft) {
                self.customView.layer.position = CGPointMake(startPosition.x * 1.0, -_customView.pv_Height*0.5);
                self.customView.transform = CGAffineTransformMakeRotation(LST_DEGREES_TO_RADIANS(15.0));
            } else {
                self.customView.layer.position = CGPointMake(startPosition.x * 1.0, -_customView.pv_Height*0.5);
                self.customView.transform = CGAffineTransformMakeRotation(LST_DEGREES_TO_RADIANS(-15.0));
            }
            
            [UIView animateWithDuration:duration delay:0 usingSpringWithDamping:0.75 initialSpringVelocity:1.0 options:UIViewAnimationOptionCurveEaseIn animations:^{
                wk_self.customView.layer.position = startPosition;
            } completion:nil];
            
            [UIView animateWithDuration:duration*0.6 animations:^{
                wk_self.customView.layer.transform = CATransform3DMakeRotation(LST_DEGREES_TO_RADIANS((popStyle == LSTPopStyleCardDropFromRight) ? 5.5 : -5.5), 0, 0, 0);
            } completion:^(BOOL finished) {
                [UIView animateWithDuration:duration*0.2 animations:^{
                    wk_self.customView.transform = CGAffineTransformMakeRotation(LST_DEGREES_TO_RADIANS((popStyle == LSTPopStyleCardDropFromRight) ? -1.0 : 1.0));
                } completion:^(BOOL finished) {
                    [UIView animateWithDuration:duration*0.2 animations:^{
                        wk_self.customView.transform = CGAffineTransformMakeRotation(0.0);
                    } completion:nil];
                }];
            }];
        }
            break;
        default:
            break;
    }
}

#pragma mark - ***** dismiss *****

- (void)dismiss {
    [self dismissWithStyle:self.dismissStyle duration:self.dismissDuration];
}

- (void)dismissWithStyle:(LSTDismissStyle)dismissStyle {
    [self dismissWithStyle:dismissStyle duration:self.dismissDuration];
}

- (void)dismissWithDuration:(NSTimeInterval)duration {
    [self dismissWithStyle:self.dismissStyle duration:duration];
}

- (void)dismissWithStyle:(LSTDismissStyle)dismissStyle duration:(NSTimeInterval)duration  {
    [self dismissWithDismissStyle:dismissStyle duration:duration isRemove:YES];
}

- (void)dismissWithDismissStyle:(LSTDismissStyle)dismissStyle
                       duration:(NSTimeInterval)duration
                       isRemove:(BOOL)isRemove {
    
    NSTimeInterval resDuration = [self getDismissDuration:duration];
    
    if (isRemove) {
        //Transfer the current popView to the queue to be removed to avoid thread security problems
        [LSTPopViewManager transferredToRemoveQueueWithPopView:self];
    }
    
    [self lst_PopViewWillDismissForPopView:self];
    
    self.popViewWillDismissBlock? self.popViewWillDismissBlock():nil;
    
    LSTPopViewWK(self)
    [self dismissAnimationWithDismissStyle:dismissStyle duration:resDuration];
    
    if (!self.isSingle && (isRemove && [LSTPopViewManager getAllPopViewForPopView:self].count >= 1)){
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(resDuration * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            //popView out stack
            if (!self.isStack && [LSTPopViewManager getAllPopViewForPopView:self].count >= 1) {
                NSArray *popViewArr = [LSTPopViewManager getAllPopViewForPopView:self];
                id obj = popViewArr[popViewArr.count-1];
                LSTPopView *tPopView = (LSTPopView *)obj;
                if (!tPopView.superview) {
                    [tPopView.container addSubview:tPopView];
                    [LSTPopViewM().showList addPointer:(__bridge void * _Nullable)tPopView];
                }
                [tPopView popWithPopStyle:LSTPopStyleFade duration:0.25 isOutStack:YES];
            }
        });
    }
    if (isRemove) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(resDuration * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            [wk_self removeFromSuperview];
        });
    }
}

- (void)dismissAnimationWithDismissStyle:(LSTDismissStyle)dismissStyle duration:(NSTimeInterval)duration {
    
    LSTPopViewWK(self);
    if (dismissStyle == LSTPopStyleFade) {
        [UIView animateWithDuration:0.2 animations:^{
            wk_self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
            wk_self.customView.alpha = 0.0f;
        }];
    }else if (dismissStyle == LSTPopStyleNO){
        wk_self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
        wk_self.customView.alpha = 0.0f;
    }else {//There are animations.
        [UIView animateWithDuration:duration*0.8 animations:^{
            wk_self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
        }];
        [self hanleDismissAnimationWithDuration:duration withDismissStyle:dismissStyle];
    }
}

- (void)hanleDismissAnimationWithDuration:(NSTimeInterval)duration
                         withDismissStyle:(LSTDismissStyle)dismissStyle {
    
    [UIView animateWithDuration:duration*0.8 animations:^{
        self.alpha = 0;
    }];
    
    LSTPopViewWK(self);;
    switch (dismissStyle) {
        case LSTDismissStyleScale:
        {
            [self animationWithLayer:self.customView.layer duration:((0.2*duration)/0.8) values:@[@1.0, @0.66, @0.33, @0.01]];
        }
            break;
        case LSTDismissStyleSmoothToTop:
        case LSTDismissStyleSmoothToBottom:
        case LSTDismissStyleSmoothToLeft:
        case LSTDismissStyleSmoothToRight:
        {
            CGPoint startPosition = self.customView.layer.position;
            CGPoint endPosition = self.customView.layer.position;
            if (dismissStyle == LSTDismissStyleSmoothToTop) {
                endPosition = CGPointMake(startPosition.x, -(_customView.pv_Height*0.5));
                
            } else if (dismissStyle == LSTDismissStyleSmoothToBottom) {
                endPosition = CGPointMake(startPosition.x, CGRectGetMaxY(self.frame) + _customView.pv_Height*0.5);
                
            } else if (dismissStyle == LSTDismissStyleSmoothToLeft) {
                endPosition = CGPointMake(-_customView.pv_Width*0.5, startPosition.y);
                
            } else {
                endPosition = CGPointMake(CGRectGetMaxX(self.frame) + _customView.pv_Width*0.5, startPosition.y);
            }
            
            [UIView animateWithDuration:duration delay:0 usingSpringWithDamping:1.0 initialSpringVelocity:1.0 options:UIViewAnimationOptionCurveEaseOut animations:^{
                wk_self.customView.layer.position = endPosition;
            } completion:nil];
        }
            break;
        case LSTDismissStyleCardDropToLeft:
        case LSTDismissStyleCardDropToRight:
        {
            CGPoint startPosition = self.customView.layer.position;
            BOOL isLandscape = UIInterfaceOrientationIsLandscape([UIApplication sharedApplication].statusBarOrientation);
            __block CGFloat rotateEndY = 0.0f;
            [UIView animateWithDuration:duration delay:0 usingSpringWithDamping:0.5 initialSpringVelocity:1.0 options:UIViewAnimationOptionCurveEaseIn animations:^{
                if (dismissStyle == LSTDismissStyleCardDropToLeft) {
                    wk_self.customView.transform = CGAffineTransformMakeRotation(M_1_PI * 0.75);
                    if (isLandscape) rotateEndY = fabs(wk_self.customView.frame.origin.y);
                    wk_self.customView.layer.position = CGPointMake(startPosition.x, CGRectGetMaxY(wk_self.frame) + startPosition.y + rotateEndY);
                } else {
                    wk_self.customView.transform = CGAffineTransformMakeRotation(-M_1_PI * 0.75);
                    if (isLandscape) rotateEndY = fabs(wk_self.customView.frame.origin.y);
                    wk_self.customView.layer.position = CGPointMake(startPosition.x * 1.25, CGRectGetMaxY(wk_self.frame) + startPosition.y + rotateEndY);
                }
            } completion:nil];
        }
            break;
        case LSTDismissStyleCardDropToTop:
        {
            CGPoint startPosition = self.customView.layer.position;
            CGPoint endPosition = CGPointMake(startPosition.x, -startPosition.y);
            [UIView animateWithDuration:duration*0.2 animations:^{
                wk_self.customView.layer.position = CGPointMake(startPosition.x, startPosition.y + 50.0f);
            } completion:^(BOOL finished) {
                [UIView animateWithDuration:duration*0.8 animations:^{
                    wk_self.customView.layer.position = endPosition;
                } completion:nil];
            }];
        }
            break;
        default:
            break;
    }
}

#pragma mark - ***** other *****

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    if([keyPath isEqualToString:@"frame"]) {
        CGRect newFrame = CGRectNull;
        CGRect oldFrame = CGRectNull;
        if([object valueForKeyPath:keyPath] != [NSNull null]) {
            //Here is to get the new frame
            newFrame = [[object valueForKeyPath:keyPath] CGRectValue];
            oldFrame = [[change valueForKeyPath:@"old"] CGRectValue];
            if (!CGSizeEqualToSize(newFrame.size, oldFrame.size)) {
                [self setCustomViewFrameWithHeight:newFrame.size.height-oldFrame.size.height];
            }
        }
    }else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

//Pressing down the button event Button shrinks
- (void)bgLongPressEvent:(UIGestureRecognizer *)ges {
    
    //    [self.delegateMarr enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
    //
    //        if ([obj respondsToSelector:@selector(lst_PopViewBgLongPressForPopView:)]) {
    //            [obj lst_PopViewBgLongPressForPopView:self];
    //        }
    //
    //    }];
    
    self.bgLongPressBlock? self.bgLongPressBlock():nil;

    //    switch (ges.state) {
    //        case UIGestureRecognizerStateBegan:
    //        {
    //            CGFloat scale = 0.95;
    //            [UIView animateWithDuration:0.35 animations:^{
    //                ges.view.transform = CGAffineTransformMakeScale(scale, scale);
    //            }];
    //        }
    //            break;
    //        case UIGestureRecognizerStateEnded:
    //        case UIGestureRecognizerStateCancelled:
    //        {
    //            [UIView animateWithDuration:0.35 animations:^{
    //                ges.view.transform = CGAffineTransformMakeScale(1.0, 1.0);
    //            } completion:^(BOOL finished) {
    //            }];
    //        }
    //            break;
    //
    //        default:
    //            break;
    //    }
}

- (void)customViewClickEvent:(UIGestureRecognizer *)ges {
    if (self.isClickFeedback) {
        CGFloat scale = 0.95;
        [UIView animateWithDuration:0.25 animations:^{
            ges.view.transform = CGAffineTransformMakeScale(scale, scale);
        } completion:^(BOOL finished) {
            [UIView animateWithDuration:0.25 animations:^{
                ges.view.transform = CGAffineTransformMakeScale(1.0, 1.0);
            } completion:^(BOOL finished) {
                
            }];
        }];
    }
}

- (void)popViewBgViewTap:(UIButton *)tap {
    
    //    [self.delegateMarr enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
    //
    //        if ([obj respondsToSelector:@selector(lst_PopViewBgClickForPopView:)]) {
    //            [obj lst_PopViewBgClickForPopView:self];
    //        }
    //
    //    }];
    
    if (self.bgClickBlock) {
        if (self.isShowKeyboard) {
            [self endEditing:YES];
        }
        self.bgClickBlock();
    }
    
    if (_isClickBgDismiss) {
        [self dismiss];
    }
}

- (NSTimeInterval)getPopDuration:(NSTimeInterval)currDuration {
    
    if (_popStyle == LSTPopStyleNO) { return 0.0f; }
    
    if (_popStyle == LSTPopStyleFade) { return 0.2f; }
    
    if (currDuration<=0) { self.popDuration = LSTPopViewDefaultDuration; }
    
    if (self.popDuration == LSTPopViewDefaultDuration) {
        NSTimeInterval defaultDuration = 0.0f;

        if (_popStyle == LSTPopStyleScale) {
            defaultDuration = 0.3f;
        }
        if (_popStyle == LSTPopStyleSmoothFromTop ||
            _popStyle == LSTPopStyleSmoothFromLeft ||
            _popStyle == LSTPopStyleSmoothFromBottom ||
            _popStyle == LSTPopStyleSmoothFromRight ||
            _popStyle == LSTPopStyleSpringFromTop ||
            _popStyle == LSTPopStyleSpringFromLeft ||
            _popStyle == LSTPopStyleSpringFromBottom ||
            _popStyle == LSTPopStyleSpringFromRight ||
            _popStyle == LSTPopStyleCardDropFromLeft ||
            _popStyle == LSTPopStyleCardDropFromRight) {
            defaultDuration = 0.6f;
        }
        return defaultDuration;
    } else {
        return currDuration;
    }
}
- (NSTimeInterval)getDismissDuration:(NSTimeInterval)currDuration {
    
    if (_dismissStyle == LSTDismissStyleNO) { return 0.0f; }
    
    if (_dismissStyle == LSTDismissStyleFade) { return 0.2f; }
    
    if (self.dismissDuration == LSTPopViewDefaultDuration) {
        NSTimeInterval defaultDuration = 0.0f;
        if (_dismissStyle == LSTDismissStyleNO) {
            defaultDuration = 0.0f;
        }
        
        if (_dismissStyle == LSTDismissStyleScale) {
            defaultDuration = 0.3f;
        }
        if (_dismissStyle == LSTDismissStyleSmoothToTop ||
            _dismissStyle == LSTDismissStyleSmoothToBottom ||
            _dismissStyle == LSTDismissStyleSmoothToLeft ||
            _dismissStyle == LSTDismissStyleSmoothToRight ||
            _dismissStyle == LSTDismissStyleCardDropToLeft ||
            _dismissStyle == LSTDismissStyleCardDropToRight ||
            _dismissStyle == LSTDismissStyleCardDropToTop) {
            defaultDuration = 0.5f;
        }
        return defaultDuration;
    } else {
        return currDuration;
    }
}
- (NSTimeInterval)getDragDismissDuration {
    if (self.dragDismissDuration ==  LSTPopViewDefaultDuration) {
        return [self getDismissDuration:self.dismissDuration];
    } else {
        return self.dragDismissDuration;
    }
}

- (void)animationWithLayer:(CALayer *)layer duration:(CGFloat)duration values:(NSArray *)values {
    CAKeyframeAnimation *KFAnimation = [CAKeyframeAnimation animationWithKeyPath:@"transform"];
    KFAnimation.duration = duration;
    KFAnimation.removedOnCompletion = NO;
    KFAnimation.fillMode = kCAFillModeForwards;
    //    KFAnimation.delegate = self;//The strong application popView cannot be released
    NSMutableArray *valueArr = [NSMutableArray arrayWithCapacity:values.count];
    for (NSUInteger i = 0; i<values.count; i++) {
        CGFloat scaleValue = [values[i] floatValue];
        [valueArr addObject:[NSValue valueWithCATransform3D:CATransform3DMakeScale(scaleValue, scaleValue, scaleValue)]];
    }
    KFAnimation.values = valueArr;
    KFAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn];
    [layer addAnimation:KFAnimation forKey:nil];
}

- (UIColor *)lst_BlackWithAlpha:(CGFloat)alpha {
    return [UIColor colorWithRed:0 green:0 blue:0 alpha:alpha];
}

- (UIColor *)getNewColorWith:(UIColor *)color alpha:(CGFloat)alpha {
    
    if (self.isHideBg) {
        return UIColor.clearColor;
    }
    if (self.bgColor == UIColor.clearColor) {
        return UIColor.clearColor;
    }
    
    CGFloat red = 0.0;
    CGFloat green = 0.0;
    CGFloat blue = 0.0;
    CGFloat resAlpha = 0.0;
    [color getRed:&red green:&green blue:&blue alpha:&resAlpha];
    UIColor *newColor = [UIColor colorWithRed:red green:green blue:blue alpha:alpha];
    return newColor;
}

/** Delete the designated agent */
- (void)removeForDelegate:(id<LSTPopViewProtocol>)delegate {
    if (delegate) {
        if ([self.delegates containsObject:delegate]) {
            [self.delegates removeObject:delegate];
        }
    }
}

/** Delete the proxy pool Delete all agents */
- (void)removeAllDelegate {
    if (self.delegates.count > 0) {
        [self.delegates removeAllObjects];
    }
}

- (void)beginImpactFeedback {
    if (self.isImpactFeedback) {
        if (@available(iOS 10.0, *)) {
            UIImpactFeedbackGenerator *feedBackGenertor = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleMedium];
            [feedBackGenertor impactOccurred];
        }
    }
}

#pragma mark - ***** Horizontal and vertical screen changes *****

- (void)statusBarOrientationChange:(NSNotification *)notification {
    if (self.isObserverScreenRotation) {
        CGRect originRect = self.customView.frame;
        self.frame = CGRectMake(0, 0, pv_ScreenWidth(), pv_ScreenHeight());
        self.backgroundView.frame = self.bounds;
        self.customView.frame = originRect;
        [self setCustomViewFrameWithHeight:0];
    }
}

#pragma mark - ***** Keyboard pop-up/retract *****

- (void)keyboardWillShow:(NSNotification *)notification{
    //    LSTPVLog(@"keyboardWillShow");
    _isShowKeyboard = YES;
  
    self.keyboardWillShowBlock? self.keyboardWillShowBlock():nil;
    
    if (!self.isAvoidKeyboard) { return; }
    CGFloat customViewMaxY = self.customView.pv_Bottom + self.avoidKeyboardSpace;
    CGFloat duration = [notification.userInfo[UIKeyboardAnimationDurationUserInfoKey] floatValue];
    CGRect keyboardEedFrame = [notification.userInfo[UIKeyboardFrameEndUserInfoKey] CGRectValue];
    CGFloat keyboardMaxY = keyboardEedFrame.origin.y;
    self.isAvoidKeyboard = YES;
    self.avoidKeyboardOffset = customViewMaxY - keyboardMaxY;
    self.keyboardY = keyboardEedFrame.origin.y;
    //The keyboard blocks the pop-up window.
    if ((keyboardMaxY<customViewMaxY) || ((_originFrame.origin.y + _customView.pv_Height) > keyboardMaxY)) {
        [UIView animateWithDuration:duration animations:^{
            self.customView.pv_Y = self.customView.pv_Y - self.avoidKeyboardOffset;
        }];
    }
}

- (void)keyboardDidShow:(NSNotification *)notification{
    _isShowKeyboard = YES;
    self.keyboardDidShowBlock? self.keyboardDidShowBlock():nil;
}

- (void)keyboardWillHide:(NSNotification *)notification{
   
    _isShowKeyboard = NO;
    self.keyboardWillHideBlock? self.keyboardWillHideBlock():nil;
    if (!self.isAvoidKeyboard) {
        return;
    }
    CGFloat duration = [notification.userInfo[UIKeyboardAnimationDurationUserInfoKey] floatValue];
    [UIView animateWithDuration:duration animations:^{
        self.customView.pv_Y = self.originFrame.origin.y;
    }];
}

- (void)keyboardDidHide:(NSNotification *)notification{
    _isShowKeyboard = NO;
    self.keyboardDidHideBlock? self.keyboardDidHideBlock():nil;
}

- (void)keyboardWillChangeFrame:(NSNotification *)notification{
    if (self.keyboardFrameWillChangeBlock) {
        CGRect keyboardBeginFrame = [notification.userInfo[UIKeyboardFrameBeginUserInfoKey] CGRectValue];
        CGRect keyboardEedFrame = [notification.userInfo[UIKeyboardFrameEndUserInfoKey] CGRectValue];
        CGFloat duration = [notification.userInfo[UIKeyboardAnimationDurationUserInfoKey] floatValue];
        self.keyboardFrameWillChangeBlock(keyboardBeginFrame,keyboardEedFrame,duration);
    }
}

- (void)keyboardDidChangeFrame:(NSNotification *)notification{
    if (self.keyboardFrameDidChangeBlock) {
        CGRect keyboardBeginFrame = [notification.userInfo[UIKeyboardFrameBeginUserInfoKey] CGRectValue];
        CGRect keyboardEedFrame = [notification.userInfo[UIKeyboardFrameEndUserInfoKey] CGRectValue];
        CGFloat duration = [notification.userInfo[UIKeyboardAnimationDurationUserInfoKey] floatValue];
        self.keyboardFrameDidChangeBlock(keyboardBeginFrame,keyboardEedFrame,duration);
    }
    
}

#pragma mark - ***** UIGestureRecognizerDelegate *****

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch {
    
    if(gestureRecognizer == self.panGesture) {
        UIView *touchView = touch.view;
        while (touchView != nil) {
            if([touchView isKindOfClass:[UIScrollView class]]) {
                self.isDragScrollView = YES;
                self.scrollerView = (UIScrollView *)touchView;
                break;
            } else if(touchView == self.customView) {
                self.isDragScrollView = NO;
                break;
            }
            touchView = (id)[touchView nextResponder];
        }
    }
    return YES;
}


- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer {
    if(gestureRecognizer == self.tapGesture) {
        //If it is a click gesture
        CGPoint point = [gestureRecognizer locationInView:self.customView];
        BOOL iscontain = [self.customView.layer containsPoint:point];
        if(iscontain) {
            return NO;
        }
    } else if(gestureRecognizer == self.panGesture){
        //If it is a drag gesture added by yourself
        //        LSTPVLog(@"gestureRecognizerShouldBegin");
    }
    return YES;
}

//3. Whether to coexist with other gestures, generally use the default value (default return NO: do not coexist with any gestures)
- (BOOL)gestureRecognizer:(UIPanGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if(gestureRecognizer == self.panGesture) {
        if ([otherGestureRecognizer isKindOfClass:NSClassFromString(@"UIScrollViewPanGestureRecognizer")] || [otherGestureRecognizer isKindOfClass:NSClassFromString(@"UIPanGestureRecognizer")] ) {
            if([otherGestureRecognizer.view isKindOfClass:[UIScrollView class]] ) {
                return YES;
            }
        }
    }
    return NO;
}

//Drag and drop gestures
- (void)pan:(UIPanGestureRecognizer *)panGestureRecognizer {
    
    self.panOffsetBlock?self.panOffsetBlock(CGPointMake(_customView.pv_X-_originFrame.origin.x, _customView.pv_Y-_originFrame.origin.y)):nil;
    
    if (self.dragStyle == LSTDragStyleNO) {return;}
    // Get the offset of the finger
    CGPoint transP = [panGestureRecognizer translationInView:self.customView];

    CGPoint velocity = [panGestureRecognizer velocityInView:[UIApplication sharedApplication].keyWindow];
    if(self.isDragScrollView) {
        //If the current drag is tableView
        if(self.scrollerView.contentOffset.y <= 0) {
            //If tableView is placed at the top
            if(transP.y > 0) {
                //If you drag down
                self.scrollerView.contentOffset = CGPointMake(0, 0);
                self.scrollerView.panGestureRecognizer.enabled = NO;
                self.isDragScrollView = NO;
                //Drag down
                self.customView.frame = CGRectMake(_customView.pv_X, _customView.pv_Y + transP.y, _customView.pv_Width, _customView.pv_Height);
            } else {
                //If you drag up
            }
        }
    } else {
        
        CGFloat customViewX = self.customView.pv_X;
        CGFloat customViewY = self.customView.pv_Y;
        //X moves in the right direction
        if ((self.dragStyle & LSTDragStyleX_Positive) && (customViewX >= _originFrame.origin.x)) {
            if (transP.x > 0) {
                customViewX = customViewX + transP.x;
            } else if(transP.x < 0 && customViewX > _originFrame.origin.x ){
                customViewX = (customViewX + transP.x) > _originFrame.origin.x? (customViewX + transP.x):(_originFrame.origin.x);
            }
        }
        //X Negative direction movement
        if ((self.dragStyle & LSTDragStyleX_Negative) && (customViewX <= self.originFrame.origin.x)) {
            if (transP.x < 0) {
                customViewX = customViewX + transP.x;
            } else if(transP.x > 0 && customViewX < _originFrame.origin.x ){
                customViewX = (customViewX + transP.x) < _originFrame.origin.x? (customViewX + transP.x):(_originFrame.origin.x);
            }
        }
        //Y moves in the right direction
        if (self.dragStyle & LSTDragStyleY_Positive && (customViewY >= _originFrame.origin.y)) {
            if (transP.y > 0) {
                customViewY = customViewY + transP.y;
            } else if(transP.y < 0 && customViewY > _originFrame.origin.y ){
                customViewY = (customViewY + transP.y) > _originFrame.origin.y ?(customViewY + transP.y):(_originFrame.origin.y);
            }
        }
        //Y moves in the negative direction
        if (self.dragStyle & LSTDragStyleY_Negative&&(customViewY <= _originFrame.origin.y)) {
            if (transP.y < 0) {
                customViewY = customViewY + transP.y;
            } else if(transP.y > 0 && customViewY < _originFrame.origin.y){
                customViewY = (customViewY + transP.y) < _originFrame.origin.y?(customViewY + transP.y):(_originFrame.origin.y);
            }
        }
        self.customView.frame = CGRectMake(customViewX, customViewY, _customView.pv_Width, _customView.pv_Height);
    }
    
    [panGestureRecognizer setTranslation:CGPointZero inView:self.customView];

    if(panGestureRecognizer.state == UIGestureRecognizerStateEnded) {
        
        if (self.scrollerView) {
            self.scrollerView.panGestureRecognizer.enabled = YES;
        }

        LSTPopViewWK(self)
        //Drag and release the back Block
        void (^dragReboundBlock)(void) = ^ {
            [UIView animateWithDuration:wk_self.dragReboundTime
                                  delay:0.1f
                                options:UIViewAnimationOptionCurveEaseOut
                             animations:^{
                CGRect frame = wk_self.customView.frame;
                frame.origin.y = wk_self.frame.size.height - wk_self.customView.frame.size.height;
                wk_self.customView.frame = wk_self.originFrame;
            } completion:^(BOOL finished) {}];
        };
        //Scan to remove Block
        void (^sweepBlock)(BOOL,BOOL,BOOL,BOOL) = ^(BOOL isX_P, BOOL isX_N, BOOL isY_P, BOOL isY_N) {
            
            if (isX_P==NO && isX_N==NO && isY_P==NO && isY_N==NO) {
                dragReboundBlock();
                return;
            }
            
            if (isY_P==NO && isY_N==NO && self.sweepDismissStyle == LSTSweepDismissStyleSmooth) {//The X-axis can be swiped
                if (velocity.x>0) {
                    [self dismissWithStyle:LSTDismissStyleSmoothToRight duration:self.dismissDuration];
                } else {
                    [self dismissWithStyle:LSTDismissStyleSmoothToLeft duration:self.dismissDuration];
                }
                return;
            }
            
            if (isX_P==NO && isX_N==NO && self.sweepDismissStyle == LSTSweepDismissStyleSmooth) {//The Y axis can be swiped
                if (velocity.y>0) {
                    [self dismissWithStyle:LSTDismissStyleSmoothToBottom duration:self.dismissDuration];
                } else {
                    [self dismissWithStyle:LSTDismissStyleSmoothToTop duration:self.dismissDuration];
                }
                return;
            }
            // Remove and fly out at gesture speed
            [UIView animateWithDuration:0.5 animations:^{
                wk_self.backgroundView.backgroundColor = [self getNewColorWith:self.bgColor alpha:0.0];
                self.customView.center = CGPointMake(isX_P || isX_N?velocity.x:self.customView.pv_CenterX, isY_P||isY_N?velocity.y:self.customView.pv_CenterY);
            } completion:^(BOOL finished) {
                [wk_self dismissWithStyle:LSTDismissStyleFade duration:0.1];
            }];
        };
        
        double velocityX =  sqrt(pow(velocity.x, 2));
        double velocityY =  sqrt(pow(velocity.y, 2));
        if((velocityX >= self.swipeVelocity)||(velocityY >= self.swipeVelocity)) {//Swipe
            
            if (self.scrollerView.contentOffset.y>0) {
                return;
            }
            
            //The direction that can be swiped to remove
            BOOL isX_P = NO;
            BOOL isX_N = NO;
            BOOL isY_P = NO;
            BOOL isY_N = NO;
            
            if ((self.dragStyle & LSTDragStyleX_Positive) && velocity.x>0 && velocityX >= self.swipeVelocity) {
                isX_P = self.sweepStyle & LSTSweepStyleX_Positive? YES:NO;
            }
            if ((self.dragStyle & LSTDragStyleX_Negative) && velocity.x<0 && velocityX >= self.swipeVelocity) {
                isX_N = self.sweepStyle & LSTSweepStyleX_Negative? YES:NO;
            }
            
            if ((self.dragStyle & LSTDragStyleY_Positive) && velocity.y>0 && velocityY >= self.swipeVelocity) {
                isY_P = self.sweepStyle & LSTSweepStyleY_Positive? YES:NO;
            }
            if ((self.dragStyle & LSTDragStyleY_Negative) && velocity.y <0 && velocityY >= self.swipeVelocity) {
                isY_N = self.sweepStyle & LSTSweepStyleY_Negative? YES:NO;
            }
            sweepBlock(isX_P,isX_N,isY_P,isY_N);
//            LSTPVLog(@"isX=%@,isY=%@,velocityX=%lf,velocityY=%lf",isX?@"YES":@"NO",isY?@"YES":@"NO",velocityX,velocityY);
        }else {//Ordinary drag
            BOOL isCanDismiss = NO;
            if (self.dragStyle & LSTDragStyleAll) {
                
                if (fabs(self.customView.frame.origin.x - self.originFrame.origin.x)>=self.dragDistance && self.dragDistance!=0) {
                    isCanDismiss = YES;
                }
                if (fabs(self.customView.frame.origin.y - self.originFrame.origin.y)>=self.dragDistance && self.dragDistance!=0) {
                    isCanDismiss = YES;
                }
                
                if (isCanDismiss) {
                    [self dismissWithStyle:_isDragDismissStyle? self.dragDismissStyle:self.dismissStyle
                                  duration:self.getDragDismissDuration];
                }else {
                    dragReboundBlock();
                }
            } else {
                dragReboundBlock();
            }
        }
    }
}

#pragma mark - ***** Lazy loading *****

- (NSHashTable<id<LSTPopViewProtocol>> *)delegates {
    if (_delegates) return _delegates;
    _delegates = [NSHashTable hashTableWithOptions:NSPointerFunctionsWeakMemory];
    return _delegates;
}

- (UIView *)parentView {
    return self.container;
}

- (UIView *)currCustomView {
    return self.customView;
}

#pragma mark - ***** The following is the window management api *****

/** Save popView */
+ (void)savePopView:(LSTPopView *)popView {
    [LSTPopViewManager savePopView:popView];
}

/** Get all popView globally (in the whole app) */
+ (NSArray *)getAllPopView {
    return [LSTPopViewManager getAllPopView];
}

/** Get all popView on the current page */
+ (NSArray *)getAllPopViewForParentView:(UIView *)parentView {
    return [LSTPopViewManager getAllPopViewForParentView:parentView];
}

/** Get all popViews of the specified formation on the current page */
+ (NSArray *)getAllPopViewForPopView:(LSTPopView *)popView {
    return [LSTPopViewManager getAllPopViewForPopView:popView];
}

/**
 Read popView (may read pop-ups across formations)
 It is recommended to use the getPopViewForGroupId:forkey: method for accurate reading.
 */
+ (LSTPopView *)getPopViewForKey:(NSString *)key {
    return [LSTPopViewManager getPopViewForKey:key];
}

/** Remove popView */
+ (void)removePopView:(LSTPopView *)popView {
    [LSTPopViewManager removePopView:popView];
}

/**
 Remove popView through the unique key (pop-up windows may be deleted by mistake across formations)
 It is recommended to use the removePopViewForGroupId:forkey: method for precise deletion.
 */
+ (void)removePopViewForKey:(NSString *)key {
    [LSTPopViewManager removePopViewForKey:key];
}

/** Remove all popView */
+ (void)removeAllPopView {
    [LSTPopViewManager removeAllPopView];
}

/** Remove the last pop-up popView */
+ (void)removeLastPopView {
    return [LSTPopViewManager removeLastPopView];
}

/** Turn on the debugging view. It is recommended to set it to online hiding test opening */
+ (void)setLogStyle:(LSTPopViewLogStyle)logStyle {
    _logStyle = logStyle;
    
    if (logStyle == LSTPopViewLogStyleNO) {
        [LSTPopViewM().infoView removeFromSuperview];
        LSTPopViewM().infoView = nil;
    }
}

@end



