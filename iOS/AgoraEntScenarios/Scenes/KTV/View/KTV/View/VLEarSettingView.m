//
//  VLEarSettingView.m
//  AgoraEntScenarios
//
//  Created by CP on 2023/6/5.
//

#import "VLEarSettingView.h"
#import "VLKTVSliderView.h"
#import <Masonry/Masonry.h>
#import "HeadSetManager.h"
#import "VLToast.h"
#import "AESmacro.h"
#import "VLMacroDefine.h"
#import "AgoraEntScenarios-Swift.h"
@interface VLEarSettingView()<VLKTVSliderViewDelegate>
@property (nonatomic, strong) UIButton *backButton;
@property (nonatomic,strong) UILabel *titleLabel;
@property (nonatomic,strong) UILabel *earLabel;
@property (nonatomic,strong) UISwitch *earSwitch;
@property (nonatomic,strong) UIButton *earWarningBtn;
@property (nonatomic,strong) UILabel *earWarningLabel;
@property (nonatomic, strong) VLKTVSliderView *earSlider;
@property (nonatomic,strong) UILabel *earSetLabel;
@property (nonatomic,strong) UIView *earSetView;
@property (nonatomic, assign) BOOL isEarOn;
@property (nonatomic, assign) float sliderValue;
@property (nonatomic, strong) HeadSetManager *headeSet;
@end
@implementation VLEarSettingView
#pragma mark - Lazy

- (UIButton *)backButton {
    if (!_backButton) {
        _backButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_backButton setImage:[UIImage ktv_sceneImageWithName:@"ktv_back_whiteIcon"] forState:UIControlStateNormal];
        [_backButton addTarget:self action:@selector(backAction) forControlEvents:UIControlEventTouchUpInside];
    }
    return _backButton;
}

#pragma mark - lift cycle

- (instancetype)initWithFrame:(CGRect)frame isEarOn:(BOOL)isEarOn vol:(CGFloat)vol withDelegate:(id<VLEarSettingViewViewDelegate>)delegate {
    if (self = [super initWithFrame:frame]) {
        kWeakSelf(self);
        self.headeSet = [HeadSetManager initHeadsetObserverWithCallback:^(BOOL inserted) {
            weakself.earWarningBtn.selected = inserted;
            if(!inserted){
                weakself.earSwitch.on = false;
                weakself.earSwitch.userInteractionEnabled = false;
                weakself.earSlider.alpha =  0.6;
                //If it has been turned on but the headphones are unplugged, it will be forcibly turned off.
                if(weakself.isEarOn){
                    if([weakself.delegate respondsToSelector:@selector(onVLKTVEarSettingViewSwitchChanged:)]){
                        [weakself.delegate onVLKTVEarSettingViewSwitchChanged:false];
                    }
                }
            } else {
                weakself.earSwitch.userInteractionEnabled = true;
                weakself.earSlider.alpha = weakself.isEarOn ? 1 : 0.6;
            }
            weakself.earSwitch.alpha = inserted ? 1 : 0.6;
            weakself.earWarningBtn.titleLabel.textColor = inserted ? [UIColor whiteColor] : [UIColor redColor];
        }];
        [self initSubViews];
        [self addSubViewConstraints];
        self.isEarOn = isEarOn;
        self.sliderValue = vol;
        self.earSlider.value = vol / 100.0;
        self.delegate = delegate;
    }
    return self;
}

- (void)initSubViews {
    
    self.backgroundColor = UIColorMakeWithHex(@"#152164");
    
    _titleLabel = [[UILabel alloc]init];
    _titleLabel.text = KTVLocalizedString(@"ktv_music_menu_dialog_ear");
    _titleLabel.textAlignment = NSTextAlignmentCenter;
    _titleLabel.textColor = [UIColor whiteColor];
    [self addSubview:_titleLabel];
    
    [self addSubview:self.backButton];
    
    _earLabel = [[UILabel alloc]init];
    _earLabel.text = KTVLocalizedString(@"ktv_ear_switch");
    _earLabel.textColor = [UIColor whiteColor];
    [self addSubview:_earLabel];
    
    _earSwitch = [[UISwitch alloc]init];
    _earSwitch.onTintColor = UIColorMakeWithHex(@"#099DFD");
    [_earSwitch addTarget:self action:@selector(earChange:) forControlEvents:UIControlEventTouchUpInside];
    _earSwitch.userInteractionEnabled = [self.headeSet hasHeadset];
    _earSwitch.alpha = [self.headeSet hasHeadset] ? 1: 0.6;
    [self addSubview:_earSwitch];
    
    _earWarningBtn = [[UIButton alloc]init];
    [_earWarningBtn setTitle:KTVLocalizedString(@"ktv_ear_without_headset") forState:UIControlStateNormal] ;
    [_earWarningBtn setTitle:KTVLocalizedString(@"ktv_ear_warning") forState:UIControlStateSelected] ;
    _earWarningBtn.font = [UIFont systemFontOfSize:12];
    _earWarningBtn.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeft;
    _earWarningBtn.imageView.contentMode = UIViewContentModeScaleAspectFit;
    [_earWarningBtn setTitleColor:[UIColor redColor] forState:UIControlStateNormal];
    [_earWarningBtn setTitleColor:[UIColor whiteColor] forState:UIControlStateSelected];
    [_earWarningBtn setImage:[UIImage ktv_sceneImageWithName:@"ktv_add_circle" ] forState:UIControlStateNormal];
    [_earWarningBtn setImage:[UIImage ktv_sceneImageWithName:@"ktv_add_circle_in" ] forState:UIControlStateSelected];
    _earWarningBtn.selected = [self.headeSet hasHeadset];

    // Create an NSMutableAttributedString object
    NSMutableAttributedString *attributedText = [[NSMutableAttributedString alloc] initWithString:KTVLocalizedString(@"ktv_ear_warning2")];

    // Add pictures to rich text
    NSTextAttachment *textAttachment = [[NSTextAttachment alloc] init];
    textAttachment.image = [UIImage ktv_sceneImageWithName:@"ktv_add_circle_in" ];
    textAttachment.bounds = CGRectMake(0, -3, 15, 15); // Adjust the position and size of the picture, and shift 3 pixels upwards.
    NSAttributedString *imageString = [NSAttributedString attributedStringWithAttachment:textAttachment];
    [attributedText insertAttributedString:imageString atIndex:0]; // Insert the picture into the beginning of the rich text

    // Set the text to automatically overline, and set the paragraph style to make the text and pictures start from the upper left corner.
    NSMutableParagraphStyle *paragraphStyle = [[NSMutableParagraphStyle alloc] init];
    paragraphStyle.lineBreakMode = NSLineBreakByWordWrapping;
    paragraphStyle.alignment = NSTextAlignmentLeft; // Left alignment of text
  //  paragraphStyle.firstLineHeadIndent = textAttachment.bounds.size.width; // The first line indents the width of the picture, so that the text starts from the upper left corner.
    [attributedText addAttribute:NSParagraphStyleAttributeName value:paragraphStyle range:NSMakeRange(0, attributedText.length)];

    // Create a UILabel and set rich text
    UILabel *earWarningLabel = [[UILabel alloc] init];
    earWarningLabel.attributedText = attributedText;
    earWarningLabel.numberOfLines = 0; // Set to 0 to indicate automatic line wrapping
  //  earWarningLabel.backgroundColor = [UIColor whiteColor];
    earWarningLabel.font = [UIFont systemFontOfSize:12];
    earWarningLabel.textColor = [UIColor whiteColor];
    [earWarningLabel sizeToFit]; // Adaptive content size
    [self addSubview:earWarningLabel];
    self.earWarningLabel = earWarningLabel;
    self.earWarningLabel.hidden = ![self.headeSet hasHeadset];

    [self addSubview:_earWarningBtn];
    [self addSubview:self.earWarningLabel];
    
    _earSetView = [[UIView alloc]init];
    _earSetView.backgroundColor = [UIColor colorWithRed:0.031 green:0.028 blue:0.185 alpha:0.2];
    [self addSubview:_earSetView];
    _earSetView.hidden = ![self.headeSet hasHeadset];
    
    _earSetLabel = [[UILabel alloc]init];
    _earSetLabel.text = KTVLocalizedString(@"ktv_ear_setting");
    _earSetLabel.textColor = [UIColor grayColor];
    _earSetLabel.font = [UIFont systemFontOfSize:12];
    [_earSetView addSubview:_earSetLabel];
    
    [self addSubview:self.earSlider];
    self.earSlider.hidden = ![self.headeSet hasHeadset];
}

-(void)addSubViewConstraints {
    [self.titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerX.mas_equalTo(self);
        make.height.mas_equalTo(20);
        make.top.mas_equalTo(30);
    }];
    
    [self.backButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.height.with.mas_equalTo(35);
        make.centerY.mas_equalTo(self.titleLabel);
    }];
    
    [self.earLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.top.mas_equalTo(65);
        make.height.mas_equalTo(21);
    }];
    
    [self.earSwitch mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.mas_equalTo(_earLabel);
        make.right.mas_equalTo(self).offset(-20);
        make.width.mas_equalTo(46);
        make.height.mas_equalTo(28);
    }];
    
    [self.earWarningBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.top.mas_equalTo(_earLabel.mas_bottom).offset(17);
        make.width.mas_equalTo(self.bounds.size.width - 40);
        make.height.mas_equalTo(18);
    }];
    
    [self.earWarningLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.mas_equalTo(20);
        make.top.mas_equalTo(_earWarningBtn.mas_bottom).offset(10);
        make.width.mas_equalTo(self.bounds.size.width - 40);
        make.height.mas_equalTo(30);
    }];
    
    [self.earSetView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.mas_equalTo(self);
        make.top.mas_equalTo(self.earWarningLabel.mas_bottom).offset(10);
        make.height.mas_equalTo(32);
    }];
    
    [self.earSetLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self).offset(20);
        make.centerY.mas_equalTo(_earSetView);
        make.width.mas_equalTo(100);
    }];
    
    [self.earSlider mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.mas_equalTo(self);
        make.top.mas_equalTo(self.earSetView.mas_bottom).offset(25);
        make.height.mas_equalTo(22);
    }];
}


- (void)backAction {
    [self.delegate settingViewBackAction];
}

- (VLKTVSliderView *)earSlider {
    if (!_earSlider) {
        _earSlider = [[VLKTVSliderView alloc] initWithMax:1 min:0];
        _earSlider.titleLabel.text = KTVLocalizedString(@"ktv_ear_vol");
        _earSlider.titleLabel.textColor = [UIColor whiteColor];
        _earSlider.delegate = self;
    }
    return _earSlider;
}

-(void)earChange:(UISwitch *)swich {
    self.isEarOn = swich.on;
    if([self.delegate respondsToSelector:@selector(onVLKTVEarSettingViewSwitchChanged:)]){
        [self.delegate onVLKTVEarSettingViewSwitchChanged:swich.on];
    }
}

-(void)setIsEarOn:(BOOL)isEarOn {
    _isEarOn = isEarOn;
    if([self.headeSet hasHeadset] == NO){
        self.earSwitch.on = false;
        _earSlider.alpha = 0.6;
        _earSlider.userInteractionEnabled = false;
    } else {
        _earSwitch.on = isEarOn;
        _earSlider.alpha = isEarOn ? 1 : 0.6;
        _earSlider.userInteractionEnabled = isEarOn;
    }
}

//-(void)setSliderValue:(float)sliderValue {
//    _sliderValue = sliderValue;
//}

- (void)sliderView:(VLKTVSliderView *)sliderView valueChanged:(float)value{
    self.sliderValue = value ;
    if([self.delegate respondsToSelector:@selector(onVLKTVEarSettingViewValueChanged:)]){
        [self.delegate onVLKTVEarSettingViewValueChanged:value];
    }
}
@end
