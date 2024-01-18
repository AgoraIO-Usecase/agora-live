package io.agora.scene.voice.model.annotation

import androidx.annotation.IntDef

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    MicStatus.Unknown,
    MicStatus.Idle,
    MicStatus.Normal,
    MicStatus.Mute,
    MicStatus.ForceMute,
    MicStatus.Lock,
    MicStatus.LockForceMute,
    MicStatus.BotInactive,
    MicStatus.BotActivated,
)
annotation class MicStatus {
    companion object {
        const val Unknown = -100
        const val Idle = -1
        const val Normal = 0
        const val Mute = 1
        const val ForceMute = 2
        const val Lock = 3
        const val LockForceMute = 4

        const val BotActivated = 5

        const val BotInactive = -2
    }
}
