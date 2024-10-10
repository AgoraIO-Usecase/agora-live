//
//  VLSongItmModel.h
//  VoiceOnLine
//

#import "VLBaseModel.h"

NS_ASSUME_NONNULL_BEGIN

@interface VLSongItmModel : VLBaseModel

//@property (nonatomic, strong) NSArray *lyricType;
//@property (nonatomic, copy) NSString *releaseTime;
///Perform a song
@property (nonatomic, copy) NSString *singer;
//@property (nonatomic, copy) NSString *vendorId;
//@property (nonatomic, copy) NSString *mv;
//@property (nonatomic, copy) NSString *updateTime;
//@property (nonatomic, copy) NSString *pitchType;
//@property (nonatomic, copy) NSString *type;
//@property (nonatomic, copy) NSString *duration;
@property (nonatomic, copy) NSString *songName;
@property (nonatomic, copy) NSString *songNo;
@property (nonatomic, copy) NSString *imageUrl;
//@property (nonatomic, copy) NSString *highPart;
//@property (nonatomic, copy) NSString *status;
///Song link
//@property (nonatomic, copy) NSString *songUrl;
///Lyrics
@property (nonatomic, copy) NSString *lyric;

//Has it been ordered?
@property (nonatomic, assign) BOOL ifChoosed;
//@property (nonatomic, assign) BOOL ifChorus;


@end

NS_ASSUME_NONNULL_END
