//
//  VLChoosedSongView.h
//  VoiceOnLine
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN
@class VLSongList;


@interface VLSongList : UIView

- (instancetype)initWithFrame:(CGRect)frame;

- (void)setSelSongsArray:(NSArray * _Nonnull)selSongsArray isOwner:(BOOL)isOwner;
@end

NS_ASSUME_NONNULL_END
