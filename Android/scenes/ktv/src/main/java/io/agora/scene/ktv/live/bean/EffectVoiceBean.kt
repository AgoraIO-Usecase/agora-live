package io.agora.scene.ktv.live.bean

import androidx.annotation.DrawableRes

/**
 * Effect voice bean
 *
 * @property id
 * @property audioEffect
 * @property resId
 * @property title
 * @property isSelect
 * @constructor Create empty Effect voice bean
 */
data class EffectVoiceBean constructor(
    var id: Int,
    var audioEffect: Int,
    @field:DrawableRes var resId: Int,
    var title: String,
    var isSelect: Boolean = false)
{
}
