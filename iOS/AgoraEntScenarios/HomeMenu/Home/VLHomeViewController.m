//
//  VLHomeViewController.m
//  VoiceOnLine
//

#import "VLHomeViewController.h"
#import "VLHomeView.h"
#import "VLMacroDefine.h"
#import "MenuUtils.h"
#import "AESMacro.h"
#import "VLToast.h"
#import "AgoraEntScenarios-Swift.h"

@interface VLHomeViewController ()<VLHomeViewDelegate>

@end

@implementation VLHomeViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setBackgroundImage:@"home_bg_image"];
    [self setNaviTitleName:@"Agora"];
    [[NetworkManager shared] reportDeviceInfoWithSceneName: @""];
    
    [self setUpUI];
}

- (void)setUpUI {
    VLHomeView *homeView = [[VLHomeView alloc]initWithDelegate:self];
    homeView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:homeView];
    homeView.translatesAutoresizingMaskIntoConstraints = NO;
    [[homeView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor] setActive:YES];
    [[homeView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor constant:44] setActive:YES];
    [[homeView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor] setActive:YES];
    [[homeView.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor] setActive:YES];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
//    self.hidesBottomBarWhenPushed = YES;
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
//    self.hidesBottomBarWhenPushed = NO;
}


#pragma mark - Public Methods
- (void)configNavigationBar:(UINavigationBar *)navigationBar {
    [super configNavigationBar:navigationBar];
}
- (BOOL)preferredNavigationBarHidden {
    return true;
}


- (void)itemClickAction:(int)tagValue {
    NSArray* sceneNames = @[@"LiveShow", @"E-Commerce", @"VoiceChat"];
    [[NetworkManager shared] reportSceneClickWithSceneName:sceneNames[tagValue]];
    [[NetworkManager shared] reportDeviceInfoWithSceneName:sceneNames[tagValue]];
    [[NetworkManager shared] reportUserBehaviorWithSceneName:sceneNames[tagValue]];
    switch (tagValue) {
        case 0: {
            ShowRoomListVC *vc = [ShowRoomListVC new];
            [self.navigationController pushViewController:vc animated:YES];
            break;
        }
        case 1: {
            // TODO: Commerce场景报错先去掉
//            CommerceRoomListVC *vc = [CommerceRoomListVC new];
//            [self.navigationController pushViewController:vc animated:YES];
            break;
        }
        case 2: {
            VRRoomsViewController *vc = [[VRRoomsViewController alloc] initWithUser:[VLUserCenter user]];
            [self.navigationController pushViewController:vc animated:YES];
            break;
        }
        default: break;
    }
}

@end
