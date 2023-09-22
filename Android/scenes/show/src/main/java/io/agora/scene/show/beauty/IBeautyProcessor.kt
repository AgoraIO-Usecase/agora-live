package io.agora.scene.show.beauty

import io.agora.beautyapi.faceunity.FaceUnityBeautyAPI
import io.agora.rtc2.RtcEngine
import io.agora.scene.show.RtcEngineInstance
import java.util.concurrent.Executors

/**
 * I beauty processor
 *
 * @constructor Create empty I beauty processor
 */
abstract class IBeautyProcessor {
    /**
     * Worker executor
     */
    private val workerExecutor = Executors.newSingleThreadExecutor()

    /**
     * Is beauty enable
     */
    @Volatile
    private var isBeautyEnable = true

    /**
     * Initialize
     *
     * @param rtcEngine
     */
    abstract fun initialize(
        rtcEngine: RtcEngine
    )

    /**
     * Set face beautify after cached
     *
     * @param itemId
     * @param intensity
     */
    protected abstract fun setFaceBeautifyAfterCached(itemId: Int, intensity: Float)

    /**
     * Set effect after cached
     *
     * @param itemId
     * @param intensity
     */
    protected abstract fun setEffectAfterCached(itemId: Int, intensity: Float)

    /**
     * Set sticker after cached
     *
     * @param itemId
     */
    protected abstract fun setStickerAfterCached(itemId: Int)

    /**
     * Set a r mark after cached
     *
     * @param itemId
     */
    protected abstract fun setARMarkAfterCached(itemId: Int)

    /**
     * Get sense time beauty a p i
     *
     * @return
     */
    abstract fun getSenseTimeBeautyAPI(): FaceUnityBeautyAPI

    /**
     * Restore
     *
     */
    protected fun restore() {
        BeautyCache.restoreByOperation(this)
    }

    /**
     * Release
     *
     */// Publish Functions
    open fun release() {
        if (workerExecutor.isShutdown) {
            workerExecutor.shutdownNow()
        }
    }

    /**
     * Reset
     *
     */
    fun reset() {
        BeautyCache.reset()
        BeautyCache.restoreByOperation(this)
        RtcEngineInstance.rtcEngine.enableVirtualBackground(false, null, null)
    }

    /**
     * Set beauty enable
     *
     * @param enable
     */
    open fun setBeautyEnable(enable: Boolean) {
        isBeautyEnable = enable
    }

    /**
     * Is beauty enable
     *
     */
    fun isBeautyEnable() = isBeautyEnable

    /**
     * Set bg
     *
     * @param intensity
     */
    fun setBg(intensity: Float) {
        BeautyCache.cacheItemValue(
            GROUP_ID_VIRTUAL_BG,
            ITEM_ID_VIRTUAL_BG_GREEN_SCREENSTRENGTH,
            intensity
        )
    }

    /**
     * Get green screen strength
     *
     * @return
     */
    fun getGreenScreenStrength(): Float {
        return BeautyCache.getItemValue(ITEM_ID_VIRTUAL_BG_GREEN_SCREENSTRENGTH)
    }

    /**
     * Set green screen
     *
     * @param greenScreen
     */
    fun setGreenScreen(greenScreen: Boolean) {
        BeautyCache.cacheItemValue(
            GROUP_ID_VIRTUAL_BG,
            ITEM_ID_VIRTUAL_BG_GREEN_SCREEN,
            if (greenScreen) 1f else 0f
        )
    }

    /**
     * Green screen
     *
     * @return
     */
    fun greenScreen(): Boolean {
        return BeautyCache.getItemValue(ITEM_ID_VIRTUAL_BG_GREEN_SCREEN) == 1f
    }

    /**
     * Set face beautify
     *
     * @param itemId
     * @param intensity
     */
    fun setFaceBeautify(itemId: Int, intensity: Float) {
        BeautyCache.cacheItemValue(GROUP_ID_BEAUTY, itemId, intensity)
        BeautyCache.cacheOperation(GROUP_ID_BEAUTY, itemId)
        workerExecutor.execute {
            setFaceBeautifyAfterCached(itemId, intensity)
        }
    }


    /**
     * Set effect
     *
     * @param itemId
     * @param intensity
     */
    fun setEffect(itemId: Int, intensity: Float) {
        BeautyCache.cacheItemValue(GROUP_ID_EFFECT, itemId, intensity)
        BeautyCache.cacheOperation(GROUP_ID_EFFECT, itemId)
        workerExecutor.execute {
            setEffectAfterCached(itemId, intensity)
        }
    }

    /**
     * Set sticker
     *
     * @param itemId
     */
    fun setSticker(itemId: Int) {
        BeautyCache.resetGroupOperation(GROUP_ID_AR)
        BeautyCache.cacheOperation(GROUP_ID_STICKER, itemId)
        workerExecutor.execute {
            setStickerAfterCached(itemId)
        }
    }

    /**
     * Set a r mark
     *
     * @param itemId
     */
    fun setARMark(itemId: Int) {
        BeautyCache.resetGroupOperation(GROUP_ID_STICKER)
        BeautyCache.cacheOperation(GROUP_ID_AR, itemId)
        workerExecutor.execute {
            setARMarkAfterCached(itemId)
        }
    }

    /**
     * Set adjust
     *
     * @param itemId
     * @param intensity
     */
    fun setAdjust(itemId: Int, intensity: Float) {
        BeautyCache.cacheItemValue(GROUP_ID_ADJUST, itemId, intensity)
        BeautyCache.cacheOperation(GROUP_ID_ADJUST, itemId)
        workerExecutor.execute {
            setFaceBeautifyAfterCached(itemId, intensity)
        }
    }
}