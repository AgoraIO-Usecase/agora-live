//
//  EffectCollectionViewCell.m
//  AgoraEntScenarios
//
//  Created by CP on 2023/6/1.
//

#import "EffectCollectionViewCell.h"
#import "Masonry/Masonry.h"

@implementation EffectCollectionViewCell

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        [self setupSubviews];
    }
    return self;
}

- (void)setupSubviews {
    self.clipsToBounds = YES;
    
    //初始化背景图片
    self.bgImageView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, CGRectGetWidth(self.frame), CGRectGetHeight(self.frame))];
    self.bgImageView.contentMode = UIViewContentModeScaleAspectFill;
    self.bgImageView.layer.cornerRadius = 6;
    self.bgImageView.layer.masksToBounds = YES;
    [self.contentView addSubview:self.bgImageView];
    [self.bgImageView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.top.bottom.mas_equalTo(0);
    }];
    
    //初始化文本标签
    self.titleLabel = [[UILabel alloc] initWithFrame:CGRectMake(12, 0, CGRectGetWidth(self.frame)-12, CGRectGetHeight(self.frame))];
    self.titleLabel.textColor = [UIColor whiteColor];
    self.titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
    self.titleLabel.textAlignment = NSTextAlignmentCenter;
    self.titleLabel.numberOfLines = 0;
    self.titleLabel.lineBreakMode = NSLineBreakByWordWrapping;
    [self.contentView addSubview:self.titleLabel];
    [self.titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.center.equalTo(self);
        make.width.equalTo(self).mas_equalTo(-12);
    }];
 }


@end
