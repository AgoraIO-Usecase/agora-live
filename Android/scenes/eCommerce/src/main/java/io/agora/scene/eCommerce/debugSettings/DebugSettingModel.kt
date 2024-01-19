package io.agora.scene.eCommerce.debugSettings

/**
 * Debug setting model
 *
 * @property pvcEnabled
 * @property autoFocusFaceModeEnabled
 * @property exposurePositionX
 * @property exposurePositionY
 * @property cameraSelect
 * @property videoFullrangeExt
 * @property matrixCoefficientsExt
 * @property enableHWEncoder
 * @property codecType
 * @property mirrorMode
 * @property renderMode
 * @property colorEnhance
 * @property dark
 * @property noise
 * @property srEnabled
 * @property srType
 * @constructor Create empty Debug setting model
 */
data class DebugSettingModel(
    var pvcEnabled: Boolean = false,
    var autoFocusFaceModeEnabled: Boolean = true,
    var exposurePositionX: Float? = null,
    var exposurePositionY: Float? = null,
    var cameraSelect: Int? = null,
    var videoFullrangeExt: Int? = null,
    var matrixCoefficientsExt: Int? = null,
    var enableHWEncoder: Boolean = true,
    var codecType: Int = 3,     // 2 -> h264, 3 -> h265
    var mirrorMode: Boolean = false,
    var renderMode: Int = 1,       // 0 -> hidden, 1 -> fix
    var colorEnhance: Boolean = false,
    var dark: Boolean = false,
    var noise: Boolean = false,
    var srEnabled: Boolean = false,
    var srType: Double = 1.0         // 1 -> 6, 1.33 -> 7, 1.5 -> 8, 2 -> 3
)