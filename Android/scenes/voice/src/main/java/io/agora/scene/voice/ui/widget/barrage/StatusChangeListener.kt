package io.agora.scene.voice.ui.widget.barrage

import android.widget.TextView

interface StatusChangeListener {

    fun onShortSubtitleShow(textView: TextView)


    fun onLongSubtitleRollEnd(textView: TextView)
}