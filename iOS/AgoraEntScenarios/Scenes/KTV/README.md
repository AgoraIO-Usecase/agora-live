# Online KTV

> This document mainly introduces how to quickly run through the <mark>online KTV</mark> sample project
> 
> Demo Effect:
> 
> <img src="https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/agora-live/ktv_create_room.png" width="300" height="640"><img src="https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/agora-live/ktv_room_detail.png" width="300" height="640">

## Prerequisites

- Xcode 14 or higher
- A mobile device that runs iPhone 13.0 or higher

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.
2. Follow [Signaling Beginner's Guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=ios) to enable signaling in Agora Console. You should enable the following:
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
4. Now you can run the project with Xcode to experience the application.

## Source Code sitemap

| Path(iOS/AgoraEntScenarios/Scenes/Commerce) | Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| Scenes/KTV/Service/                              | KTV service protocol and implement.                                     |
| Scenes/KTV/Controller/VLOnLineListVC.h                  | KTV room list view.                                                               |
| Scenes/KTV/Controller/VLKTVViewController.h                | KTV room detail view.                                                             |
| Scenes/Commerce/View/                               | KTV UI widgets.                                                         |

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
