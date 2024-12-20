//
//  VLKTVItemBaseView.m
//  VoiceOnLine
//

#import "VLKTVItemBaseView.h"
@import Masonry;
@interface VLKTVItemBaseView()

@end

@implementation VLKTVItemBaseView

- (instancetype)init {
    if (self = [super init]) {
        // (Parent method) If the subview implements this method here, it will be directly transferred to the subview and its own method will not be loaded.
//        [self initSubViews];
//        [self addSubViewConstraints];
        
        [self setupSubViews];
        [self setupConstraints];
    }
    return self;
}

- (void)setupSubViews {
    [self addSubview:self.titleLabel];
}

- (void)setupConstraints {
    [self.titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.centerY.mas_equalTo(self);
    }];
}

- (UILabel *)titleLabel {
    if (!_titleLabel) {
        _titleLabel = [[UILabel alloc] init];
        _titleLabel.text = @"";
        _titleLabel.font = [UIFont systemFontOfSize:15];
        _titleLabel.textColor = [UIColor whiteColor];
       // _titleLabel.textColor = UIColorMakeWithHex(@"#979CBB");
    }
    return _titleLabel;
}

@end
