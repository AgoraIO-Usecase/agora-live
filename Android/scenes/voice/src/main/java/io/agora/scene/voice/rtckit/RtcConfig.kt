package io.agora.scene.voice.rtckit

import io.agora.voice.common.constant.ConfigConstants

data class RtcChannelTemp constructor(
    var broadcaster: Boolean = true,
    var firstActiveBot: Boolean = true,
    var firstSwitchAnis: Boolean = true,
    var AINSMode: Int = ConfigConstants.AINSMode.AINS_Medium,
    var isAIAECOn: Boolean = false,
    var isAIAGCOn: Boolean = false,
) {
    fun reset() {
        broadcaster = true
        firstActiveBot = true
        firstSwitchAnis = true
        AINSMode = ConfigConstants.AINSMode.AINS_Medium
        isAIAECOn = false
        isAIAGCOn = false
    }
}