package io.agora.scene.voice.model.annotation

import androidx.annotation.IntDef


@Retention(AnnotationRetention.SOURCE)
@IntDef(
    MicClickAction.Invite,
    MicClickAction.ForbidMic,
    MicClickAction.UnForbidMic,
    MicClickAction.Mute,
    MicClickAction.UnMute,
    MicClickAction.Lock,
    MicClickAction.UnLock,
    MicClickAction.KickOff,
    MicClickAction.OffStage,
    MicClickAction.Accept,
)
annotation class MicClickAction {
    companion object {

        const val Invite = 0

        const val ForbidMic = 1

        const val UnForbidMic = 2

        const val Mute = 3

        const val UnMute = 4

        const val Lock = 5

        const val UnLock = 6

        const val KickOff = 7

        const val OffStage = 8

        const val Accept = 9
    }
}
