package io.agora.voice.common.ui.adapter.listener;

import android.view.View;

/**
 * Item long click listener
 */
public interface OnItemLongClickListener {
    /**
     * Item long click
     *
     * @param view     the view
     * @param position the position
     * @return the boolean
     */
    boolean onItemLongClick(View view, int position);
}
