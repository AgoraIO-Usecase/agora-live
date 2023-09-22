package io.agora.scene.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * The type Cus horizontal scroll view.
 */
public class CusHorizontalScrollView extends HorizontalScrollView {
    /**
     * The constant TAG.
     */
    private final static String TAG = "CusHorizontalScrollView";

    /**
     * Instantiates a new Cus horizontal scroll view.
     *
     * @param context the context
     */
    public CusHorizontalScrollView(Context context) {
        super(context);
    }

    /**
     * Instantiates a new Cus horizontal scroll view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public CusHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Instantiates a new Cus horizontal scroll view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public CusHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * On intercept touch event boolean.
     *
     * @param ev the ev
     * @return the boolean
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Dispatch touch event boolean.
     *
     * @param ev the ev
     * @return the boolean
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    /**
     * On touch event boolean.
     *
     * @param ev the ev
     * @return the boolean
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                getParent().getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;

            default:
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Is scroll to right boolean.
     *
     * @return the boolean
     */
    private boolean isScrollToRight() {
        return getChildAt(getChildCount() - 1).getRight() == getScrollX() + getWidth();
    }

    /**
     * Is scroll to left boolean.
     *
     * @return the boolean
     */
    private boolean isScrollToLeft() {
        return getScrollX() == 0;
    }
}