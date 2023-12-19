//
//  AEACategoryView.h
//  AgoraEditAvatar
//
//  Created by FanPengpeng on 2022/9/20.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN



@interface CommerceAEACategoryItem : NSObject

@property (strong, nonatomic) UIImage *normalImage;
@property (strong, nonatomic) UIImage *selectedImage;

+ (instancetype)itemWithNormalImage:(UIImage * __nullable)normalImage selectedImage:(UIImage * __nullable)selectedImage;

@end

@class CommerceAEACategoryView;
@protocol CommerceAEACategoryViewDelegate <NSObject>

- (void)categoryView:(CommerceAEACategoryView *)categoryView didSelectItem:(CommerceAEACategoryItem *)item index:(NSInteger) index;

@end

@interface CommerceAEACategoryViewLayout : NSObject

@property (assign, nonatomic) CGSize itemSize;
@property (assign, nonatomic) CGFloat minSpacing;
@property (assign, nonatomic) UIEdgeInsets contentInsets;

+ (instancetype)defaultLayout;

@end

@interface CommerceAEACategoryView : UIView

@property (assign, nonatomic) NSInteger defaultSelectedIndex;

@property (assign, nonatomic) BOOL showBottomLine;

@property (strong, nonatomic) NSArray<NSString *> *titles;

@property (strong, nonatomic) NSArray<CommerceAEACategoryItem *> *items;

@property (weak, nonatomic) id<CommerceAEACategoryViewDelegate> delegate;
@property (nullable,strong, nonatomic) UIFont *titleFont;
@property (nullable,strong, nonatomic) UIFont *titleSelectedFont;
@property (nullable,strong, nonatomic) UIColor *titleColor;
@property (nullable,strong, nonatomic) UIColor *titleSelectedColor;
@property (nullable,strong, nonatomic) UIView *indicator;

+ (instancetype)defaultCategoryView;

+ (instancetype)categoryViewWithLayout:(CommerceAEACategoryViewLayout *)layout;

- (instancetype)init NS_UNAVAILABLE;

- (instancetype)initWithFrame:(CGRect)frame NS_UNAVAILABLE;

- (instancetype)initWithCoder:(NSCoder *)aDecoder NS_UNAVAILABLE;

@end

NS_ASSUME_NONNULL_END
