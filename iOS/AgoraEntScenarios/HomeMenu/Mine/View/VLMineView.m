//
//  VLMineView.m
//  VoiceOnLine
//

#import "VLMineView.h"
#import "VLHotSpotBtn.h"
#import "VLMineTCell.h"
#import "VLLoginModel.h"
#import "VLMacroDefine.h"
#import "UIView+VL.h"
#import "MenuUtils.h"
#import "VLMineCellModel.h"
#import "AgoraEntScenarios-Bridging-Header.h"
#import "AESMacro.h"
#import "VLUserCenter.h"
#import "VLAlert.h"

@import SDWebImage;

static NSString * const kSwitchCellID = @"switchCellID";
static NSString * const kDefaultCellID = @"kDefaultCellID";

@interface VLMineView ()<UITableViewDelegate,UITableViewDataSource>

@property(nonatomic, weak) id <VLMineViewDelegate>delegate;
@property (nonatomic, strong) UIView *mineTopView;
@property (nonatomic, strong) VLMineTCell *mineTopCell;
@property (nonatomic, strong) UIView *mineTopBackgroundView;
@property (nonatomic, strong) UIButton *logoutButton;

@property (nonatomic, strong) UIImageView *avatarImgView;
@property (nonatomic, strong) UILabel *nickNameLabel;
@property (nonatomic, strong) UILabel *IDLabel;
@property (nonatomic, strong) VLHotSpotBtn *editBtn;

@property (nonatomic, strong) UITableView *mineTable;
@property (nonatomic, strong) NSArray *itemsArray;
@property (nonatomic, strong) NSMutableArray *dataArray;

@end

@implementation VLMineView

- (instancetype)initWithFrame:(CGRect)frame withDelegate:(id<VLMineViewDelegate>)delegate {
    if (self = [super initWithFrame:frame]) {
        self.delegate = delegate;
        [self setupView];
        [self setupData];
    }
    return self;
}

- (void)setupView {
    [self addSubview:self.mineTopBackgroundView];
    [self.mineTopBackgroundView addSubview:self.mineTopView];
    [self.mineTopView addSubview:self.avatarImgView];
    [self.mineTopView addSubview:self.nickNameLabel];
    [self.mineTopView addSubview:self.IDLabel];
    [self.mineTopView addSubview:self.editBtn];
    [self addSubview:self.mineTable];
    if ([VLUserCenter user].type == SSOLOGIN) {
        [self.mineTopBackgroundView addSubview:self.mineTopCell];
        self.mineTopBackgroundView.frame = CGRectMake(20, kTopNavHeight+VLREALVALUE_WIDTH(35), self.width-40, VLREALVALUE_WIDTH(60)+50 + 60);
        self.mineTopCell.frame = CGRectMake(0, self.mineTopView.bottom, self.mineTopBackgroundView.width, 60);
        self.mineTopCell.backgroundColor = [UIColor clearColor];
        self.mineTopCell.contentView.backgroundColor = [UIColor clearColor];
        [self.mineTopCell setIconImageName:@"mine_invite_code_icon" title:NSLocalizedString(@"app_mine_invite_code", nil)];
    } else {
        self.mineTopBackgroundView.frame = CGRectMake(20, kTopNavHeight+VLREALVALUE_WIDTH(35), self.width-40, VLREALVALUE_WIDTH(60)+50);
    }
    
    [self addSubview:self.logoutButton];
}

- (void)layoutSubviews {
    self.logoutButton.frame = CGRectMake(20, self.mineTable.bottom + 20, self.width-40, 51);
    [super layoutSubviews];
}

- (void)setupData {
    self.dataArray = [self.itemsArray mutableCopy];
    BOOL developIsOn = [AppContext shared].isDebugMode;
    if (developIsOn) {
        VLMineCellModel *model = [VLMineCellModel modelWithItemImg:@"mine_debug_icon" title:NSLocalizedString(@"app_debug_mode", nil) style:VLMineCellStyleSwitch];
        [self.dataArray addObject:model];
    }
    _mineTable.frame = CGRectMake(20, _mineTopBackgroundView.bottom+VLREALVALUE_WIDTH(15), SCREEN_WIDTH-40, VLREALVALUE_WIDTH(58)* self.dataArray.count + 10);
}

- (void)editButtonClickEvent {
    if ([self.delegate respondsToSelector:@selector(mineViewDidCickUser:)]) {
        [self.delegate mineViewDidCickUser:VLMineViewUserClickTypeNickName];
    }
}

- (void)refreseUserInfo:(VLLoginModel *)loginModel {
    self.nickNameLabel.text = loginModel.name;
    if ([loginModel.headUrl hasPrefix:@"http"]) {
        [self.avatarImgView sd_setImageWithURL:[NSURL URLWithString:loginModel.headUrl] placeholderImage:[UIImage imageNamed:@"mine_avatar_placeHolder"]];
    } else {
        self.avatarImgView.image = [UIImage imageNamed:loginModel.headUrl];
    }
    if (loginModel.userNo.length > 0) {
        self.IDLabel.text = [NSString stringWithFormat:@"ID: %@",loginModel.userNo];
    }
    
}

- (void)refreseAvatar:(UIImage *)avatar {
    self.avatarImgView.image = avatar;
}

- (void)refreseNickName:(NSString *)nickName {
    self.nickNameLabel.text = nickName;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.dataArray.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    VLMineCellModel *model = self.dataArray[indexPath.row];
    VLMineTCell *cell = [tableView dequeueReusableCellWithIdentifier:kDefaultCellID forIndexPath:indexPath];
    [cell setIconImageName:model.itemImgStr title:model.titleStr];
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return VLREALVALUE_WIDTH(58);
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    if ([self.delegate respondsToSelector:@selector(mineViewDidCick:)]) {
        [self.delegate mineViewDidCick:indexPath.row];
    }
}

- (void)codeCellClickAction {
    [self.delegate mineViewDidCick:VLMineViewClickTypeInviteCode];
}

- (void)logoutAction {
    [[VLAlert shared] showAlertWithFrame:UIScreen.mainScreen.bounds title:@"Logout" message:NSLocalizedString(@"logout_message", nil) placeHolder:@"" type:ALERTYPENORMAL buttonTitles:@[NSLocalizedString(@"cancel", nil), NSLocalizedString(@"exit", nil)] completion:^(bool flag, NSString * _Nullable text) {
        if(flag == YES){
            [[VLUserCenter center] logout];
            [SSOWebViewController clearWebViewCache];
            [[UIApplication sharedApplication].delegate.window configRootViewController];
        }

        [[VLAlert shared] dismiss];
    }];
}

- (UIButton *)logoutButton {
    if (!_logoutButton) {
        _logoutButton = [UIButton buttonWithType:UIButtonTypeCustom];
        _logoutButton.backgroundColor = [UIColor colorWithWhite:1 alpha:0.7];
        _logoutButton.layer.cornerRadius = 10;
        _logoutButton.layer.masksToBounds = YES;
        [_logoutButton setTitle:@"Logout" forState:UIControlStateNormal];
        _logoutButton.titleLabel.font = [UIFont systemFontOfSize:15];
        [_logoutButton setTitleColor:UIColorMakeWithHex(@"#727272") forState:UIControlStateNormal];
        [_logoutButton addTarget:self action:@selector(logoutAction) forControlEvents: UIControlEventTouchUpInside];
    }
    return _logoutButton;
}

- (VLMineTCell *)mineTopCell {
    if (!_mineTopCell) {
        _mineTopCell = [VLMineTCell new];
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(codeCellClickAction)];
        [_mineTopCell.contentView addGestureRecognizer:tap];
        _mineTopCell.contentView.userInteractionEnabled = YES;
    }
    return _mineTopCell;
}

- (UIView *)mineTopView {
    if (!_mineTopView) {
        _mineTopView = [[UIView alloc]initWithFrame:CGRectMake(0, 0, self.width-40, VLREALVALUE_WIDTH(60)+50)];
        _mineTopView.layer.cornerRadius = 10;
        _mineTopView.layer.masksToBounds = YES;
        _mineTopView.backgroundColor = UIColorWhite;
    }
    return _mineTopView;
}

- (UIView *)mineTopBackgroundView {
    if (!_mineTopBackgroundView) {
        _mineTopBackgroundView = [[UIView alloc]initWithFrame:CGRectMake(20, kTopNavHeight+VLREALVALUE_WIDTH(35), self.width-40, VLREALVALUE_WIDTH(60)+50)];
        _mineTopBackgroundView.layer.cornerRadius = 10;
        _mineTopBackgroundView.layer.masksToBounds = YES;
        _mineTopBackgroundView.backgroundColor = [UIColor colorWithHexString:@"#F2FFFC"];
    }
    return _mineTopBackgroundView;
}

- (UIImageView *)avatarImgView {
    if (!_avatarImgView) {
        _avatarImgView = [[UIImageView alloc] init];
        _avatarImgView.frame = CGRectMake(20, 25, VLREALVALUE_WIDTH(60), VLREALVALUE_WIDTH(60));
        _avatarImgView.layer.cornerRadius = VLREALVALUE_WIDTH(60)*0.5;
        _avatarImgView.layer.masksToBounds = YES;
        _avatarImgView.userInteractionEnabled = YES;
        _avatarImgView.contentMode = UIViewContentModeScaleAspectFit;
        _avatarImgView.image = UIImageMake(@"mine_avatar_placeHolder");
        [_avatarImgView vl_whenTapped:^{
            if ([self.delegate respondsToSelector:@selector(mineViewDidCickUser:)]) {
                [self.delegate mineViewDidCickUser:VLMineViewUserClickTypeAvatar];
            }
        }];
    }
    return _avatarImgView;
}

- (UILabel *)nickNameLabel {
    if (!_nickNameLabel) {
        _nickNameLabel = [[UILabel alloc] initWithFrame:CGRectMake(_avatarImgView.right+15, _avatarImgView.top+5, 120, 23)];
        _nickNameLabel.font = [UIFont systemFontOfSize:18 weight:UIFontWeightSemibold];
        _nickNameLabel.textColor = UIColorMakeWithHex(@"#040925");
        _nickNameLabel.text = AGLocalizedString(@"userName");
        _nickNameLabel.userInteractionEnabled = YES;
    }
    return _nickNameLabel;
}

- (UILabel *)IDLabel {
    if (!_IDLabel) {
        _IDLabel = [[UILabel alloc] initWithFrame:CGRectMake(_avatarImgView.right+15, _avatarImgView.centerY+5, SCREEN_WIDTH-VLREALVALUE_WIDTH(60)-120, 14)];
        _IDLabel.textColor = UIColorMakeWithHex(@"#6C7192");
        _IDLabel.font = UIFontMake(12);
        _IDLabel.text = @"ID: 545021509X";
//        _IDLabel.backgroundColor = UIColorRed;
        _IDLabel.lineBreakMode = NSLineBreakByWordWrapping | NSLineBreakByCharWrapping;
        _IDLabel.userInteractionEnabled = YES;
        _IDLabel.hidden = YES;
    }
    return _IDLabel;
}

- (VLHotSpotBtn *)editBtn {
    if (!_editBtn) {
        _editBtn = [[VLHotSpotBtn alloc]initWithFrame:CGRectMake(self.width-40-15-20, _nickNameLabel.centerY-10, 20, 20)];
        [_editBtn setImage:UIImageMake(@"mine_edit_icon") forState:UIControlStateNormal];
        [_editBtn addTarget:self action:@selector(editButtonClickEvent) forControlEvents:UIControlEventTouchUpInside];
        _editBtn.hidden = VLUserCenter.user.type == SSOLOGIN;
    }
    return _editBtn;
}

- (UITableView *)mineTable{
    if(!_mineTable) {
        _mineTable = [[UITableView alloc]initWithFrame:CGRectMake(20, _mineTopView.bottom+VLREALVALUE_WIDTH(15), SCREEN_WIDTH-40, VLREALVALUE_WIDTH(58)*5+10)];
        _mineTable.dataSource = self;
        _mineTable.delegate = self;
        _mineTable.backgroundColor = UIColorWhite;
        _mineTable.layer.cornerRadius = 10;
        _mineTable.layer.masksToBounds = YES;
        _mineTable.scrollEnabled = NO;
        _mineTable.separatorStyle = UITableViewCellSeparatorStyleNone;
        [_mineTable registerClass:[VLMineTCell class] forCellReuseIdentifier:kDefaultCellID];
        [_mineTable registerClass:[VLMineSwitchCell class] forCellReuseIdentifier:kSwitchCellID];
    }
    return _mineTable;
}

- (NSArray *)itemsArray {
    if (!_itemsArray) {
        _itemsArray = @[
            [VLMineCellModel modelWithItemImg:@"mine_screct_icon" title:NSLocalizedString(@"app_user_agreement", nil)],
            [VLMineCellModel modelWithItemImg:@"mine_aboutus_icon" title:NSLocalizedString(@"app_about_us", nil)]
        ];
    }
    return _itemsArray;
}

- (void)refreshTableView {
    [self setupData];
    [self.mineTable reloadData];
}

@end

