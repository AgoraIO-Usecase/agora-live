//
//  VLHomeOnListCCell.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>

@class AUIRoomInfo;
@interface VLHomeOnLineListCCell : UICollectionViewCell
@property (nonatomic, strong) AUIRoomInfo *listModel;
@property (nonatomic, strong) UIImageView *bgImgView;
@property (nonatomic, copy) void (^joinBtnClickBlock)(AUIRoomInfo *model);
@end


