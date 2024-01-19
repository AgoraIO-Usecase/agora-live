package io.agora.scene.voice.rtckit.listener


abstract class RtcMicVolumeListener {

    abstract fun onBotVolume(speaker: Int, finished: Boolean)

    abstract fun onUserVolume(rtcUid: Int, volume: Int)
}