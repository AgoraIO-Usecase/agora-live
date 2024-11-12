# Live Streaming

This document introduces how to quickly run through Live Streaming sample project.

Demo effect:

<img src="https://download.agora.io/demo/release/LiveStreamingShot01.png" width="300" height="640" /><img src="https://download.agora.io/demo/release/LiveStreamingShot02.png" width="300" height="640" />

## Prerequisites

- Android Studio 4.0 or higher
- Android SDK API Level 24 or higher
- A mobile device that runs Android 7.0 or higher
- FaceUnity beauty authpack file

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.
2. Follow [The Restfull Document](https://docs.agora.io/en/video-calling/reference/restful-authentication) to get the **Customer ID** and **Customer Secret**.
3. Follow [Signaling Beginner's Guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=android) to enable signaling in Agora Console. You should enable the following:
* Using storage
* User attribute callback
* Channel attribute callback
* Distributed lock
4. Please contact technical support to enable robot room streaming service.
```json
If this step is not executed, you will not be able to see the video in robot rooms.
```
5. Open the `Android` project and fill in properties got above to the root [gradle.properties](../gradle.properties) file.

```
# RTM RTC SDK key Config
AGORA_APP_ID=<Your Agora App ID>
AGORA_APP_CERTIFICATE=<Your Agora App Certificate>

# Cloud Player Config
CLOUD_PLAYER_KEY=<Your Agora Customer ID>
CLOUD_PLAYER_SECRET=<Your Agora Customer Secret>
```

6. Obtain beauty resources and certificate files from FaceUnity:
   * Copy beauty certificate file `authpack.java` to `scenes/show/src/main/java/io/agora/scene/show/beauty` directory
   * Copy beauty resource files to `scenes/show/src/main/assets/beauty_faceunity` directory
```json
If this step is not executed, you will not be able to experience the beauty feature.
```
7. Now you can run the project with android studio to experience the application.

## Source Code Sitemap

| Path(Android/scenes/show/src/main/java/io/agora) | Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| beautyapi/faceunity/                             | [Agora FaceUntiy Beauty Scenes API](https://github.com/AgoraIO-Community/BeautyAPI). |
| scene/show/vodeoSwitcherAPI/                     | Agora Video Switcher Scenes API.                                                     |
| scene/show/service/                              | Living streaming service protocol and implement.                                     |
| scene/show/RoomListActivity.kt                   | Living room list view.                                                               |
| scene/show/LivePrepareActivity.kt                | Living prepare view.                                                                 |
| scene/show/LiveDetailActivity.kt                 | Living room detail scroll page view.                                                 |
| scene/show/LiveDetailFragment.kt                 | Living room detail view.                                                             |
| scene/show/RtcEngineInstance.kt                  | RTC Engine initializing.                                                             |
| scene/show/VideoSetting.kt                       | RTC video setting.                                                                   |
| scene/show/ShowLogger.kt                         | Living streaming logger.                                                             |
| scene/show/debugSettings/                        | Living streaming debug setting ui.                                                   |
| scene/show/beauty/                               | Living streaming beauty implement.                                                   |
| scene/show/utils/                                | Living streaming utils.                                                              |
| scene/show/widget/                               | Living streaming UI widgets.                                                         |

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

