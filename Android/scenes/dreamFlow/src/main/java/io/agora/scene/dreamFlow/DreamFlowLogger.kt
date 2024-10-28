package io.agora.scene.dreamFlow

import io.agora.scene.base.AgoraScenes
import io.agora.scene.base.EntLogger

/**
 * Show logger
 *
 * @constructor Create empty Show logger
 */
object DreamFlowLogger {

    private val entLogger = EntLogger(EntLogger.Config(AgoraScenes.DreamFlow.name))

    /**
     * D
     *
     * @param tag
     * @param message
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        entLogger.d(tag, message)
    }

    /**
     * E
     *
     * @param tag
     * @param throwable
     * @param message
     */
    @JvmStatic
    fun e(tag: String, throwable: Throwable? = null, message: String = "") {
        if (throwable != null) {
            entLogger.e(tag, throwable, message)
        } else {
            entLogger.e(tag, message)
        }
    }

}