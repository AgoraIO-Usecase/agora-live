package io.agora.scene.eCommerce.videoLoaderAPI

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.rtc2.RtcEngineEx

/**
 * On room list scroll event handler
 *
 * @property mRtcEngine
 * @property localUid
 * @constructor Create empty On room list scroll event handler
 */
abstract class OnRoomListScrollEventHandler constructor(
    private val mRtcEngine: RtcEngineEx,
    private val localUid: Int
): RecyclerView.OnScrollListener() {
    private val tag = "[VideoLoader]Scroll0"
    private val roomList = ArrayList<VideoLoader.RoomInfo>()

    /**
     * Update room list
     *
     * @param list
     */
    fun updateRoomList(list: ArrayList<VideoLoader.RoomInfo>) {
        roomList.addAll(list)
        preloadChannels()
    }

    // RecyclerView.OnScrollListener
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
            Log.d("RoomListActivity", "firstVisible $firstVisibleItem, lastVisible $lastVisibleItem")
            val firstPreloadPosition = if (firstVisibleItem - 7 < 0) 0 else firstVisibleItem - 7
            val lastPreloadPosition = if (firstPreloadPosition + 19 >= roomList.size)
                roomList.size - 1 else firstPreloadPosition + 19
            preloadChannels(firstPreloadPosition, lastPreloadPosition)
        }
    }

    // ------------------------ inner ------------------------
    private fun preloadChannels() {
        if (roomList.isNotEmpty()) {
            roomList.take(20).forEach { room ->
                val info = room.anchorList[0]
                val ret = mRtcEngine.preloadChannel(info.token, info.channelId, localUid)
                Log.d(tag, "call rtc sdk preloadChannel ${info.channelId} ret:$ret")
            }
        }
    }

    private fun preloadChannels(from: Int, to: Int) {
        if (roomList.isNotEmpty()) {
            val size = roomList.size
            for (i in from until to + 1) {
                if (i >= size) return
                val info = roomList[i].anchorList[0]
                val ret = mRtcEngine.preloadChannel(info.token, info.channelId, localUid)
                Log.d(tag, "call rtc sdk preloadChannel ${info.channelId} ret:$ret")
            }
        }
    }
}