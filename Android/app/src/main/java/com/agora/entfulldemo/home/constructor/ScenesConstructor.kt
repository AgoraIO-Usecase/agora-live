package com.agora.entfulldemo.home.constructor

import android.content.Context
import com.agora.entfulldemo.R
import io.agora.scene.base.AgoraScenes

/**
 * @author create by zhangwei03
 */
object ScenesConstructor {


    /**
     * Build data
     *
     * @param context
     * @return
     */
    @JvmStatic
    fun buildData(context: Context): List<ScenesModel> {
        return mutableListOf(
//            ScenesModel(
//                AgoraScenes.LiveShow,
//                "io.agora.scene.show.RoomListActivity",
//                context.getString(R.string.app_show_live),
//                R.mipmap.bg_btn_home2,
//                R.mipmap.bg_btn_home_live,
//                true
//            ),
//            ScenesModel(
//                AgoraScenes.ECommerce,
//                "io.agora.scene.eCommerce.RoomListActivity",
//                context.getString(R.string.app_e_commerce),
//                R.mipmap.bg_btn_home_e_commerce,
//                0,
//                true
//            ),
//            ScenesModel(
//                AgoraScenes.ChatRoom,
//                "io.agora.scene.voice.ui.activity.VoiceRoomListActivity",
//                context.getString(R.string.app_chat_room),
//                R.mipmap.bg_btn_home3,
//                R.mipmap.bg_btn_home_chat,
//                true
//            ),
//            ScenesModel(
//                AgoraScenes.KTV,
//                "io.agora.scene.ktv.create.RoomListActivity",
//                context.getString(R.string.app_ktv),
//                R.mipmap.bg_btn_home1,
//                R.mipmap.bg_btn_home_ktv,
//                true
//            ),
            ScenesModel(
                AgoraScenes.DreamFlow,
                "io.agora.scene.dreamFlow.RoomListActivity",
                "Stylize",
                R.mipmap.bg_btn_home3,
                R.mipmap.bg_btn_home_dream_flow,
                true
            )
        )
    }
}