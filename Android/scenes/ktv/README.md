# Online KTV

> This document mainly explains how to quickly run through the <mark> Online KTV </mark> sample project.
>
> Demo effect:
> 
> <img src="https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ktv/ktv_130_1.png" width="300" height="640"><img src="https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ktv/ktv_130_2.png" width="300" height="640">
---

## 1. Prerequisites

- <mark>Minimum compatibility with Android 7.0</mark>(SDK API Level 24).
- Android SDK API Level 21 or higher.
- A mobile device that runs Android 5.0 or higher.

---

## 2.Project Setup

1. Follow [The Account Document](https://docs.agora.io/en/video-calling/reference/manage-agora-account) to get the **App ID** and **App Certificate**.
2. Follow [Signaling Beginner's Guide](https://docs.agora.io/en/signaling/get-started/beginners-guide?platform=android) to enable signaling in Agora Console. You should enable the following:
* Using storage
* User attribute callback
* Channel attribute callback
* Distributed lock
3. Open the `Android` project and fill in properties got above to the root [gradle.properties](../../gradle.properties) file.

  ``` 
    # RTC SDK key Config
    AGORA_APP_ID=<Your Agora App ID>
    AGORA_APP_CERTIFICATE=<Your Agora App Certificate>
  ```

3. Now you can run the project with android studio to experience the application.

---

## 3. Project Introduction

### 3.1 Overview

> The **Online KTV**project is the open-source code for Agora’s online karaoke room scenario. Developers can obtain and integrate it into their APP projects. This source code will be updated in sync with the Voice Entertainment Demo. To access more new features and better sound effects, we strongly recommend downloading the latest code for integration.

### 3.2 Introduction to the Project File Structure

```
├── scene
│   ├── ktv
│   │   └── main
│   │       ├── java
│   │       │   └── io.agora.scene.ktv
│   │       │                       ├── create         #Room list
│   │       │                       ├── debugSettings  #Debug page
|   |       |                       ├── ktvapi         #KTV api module
│   │       │                       ├── live           #Living room detail
│   │       │                       ├── service        #Service
│   │       │                       ├── widget         #UI widget
│   │       │                       └── KTVLogger.kt   #Log
│   │       ├── res              #resource
│   │       │   ├── drawable
│   │       │   ├── layout
│   │       │   ├── mipmap
│   │       │   └── values
│   │       └── AndroidManifest.xml
│   │   
│   ├── build.gradle
│   ├── build
│   └── gradle
│       └── wrapper
│           ├── gradle-wrapper.jar
│           └── gradle-wrapper.properties
├── build.gradle     
├── config.gradle       #common dependency configuration
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── gradle.properties  #Project basic account configuration(appid、app certificate)
└── settings.gradle
```

### 3.3 Function introduction

> The online KTV scenario currently includes the following functions, which you can refer to the comments and call from the code as needed
>
> Scene function code root directory **Android/scenes/ktv**
>
> ---
>
> #### KTV scene API
>
> ![xxx](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ktv/ktv_130_4.png)
>
> KTV scene API is a module that helps you quickly integrate the KTV capability of Agora, with which you can easily obtain song list information, load songs, switch singing roles, control music playback, etc. Through [**KTVApi**](src/main/java/io/agora/scene/ktv/live/KTVApi.kt), you can define the protocol, and through [**KTVApiImp**](src/main/java/io/agora/scene/ktv/live/KTVApiImp.kt) to implement, you can directly copy these two files to your project for use, quickly integrating the KTV capability of Agora
>
>
> * Load songs
>
>   ```kotlin
>   /**
>    * Synchronously load the song URI; only one song can be loaded at a time using loadSong
>    * @param url music url
>    * @param config load song configuration
>    *
>    * Recommended call：
>    * When the song begins：
>    * SoloSinger switchSingerRole(SoloSinger)-->loadMusic(musicUri, KTVLoadMusicConfiguration(songIdentifier, mainSingerUid, mode=LOAD_MUSIC_AND_LRC, needPrelude=false))---> startSing(musicUri,0)
>    * Audience loadMusic(musicUri, KTVLoadMusicConfiguration(songIdentifier, mainSingerUid, mode=LOAD_LRC_ONLY, needPrelude=false))
>    * When joining the chorus：
>    * Preparing to join as a co-singer：loadMusic(musicUri,KTVLoadMusicConfiguration(autoPlay=false, mode=LOAD_MUSIC_ONLY, songCode, mainSingerUid))
>    * loadMusic and rtm joinChorus callback success then switchSingerRole(CoSinger)
>    */
>   fun loadMusic(
>     url: String,
>     config: KTVLoadMusicConfiguration,
>   ){}
>   ```
>
> * Switch the singing role
>
>   Through this interface, you can switch between different roles during the singing process, and the result of the role switch will be notified to you via callback.
>
>   ```kotlin
>   /**
>    * Asynchronously switch singing roles, and the result will be notified to the business layer via callback.
>    * @param newRole The new singing role.
>    * @param onSwitchRoleState The result of switching the singing role.
>    *
>    * Allowed call paths:
>    * 1、Audience -》SoloSinger   When the song you selected is playing.
>    * 2、Audience -》LeadSinger   When the song you selected is playing and a co-singer has joined at the start of the song.
>    * 3、SoloSinger -》Audience   When the solo performance ends.
>    * 4、Audience -》CoSinger     When joining a chorus.
>    * 5、CoSinger -》Audience     When leaving the chorus.
>    * 6、SoloSinger -》LeadSinger When the first co-singer joins the chorus, the lead singer switches from solo to lead singer.
>    * 7、LeadSinger -》SoloSinger When the last co-singer leaves the chorus, the lead singer switches from lead singer to solo singer.
>    * 8、LeadSinger -》Audience   When the song ends with the lead singer role.
>    */
>   fun switchSingerRole(
>     newRole: KTVSingRole,
>     onSwitchRoleStateListener: OnSwitchRoleStateListener?
>   ){}
>   ```
>
> * Control the song
>
>   ```kotlin
>   /**
>   * Start sing
>   */
>   fun startSing(songCode: Long, startPos: Long){}
>   
>   /**
>   * Resume sing
>   */
>   fun resumeSing(){}
>   
>   /**
>   * Pause sing
>   */
>   fun pauseSing(){}
>   
>   /**
>   * Seek to a specific position
>   */
>   fun seekSing(time: Long){}
>   ```
>
> * Work together with the lyrics component
>
>   You can pass in your custom lyrics component to work with the KTVApi module. Your lyrics component needs to extend the **ILrcView** class and implement the following three interfaces. The KTVApi module will send the singing pitch, song playback progress, and lyrics URL to your lyrics component through the following three callbacks.
>
>   ```kotlin
>   interface ILrcView {
>       /**
>        * When the KTVApi updates the pitch, it will call this method to pass the pitch value to your lyrics component
>        * @param pitch pitch value
>        */
>       fun onUpdatePitch(pitch: Float?)
>       
>       /**
>        * When the KTVApi updates the music playback progress, it will call this method to pass the progress value to your lyrics component, and the progress value will be updated every 50ms
>        * @param progress song playback real progress, updated every 50ms
>        */
>       fun onUpdateProgress(progress: Long?)
>       
>       /**
>        * When the KTVApi gets the lyrics address, it will actively call this method to pass the lyrics address url to your lyrics component, and you need to download the lyrics in this callback
>        */
>       fun onDownloadLrcData(url: String?)
>   }
>       
>   /**
>    * Set the lyrics component; it can take effect at any time when set.
>    * @param view The lyrics component view that is passed in needs to extend ILrcView and implement the three interfaces of ILrcView.
>    */
>   fun setLrcView(view: ILrcView){}
>   ```
>
>   
>
> #### Business server interaction module
>
> ![xxx](https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/github_readme/ktv/ktv_130_3.png)
>
> The interaction between the scenario and the business server mainly involves room changes, such as creating, joining, or leaving a room.，[**RoomService**](../../RTMSyncManager/src/main/java/io/agora/rtmsyncmanager/RoomService.kt) 
>
>   ```kotlin
>   fun getRoomList(
>         appId: String,
>         sceneId: String,
>         lastCreateTime: Long,
>         pageSize: Int,
>         cleanClosure: ((AUIRoomInfo) -> Boolean)? = null,
>         completion: (AUIException?, Long?, List<AUIRoomInfo>?) -> Unit
>     ){}
>   fun createRoom(appId: String, sceneId: String, room: AUIRoomInfo, completion: (AUIRtmException?, AUIRoomInfo?) 
> -> Unit){}
>   fun enterRoom(appId: String, sceneId: String, roomId: String, completion: (AUIRtmException?) -> Unit){}
>   fun leaveRoom(appId: String, sceneId: String, roomId: String){}
>   ```
>
> #### Other Features
>
> * Sound effects, voice beautification
>
>   Agora’s best voice beautification
>
>   Implementation reference [**MusicSettingDialog#Callback**](src/main/java/io/agora/scene/ktv/live/fragmentdialog/MusicSettingDialog.kt) **onEffectChanged**

---
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
