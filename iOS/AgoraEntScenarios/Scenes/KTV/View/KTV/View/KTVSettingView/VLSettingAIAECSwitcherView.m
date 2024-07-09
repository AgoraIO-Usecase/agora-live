//
//  VLSRSwitcherView.m
//  VoiceOnLine
//

#import "VLSettingAIAECSwitcherView.h"
#import "AESMacro.h"
#import "VLToast.h"
@import Masonry;

@interface VLSettingAIAECSwitcherView()<UITextFieldDelegate>

@property (nonatomic, assign) NSInteger value;
@property (nonatomic, strong) UISwitch *switcher;
@property (nonatomic, strong) UISlider *sliderView;
@property (nonatomic, assign) NSInteger minValue;
@property (nonatomic, assign) NSInteger maxValue;
@property (nonatomic, strong) UIButton *addButton;
@property (nonatomic, strong) UIButton *reduceButton;
@property (nonatomic, strong) UILabel *minLabel;
@property (nonatomic, strong) UILabel *maxLabel;
@property (nonatomic, strong) UILabel *switchLabel;
@property (nonatomic, strong) UILabel *sliderLabel;
@end

@implementation VLSettingAIAECSwitcherView

- (instancetype)initWithMax:(NSInteger)max min:(NSInteger)min {
    if (self = [super init]) {
        self.maxValue = max;
        self.minValue = min;
        [self createViews];
        [self createConstraints];
        _minLabel.text = [NSString stringWithFormat:@"%ld", _minValue];
        _maxLabel.text = [NSString stringWithFormat:@"%ld", _maxValue];
    }
    return self;
}

- (void)createViews {
    [self addSubview:self.switchLabel];
    [self addSubview:self.switcher];
    [self addSubview:self.sliderLabel];
    [self addSubview:self.sliderView];
    [self addSubview:self.reduceButton];
    [self addSubview:self.addButton];
    [self addSubview:self.minLabel];
    [self addSubview:self.maxLabel];
}

- (void)createConstraints {
    [self.switchLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.mas_equalTo(20);
        make.left.mas_equalTo(20);
    }];
    [self.switcher mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.equalTo(self.switchLabel);
        make.right.mas_equalTo(-20);
    }];
    [self.sliderLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.top.equalTo(self.switchLabel.mas_bottom).offset(32);
    }];
    [self.reduceButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(90);
        make.width.mas_equalTo(@(26));
        make.centerY.mas_equalTo(self.sliderLabel);
    }];
    [self.addButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.mas_equalTo(self).offset(-8);
        make.width.mas_equalTo(@(26));
        make.centerY.mas_equalTo(self.sliderLabel);
    }];
    [self.minLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.reduceButton.mas_right);
        make.centerY.equalTo(self.sliderLabel);
        make.width.mas_equalTo(30);
    }];
    [self.maxLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self.addButton.mas_left);
        make.centerY.equalTo(self.sliderLabel);
        make.width.mas_equalTo(30);
    }];
    [self.sliderView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self.maxLabel.mas_left);
        make.centerY.mas_equalTo(self.sliderLabel);
        make.left.mas_equalTo(self.minLabel.mas_right);
        make.height.mas_equalTo(15);
    }];
}

- (void)setOn:(BOOL)on value:(NSInteger)value {
    [_switcher setOn:on];
    [self updateSwitcherOn:on];
    _value = value;
    self.sliderView.value = value;
}

//处理
- (void)onClickSwitcher:(UISwitch *)sender {
    if ([self.delegate respondsToSelector:@selector(aecSwitcherView:on:)]) {
        [self.delegate aecSwitcherView:self on:sender.isOn];
    }
    [self updateSwitcherOn:sender.isOn];
}

- (void)updateSwitcherOn:(BOOL)isOn {
    if (isOn) {
        _sliderLabel.hidden = false;
        _sliderView.hidden = false;
        _addButton.hidden = false;
        _reduceButton.hidden = false;
    } else {
        _sliderLabel.hidden = true;
        _sliderView.hidden = true;
        _addButton.hidden = true;
        _reduceButton.hidden = true;
    }
}


- (void)onSliderChanged:(UISlider*)slider {
    NSInteger sliderValue = slider.value;
    if (_value != sliderValue) {
        _value = sliderValue;
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:level:)]) {
            [self.delegate aecSwitcherView:self level:_value];
        }
    }
}

- (void)onClickAdd:(UIButton *)sender {
    if (_value < self.maxValue) {
        _value++;
        self.sliderView.value = _value;
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:level:)]) {
            [self.delegate aecSwitcherView:self level:_value];
        }
    }
}

- (void)onClickReduce:(UIButton *)sender {
    if (_value > self.minValue) {
        _value--;
        self.sliderView.value = _value;
        if ([self.delegate respondsToSelector:@selector(aecSwitcherView:level:)]) {
            [self.delegate aecSwitcherView:self level:_value];
        }
    }
}

#pragma mark - Lazy

- (UISwitch *)switcher {
    if (!_switcher) {
        _switcher = [[UISwitch alloc] init];
        _switcher.onTintColor = UIColorMakeWithHex(@"#009FFF");
        [_switcher addTarget:self action:@selector(onClickSwitcher:) forControlEvents:UIControlEventTouchUpInside];
    }
    return _switcher;
}

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

- (UILabel *)switchLabel {
    if (!_switchLabel) {
        _switchLabel = [[UILabel alloc] init];
        _switchLabel.text = KTVLocalizedString(@"ktv_aiaec_switch");
        _switchLabel.font = [UIFont systemFontOfSize:15];
        _switchLabel.textColor = [UIColor whiteColor];
    }
    return _switchLabel;
}

- (UILabel *)sliderLabel {
    if (!_sliderLabel) {
        _sliderLabel = [[UILabel alloc] init];
        _sliderLabel.text = KTVLocalizedString(@"ktv_aiaec_level");
        _sliderLabel.font = [UIFont systemFontOfSize:15];
        _sliderLabel.textColor = [UIColor whiteColor];
    }
    return _sliderLabel;
}

@end
