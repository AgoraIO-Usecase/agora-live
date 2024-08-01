package io.agora.scene.voice.ui.widget.top

import android.view.View

interface OnLiveTopClickListener {

    fun onClickBack(view: View)

    fun onClickMore(view: View)

    fun onClickRank(view: View,pageIndex:Int = 0)

    fun onClickNotice(view: View)

    fun onClickSoundSocial(view: View)
}