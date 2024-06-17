package io.agora.scene.show.videoLoaderAPI

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.RtcEngineEx

/**
 * On live room item touch event handler
 *
 * @property mRtcEngine
 * @property roomInfo
 * @property localUid
 * @constructor Create empty On live room item touch event handler
 */
abstract class OnLiveRoomItemTouchEventHandler constructor(
    private val mRtcEngine: RtcEngineEx,
    private val roomInfo: VideoLoader.RoomInfo,
    private val localUid: Int
): View.OnTouchListener {
    private val tag = "[VideoLoader]Touch"
    private val videoLoader by lazy { VideoLoader.getImplInstance(mRtcEngine) }
    private val clickInternal = 500L
    private var lastClickTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (System.currentTimeMillis() - lastClickTime <= clickInternal) return true
        val motionEvent = event ?: return true
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                VideoLoader.videoLoaderApiLog(tag, "click down, roomInfo:${roomInfo}")
                roomInfo.anchorList.forEach { anchorInfo->
                    videoLoader.switchAnchorState(AnchorState.JOINED_WITHOUT_AUDIO, anchorInfo, localUid)
                    onRequireRenderVideo(anchorInfo)?.let { canvas ->
                        videoLoader.renderVideo(anchorInfo, localUid, canvas)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                VideoLoader.videoLoaderApiLog(tag, "click cancel, roomInfo:${roomInfo}")
                roomInfo.anchorList.forEach {
                    videoLoader.switchAnchorState(AnchorState.IDLE, it, localUid)
                }
            }
            MotionEvent.ACTION_UP -> {
                VideoLoader.videoLoaderApiLog(tag, "click up, roomInfo:${roomInfo}")
                lastClickTime = System.currentTimeMillis()
                roomInfo.anchorList.forEach { anchorInfo ->
                    videoLoader.switchAnchorState(AnchorState.JOINED, anchorInfo, localUid)
                    mRtcEngine.startMediaRenderingTracingEx(RtcConnection(anchorInfo.channelId, localUid))
                    (videoLoader as VideoLoaderImpl).getProfiler(anchorInfo.channelId).perceivedStartTime = System.currentTimeMillis()
                }
            }
        }
        return true
    }

    /**
     * On require render video
     *
     * @param info
     * @return
     */
    abstract fun onRequireRenderVideo(info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer?
}
