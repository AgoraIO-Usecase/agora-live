//
//  VLSelectSongTableItemView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>
#import "JXCategoryListContainerView.h"

NS_ASSUME_NONNULL_BEGIN
@class VLRoomListModel;
@interface VLSelectSongTableItemView : UIView <JXCategoryListContentViewDelegate>
@property (nonatomic, strong) NSArray *selSongsArray;

- (instancetype)initWithFrame:(CGRect)frame
                    withRooNo:(NSString *)roomNo;

- (void)loadDatasWithIfRefresh:(BOOL)ifRefresh;
//Update the status of the songs ordered by others
- (void)setSelSongArrayWith:(NSArray *)array;//Update the status of the songs ordered by others
@end

NS_ASSUME_NONNULL_END
