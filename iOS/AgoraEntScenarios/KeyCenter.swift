//
//  KeyCenter.swift
//  OpenLive
//
//  Created by GongYuhua on 6/25/16.
//  Copyright © 2016 Agora. All rights reserved.
//

@objcMembers
class KeyCenter: NSObject {
    
    /**
     Agora APP ID.
     Agora assigns App IDs to app developers to identify projects and organizations.
     If you have multiple completely separate apps in your organization, for example built by different teams,
     you should use different App IDs.
     If applications need to communicate with each other, they should use the same App ID.
     In order to get the APP ID, you can open the agora console (https://console.agora.io/) to create a project,
     then the APP ID can be found in the project detail page.
     */
    static let AppId: String = <#YOUR APPID#>
    /**
     Certificate.
     Agora provides App certificate to generate Token. You can deploy and generate a token on your server,
     or use the console to generate a temporary token.
     In order to get the APP ID, you can open the agora console (https://console.agora.io/) to create a project with the App Certificate enabled,
     then the APP Certificate can be found in the project detail page.
     PS: If the project does not have certificates enabled, leave this field blank.
     */
    static let Certificate: String? = nil
    
    /**
     Token.
     Agora provides Temporary Access Token to join the spatial channel with APP ID which enable App Certificate.
     You can use it to test your project.
     You can generate the temporary access token in the project console with the App Certificate enabled.
     PS：If agora_app_certificate is configured, this field will be invalid.
     */
    
    static var Token: String? = nil
    /**
     EaseMob APPKEY.
     The application name filled in when creating an application on the EaseMob  console.
     If you need to use Chat Room, you must to set this parameter.
     Please refer to the information on obtaining instant messaging IM from EaseMob for more information(http://docs-im-beta.easemob.com/product/enable_and_configure_IM.html#%E8%8E%B7%E5%8F%96%E7%8E%AF%E4%BF%A1%E5%8D%B3%E6%97%B6%E9%80%9A%E8%AE%AF-im-%E7%9A%84%E4%BF%A1%E6%81%AF).
     */
    static var IMAppKey: String? = nil
    /**
     EaseMob Client ID.
     Client id of EaseMob, used to generate app tokens to call REST API.
     If you need to use Chat Room, you must to set this parameter.
     Please refer to the information on obtaining instant messaging IM from EaseMob for more information( https://console.easemob.com/user/login/ ).
     */
    
    static var IMClientId: String? = nil
    
    /**
     EaseMob Client Secret.
     Client Secret of EaseMob, used to generate app tokens to call REST API.
     If you need to use Chat Room, you must to set this parameter.
     Please refer to the information on obtaining instant messaging IM from EaseMob for more information( https://console.easemob.com/user/login/ ).
     */
    static var IMClientSecret: String? = nil
    
    static let CloudPlayerKey: String? = nil
    static let CloudPlayerSecret: String? = nil
    
    static var HostUrl: String = "https://gateway-fulldemo.agoralab.co"
    static var baseServerUrl: String? = "https://service.agora.io/toolbox-overseas/v1/"
    static var onlineBaseServerUrl: String? = baseServerUrl
    static var RTMHostUrl: String = "https://service-staging.agora.io/room-manager-overseas"
}
