//
//  VLKTVSettingView.h
//  VoiceOnLine
//

#import "VLBaseView.h"
@class VLKTVSettingModel;

typedef enum : NSUInteger {
    VLKTVValueDidChangedTypeEar = 0,      // Ear return
    VLKTVValueDidChangedTypeSoundCard = 1,      // Sound card
    VLKTVValueDidChangedTypeMV,           // MV
    VLKTVValueDidChangedRiseFall,         // Up and down tune
    VLKTVValueDidChangedTypeSound,        // Sound
    VLKTVValueDidChangedTypeAcc,          // Accompany
    VLKTVValueDidChangedTypeRemoteValue,  //Remote volume
    VLKTVValueDidChangedTypeListItem,      // List
    VLKTVValueDidChangedTypeLrc, //Lyrics level
    VLKTVValueDidChangedTypeVqs, //Acoustic fidelity
    VLKTVValueDidChangedTypeAns, //Reduce the noise
    VLKTVValueDidChangedTypebro, //Professional anchor
    VLKTVValueDidChangedTypeaiaec, //AIAec
    VLKTVValueDidChangedTypeDelay,
    VLKTVValueDidChangedTypeAecLevel,
    VLKTVValueDidChangedTypeenableMultipath,
} VLKTVValueDidChangedType;

@protocol VLKTVSettingViewDelegate <NSObject>
- (void)settingViewSettingChanged:(VLKTVSettingModel *)setting valueDidChangedType:(VLKTVValueDidChangedType)type;
- (void)settingViewSettingChanged:(VLKTVSettingModel *)setting effectChoosed:(NSInteger)effectIndex;
- (void)settingViewBackAction;

@end

@interface VLKTVSettingView : VLBaseView

- (instancetype)initWithSetting:(VLKTVSettingModel *)setting;

@property (nonatomic, weak) id <VLKTVSettingViewDelegate> delegate;

- (void)setIsEarOn:(BOOL)isEarOn;
- (void)setAccValue:(float)accValue;
-(void)setIspause:(BOOL)isPause;
-(void)setSelectEffect:(NSInteger)index;
-(void)setUseSoundCard:(BOOL)useSoundCard;
-(void)setChorusStatus:(BOOL)status;
-(void)setAEClevel:(NSInteger)level;
@end

@interface VLKTVSettingModel : NSObject

#define kKindUnSelectedIdentifier 10000

///Ear return
@property (nonatomic, assign) BOOL soundOn;
@property (nonatomic, assign) BOOL mvOn;
@property (nonatomic, assign) BOOL soundCardOn;
@property (nonatomic, assign) float soundValue;
@property (nonatomic, assign) float accValue;
@property (nonatomic, assign) float remoteValue;
@property (nonatomic, assign) NSInteger toneValue;
@property (nonatomic, assign) int remoteVolume;
@property (nonatomic, assign) NSInteger selectEffect;
@property (nonatomic, assign) NSInteger lrcLevel;
@property (nonatomic, assign) NSInteger vqs;
@property (nonatomic, assign) NSInteger ans;
@property (nonatomic, assign) BOOL isPerBro;//Professional anchor
@property (nonatomic, assign) BOOL isDelay;//Low latency
@property (nonatomic, assign) BOOL enableAec;
@property (nonatomic, assign) NSInteger aecLevel;
@property (nonatomic, assign) BOOL enableMultipath;
/// list option
@property (nonatomic, assign) NSInteger kindIndex;

- (void)setDefaultProperties;

@end
