//
//  VLSRSwitcherView.m
//  VoiceOnLine
//

#import "VLSettingAIAECSwitcherView.h"
#import "AESMacro.h"
#import "VLToast.h"
@import Masonry;

@interface VLSettingAIAECSwitcherView()<UITextFieldDelegate>

@property (nonatomic, strong) UISlider *sliderView;
@property (nonatomic, assign) NSInteger minValue;
@property (nonatomic, assign) NSInteger maxValue;
@property (nonatomic, strong) UIButton *addButton;
@property (nonatomic, strong) UIButton *reduceButton;
@property (nonatomic, strong) UILabel *minLabel;
@property (nonatomic, strong) UILabel *maxLabel;
@property (nonatomic, strong) UILabel *titleLabel;
@end

@implementation VLSettingAIAECSwitcherView

- (instancetype)initWithMax:(NSInteger)max min:(NSInteger)min title:(NSString *)title {
    if (self = [super init]) {
        self.maxValue = max;
        self.minValue = min;
        [self createViews];
        [self createConstraints];
        _minLabel.text = [NSString stringWithFormat:@"%ld", _minValue];
        _maxLabel.text = [NSString stringWithFormat:@"%ld", _maxValue];
        _titleLabel.text = title;
    }
    return self;
}

- (void)createViews {
    [self addSubview:self.titleLabel];
    [self addSubview:self.sliderView];
    [self addSubview:self.reduceButton];
    [self addSubview:self.addButton];
    [self addSubview:self.minLabel];
    [self addSubview:self.maxLabel];
}

- (void)createConstraints {
    [self.titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.centerY.mas_equalTo(self);
    }];
    [self.reduceButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(90);
        make.width.mas_equalTo(@(26));
        make.centerY.mas_equalTo(self);
    }];
    [self.addButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.mas_equalTo(self).offset(-8);
        make.width.mas_equalTo(@(26));
        make.centerY.mas_equalTo(self);
    }];
    [self.minLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.reduceButton.mas_right);
        make.centerY.equalTo(self);
        make.width.mas_equalTo(30);
    }];
    [self.maxLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self.addButton.mas_left);
        make.centerY.equalTo(self);
        make.width.mas_equalTo(30);
    }];
    [self.sliderView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self.maxLabel.mas_left);
        make.centerY.mas_equalTo(self);
        make.left.mas_equalTo(self.minLabel.mas_right);
        make.height.mas_equalTo(15);
    }];
}

- (void)setValue:(NSInteger)value {
    _value = value;
    self.sliderView.value = value;
}

//处理
- (void)onSliderChanged:(UISlider*)slider {
    NSInteger sliderValue = slider.value;
    if (_value != sliderValue) {
        [self updateValueFrom:_value to:sliderValue];
        _value = sliderValue;
    }
}

- (void)onClickAdd:(UIButton *)sender {
    if (_value < self.maxValue) {
        [self updateValueFrom:_value to:_value + 1];
        _value++;
        self.sliderView.value = _value;
    }
}

- (void)onClickReduce:(UIButton *)sender {
    if (_value > self.minValue) {
        [self updateValueFrom:_value to:_value - 1];
        _value--;
        self.sliderView.value = _value;
    }
}

- (void)updateValueFrom:(NSInteger)from to:(NSInteger)to {
    if (from == 0) {
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:on:)]) {
            [self.delegate aecSwitcherView:self on:true];
        }
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:level:)]) {
            [self.delegate aecSwitcherView:self level:to];
        }
    } else if (to == 0) {
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:on:)]) {
            [self.delegate aecSwitcherView:self on:false];
        }
    } else {
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:level:)]) {
            [self.delegate aecSwitcherView:self level:to];
        }
    }
}

#pragma mark - Lazy

- (UISlider *)sliderView {
    if (!_sliderView) {
        _sliderView = [[UISlider alloc]init];
        [_sliderView setThumbImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_slider" ] forState:UIControlStateNormal];
        [_sliderView setThumbImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_slider" ] forState:UIControlStateHighlighted];
        _sliderView.maximumValue = self.maxValue;
        _sliderView.minimumValue = self.minValue;
        [_sliderView addTarget:self action:@selector(onSliderChanged:) forControlEvents:UIControlEventTouchUpInside];
        [_sliderView addTarget:self action:@selector(onSliderChanged:) forControlEvents:UIControlEventTouchUpOutside];
    }
    return _sliderView;
}

- (UIButton *)addButton {
    if (!_addButton) {
        _addButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_addButton setImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_add" ] forState:UIControlStateNormal];
        [_addButton addTarget:self action:@selector(onClickAdd:) forControlEvents:UIControlEventTouchUpInside];
    }
    return _addButton;
}

- (UIButton *)reduceButton {
    if (!_reduceButton) {
        _reduceButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_reduceButton setImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_reduce" ] forState:UIControlStateNormal];
        [_reduceButton addTarget:self action:@selector(onClickReduce:) forControlEvents:UIControlEventTouchUpInside];
    }
    return _reduceButton;
}

- (UILabel *)minLabel {
    if (!_minLabel) {
        _minLabel = [[UILabel alloc] init];
        _minLabel.font = [UIFont systemFontOfSize:11];
        _minLabel.textColor = [UIColor colorWithHexString:@"#BABCCD"];
        _minLabel.textAlignment = NSTextAlignmentCenter;
    }
    return _minLabel;
}

- (UILabel *)maxLabel {
    if (!_maxLabel) {
        _maxLabel = [[UILabel alloc] init];
        _maxLabel.font = [UIFont systemFontOfSize:11];
        _maxLabel.textColor = [UIColor colorWithHexString:@"#BABCCD"];
        _maxLabel.textAlignment = NSTextAlignmentCenter;
    }
    return _maxLabel;
}

- (UILabel *)titleLabel {
    if (!_titleLabel) {
        _titleLabel = [[UILabel alloc] init];
        _titleLabel.text = @"";
        _titleLabel.font = [UIFont systemFontOfSize:15];
        _titleLabel.textColor = [UIColor whiteColor];
    }
    return _titleLabel;
}

@end
