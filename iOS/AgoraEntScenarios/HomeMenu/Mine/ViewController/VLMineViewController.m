//
//  VLMineViewController.m
//  VoiceOnLine
//

#import "VLMineViewController.h"
#import <MobileCoreServices/MobileCoreServices.h>
//#import "AppDelegate+Config.h"
#import "UIWindow+Router.h"
#import "VLCommonWebViewController.h"
#import "VLMineView.h"
#import "VLUploadImageResModel.h"
#import "VLUserCenter.h"
#import "VLMacroDefine.h"
#import "VLURLPathConfig.h"
#import "VLFontUtils.h"
#import "VLToast.h"
#import "VLAPIRequest.h"
#import "VLGlobalHelper.h"
#import "MenuUtils.h"
#import <Photos/Photos.h>
#import "AgoraEntScenarios-Swift.h"
#import "AESMacro.h"
@import Masonry;
@import LEEAlert;

typedef NS_ENUM(NSUInteger, AVAuthorizationRequestType){
    photoLibrary = 0,
    camera = 1,
};

@interface VLMineViewController ()
<UINavigationControllerDelegate,UIImagePickerControllerDelegate,VLMineViewDelegate, UITextFieldDelegate, AboutAgoraEntertainmentViewControllerDelegate>

@property (nonatomic, strong) VLMineView *mineView;
@property (nonatomic, assign) NSInteger maxInputLength;

@end

@implementation VLMineViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setBackgroundImage:@"home_bg_image"];
    [self setNaviTitleName:@"Agora"];
    [self setUpUI];
    self.maxInputLength = 10;
}

- (void)setUpUI {
    VLMineView *mineView = [[VLMineView alloc]initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT-kBottomTabBarHeight) withDelegate:self];
    [mineView refreseUserInfo:VLUserCenter.user];
    [self.view addSubview:mineView];
    self.mineView = mineView;
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
//    [self loadRequestUserInfoRequest];
}

#pragma mark - VLMineViewDelegate

- (void)mineViewDidCick:(VLMineViewClickType)type {
    switch (type) {
        case VLMineViewClickTypeUserProtocol:
            [self pushWebView:kURLPathH5TermsOfService];
            break;
        case VLMineViewClickTypeAboutUS:
            [self about];
            break;
        case VLMineViewClickTypeInviteCode:
            [self gotoInviteCodePage];
            break;
        case VLMineViewClickTypeDebug:
            [self gotoDebugMode];
        default:
            break;
    }
}

- (void)mineViewDidCickUser:(VLMineViewUserClickType)type {
    if (type == VLMineViewUserClickTypeNickName) {
        [self showUpdateNickNameAlert];
    } else if (type == VLMineViewUserClickTypeAvatar) {
//        [self showUploadPicAlter];
    }
}

- (void)pushWebView:(NSString *)string {
    VLCommonWebViewController *webVC = [[VLCommonWebViewController alloc] init];
    webVC.urlString = string;
    [self.navigationController pushViewController:webVC animated:YES];
}

- (void)about {
    AboutAgoraEntertainmentViewController *VC = [[AboutAgoraEntertainmentViewController alloc] init];
    VC.hidesBottomBarWhenPushed = YES;
    VC.aboutAgoraDeletate = self;
    [self.navigationController pushViewController:VC animated:YES];
}

- (void)gotoDebugMode {
    DebugModeViewController *VC = [DebugModeViewController new];
    VC.hidesBottomBarWhenPushed = YES;
    [self.navigationController pushViewController:VC animated:YES];
}

- (void)gotoInviteCodePage {
    InviteCodeViewController *vc = [InviteCodeViewController new];
    [self.navigationController pushViewController:vc animated:YES];
}

- (void)userLogout {
    [[VLUserCenter center] logout];
//    [[VLGlobalHelper app] configRootViewController];
    [UIApplication.sharedApplication.delegate.window configRootViewController];
}

- (void)showUpdateNickNameAlert {

//    VL(weakSelf);
    __block UITextField *TF = nil;

    [LEEAlert alert].config
    .LeeTitle(AGLocalizedString(@"edit_name"))
    .LeeAddTextField(^(UITextField *textField) {
        textField.placeholder = AGLocalizedString(@"input_edit_name");
        textField.textColor = UIColorBlack;
        textField.clearButtonMode=UITextFieldViewModeWhileEditing;
        textField.font = UIFontMake(15);
        if (VLUserCenter.user.name.length > 0) {
            textField.text = VLUserCenter.user.name;
        }
        textField.delegate = self;
        [textField becomeFirstResponder];
        TF = textField; // Assignment
    })
    .LeeAddAction(^(LEEAction *action) {
        action.type = LEEActionTypeCancel;
        action.title = AGLocalizedString(@"cancel");
        action.titleColor = UIColorMakeWithHex(@"#000000");
        action.backgroundColor = UIColorMakeWithHex(@"#EFF4FF");
        action.cornerRadius = 20;
        action.height = 40;
        action.font = UIFontBoldMake(16);
        action.insets = UIEdgeInsetsMake(10, 20, 20, 20);
        action.borderColor = UIColorMakeWithHex(@"#EFF4FF");
        action.clickBlock = ^{
            
        };
    })
    .LeeAddAction(^(LEEAction *action) {
        VL(weakSelf);
        action.type = LEEActionTypeCancel;
        action.title = AGLocalizedString(@"confirm");
        action.titleColor = UIColorMakeWithHex(@"#FFFFFF");
        action.backgroundColor = UIColorMakeWithHex(@"#2753FF");
        action.cornerRadius = 20;
        action.height = 40;
        action.insets = UIEdgeInsetsMake(10, 20, 20, 20);
        action.font = UIFontBoldMake(16);
        action.clickBlock = ^{
            [weakSelf loadUpdateNickNameRequest:TF.text];
        };
    })
    .leeShouldActionClickClose(^(NSInteger index){
        // Whether the callback can be turned off is called when the callback is about to be turned off to decide whether to perform the closing process based on the return value
        // Here is an example of a combination with an input box non-null check
        BOOL result = ![TF.text isEqualToString:@""];
        result = index == 1 ? result : YES;
        return result;
    })
    .LeeShow();
}


- (BOOL)getLibraryAccess {
    return [NSUserDefaults.standardUserDefaults boolForKey:@"LibraryAccess"];
}

- (void)setLibraryAccess:(BOOL)isOpen {
    [NSUserDefaults.standardUserDefaults setBool:isOpen forKey:@"LibraryAccess"];
}

- (void)showAlert {
    UIAlertController *vc = [UIAlertController alertControllerWithTitle:AGLocalizedString(@"app_need_request_photo")
                                                                message:AGLocalizedString(@"app_need_request_photo and upload avatar")
                                                         preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *action1 = [UIAlertAction actionWithTitle:AGLocalizedString(@"not_allowed")
                                                      style:UIAlertActionStyleDefault
                                                    handler:^(UIAlertAction * _Nonnull action) {
        [self setLibraryAccess:NO];
    }];
    UIAlertAction *action2 = [UIAlertAction actionWithTitle:AGLocalizedString(@"requset_ok")
                                                      style:UIAlertActionStyleDefault
                                                    handler:^(UIAlertAction * _Nonnull action) {
        [self setLibraryAccess:YES];
        [self presentviewcontrollerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary];
    }];
    [vc addAction:action1];
    [vc addAction:action2];
    [self.navigationController presentViewController:vc
                                            animated:YES
                                          completion:nil];
}

- (void)showUploadPicAlter {
    kWeakSelf(self)
    [LEEAlert actionsheet].config
    .LeeAddAction(^(LEEAction * _Nonnull action) {
        action.type = LEEActionTypeDefault;
        action.title = AGLocalizedString(@"app_upload_avatar");
        action.height = 20;
        action.titleColor = [UIColor whiteColor];
        action.font = VLUIFontMake(14);
    })
    .LeeAddAction(^(LEEAction * _Nonnull action) {
        action.type = LEEActionTypeDefault;
        action.title = AGLocalizedString(@"take_photo_and_upload");
        action.clickBlock = ^{
            [weakself requestAuthorizationForCamera];
        };
    })
    .LeeAddAction(^(LEEAction * _Nonnull action) {
        action.type = LEEActionTypeDefault;
        action.title = AGLocalizedString(@"local_upload");
        action.clickBlock = ^{
            [weakself requestAuthorizationForPhotoLibrary];
        };
    })
    .LeeAddAction(^(LEEAction * _Nonnull action) {
        action.type = LEEActionTypeCancel;
        action.title = AGLocalizedString(@"cancel");
        action.clickBlock = ^{
        };
    })
    .LeeShow();
}

- (void)requestAuthorizationForPhotoLibrary {
    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (status == PHAuthorizationStatusAuthorized) {
                [self presentviewcontrollerWithSourceType: UIImagePickerControllerSourceTypePhotoLibrary];
            }else{
                [self showAlertWithMessage:@"Album permissions are not set, please enable album permissions"];
            }
        });
    }];
}

- (void)requestAuthorizationForCamera{
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if(granted == true){
                [self presentviewcontrollerWithSourceType:UIImagePickerControllerSourceTypeCamera];
            } else {
                [self showAlertWithMessage:@"Camera permissions are not set. Please enable camera permissions"];
            }
        });
    }];
}

-(void)showAlertWithMessage:(NSString *)mes {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Tips" message:mes preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *okAction = [UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [[UIApplication sharedApplication]openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString] options:nil completionHandler:nil];
    }];
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleDefault handler:nil];
    [alertController addAction:cancelAction];
    [alertController addAction:okAction];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)presentviewcontrollerWithSourceType:(UIImagePickerControllerSourceType)sourceType {
    if (sourceType == UIImagePickerControllerSourceTypePhotoLibrary && ![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary]) {
        [VLToast toast:AGLocalizedString(@"comm_permission_leak_sdcard_title")];
        return ;
    }
    if (sourceType == UIImagePickerControllerSourceTypeCamera && ![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        [VLToast toast:AGLocalizedString(@"comm_permission_leak_camera_title")];
        return ;
    }
    UIImagePickerController *controller = [[UIImagePickerController alloc]init];
    [controller.navigationBar setTintColor:UIColorMakeWithHex(@"#ff6535")];
    controller.delegate = self;
    controller.allowsEditing = YES;
    controller.sourceType = sourceType;
    NSMutableArray *mediaTypes = [[NSMutableArray alloc] init];
    [mediaTypes addObject:(__bridge NSString *)kUTTypeImage];
    controller.mediaTypes = mediaTypes;
    [self presentViewController:controller animated:YES completion:^(void){
    }];
}

#pragma mark for debug
- (void)motionEnded:(UIEventSubtype)motion withEvent:(UIEvent *)event {
    [super motionEnded:motion withEvent:event];
    
    UIActivityViewController *controller = [[UIActivityViewController alloc] initWithActivityItems:@[[NSURL fileURLWithPath:[AgoraEntLog cacheDir]]]
                                                                             applicationActivities:nil];

    [self presentViewController:controller animated:YES completion:nil];
}


#pragma mark - UIImagePickerControllerDelegate
- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    [picker dismissViewControllerAnimated:YES completion:^{
        UIImage *image = info[UIImagePickerControllerEditedImage];
        [self uploadHeadImageWithImage:image];
    }];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [picker dismissViewControllerAnimated:YES completion:^(){
    }];
}


#pragma mark - Request
/// Get user information
- (void)loadRequestUserInfoRequest {
    NSDictionary *param = @{@"userNo":VLUserCenter.user.userNo ?: @""};
    [VLAPIRequest getRequestURL:kURLPathGetUserInfo parameter:param showHUD:NO success:^(VLResponseDataModel * _Nonnull response) {
        if (response.code == 0) {
//            VLLoginModel *userInfo = [VLLoginModel vj_modelWithDictionary:response.data];
        }
    } failure:^(NSError * _Nullable error, NSURLSessionDataTask * _Nullable task) {
        
    }];
}

- (void)loadUpdateUserIconRequest:(NSString *)iconUrl image:(UIImage *)image{
    NSDictionary *param = @{
        @"userNo" : VLUserCenter.user.userNo ?: @"",
        @"headUrl" : iconUrl ?: @""
    };
    
    [VLAPIRequest postRequestURL:kURLPathUploadUserInfo parameter:param showHUD:YES success:^(VLResponseDataModel * _Nonnull response) {
        if (response.code == 0) {
            [VLToast toast:AGLocalizedString(@"app_edit_success")];
            [self.mineView refreseAvatar:image];
            VLUserCenter.user.headUrl = iconUrl;
            [[VLUserCenter center] storeUserInfo:VLUserCenter.user];
        }else{
            [VLToast toast:response.message];
        }
    } failure:^(NSError * _Nullable error, NSURLSessionDataTask * _Nullable task) {
    }];
}


- (void)loadUpdateNickNameRequest:(NSString *)nickName {
    NSDictionary *param = @{
        @"userNo" : VLUserCenter.user.userNo ?: @"",
        @"name" : nickName ?: @""
    };
    [self.mineView refreseNickName:nickName];
    VLUserCenter.user.name = nickName;
    [[VLUserCenter center] storeUserInfo:VLUserCenter.user];
}

/// Upload pictures
/// @param image Image
- (void)uploadHeadImageWithImage:(UIImage *)image {
    [VLAPIRequest uploadImageURL:kURLPathUploadImage showHUD:YES appendKey:@"file" images:@[image] success:^(VLResponseDataModel * _Nonnull response) {
        if (response.code == 0) {
            VLUploadImageResModel *model = [VLUploadImageResModel vj_modelWithDictionary:response.data];
            [self loadUpdateUserIconRequest:model.url image:image];
        }
        else {
            [VLToast toast:response.message];
        }
    } failure:^(NSError * _Nullable error, NSURLSessionDataTask * _Nullable task) {
    }];
}

// Continuous click event
- (void)didTapedVersionLabel {
    [AppContext shared].isDebugMode = YES;
    [self.mineView refreshTableView];
}

#pragma mark - Public Methods

- (void)configNavigationBar:(UINavigationBar *)navigationBar {
    [super configNavigationBar:navigationBar];
}
- (BOOL)preferredNavigationBarHidden {
    return true;
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSInteger newLength = textField.text.length + string.length - range.length;
    return newLength <= self.maxInputLength;
}

#pragma mark - AboutAgoraEntertainmentViewControllerDelegate

- (void)debugModeChangedCallback {
    [self.mineView refreshTableView];
}

@end
