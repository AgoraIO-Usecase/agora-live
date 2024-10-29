package io.agora.scene.eCommerce.videoLoaderAPI

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import androidx.viewpager2.widget.ViewPager2
import io.agora.rtc2.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * A g slicing type
 *
 * @property value
 * @constructor Create empty A g slicing type
 */
enum class AGSlicingType(val value :Int) {
    /**
     * Visible
     *
     * @constructor Create empty Visible
     */
    VISIBLE(0),

    /**
     * End Drag
     *
     * @constructor Create empty End Drag
     */
    END_DRAG(1),

    /**
     * End Scroll
     *
     * @constructor Create empty End Scroll
     */
    END_SCROLL(2),

    /**
     * Never
     *
     * @constructor Create empty Never
     */
    NEVER(3)
}

/**
 * On page scroll event handler
 *
 * @property mRtcEngine
 * @property localUid
 * @property needPreJoin
 * @property videoScrollMode
 * @constructor Create empty On page scroll event handler
 */
abstract class OnPageScrollEventHandler constructor(
    private val mRtcEngine: RtcEngineEx,
    private val localUid: Int,
    private val needPreJoin: Boolean,
    private val videoScrollMode: AGSlicingType
) : ViewPager2.OnPageChangeCallback() {
    private val tag = "[VideoLoader]Scroll"
    private val videoLoader by lazy { VideoLoader.getImplInstance(mRtcEngine) }
    private val roomList = SparseArray<VideoLoader.RoomInfo>()

    /**
     * Clean cache
     *
     */
    fun cleanCache() {
        mainHandler.removeCallbacksAndMessages(null)
        roomsForPreloading.clear()
        roomsJoined.clear()
    }

    // ViewPager2.OnPageChangeCallback()
    private val POSITION_NONE = -1
    private var currLoadPosition = POSITION_NONE
    private val PRE_LOAD_OFFSET = 0.3f
    private var preLoadPosition = POSITION_NONE
    private var lastOffset = 0f
    private var scrollStatus: Int = ViewPager2.SCROLL_STATE_IDLE

    private val roomsForPreloading = Collections.synchronizedList(mutableListOf<VideoLoader.RoomInfo>())
    private val roomsJoined = Collections.synchronizedList(mutableListOf<VideoLoader.RoomInfo>())

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * On room created
     *
     * @param position
     * @param info
     * @param isCurrentItem
     */// call in createFragment
    fun onRoomCreated(position: Int, info: VideoLoader.RoomInfo, isCurrentItem: Boolean) {
        VideoLoader.videoLoaderApiLog(tag, "onRoomCreated position:$position, info:$info, isCurrentItem:$isCurrentItem")
        roomList.put(position, info)
        if (isCurrentItem) {
            info.anchorList.forEach { anchorInfo ->
                videoLoader.switchAnchorState(AnchorState.JOINED, anchorInfo, localUid)
                onRequireRenderVideo(position, anchorInfo)?.let {
                    videoLoader.renderVideo(
                        anchorInfo,
                        localUid,
                        it
                    )
                }
                mRtcEngine.startMediaRenderingTracingEx(RtcConnection(anchorInfo.channelId, localUid))
                (videoLoader as VideoLoaderImpl).getProfiler(anchorInfo.channelId).perceivedStartTime = System.currentTimeMillis()
                (videoLoader as VideoLoaderImpl).getProfiler(anchorInfo.channelId).reportExt = mutableMapOf("videoScrollMode" to videoScrollMode, "needPreJoin" to needPreJoin)
            }


            mainHandler.postDelayed({
                roomsJoined.add(info)
                preJoinRooms()
            }, 200)
            onPageStartLoading(position)
            onPageLoaded(position)
        }
    }

    /**
     * Update room list
     *
     * @param list
     */
    fun updateRoomList(list: ArrayList<VideoLoader.RoomInfo>) {
        roomsForPreloading.addAll(list)
    }

    /**
     * Update room info
     *
     * @param position
     * @param info
     */
    fun updateRoomInfo(position: Int, info: VideoLoader.RoomInfo) {
        if (info.roomId != roomList[position].roomId) return
        val oldAnchorList = roomList[position].anchorList
        val newAnchorList = info.anchorList
        newAnchorList.forEach { newInfo ->
            videoLoader.switchAnchorState(AnchorState.JOINED, newInfo, localUid)
            onRequireRenderVideo(position, newInfo)?.let { canvas ->
                videoLoader.renderVideo(
                    newInfo,
                    localUid,
                    canvas
                )
            }
        }

        oldAnchorList.forEach { oldInfo ->
            if (newAnchorList.none { new -> new.channelId == oldInfo.channelId }) {
                videoLoader.switchAnchorState(AnchorState.IDLE, oldInfo, localUid)
            }
        }

        val roomInfo = roomsForPreloading.filter { it.roomId == info.roomId }.getOrNull(0) ?: return
        val index = roomsForPreloading.indexOf(roomInfo)
        roomsForPreloading[index] = info
        roomList[position] = info
    }

    /**
     * Get current room position
     *
     * @return
     */
    fun getCurrentRoomPosition(): Int {
        return currLoadPosition
    }

    // ViewPager2
    override fun onPageScrollStateChanged(state: Int) {
        super.onPageScrollStateChanged(state)
        //Log.d(tag, "PageChange onPageScrollStateChanged state=$state")
        when (state) {
            ViewPager2.SCROLL_STATE_IDLE -> {
                if(preLoadPosition != POSITION_NONE){
                    leaveRoom(roomList[preLoadPosition] ?: return)
                    onPageLeft(preLoadPosition)
                }
                joinRoomAndStartAudio(roomList[currLoadPosition] ?: return)
                roomsJoined.add(roomList[currLoadPosition] ?: return)
                preJoinRooms()
                pageLoaded(currLoadPosition, roomList[currLoadPosition])
                preLoadPosition = POSITION_NONE
                lastOffset = 0f
            }
            ViewPager2.SCROLL_STATE_SETTLING -> {
            }
            ViewPager2.SCROLL_STATE_DRAGGING -> {

            }
        }
        scrollStatus = state
    }

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        super.onPageScrolled(position, positionOffset, positionOffsetPixels)
        //Log.d(tag, "PageChange onPageScrolled positionOffset=$positionOffset positionOffsetPixels=$positionOffsetPixels scrollStatus=$scrollStatus")
        if (scrollStatus == ViewPager2.SCROLL_STATE_DRAGGING) {
            if (lastOffset >= 0f) {
                val isMoveUp = (positionOffset - lastOffset) > 0
                //Log.d("hugo", "lastOffset = $lastOffset, positionOffset = $positionOffset")
                if (((lastOffset < PRE_LOAD_OFFSET && lastOffset >= 0) && (1 - positionOffset) < PRE_LOAD_OFFSET) || (lastOffset == 0f && !isMoveUp && positionOffset > 0.5)) {
                    Log.d(tag, "page up")
                    preLoadPosition = currLoadPosition - 1
                    joinRoomWithoutAudio(preLoadPosition, roomList[preLoadPosition] ?: return, localUid)
                    onPageStartLoading(preLoadPosition)
                } else if ((positionOffset < PRE_LOAD_OFFSET && ((1 - lastOffset) < PRE_LOAD_OFFSET && (1 - lastOffset) >= 0)) || (lastOffset == 0f && isMoveUp && positionOffset > 0)) {
                    Log.d(tag, "page down")
                    preLoadPosition = currLoadPosition + 1
                    joinRoomWithoutAudio(preLoadPosition, roomList[preLoadPosition] ?: return, localUid)
                    onPageStartLoading(preLoadPosition)
                }
            }
            lastOffset = positionOffset
            Log.d(tag, "PageChange onPageScrolled preLoadPosition=$preLoadPosition")
        }
    }

    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        //Log.d(tag, "PageChange onPageSelected position=$position, currLoadPosition=$currLoadPosition, preLoadPosition=$preLoadPosition")

        if (currLoadPosition != POSITION_NONE) {
            if (preLoadPosition != POSITION_NONE) {
                if (position == preLoadPosition) {
                    leaveRoom(roomList[currLoadPosition] ?: return)
                    onPageLeft(currLoadPosition)
                } else {
                    leaveRoom(roomList[preLoadPosition] ?: return)
                    onPageLeft(preLoadPosition)

                    roomsJoined.add(roomList[currLoadPosition] ?: return)
                    preJoinRooms()
                    joinRoomAndStartAudio(roomList[currLoadPosition] ?: return)
                    pageLoaded(currLoadPosition, roomList[currLoadPosition])
                }
            }

//            if (currLoadPosition != position) {
//                leaveRoom(roomList[currLoadPosition] ?: return)
//                onPageLeft(currLoadPosition)
//
//                joinRoomWithoutAudio(position, roomList[position] ?: return, localUid)
//                onPageStartLoading(position)
//            }
        }
        currLoadPosition = position
        preLoadPosition = POSITION_NONE
        lastOffset = 0f
    }

    /**
     * On page start loading
     *
     * @param position
     */// OnPageStateEventHandler
    abstract fun onPageStartLoading(position: Int)

    /**
     * On page loaded
     *
     * @param position
     */
    abstract fun onPageLoaded(position: Int)

    /**
     * On page left
     *
     * @param position
     */
    abstract fun onPageLeft(position: Int)

    /**
     * On require render video
     *
     * @param position
     * @param info
     * @return
     */
    abstract fun onRequireRenderVideo(position: Int, info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer?

    // ------------------------ inner ---------------------------
    private fun joinRoomWithoutAudio(position: Int, roomInfo: VideoLoader.RoomInfo, uid: Int) {
        VideoLoader.videoLoaderApiLog(tag, "joinChannel roomInfo=$roomInfo")

        roomInfo.anchorList.forEach { anchorInfo ->
            videoLoader.switchAnchorState(AnchorState.JOINED_WITHOUT_AUDIO, anchorInfo, uid)
            if (videoScrollMode == AGSlicingType.VISIBLE) {
                onRequireRenderVideo(position, anchorInfo)?.let {
                    videoLoader.renderVideo(
                        anchorInfo,
                        localUid,
                        it
                    )
                }
            }

            mRtcEngine.startMediaRenderingTracingEx(RtcConnection(anchorInfo.channelId, localUid))
            (videoLoader as VideoLoaderImpl).getProfiler(anchorInfo.channelId).perceivedStartTime = System.currentTimeMillis()
            (videoLoader as VideoLoaderImpl).getProfiler(anchorInfo.channelId).reportExt = mutableMapOf("videoScrollMode" to videoScrollMode.value, "needPreJoin" to needPreJoin)
        }
    }

    private fun joinRoomAndStartAudio(roomInfo: VideoLoader.RoomInfo) {
        roomInfo.anchorList.forEach {
            videoLoader.switchAnchorState(AnchorState.JOINED, it, localUid)
        }
    }

    private fun leaveRoom(roomInfo: VideoLoader.RoomInfo) {
        VideoLoader.videoLoaderApiLog(tag, "switchRoomState, hideChannel: $roomInfo")
        roomsJoined.removeIf { it.roomId == roomInfo.roomId }
        val currentRoom = roomsJoined.firstOrNull() ?: return
        roomInfo.anchorList.forEach {
            if (needPreJoin && currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                videoLoader.switchAnchorState(AnchorState.PRE_JOINED, it, localUid)
            }
        }
    }

    private fun preJoinRooms() {
        val size = roomsForPreloading.size
        val currentRoom = roomsJoined.firstOrNull() ?: return
        val index =
            roomsForPreloading.indexOfFirst { it.roomId == currentRoom.roomId }
        VideoLoader.videoLoaderApiLog(tag, "switchRoomState, index: $index, connectionsJoined:$roomsJoined")
        VideoLoader.videoLoaderApiLog(tag, "switchRoomState, roomsForPreloading: $roomsForPreloading")

        val roomPreLoaded = mutableListOf<VideoLoader.RoomInfo>()
        val anchorPreJoined = mutableListOf<VideoLoader.AnchorInfo>()
        for (i in (index - 1)..(index + 3 / 2)) {
            if (i == index) {
                continue
            }
            // workaround
            if (size == 0) {
                return
            }
            val realIndex = (if (i < 0) size + i else i) % size
            if (realIndex < 0 || realIndex >= size) {
                continue
            }
            val roomInfo = roomsForPreloading[realIndex]
            if (roomsJoined.any { it.roomId == roomInfo.roomId }) {
                continue
            }
            if (videoLoader.getAnchorState(roomInfo.roomId, localUid) != AnchorState.PRE_JOINED) {
                VideoLoader.videoLoaderApiLog(tag, "getAnchorState $roomsForPreloading")
                videoLoader.preloadAnchor(roomInfo.anchorList, localUid)
                roomInfo.anchorList.forEach {
                    if (needPreJoin && currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                        videoLoader.switchAnchorState(AnchorState.PRE_JOINED, it, localUid)
                        anchorPreJoined.add(it)
                    }
                }
            }
            roomPreLoaded.add(roomInfo)
        }

        VideoLoader.videoLoaderApiLog(tag, "switchRoomState, connPreLoaded: $roomPreLoaded")
        // Non-preJoin rooms need to exit the channel.
        roomsForPreloading.forEach { room ->
            if (needPreJoin && videoLoader.getAnchorState(room.roomId, localUid) == AnchorState.PRE_JOINED && roomPreLoaded.none {room.roomId == it.roomId}) {
                VideoLoader.videoLoaderApiLog(tag, "switchRoomState, remove: $room")
                room.anchorList.forEach {
                    if (currentRoom.anchorList.none { joined -> joined.channelId == it.channelId } && anchorPreJoined.none { preJoined -> preJoined.channelId == it.channelId }) {
                        videoLoader.switchAnchorState(AnchorState.IDLE, it, localUid)
                    }
                }
            } else if (!needPreJoin && videoLoader.getAnchorState(room.roomId, localUid) != AnchorState.IDLE && roomsJoined.none {room.roomId == it.roomId}) {
                VideoLoader.videoLoaderApiLog(tag, "switchRoomState, remove: $room")
                room.anchorList.forEach {
                    if (currentRoom.anchorList.none { joined -> joined.channelId == it.channelId }) {
                        videoLoader.switchAnchorState(AnchorState.IDLE, it, localUid)
                    }
                }
            }
        }
    }

    private fun pageLoaded(position: Int, roomInfo: VideoLoader.RoomInfo) {
        onPageLoaded(position)
        if (videoScrollMode == AGSlicingType.END_SCROLL) {
            roomInfo.anchorList.forEach { anchorInfo ->
                onRequireRenderVideo(position, anchorInfo)?.let {
                    videoLoader.renderVideo(
                        anchorInfo,
                        localUid,
                        it
                    )
                }
            }
        }
    }
}