package com.agora.entfulldemo.home.constructor

import androidx.annotation.DrawableRes

/**
 * Scenes model
 *
 * @property scene
 * @property clazzName
 * @property name
 * @property background
 * @property icon
 * @property active
 * @property tip
 * @constructor Create empty Scenes model
 */
data class ScenesModel constructor(
    val scene: AgoraScenes,
    val clazzName: String,
    val name: String,
    @DrawableRes val background: Int,
    @DrawableRes val icon: Int,
    val active: Boolean = false,
    val tip: String = "",
)
