//
//  VLKTVSwitcherView.m
//  VoiceOnLine
//

#import "VLKTVSwitcherView.h"
#import "AgoraEntScenarios-Swift.h"
#import "AESMacro.h"
@import Masonry;

@interface VLKTVSwitcherView()

@property (nonatomic, strong) UILabel *subLabel;
@property (nonatomic, strong) UIImageView *rightIcon;
@property (nonatomic, strong) UIButton *coverBtn;
@end

@implementation VLKTVSwitcherView


- (instancetype)init {
    if (self = [super init]) {
        [self initSubViews];
        [self addSubViewConstraints];
    }
    return self;
}

- (void)initSubViews {
    [self addSubview:self.rightIcon];
    [self addSubview:self.subLabel];
    [self addSubview:self.coverBtn];
}

- (void)addSubViewConstraints {
    
    [self.rightIcon mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.mas_equalTo(self).offset(-8);
        make.centerY.mas_equalTo(self);
        make.width.mas_equalTo(@(16));
        make.height.mas_equalTo(@(16));
    }];
    
    [self.subLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.mas_equalTo(self);
        make.right.mas_equalTo(self.rightIcon.mas_left).offset(-10);
    }];
    
    [self.coverBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.top.bottom.mas_equalTo(self);
    }];
    
}

- (void)setOn:(BOOL)on {
    _on = on;
    self.subLabel.text = _on ? KTVLocalizedString(@"ktv_soundcard_open") : KTVLocalizedString(@"ktv_close_aec");
}

- (UILabel *)subLabel {
    if (!_subLabel) {
        _subLabel = [[UILabel alloc] init];
        _subLabel.font = [UIFont systemFontOfSize:12];
        _subLabel.textColor = UIColorMakeWithHex(@"#6C7192");
    }
    return _subLabel;
}

-(UIImageView *)rightIcon {
    if(!_rightIcon){
        _rightIcon = [[UIImageView alloc]init];
        _rightIcon.image = [UIImage ktv_sceneImageWithName:@"ktv_arrow_right" ];
    }
    return _rightIcon;
}

-(UIButton *)coverBtn {
    if(!_coverBtn){
        _coverBtn = [[UIButton alloc]init];
        _coverBtn.backgroundColor = [UIColor clearColor];
        [_coverBtn addTarget:self action:@selector(click) forControlEvents:UIControlEventTouchUpInside];
    }
    return _coverBtn;
}

-(void)click {
    if([_delegate respondsToSelector:@selector(switcherView:on:)]){
        [self.delegate switcherView:self on:_on];
    }
}

@end
