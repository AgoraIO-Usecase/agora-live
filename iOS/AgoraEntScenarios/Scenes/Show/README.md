# Live Streaming

This document introduces how to quickly run through Live Streaming sample project.

Demo effect:

<img src="https://download.agora.io/demo/release/LiveStreamingShot01.png" width="300" height="640" /><img src="https://download.agora.io/demo/release/LiveStreamingShot02.png" width="300" height="640" />

## Prerequisites

- Xcode 14 or higher
- A mobile device that runs iPhone 13.0 or higher
- FU beauty authpack file

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.
2. Follow [The Restfull Document](https://docs.agora.io/en/video-calling/reference/restful-authentication) to get the **Customer ID** and **Customer Secret**.
3. Follow [Signaling Beginner's Guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=ios) to enable signaling in Agora Console. You should enable the following:
* Using storage
* User attribute callback
* Channel attribute callback
* Distributed lock
4. Please contact technical support to enable robot room streaming service.
```json
If this step is not executed, you will not be able to see the video in robot rooms.
```
4. Open the `iOS` project and fill in properties got above to the root [KeyCenter.swift](../../KeyCenter.swift) file. 

	```
	# RTM RTC SDK key Config
	static let AppId: String = <#YOUR AppId#>
	```
5. Now you can run the project with Xcode to experience the application.

## Source Code Sitemap

| Path(iOS/AgoraEntScenarios/Scenes/Show) | Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| beautyapi/faceunity/                             | [Agora FaceUntiy Beauty Scenes API](https://github.com/AgoraIO-Community/BeautyAPI). |
| Scenes/Show/Service/                              | Living streaming service protocol and implement.                                     |
| Scenes/Show/Controller/ShowRoomListVC.swift                  | Living room list view.                                                               |
| Scenes/Show/Controller/ShowLivePagesViewController.swift                | Living room detail scroll page view.                                                 |
| Scenes/Show/Controller/ShowLiveViewController.swift                | Living room detail view.                                                             |
| Scenes/Show/Models/ShowAgoraKitManager.swift                  | RTC Engine initializing.                                                                                                              |
| Scenes/Show/Beauty/                              | Living streaming beauty implement.                                                   |                                                         |
| Scenes/Show/View/                               | Living streaming UI widgets.                                                         |

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

