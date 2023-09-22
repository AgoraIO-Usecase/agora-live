package io.agora.scene.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import io.agora.scene.base.R;
import io.agora.scene.base.utils.UiUtil;

/**
 * The type Custom volume seek bar view.
 */
public class CustomVolumeSeekBarView extends View {
    /**
     * The Total pitch.
     */
    private final int totalPitch = 11;

    /**
     * The Current pitch.
     */
    private int currentPitch = 5;

    /**
     * The Padding bottom.
     */
    private int paddingBottom = UiUtil.dp2px(3);

    /**
     * The M select blue color.
     */
    private final int mSelectBlueColor = ContextCompat.getColor(getContext(), R.color.blue_9F);
    /**
     * The M un select blue color.
     */
    private final int mUnSelectBlueColor = ContextCompat.getColor(getContext(), R.color.white);

    /**
     * The M paint.
     */
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    /**
     * Instantiates a new Custom volume seek bar view.
     *
     * @param context the context
     */
    public CustomVolumeSeekBarView(Context context) {
        super(context);
        initView();
    }

    /**
     * Instantiates a new Custom volume seek bar view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public CustomVolumeSeekBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    /**
     * Instantiates a new Custom volume seek bar view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public CustomVolumeSeekBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * Init view.
     */
    private void initView() {
        mPaint.setStrokeWidth(UiUtil.dp2px(3));
    }

    /**
     * On draw.
     *
     * @param canvas the canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawStartLine(canvas);
    }

    /**
     * Draw start line.
     *
     * @param canvas the canvas
     */
    private void drawStartLine(Canvas canvas) {
        for (int i = 0; i < totalPitch; i++) {
            if (i <= currentPitch) {
                mPaint.setColor(mSelectBlueColor);
            } else {
                mPaint.setColor(mUnSelectBlueColor);
            }
            int left = i * UiUtil.dp2px(17);
            int top = getBottom() - paddingBottom - UiUtil.dp2px((i + 1) * 2);
            int right = left + paddingBottom;
            int bottom = getBottom() - paddingBottom;
            canvas.drawRect(left, top, right, bottom, mPaint);
        }
    }

    /**
     * Current pitch plus.
     */
    public void currentPitchPlus() {
        if (currentPitch == 11) {
            return;
        }
        currentPitch++;
        invalidate();
    }

    /**
     * Current pitch minus.
     */
    public void currentPitchMinus() {
        if (currentPitch < 0) {
            return;
        }
        currentPitch--;
        invalidate();
    }

}
