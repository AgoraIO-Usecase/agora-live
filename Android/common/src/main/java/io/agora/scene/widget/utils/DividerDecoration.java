package io.agora.scene.widget.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.agora.scene.base.utils.UiUtil;


/**
 * The type Divider decoration.
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private final int gapHorizontal;
    private final int gapVertical;
    private final int spanCount;

    /**
     * Instantiates a new Divider decoration.
     *
     * @param spanCount the span count
     */
    public DividerDecoration(int spanCount) {
        gapHorizontal = (int) UiUtil.dp2px(16);
        gapVertical = gapHorizontal;
        this.spanCount = spanCount;
    }

    /**
     * Instantiates a new Divider decoration.
     *
     * @param spanCount     the span count
     * @param gapHorizontal the gap horizontal
     * @param gapHeight     the gap height
     */
    public DividerDecoration(int spanCount, int gapHorizontal, int gapHeight) {
        this.gapHorizontal = (int) UiUtil.dp2px(gapHorizontal);
        this.gapVertical = (int) UiUtil.dp2px(gapHeight);
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int index = parent.getChildAdapterPosition(view);

        if (spanCount == 1) {
            outRect.left = gapHorizontal;
            outRect.right = gapHorizontal;
        } else {

//            if (index % spanCount == 0) {
//                outRect.left = gapHorizontal;
//                outRect.right = gapHorizontal / 2;
//            } else if (index % spanCount == spanCount - 1) {
//                outRect.left = gapHorizontal / 2;
//                outRect.right = gapHorizontal;
//            } else {
//                outRect.left = gapHorizontal / 2;
//                outRect.right = gapHorizontal / 2;
//            }

            outRect.left = gapHorizontal * (spanCount - index % spanCount) / spanCount;
            outRect.right = gapHorizontal * (1 + index % spanCount) / spanCount;
        }
        outRect.top = gapVertical;
    }
}
