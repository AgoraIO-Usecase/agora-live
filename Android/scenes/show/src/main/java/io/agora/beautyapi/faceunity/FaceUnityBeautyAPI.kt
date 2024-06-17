/*
 * MIT License
 *
 * Copyright (c) 2023 Agora Community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.agora.beautyapi.faceunity

import android.content.Context
import android.view.View
import com.faceunity.core.faceunity.FURenderKit
import io.agora.base.VideoFrame
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine

/**
 * Version
 */
const val VERSION = "1.0.5"

/**
 * Capture mode
 *
 * @constructor Create empty Capture mode
 */
enum class CaptureMode{
    /**
     * Agora
     *
     * @constructor Create empty Agora
     */
    Agora,

    /**
     * Custom
     *
     * @constructor Create empty Custom
     */
    Custom
}

/**
 * I event callback
 *
 * @constructor Create empty I event callback
 */
interface IEventCallback{

    /**
     * On beauty stats
     *
     * @param stats
     */
    fun onBeautyStats(stats: BeautyStats)
}

/**
 * Beauty stats
 *
 * @property minCostMs
 * @property maxCostMs
 * @property averageCostMs
 * @constructor Create empty Beauty stats
 */
data class BeautyStats(
    val minCostMs:Long,
    val maxCostMs: Long,
    val averageCostMs: Long
)

/**
 * Mirror mode
 *
 * @constructor Create empty Mirror mode
 */
enum class MirrorMode {


    /**
     * Mirror Local Remote
     *
     * @constructor Create empty Mirror Local Remote
     */
    MIRROR_LOCAL_REMOTE,

    /**
     * Mirror Local Only
     *
     * @constructor Create empty Mirror Local Only
     */
    MIRROR_LOCAL_ONLY,
    /**
     * Mirror Remote Only
     *
     * @constructor Create empty Mirror Remote Only
     */
    MIRROR_REMOTE_ONLY,

    /**
     * Mirror None
     *
     * @constructor Create empty Mirror None
     */
    MIRROR_NONE
}

/**
 * Camera config
 *
 * @property frontMirror
 * @property backMirror
 * @constructor Create empty Camera config
 */
data class CameraConfig(
    val frontMirror: MirrorMode = MirrorMode.MIRROR_LOCAL_REMOTE,
    val backMirror: MirrorMode = MirrorMode.MIRROR_NONE
)

/**
 * Config
 *
 * @property context
 * @property rtcEngine
 * @property fuRenderKit
 * @property eventCallback
 * @property captureMode
 * @property statsDuration
 * @property statsEnable
 * @property cameraConfig
 * @constructor Create empty Config
 */
data class Config(
    val context: Context,
    val rtcEngine: RtcEngine,
    val fuRenderKit: FURenderKit,
    val eventCallback: IEventCallback? = null,
    val captureMode: CaptureMode = CaptureMode.Agora,
    val statsDuration: Long = 1000,
    val statsEnable: Boolean = false,
    val cameraConfig: CameraConfig = CameraConfig()
)

/**
 * Error code
 *
 * @property value
 * @constructor Create empty Error code
 */
enum class ErrorCode(val value: Int) {
    /**
     * Error Ok
     *
     * @constructor Create empty Error Ok
     */
    ERROR_OK(0),
    /**
     * Error Has Not Initialized
     *
     * @constructor Create empty Error Has Not Initialized
     */
    ERROR_HAS_NOT_INITIALIZED(101),

    /**
     * Error Has Initialized
     *
     * @constructor Create empty Error Has Initialized
     */
    ERROR_HAS_INITIALIZED(102),
    /**
     * Error Has Released
     *
     * @constructor Create empty Error Has Released
     */
    ERROR_HAS_RELEASED(103),

    /**
     * Error Process Not Custom
     *
     * @constructor Create empty Error Process Not Custom
     */
    ERROR_PROCESS_NOT_CUSTOM(104),

    /**
     * Error Process Disable
     *
     * @constructor Create empty Error Process Disable
     */
    ERROR_PROCESS_DISABLE(105),

    /**
     * Error View Type Error
     *
     * @constructor Create empty Error View Type Error
     */
    ERROR_VIEW_TYPE_ERROR(106),

    /**
     * Error Frame Skipped
     *
     * @constructor Create empty Error Frame Skipped
     */
    ERROR_FRAME_SKIPPED(107),
}

/**
 * Beauty preset
 *
 * @constructor Create empty Beauty preset
 */
enum class BeautyPreset {
    /**
     * Custom
     *
     * @constructor Create empty Custom
     */
    CUSTOM,

    /**
     * Default
     *
     * @constructor Create empty Default
     */
    DEFAULT
}

/**
 * Create face unity beauty a p i
 *
 * @return
 */
fun createFaceUnityBeautyAPI(): FaceUnityBeautyAPI = FaceUnityBeautyAPIImpl()

/**
 * Face unity beauty a p i
 *
 * @constructor Create empty Face unity beauty a p i
 */
interface FaceUnityBeautyAPI {

    /**
     * Initialize
     *
     * @param config
     * @return
     */
    fun initialize(config: Config): Int

    /**
     * Enable
     *
     * @param enable
     * @return
     */
    fun enable(enable: Boolean): Int

    /**
     * Setup local video
     *
     * @param view
     * @param renderMode
     * @return
     */
    fun setupLocalVideo(view: View, renderMode: Int = Constants.RENDER_MODE_HIDDEN): Int

    /**
     * On frame
     *
     * @param videoFrame
     * @return
     */
    fun onFrame(videoFrame: VideoFrame): Int

    /**
     * Set beauty preset
     *
     * @param preset
     * @return
     */
    fun setBeautyPreset(preset: BeautyPreset = BeautyPreset.DEFAULT): Int

    /**
     * Update camera config
     *
     * @param config
     * @return
     */
    fun updateCameraConfig(config: CameraConfig): Int

    /**
     * Is front camera
     *
     * @return
     */
    fun isFrontCamera(): Boolean

    /**
     * Get mirror applied
     *
     * @return
     */
    fun getMirrorApplied(): Boolean

    /**
     * Set parameters
     *
     * @param key
     * @param value
     */
    fun setParameters(key: String, value: String)

    /**
     * Run on main process
     *
     * @param run
     */
    fun runOnProcessThread(run: ()->Unit)

    /**
     * Release
     *
     * @return
     */
    fun release(): Int

}