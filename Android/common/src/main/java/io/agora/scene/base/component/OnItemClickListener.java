package io.agora.scene.base.component;

import android.view.View;

import androidx.annotation.NonNull;

/**
 * The interface On item click listener.
 *
 * @param <T> the type parameter
 */
public interface OnItemClickListener<T> {
    /**
     * On item click.
     *
     * @param data     the data
     * @param view     the view
     * @param position the position
     * @param viewType the view type
     */
    default void onItemClick(@NonNull T data, View view, int position, long viewType) {

    }

    /**
     * On item click.
     *
     * @param view     the view
     * @param position the position
     * @param viewType the view type
     */
    default void onItemClick(View view, int position, long viewType) {

    }
}
