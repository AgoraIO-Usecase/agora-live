package io.agora.scene.voice.ui.widget.mic

import io.agora.scene.voice.model.VoiceMicInfoModel

/**
 * @author create by zhangwei03
 */
interface IRoomMicView {

    fun onInitMic(micInfoList: List<VoiceMicInfoModel>, isBotActive: Boolean)

    fun activeBot(active: Boolean)

    fun updateVolume(index: Int, volume: Int)

    fun updateBotVolume(speakerType: Int, volume: Int)

    fun onSeatUpdated(newMicMap: Map<Int, VoiceMicInfoModel>)

    fun findMicByUid(uid: String): Int

    fun myRtcUid(): Int
}