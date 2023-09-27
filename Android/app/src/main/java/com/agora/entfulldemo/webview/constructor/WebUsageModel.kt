package com.agora.entfulldemo.webview.constructor

import com.google.gson.annotations.SerializedName

/**
 * Web usage
 *
 * @property usage
 * @constructor Create empty Web usage
 */
data class WebUsage constructor(
    val usage:WebUsageModel,
)

/**
 * Web usage model
 *
 * @property user
 * @property deviceInfo
 * @constructor Create empty Web usage model
 */
data class WebUsageModel constructor(
    private val user: UserModel,
    @SerializedName("device_info")
    private val deviceInfo: DeviceInfo,
)

/**
 * User model
 *
 * @property avatar
 * @property name
 * @property phone
 * @constructor Create empty User model
 */
data class UserModel constructor(
    private val avatar: String,
    private val name: String,
    private val phone: String,
)

/**
 * Device info
 *
 * @property type
 * @property content
 * @property number
 * @constructor Create empty Device info
 */
data class DeviceInfo constructor(
    private val type: String,
    private val content: String,
    private val number: Int,
)
