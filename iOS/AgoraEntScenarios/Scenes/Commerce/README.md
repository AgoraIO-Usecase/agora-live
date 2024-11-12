# E-Commerce

> This document introduces how to quickly run through <mark>E-Commerce</mark> sample project.

<figure class="third">
  <img src="https://fullapp.oss-cn-beijing.aliyuncs.com/agora-live/readme/images/commerce/v1.2.0/screenshot_ios_01.png" width="300" height="640" />
  <img src="https://fullapp.oss-cn-beijing.aliyuncs.com/agora-live/readme/images/commerce/v1.2.0/screenshot_ios_02.png" width="300" height="640" />
</figure>

## Prerequisites

- Xcode 14 or higher
- A mobile device that runs iPhone 13.0 or higher

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate (if token is enabled)**.
2. Follow [Signaling Beginner's guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=ios) to enable signaling in Agora Console.You should enable the following:
* Using storage
* User attribute callback
* Channel attribute callback
* Distributed lock
3. Open the `iOS` project and fill in properties got above to the root [KeyCenter.swift](../../KeyCenter.swift) file. 

	```
	# RTM RTC SDK key Config
	static let AppId: String = <#YOUR APPID#>
	static let Certificate: String? = <#YOUR CERTIFICATE#>
	```
5. Now you can run the project with Xcode to experience the application.

## Source Code Sitemap

| Path(iOS/AgoraEntScenarios/Scenes/Commerce) | Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| Common/API/VideoLoaderAPI           |Agora Video Loader Scenario API.
| Scenes/Commerce/Service/                              | E-Commerce service protocol and implement.                                     |
| Scenes/Commerce/Controller/CommerceRoomListVC.swift                  | E-Commerce room list view.                                                               |
| Scenes/Commerce/Controller/CommerceLivePagesViewController.swift                | E-Commerce room detail scroll page view.                                                 |
| Scenes/Commerce/Controller/CommerceLiveViewController.swift                | E-Commerce room detail view.                                                             |
| Scenes/Commerce/Models/CommerceAgoraKitManager.swift                  | RTC Engine initializing.                                                                                                              |
| Scenes/Commerce/View/                               | E-Commerce streaming UI widgets.                                                         |

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

