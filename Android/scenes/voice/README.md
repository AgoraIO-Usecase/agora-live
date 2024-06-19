# Audio Chatroom

> This document introduces how to quickly run through <mark>Audio Chatroom</mark> sample project.

<figure class="third">
  <img src="https://download.agora.io/demo/release/VoiceChatShot01.png" width="300" height="640" />
  <img src="https://download.agora.io/demo/release/VoiceChatShot02.png" width="300" height="640" />
</figure>

## Prerequisites

- Android Studio 3.5 or higher.
- Android SDK API Level 21 or higher.
- A mobile device that runs Android 5.0 or higher.

## Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.
2. Follow [Enable and configure Chat](https://docs.agora.io/en/agora-chat/get-started/enable?platform=android) to enable Chat in Agora Console and get the **IM_APP_KEY**, **IM_APP_CLIENT_ID** and **IM_APP_CLIENT_SECRET**.
3. Open the `Android` project and fill in properties got above to the root [gradle.properties](../gradle.properties) file.

``` 
# RTC SDK key Config
AGORA_APP_ID=<Your Agora App ID>
AGORA_APP_CERTIFICATE=<Your Agora App Certificate(if enable token)>
  
# IM SDK key Config
IM_APP_KEY=
IM_APP_CLIENT_ID=
IM_APP_CLIENT_SECRET=
```

4. Now you can run the project with android studio to experience the application.
---

## Source Code sitemap

| Path(Android/scenes/voice/src/main/java/io/agora)| Description                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------|
| scene/voice/global/                              | Basic Module                                                    |
| scene/voice/imkit/                               | IM Manager                                                      |
| scene/voice/model/                               | Basic Model                                     |
| scene/voice/netkit/                              | Net Manager                                                               |
| scene/voice/rtckit/                              | Rtc Manager                                                                 |
| scene/voice/service/                             | Service                                                 |
| scene/voice/ui/                                  | Living room detail view.                                                             |
| scene/voice/viewmodel                            | View Model                                                             |

## Feedback

If you have any problems or suggestions regarding the sample projects, feel free to file an issue.

## Related resources

- Check our [FAQ](https://docs.agora.io/en/faq) to see if your issue has been recorded.
- Dive into [Agora SDK Samples](https://github.com/AgoraIO) to see more tutorials.
- Take a look at [Agora Use Case](https://github.com/AgoraIO-usecase) for more complicated real use case.
- Repositories managed by developer communities can be found at [Agora Community](https://github.com/AgoraIO-Community).
- If you encounter problems during integration, feel free to ask questions in [Stack Overflow](https://stackoverflow.com/questions/tagged/agora.io).

## License

The sample projects are under the MIT license.


