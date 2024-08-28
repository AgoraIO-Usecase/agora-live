//
//  VLHomeItemView.m
//  VoiceOnLine
//

#import "VLHomeItemView.h"
#import "VLHomeItemModel.h"
#import "VLMacroDefine.h"
#import "AESMacro.h"
#import <AgoraCommon/QMUICommonDefines.h>

@interface VLHomeItemView ()

@property (nonatomic, strong) UIImageView *bgImgView;
@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UIView *subTitleBgView;
@property (nonatomic, strong) UILabel *subTitleLabel;
@property (nonatomic, strong) UIImageView *itemImgView;



@end

@implementation VLHomeItemView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self setupView];
    }
    return self;
}

- (void)setupView {
    [self layoutIfNeeded];
    [self addSubview:self.bgImgView];
    [[self.bgImgView.leadingAnchor constraintEqualToAnchor:self.leadingAnchor] setActive:YES];
    [[self.bgImgView.topAnchor constraintEqualToAnchor:self.topAnchor] setActive:YES];
    [[self.bgImgView.trailingAnchor constraintEqualToAnchor:self.trailingAnchor] setActive:YES];
    [[self.bgImgView.bottomAnchor constraintEqualToAnchor:self.bottomAnchor] setActive:YES];
    
    [self addSubview:self.titleLabel];
    [[self.titleLabel.leadingAnchor constraintEqualToAnchor:self.leadingAnchor constant:22] setActive:YES];
    [[self.titleLabel.topAnchor constraintEqualToAnchor:self.topAnchor constant:VLREALVALUE_WIDTH(25)] setActive:YES];
    [[self.titleLabel.trailingAnchor constraintEqualToAnchor:self.trailingAnchor constant: -22] setActive:YES];
    
    [self addSubview:self.subTitleBgView];
    [[self.subTitleBgView.leadingAnchor constraintEqualToAnchor:self.titleLabel.leadingAnchor] setActive:YES];
    [[self.subTitleBgView.topAnchor constraintEqualToAnchor:self.titleLabel.bottomAnchor constant:3] setActive:YES];
    [[self.subTitleBgView.trailingAnchor constraintEqualToAnchor:self.titleLabel.trailingAnchor] setActive:YES];
    
    [self.subTitleBgView addSubview:self.subTitleLabel];
    [[self.subTitleLabel.leadingAnchor constraintEqualToAnchor:self.subTitleBgView.leadingAnchor] setActive:YES];
    [[self.subTitleLabel.topAnchor constraintEqualToAnchor:self.subTitleBgView.topAnchor] setActive:YES];
    [[self.subTitleLabel.trailingAnchor constraintEqualToAnchor:self.subTitleBgView.trailingAnchor] setActive:YES];
    
    [self addSubview:self.itemImgView];
}

- (UIImageView *)bgImgView {
    if (!_bgImgView) {
        _bgImgView = [[UIImageView alloc]init];
        _bgImgView.userInteractionEnabled = YES;
        _bgImgView.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return _bgImgView;
}

- (UILabel *)titleLabel {
    if (!_titleLabel) {
        _titleLabel = [[UILabel alloc]init];
        _titleLabel.font = [UIFont fontWithName:@"PingFangSC" size:22];
        _titleLabel.font = [UIFont systemFontOfSize:18 weight:UIFontWeightSemibold];
        _titleLabel.numberOfLines = 0;
        _titleLabel.textColor = UIColorMakeWithHex(@"#FFFFFF");
        _titleLabel.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return _titleLabel;
}

- (UIView *)subTitleBgView {
    if (!_subTitleBgView) {
        _subTitleBgView = [[UIImageView alloc]init];
        _subTitleBgView.layer.cornerRadius = 10;
        _subTitleBgView.layer.masksToBounds = YES;
        _subTitleBgView.translatesAutoresizingMaskIntoConstraints = NO;
//        _subTitleBgView.backgroundColor = UIColorMakeWithRGBA(0, 0, 0, 0.3);
    }
    return _subTitleBgView;
}

- (UILabel *)subTitleLabel {
    if (!_subTitleLabel) {
        _subTitleLabel = [[UILabel alloc]init];
        _subTitleLabel.textAlignment = NSTextAlignmentLeft;
        _subTitleLabel.font = [UIFont fontWithName:@"PingFangSC" size:10];
        _subTitleLabel.font = [UIFont systemFontOfSize:10 weight:UIFontWeightMedium];
        _subTitleLabel.textColor = UIColorMakeWithHex(@"#FFFFFF");
        _subTitleLabel.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return _subTitleLabel;
}

- (UIImageView *)itemImgView {
    if (!_itemImgView) {
        _itemImgView = [[UIImageView alloc]initWithFrame:CGRectMake((self.width-162)*0.5, self.height-12-121, 162, 121)];
        _itemImgView.userInteractionEnabled = YES;
    }
    return _itemImgView;
}


- (void)setItemModel:(VLHomeItemModel *)itemModel {
    _itemModel = itemModel;
    self.bgImgView.image = UIImageMake(itemModel.bgImgStr);
    
    NSMutableAttributedString *attri = [[NSMutableAttributedString alloc] initWithString:itemModel.titleStr];
    NSMutableParagraphStyle *paragraphStyle = [[NSMutableParagraphStyle alloc] init];
    paragraphStyle.lineBreakMode = NSLineBreakByTruncatingTail;
    [attri addAttribute:NSParagraphStyleAttributeName value:paragraphStyle range:NSMakeRange(0, attri.length)];
    self.titleLabel.attributedText = attri;
    
    self.subTitleLabel.text = itemModel.subTitleStr;
    self.itemImgView.image = UIImageMake(itemModel.iconImgStr);
    self.subTitleBgView.hidden = !itemModel.subTitleStr.length;
}

@end
