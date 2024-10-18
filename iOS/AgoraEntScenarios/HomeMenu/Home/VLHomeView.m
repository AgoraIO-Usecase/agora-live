//
//  VLHomeView.m
//  VoiceOnLine
//

#import "VLHomeView.h"
#import "VLHomeItemView.h"
#import "VLHomeItemModel.h"
#import "VLMacroDefine.h"
#import "MenuUtils.h"
#import "AESMacro.h"
@import YYCategories;

@interface VLHomeView ()<UICollectionViewDelegate,UICollectionViewDataSource>

@property (nonatomic, weak) id <VLHomeViewDelegate>delegate;
@property (nonatomic) UICollectionView *menuList;
@property (nonatomic, strong) NSArray *itemsArray;
@property (nonatomic) NSArray *models;

@end

@implementation VLHomeView

- (instancetype)initWithDelegate:(id<VLHomeViewDelegate>)delegate {
    if (self = [super init]) {
        self.delegate = delegate;
        [self addSubview:self.menuList];
        [[self.menuList.leadingAnchor constraintEqualToAnchor:self.leadingAnchor] setActive:YES];
        [[self.menuList.topAnchor constraintEqualToAnchor:self.topAnchor] setActive:YES];
        [[self.menuList.trailingAnchor constraintEqualToAnchor:self.trailingAnchor] setActive:YES];
        [[self.menuList.bottomAnchor constraintEqualToAnchor:self.bottomAnchor] setActive:YES];
    }
    return self;
}

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLHomeViewDelegate>)delegate {
    if (self = [super initWithFrame:frame]) {
        self.delegate = delegate;
        [self addSubview:self.menuList];
        [[self.menuList.leadingAnchor constraintEqualToAnchor:self.leadingAnchor] setActive:YES];
        [[self.menuList.topAnchor constraintEqualToAnchor:self.topAnchor] setActive:YES];
        [[self.menuList.trailingAnchor constraintEqualToAnchor:self.trailingAnchor] setActive:YES];
        [[self.menuList.bottomAnchor constraintEqualToAnchor:self.bottomAnchor] setActive:YES];
    }
    return self;
}

- (UICollectionViewFlowLayout *)flowLayout {
    UICollectionViewFlowLayout *layout = [[UICollectionViewFlowLayout alloc] init];
    CGFloat leftMargin = VLREALVALUE_WIDTH(20);
    CGFloat middleMargin = VLREALVALUE_WIDTH(11);
    CGFloat lineMargin = VLREALVALUE_WIDTH(14);
    CGFloat itemWidth = (SCREEN_WIDTH-2*leftMargin-middleMargin)/2.0;
    layout.itemSize = CGSizeMake(itemWidth, itemWidth*1.4);
    layout.minimumLineSpacing = lineMargin;
    layout.minimumInteritemSpacing = middleMargin;
    layout.sectionInset = UIEdgeInsetsMake(20, 20, 20, 20);
    return layout;
}

- (NSArray *)models {
    if (!_models) {
        _models = [VLHomeItemModel vj_modelArrayWithJson:self.itemsArray];
    }
    return _models;
}

- (UICollectionView *)menuList {
    if (!_menuList) {
        _menuList = [[UICollectionView alloc] initWithFrame:CGRectZero collectionViewLayout:[self flowLayout]];
        _menuList.backgroundColor = [UIColor clearColor];
        _menuList.dataSource = self;
        _menuList.delegate = self;
        [_menuList registerClass:[HomeMenuCell class] forCellWithReuseIdentifier:@"HomeMenuCell"];
        _menuList.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return _menuList;
}

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section {
    return self.itemsArray.count;
}

- (__kindof UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath {
    HomeMenuCell *cell = (HomeMenuCell *)[collectionView dequeueReusableCellWithReuseIdentifier:@"HomeMenuCell" forIndexPath:indexPath];
    [cell refreshWithItem:self.models[indexPath.row]];
    return cell;
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath {
    [collectionView deselectItemAtIndexPath:indexPath animated:YES];
    if (self.delegate && [self.delegate respondsToSelector:@selector(itemClickAction:)]) {
        [self.delegate itemClickAction:(int)indexPath.row];
        HomeMenuCell *cell = (HomeMenuCell *)[collectionView cellForItemAtIndexPath:indexPath];
        [cell handleClick];
    }
}

- (NSArray *)itemsArray {
    if (!_itemsArray) {
        _itemsArray = @[
            @{
                @"bgImgStr":@"home_live_bg",
                @"iconImgStr":@"home_live_icon",
                @"titleStr":NSLocalizedString(@"app_show_live", nil),
                @"subTitleStr":@""
            },
            @{
                @"bgImgStr":@"home_commerce_bg",
                @"iconImgStr":@"home_commerce_icon",
                @"titleStr":NSLocalizedString(@"app_e_commerce", nil),
                @"subTitleStr":@""
            },
            @{
                @"bgImgStr":@"home_talk_bg",
                @"iconImgStr":@"home_talk_icon",
                @"titleStr":NSLocalizedString(@"app_voice_chat", nil),
                @"subTitleStr":@""
            },
            @{
                @"bgImgStr":@"home_KTV_bg",
                @"iconImgStr":@"home_KTV_icon",
                @"titleStr":NSLocalizedString(@"app_online_ktv", nil),
                @"subTitleStr":@""
            }
        ];
    }
    return _itemsArray;
}

@end
