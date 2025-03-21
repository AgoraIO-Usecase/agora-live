//
//  VLRoomPersonView.m
//  VoiceOnLine
//

#import "VLMicSeatList.h"
#import "VLMicSeatCell.h"
#import "AgoraEntScenarios-Swift.h"
#import "AESMacro.h"
#import "VLMacroDefine.h"
#import "AppContext+KTV.h"
@import YYCategories;
@import SDWebImage;

@interface VLMicSeatList ()<UICollectionViewDataSource,UICollectionViewDelegate>

@property(nonatomic, weak) id <VLMicSeatListDelegate>delegate;

@property (nonatomic, strong) UICollectionView *personCollectionView;
//@property (nonatomic, copy) NSString *currentPlayingSongCode;
@end

@implementation VLMicSeatList

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLMicSeatListDelegate>)delegate withRTCkit:(AgoraRtcEngineKit *)RTCkit{
    if (self = [super initWithFrame:frame]) {
        self.delegate = delegate;
        self.roomSeatsArray = [[NSArray alloc]init];
        [self setupView];
    }
    return self;
}

- (void)setupView {
    
    UICollectionViewFlowLayout *flowLayOut = [[UICollectionViewFlowLayout alloc]init];
    flowLayOut.scrollDirection = UICollectionViewScrollDirectionVertical;
    
    CGFloat itemW = VLREALVALUE_WIDTH(54);
    CGFloat middleMargin = (SCREEN_WIDTH - 40 - 2*27 - 4*itemW - 10 * 2)/3.0;
    CGFloat itemH = VLREALVALUE_WIDTH(54)+33;
    flowLayOut.itemSize = CGSizeMake(itemW, itemH);
    flowLayOut.minimumInteritemSpacing = middleMargin;
    flowLayOut.minimumLineSpacing = 15;
    flowLayOut.sectionInset = UIEdgeInsetsMake(10, 10, 10, 10);
    
    self.personCollectionView = [[UICollectionView alloc] initWithFrame:CGRectMake(20, 0, self.frame.size.width - 40, self.frame.size.height) collectionViewLayout:flowLayOut];
    self.personCollectionView.dataSource = self;
    self.personCollectionView.delegate = self;
    self.personCollectionView.alwaysBounceVertical = true;
    self.personCollectionView.showsHorizontalScrollIndicator = false;
    self.personCollectionView.showsVerticalScrollIndicator = false;
    self.personCollectionView.backgroundColor = UIColorClear;
    self.personCollectionView.scrollEnabled = NO;
    if (@available(iOS 11, *)) {
        self.personCollectionView.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
    }
    [self.personCollectionView registerClass:[VLMicSeatCell class] forCellWithReuseIdentifier:[VLMicSeatCell className]];
    [self addSubview:self.personCollectionView];
}

- (void)reloadSeatIndex: (NSUInteger)seatIndex {
    KTVLogInfo(@"[MicSeatListView]reloadSeatIndex: %ld", seatIndex);
    [self.personCollectionView reloadItemsAtIndexPaths:@[[NSIndexPath indexPathForRow:seatIndex inSection:0]]];
}

- (void)reloadData {
    KTVLogInfo(@"[MicSeatListView]reloadData");
    [self.personCollectionView reloadData];
}

#pragma mark - UITableViewDelegate,UITableViewDataSource
- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section {
    return self.roomSeatsArray.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath {
    VLMicSeatCell *cell = nil;
    cell = [collectionView dequeueReusableCellWithReuseIdentifier:[VLMicSeatCell className] forIndexPath:indexPath];
    
    for (UIView *view in cell.videoView.subviews) {
        if (view.tag > viewTag) {
            [view removeFromSuperview];
        }
    }
    VLRoomSeatModel *seatModel = self.roomSeatsArray[indexPath.row];
    
    if (seatModel.owner.userName.length > 0) {
        cell.nickNameLabel.text = seatModel.owner.userName;
    }else{
        cell.nickNameLabel.text = [NSString stringWithFormat:@"%d", (int)indexPath.row + 1];
    }
    
    if ([AppContext isKtvRoomOwnerWithSeat:seatModel]) {
        cell.avatarImgView.layer.borderWidth = 2.0;
        cell.avatarImgView.layer.borderColor = UIColorMakeWithHex(@"#75ADFF").CGColor;
        cell.roomerImgView.hidden = cell.roomerLabel.hidden = NO;
        cell.nickNameLabel.textColor = UIColorMakeWithHex(@"#DBDAE9");
    }else{
        cell.roomerImgView.hidden = cell.roomerLabel.hidden = YES;
        cell.nickNameLabel.textColor = UIColorMakeWithHex(@"#AEABD0");
        cell.avatarImgView.layer.borderColor = UIColorClear.CGColor;
    }
    cell.roomerLabel.text = KTVLocalizedString(@"ktv_room_owner");
    if (seatModel.owner.userAvatar.length > 0) {
        cell.avatarImgView.image = [UIImage imageNamed:seatModel.owner.userAvatar];
    }else{
        cell.avatarImgView.image = [UIImage ktv_sceneImageWithName:@"ktv_emptySeat_icon"];
        cell.volume = 0;
    }
    
    cell.singingBtn.hidden = ![AppContext isKtvPlayingSongOwnerWithSeat:seatModel];
    if (seatModel.isAudioMuted) {
        cell.muteImgView.hidden = NO;
        cell.volume = 0;
    } else {
        cell.muteImgView.hidden = YES;
    }
    
    if ([AppContext isKtvChorusingWithSeat:seatModel]) {
        if (cell.singingBtn.hidden) {
            cell.joinChorusBtn.hidden = NO;
        } else {
            cell.joinChorusBtn.hidden = YES;
            KTVLogError(@"join chorus button and singing button can't be displayed at the same time!");
        }
    } else {
        cell.joinChorusBtn.hidden = YES;
    }
    
    if (seatModel.owner.userId.length == 0) {
        cell.muteImgView.hidden = YES;
        cell.singingBtn.hidden = YES;
    }
    
    //only display when rtcUid exists (on mic seat), and video is not muted
    cell.videoView.hidden = !([seatModel.owner.userId length] > 0 && !seatModel.isVideoMuted);
    //avatar or camera will only be displayed 1 at atime
    cell.avatarImgView.hidden = !cell.videoView.isHidden;
    if (!seatModel.isVideoMuted && seatModel.owner.userId.length > 0) { //Turn on the video
        [self.delegate onVLRoomPersonView:self onRenderVideo:seatModel inView:cell.videoView atIndex:indexPath.row];
    }
    
    return cell;
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath {
    VLRoomSeatModel *roomSeatModel = self.roomSeatsArray[indexPath.row];
    if (self.delegate && [self.delegate respondsToSelector:@selector(onVLRoomPersonView:seatItemTappedWithModel:atIndex:)]) {
        [self.delegate onVLRoomPersonView:self seatItemTappedWithModel:roomSeatModel atIndex:indexPath.row];
    }
}

- (void)updateVolumeForSpeakers:(NSArray<AgoraRtcAudioVolumeInfo *> *) speakers {
    for (AgoraRtcAudioVolumeInfo *speaker in speakers) {
        // 0 is talking by yourself.
        NSInteger speakerUid = (speaker.uid == 0) ? VLUserCenter.user.id.integerValue : speaker.uid;
        for (VLRoomSeatModel *model in self.roomSeatsArray) {
            if (model.owner.userId.length == 0) {
                continue;
            }
            if(model.owner.userId.integerValue == speakerUid) {
                if (model.isAudioMuted == 1) {
                    [self updateVolumeForIndex:model.seatIndex volume:0];
                } else {
                    [self updateVolumeForIndex:model.seatIndex volume:speaker.volume];
                }
                break;
            }
        }
    }
}

- (void)updateVolumeForIndex:(NSInteger) index volume:(NSInteger) volume {
    VLMicSeatCell *cell = [self.personCollectionView cellForItemAtIndexPath:[NSIndexPath indexPathForItem:index inSection:0]];
    cell.volume = volume;
}

- (void)updateSingBtnWithChoosedSongArray:(NSArray *)choosedSongArray {
    NSMutableSet* changeSet = [NSMutableSet set];
//    if(choosedSongArray.count == 0){
//        self.currentPlayingSongCode = @"0";
//    }
    if (choosedSongArray.count > 0) {
//        VLRoomSelSongModel *songModel = choosedSongArray.firstObject;
//        self.currentPlayingSongCode = songModel.chorusSongId;
        for (VLRoomSeatModel *seatModel in self.roomSeatsArray) {
            BOOL isSongOwner = [AppContext isKtvPlayingSongOwnerWithSeat:seatModel];
            if (isSongOwner != seatModel.isSongOwner) {
                seatModel.isSongOwner = isSongOwner;
                [changeSet addObject:@(seatModel.seatIndex)];
            }
            //Check the users on the microphone
            BOOL needtoJoinChorus = [AppContext isKtvChorusingWithSeat:seatModel];//[seatModel.chorusSongCode isEqualToString:[songModel chorusSongId]];
//            cell.joinsttus
            NSIndexPath *path = [NSIndexPath indexPathForRow:seatModel.seatIndex inSection:0];
            VLMicSeatCell* cell = [self.personCollectionView cellForItemAtIndexPath:path];
            if (needtoJoinChorus != !cell.joinChorusBtn.isHidden){
                //            if (![seatModel.chorusSongCode isEqualToString:[songModel chorusSongId]] && seatModel.chorusSongCode) {
                // seatModel.chorusSongCode = @"";
                [changeSet addObject:@(seatModel.seatIndex)];
            }
//            }
            KTVLogInfo(@"update seat index: %ld", seatModel.seatIndex);
            
        }
    } else{
        for (VLRoomSeatModel *seatModel in self.roomSeatsArray) {
            if (seatModel.isSongOwner) {
                seatModel.isSongOwner = NO;
                [changeSet addObject:@(seatModel.seatIndex)];
            }

            if ([AppContext isKtvChorusingWithSeat:seatModel]) {
               // seatModel.chorusSongCode = @"";
                [changeSet addObject:@(seatModel.seatIndex)];
            }
        }
    }
    
    if (changeSet.count == 0) {
        return;
    }
    
    NSMutableArray* indexPaths = [NSMutableArray array];
    for (NSNumber * index in changeSet) {
        [indexPaths addObject:[NSIndexPath indexPathForRow:[index integerValue] inSection:0]];
    }
    [self.personCollectionView reloadItemsAtIndexPaths:indexPaths];
}

@end
