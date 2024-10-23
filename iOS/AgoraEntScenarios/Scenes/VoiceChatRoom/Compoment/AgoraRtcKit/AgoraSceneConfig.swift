//
//  AgoraSceneConfig.swift
//  AgoraScene_iOS
//
//  Created by CP on 2022/9/5.
//

import Foundation

public enum AgoraConfig {
    private static let VMBaseUrl = "https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemochat/aisound"
    public static let CreateCommonRoom = "\(AgoraConfig.VMBaseUrl)/01CreateRoomCommonChatroom"
    public static let CreateSpatialRoom = "\(AgoraConfig.VMBaseUrl)/02CeateRoomSpaticalChatroom"
    public static let SetAINSIntroduce = "\(AgoraConfig.VMBaseUrl)/07AINSIntroduce"

    public static let baseAlienMic: [String] = [
        "/EN/01-01-B-EN.wav",
        "/EN/01-02-R-EN.wav",
        "/EN/01-03-B&R-EN.wav",
        "/EN/01-04-B-EN.wav",
        "/EN/01-05-R-EN.wav",
        "/EN/01-06-B-EN.wav",
        "/EN/01-07-R-EN.wav",
        "/EN/01-08-B-EN.wav",
    ]

    public static let spatialAlienMic: [String] = [
        "/EN/02-01-B-EN.wav",
        "/EN/02-02-R-EN.wav",
        "/EN/02-03-B&R-EN.wav",
        "/EN/02-04-B-EN.wav",
        "/EN/02-05-R-EN.wav",
        "/EN/02-06-B-EN.wav",
        "/EN/02-07-R-EN.wav",
        "/EN/02-08-B-EN.wav",
        "/EN/02-09-R-EN.wav",
        "/EN/02-10-B-EN.wav",
        "/EN/02-11-R-EN.wav",
        "/EN/02-12-B-EN.wav",
        "/EN/02-13-R-EN.wav",
        "/EN/02-14-B-EN.wav",
        "/EN/02-15-R-EN.wav",
        "/EN/02-16-B-EN.wav",
        "/EN/02-17-R-EN.wav",
        "/EN/02-18-B-EN.wav",
    ]

    public static let HighAINSIntroduc: [String] = [
        "/EN/Share/07-01-R-EN.wav",
        "/EN/High/07-02-B-EN-High.wav",
        "/EN/High/07-03-R-EN-High.wav",
        "/EN/Share/07-04-B-EN.wav",
        "/EN/Share/07-05-R-EN.wav",
        "/EN/Share/07-06-B-EN.wav",
        "/EN/Share/07-07-R-EN.wav",
    ]

    public static let MediumAINSIntroduc: [String] = [
        "/EN/Share/07-01-R-EN.wav",
        "/EN/Medium/07-02-B-EN-Medium.wav",
        "/EN/Medium/07-03-R-EN-Medium.wav",
        "/EN/Share/07-04-B-EN.wav",
        "/EN/Share/07-05-R-EN.wav",
        "/EN/Share/07-06-B-EN.wav",
        "/EN/Share/07-07-R-EN.wav",
    ]

    public static let NoneAINSIntroduc: [String] = [
        "/EN/Share/07-01-R-EN.wav",
        "/EN/None/07-02-B-EN-None.wav",
        "/EN/None/07-03-R-EN-None.wav",
        "/EN/Share/07-04-B-EN.wav",
        "/EN/Share/07-05-R-EN.wav",
        "/EN/Share/07-06-B-EN.wav",
        "/EN/Share/07-07-R-EN.wav",
    ]

    public static let SoundSelectSocial: [String] = [
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-01-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-02-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-03-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-04-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-05-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-06-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-07-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/03SoundSelectionSocialChat/EN/03-08-R-EN.wav",
    ]

    public static let SoundSelectKTV: [String] = [
        "\(AgoraConfig.VMBaseUrl)/04SoundSelectionKaraoke/EN/04-01-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/04SoundSelectionKaraoke/EN/04-02-B-EN.wav",
    ]

    public static let SoundSelectGame: [String] = [
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-01-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-02-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-03-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-04&05-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-06-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-07-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-08-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-09-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-10-B-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/05SoundSelectionGamingBuddy/EN/05-11-R-EN.wav",
    ]

    public static let SoundSelectAnchor: [String] = [
        "\(AgoraConfig.VMBaseUrl)/06SoundProfessionalBroadcaster/EN/06-01-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/06SoundProfessionalBroadcaster/EN/06-02-R-EN.wav",
        "\(AgoraConfig.VMBaseUrl)/06SoundProfessionalBroadcaster/EN/06-03-R-EN.wav",
    ]

    public static let HighSound: [String] = [
        "\(AgoraConfig.VMBaseUrl)/08AINSTVSound/EN/High/08-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/09AINSKitchenSound/EN/High/09-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/10AINStreetSound/EN/High/10-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/11AINSRobotSound/EN/High/11-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/12AINSOfficeSound/EN/High/12-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/13AINSHomeSound/EN/High/13-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/14AINSConstructionSound/EN/High/14-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/15AINSAlertSound/EN/High/15-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/16AINSApplause/EN/High/16-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/17AINSWindSound/EN/High/17-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/18AINSMicPopFilter/EN/High/18-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/19AINSAudioFeedback/EN/High/19-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/20ANISMicrophoneFingerRubSound/EN/High/20-01-B-EN-High.wav",
        "\(AgoraConfig.VMBaseUrl)/21ANISScreenTapSound/EN/High/21-01-B-EN-High.wav",
    ]

    public static let NoneSound: [String] = [
        "\(AgoraConfig.VMBaseUrl)/08AINSTVSound/EN/None/08-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/09AINSKitchenSound/EN/None/09-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/10AINStreetSound/EN/None/10-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/11AINSRobotSound/EN/None/11-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/12AINSOfficeSound/EN/None/12-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/13AINSHomeSound/EN/None/13-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/14AINSConstructionSound/EN/None/14-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/15AINSAlertSound/EN/None/15-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/16AINSApplause/EN/None/16-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/17AINSWindSound/EN/None/17-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/18AINSMicPopFilter/EN/None/18-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/19AINSAudioFeedback/EN/None/19-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/20ANISMicrophoneFingerRubSound/EN/None/20-01-B-EN-None.wav",
        "\(AgoraConfig.VMBaseUrl)/21ANISScreenTapSound/EN/None/21-01-B-EN-None.wav",
    ]
}
