package io.agora.scene.widget.utils

import android.widget.SeekBar


/**
 * Do on progress changed
 *
 * @param action
 * @receiver
 * @return
 */
public inline fun SeekBar.doOnProgressChanged(
    crossinline action: (seekBar: SeekBar?, progress: Int, fromUser: Boolean) -> Unit
): SeekBar.OnSeekBarChangeListener = setOnSeekBarChangeListener(onProgressChanged = action)

/**
 * Do on start tracking touch
 *
 * @param action
 * @receiver
 * @return
 */
public inline fun SeekBar.doOnStartTrackingTouch(
    crossinline action: (seekBar: SeekBar?) -> Unit
): SeekBar.OnSeekBarChangeListener = setOnSeekBarChangeListener(onStartTrackingTouch = action)

/**
 * Do on stop tracking touch
 *
 * @param action
 * @receiver
 * @return
 */
public inline fun SeekBar.doOnStopTrackingTouch(
    crossinline action: (seekBar: SeekBar?) -> Unit
): SeekBar.OnSeekBarChangeListener = setOnSeekBarChangeListener(onStopTrackingTouch = action)

/**
 * Set on seek bar change listener
 *
 * @param onProgressChanged
 * @param onStartTrackingTouch
 * @param onStopTrackingTouch
 * @receiver
 * @receiver
 * @receiver
 * @return
 */
public inline fun SeekBar.setOnSeekBarChangeListener(
    crossinline onProgressChanged: (seekBar: SeekBar?, progress: Int, fromUser: Boolean) -> Unit = { _, _, _ -> },
    crossinline onStartTrackingTouch: (seekBar: SeekBar?) -> Unit = { },
    crossinline onStopTrackingTouch: (seekBar: SeekBar?) -> Unit = {}
): SeekBar.OnSeekBarChangeListener {
    val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            onProgressChanged.invoke(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            onStartTrackingTouch.invoke(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            onStopTrackingTouch.invoke(seekBar)
        }

    }
    setOnSeekBarChangeListener(seekBarChangeListener)
    return seekBarChangeListener
}
