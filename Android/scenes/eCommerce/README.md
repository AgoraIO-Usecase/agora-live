# E-Commerce

This document introduces how to quickly run through E-Commerce sample project.

Demo effect:

<img src="https://download.agora.io/demo/release/CommerceShot01.png" width="300" height="640" /> <img src="https://download.agora.io/demo/release/CommerceShot02.png" width="300" height="640" />

## Prerequisites

- Android Studio 4.0 or higher
- Android SDK API Level 24 or higher
- A mobile device that runs Android 7.0 or higher

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate (if token is enabled)**.
2. Follow [Signaling Beginner's Guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=android) to enable signaling in Agora Console. You should enable the following:
* Using storage
* User attribute callback
* Channel attribute callback
* Distributed lock
3. Open the `Android` project and fill in the properties obtained above in the root [gradle.properties](../gradle.properties) file.

```
# RTC SDK key Config
AGORA_APP_ID=<Your Agora App ID>
AGORA_APP_CERTIFICATE=<Your Agora App Certificate (if token is enabled)>
```

4. Now you can run the project with Android Studio to experience the application.

## Source Code sitemap

| Path(Android/scenes/eCommmerce/src/main/java/io/agora) | Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| scene/eCommmerce/videoLoaderAPI/                       | Agora Video Loader Scenario API.                                                     |
| scene/eCommmerce/service/                              | Living streaming service protocol and implementation.                                     |
| scene/eCommmerce/RoomListActivity.kt                   | Living room list view.                                                               |
| scene/eCommmerce/LivePrepareActivity.kt                | Living prepare view.                                                                 |
| scene/eCommmerce/LiveDetailActivity.kt                 | Living room detail scroll page view.                                                 |
| scene/eCommmerce/LiveDetailFragment.kt                 | Living room detail view.                                                             |
| scene/eCommmerce/RtcEngineInstance.kt                  | RTC Engine initializing.                                                             |
| scene/eCommmerce/VideoSetting.kt                       | RTC video setting.                                                                   |
| scene/eCommmerce/CommmerceLogger.kt                    | Living streaming logger.                                                             |
| scene/eCommmerce/utils/                                | Living streaming utils.                                                              |
| scene/eCommmerce/widget/                               | Living streaming UI widgets.                                                         |
| scene/eCommmerce/shop/                                 | eCommerce UI widgets.                                                                |

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

