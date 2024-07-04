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
@property (nonatomic, assign) CGFloat max;
@property (nonatomic, assign) CGFloat min;
@property (nonatomic, assign) NSInteger minValue;
@property (nonatomic, assign) NSInteger maxValue;
@property (nonatomic, assign) NSInteger currentValue;
@property (nonatomic, strong) UIButton *addButton;
@property (nonatomic, strong) UIButton *reduceButton;
@property (nonatomic, strong) UILabel *minLabel;
@property (nonatomic, strong) UILabel *maxLabel;
@end

@implementation VLSettingAIAECSwitcherView

- (instancetype)initWithMax:(CGFloat)max min:(CGFloat)min {
    if (self = [super init]) {
        self.max = max;
        self.min = min;
        [self initSubViews];
        [self addSubViewConstraints];
    }
    return self;
}

- (void)initSubViews {
    [self addSubview:self.sliderView];
    [self addSubview:self.reduceButton];
    [self addSubview:self.addButton];
}

- (void)addSubViewConstraints {
    CGFloat padding = 8;
    [self.reduceButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(90);
        make.width.mas_equalTo(@(26));
        make.centerY.mas_equalTo(self);
    }];
    [self.addButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.mas_equalTo(self).offset(-padding);
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

- (void)setValue:(float)value {
    _value = value;
    _currentValue = (int)(_value * 100);
    self.sliderView.value = value;
}

- (void)setMax:(CGFloat)max {
    _max = max;
    _maxLabel.text = [NSString stringWithFormat:@"%.0f", max];
}

- (void)setMin:(CGFloat)min {
    _min = min;
    _minLabel.text = [NSString stringWithFormat:@"%.0f", min];
}

//处理
- (void)sliderValurChanged:(UISlider*)slider {
    [self sliderClick:slider];
}

- (void)sliderClick:(UISlider *)slider {
    if ([self.delegate respondsToSelector:@selector(sliderView:valueChanged:)]) {
        self.currentValue = (int)(slider.value * 100);
//        [self.delegate sliderView:self valueChanged:(int)(slider.value * 100)];
    }
}

- (void)buttonClcik:(UIButton *)sender {
    if (sender == self.addButton) {
        if (self.currentValue == 100) return;
        self.currentValue++;
    } else {
        if (self.currentValue == 0) return;
        self.currentValue--;
    }
    self.sliderView.value = (CGFloat)self.currentValue / 100.0;
    if ([self.delegate respondsToSelector:@selector(sliderView:valueChanged:)]) {
//        [self.delegate sliderView:self valueChanged:self.currentValue];
    }
}

- (void)onClickAdd:(UIButton *)sender {
    
}

- (void)onClickReduce:(UIButton *)sender {
    
}

- (void)updateValue:(NSInteger)value {
    
    
}


#pragma mark - Lazy

- (UISlider *)sliderView {
    if (!_sliderView) {
        _sliderView = [[UISlider alloc]init];
        [_sliderView setThumbImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_slider" ] forState:UIControlStateNormal];
        [_sliderView setThumbImage:[UIImage ktv_sceneImageWithName:@"icon_ktv_slider" ] forState:UIControlStateHighlighted];
        _sliderView.maximumValue = self.max;
        _sliderView.minimumValue = self.min;
        [_sliderView addTarget:self action:@selector(sliderValurChanged:) forControlEvents:UIControlEventTouchUpInside];
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

@end
