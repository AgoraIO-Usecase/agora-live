package io.agora.scene.widget.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.style.ReplacementSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type Outline span.
 */
public final class OutlineSpan extends ReplacementSpan {
    private final int strokeColor;
    private final float strokeWidth;

    /**
     * Instantiates a new Outline span.
     *
     * @param strokeColor the stroke color
     * @param strokeWidth the stroke width
     */
    public OutlineSpan(@ColorInt int strokeColor, @Dimension float strokeWidth) {
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }

    @Override
    public int getSize(@NotNull Paint paint, @NotNull CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fontMetrics) {
        if (fontMetrics != null && paint.getFontMetricsInt() != null) {
            fontMetrics.bottom = paint.getFontMetricsInt().bottom;
            fontMetrics.top = paint.getFontMetricsInt().top;
            fontMetrics.descent = paint.getFontMetricsInt().descent;
            fontMetrics.leading = paint.getFontMetricsInt().leading;
        }

        return (int) paint.measureText(text.subSequence(start, end).toString());
    }

    @Override
    public void draw(@NotNull Canvas canvas, @NotNull CharSequence text, int start, int end, float x, int top, int y, int bottom, @NotNull Paint paint) {
        int originTextColor = paint.getColor();
        paint.setColor(this.strokeColor);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(this.strokeWidth);
        canvas.drawText(text, start, end, x, (float) y, paint);
        paint.setColor(originTextColor);
        paint.setStyle(Style.FILL);
        canvas.drawText(text, start, end, x, (float) y, paint);
    }
}
