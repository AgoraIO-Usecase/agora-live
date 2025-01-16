# Audio Chatroom

This document introduces how to quickly run through Audio Chatroom sample project.

Demo effect:

<img src="https://fullapp.oss-cn-beijing.aliyuncs.com/agora-live/readme/images/voicechat/v1.2.0/screenshot_ios_01.png" width="300" height="640"><img src="https://fullapp.oss-cn-beijing.aliyuncs.com/agora-live/readme/images/voicechat/v1.2.0/screenshot_ios_02.png" width="300" height="640">

---

## Prerequisites

- Xcode 14 or higher
- A mobile device that runs iPhone 13.0 or higher

---

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.

2. Follow [Enable and configure Chat](https://docs.agora.io/en/agora-chat/get-started/enable?platform=ios) to enable Chat in Agora Console and get the  **IM APP KEY**, **IM Client Id** and **IM Client Secret**.

3. Open the `iOS` project and fill in properties got above to the root [KeyCenter.swift](../../KeyCenter.swift) file. 

	```
	# RTC SDK and IM key Config
	static let AppId: String = <#YOUR APPID#>

	static let Certificate: String? = <#YOUR CERTIFICATE#>
	
	static var IMAppKey: String? = <#YOUR IMAppKey#>
	
	static var IMClientId: String? = <#YOUR IMClientId#>
	
	static var IMClientSecret: String? = <#YOUR IMClientSecret#>
	```
4. Now you can run the project with Xcode to experience the application.

---


## Source Code Sitemap

| Path(iOS/AgoraEntScenarios/Scenes/VoiceChatRoom)| Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| Scenes/VoiceChatRoom/Compoment/VoiceRoomIMKit/                               | IM Manager                                                      |
| Scenes/VoiceChatRoom/Network/                              | Net Manager                                                               |
| Scenes/VoiceChatRoom/Compoment/AgoraRtcKit/                              | Rtc Manager                                                                 |
| Scenes/VoiceChatRoom/Service/                             | Service                                                 |
| Scenes/VoiceChatRoom/Controllers                                  | ViewController                                                             |
| Scenes/VoiceChatRoom/Views                            | View                                                              |

## Feedback

If you have any problems or suggestions regarding the sample projects, feel free to file an issue.

## Related Resources

- Check our [FAQ](https://docs.agora.io/en/faq) to see if your issue has been recorded.
- Dive into [Agora SDK Samples](https://github.com/AgoraIO) to see more tutorials.
- Take a look at [Agora Use Case](https://github.com/AgoraIO-usecase) for more complicated real use cases.
- Repositories managed by developer communities can be found at [Agora Community](https://github.com/AgoraIO-Community).
- If you encounter problems during integration, feel free to ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/agora.io).

## License

The sample projects are under the MIT license.