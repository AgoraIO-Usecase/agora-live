package io.agora.scene.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import io.agora.scene.base.R;

/**
 * Encryption Input View.
 */
public class RoomEncryptionInputView extends TextInputEditText {
    private Paint mSidePaint, mBackPaint, mTextPaint;
    private Context mC;
    private String mText;
    private List<RectF> mRectFS;
    private int mStrokeWidth, mSpaceX, mTextSize;
    private int mCheckedColor, mDefaultColor, mBackColor, mTextColor, mWaitInputColor;
    private int mTextLength;
    private int mCircle, mRound;
    private boolean mIsPwd, mIsWaitInput;
    private Paint l;

    /**
     * constructor.
     *
     * @param context the context
     */
    public RoomEncryptionInputView(Context context) {
        super(context);

        mC = context;
        setAttrs(null);
        init();
    }

    /**
     * constructor.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public RoomEncryptionInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mC = context;
        setAttrs(attrs);
        init();
    }

    /**
     * constructor.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public RoomEncryptionInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mC = context;
        setAttrs(attrs);
        init();
    }

    /**
     * set attrs.
     *
     * @param attrs
     */
    private void setAttrs(AttributeSet attrs) {
        TypedArray t = mC.obtainStyledAttributes(attrs, R.styleable.encryption_input_style);
        mTextLength = t.getInt(R.styleable.encryption_input_style_textLength, 6);
        mStrokeWidth = t.getDimensionPixelSize(R.styleable.encryption_input_style_strokeWidth, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
        mRound = t.getDimensionPixelSize(R.styleable.encryption_input_style_round, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        mCircle = t.getDimensionPixelSize(R.styleable.encryption_input_style_circle, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, getResources().getDisplayMetrics()));
        mTextSize = t.getDimensionPixelSize(R.styleable.encryption_input_style_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
        mCheckedColor = t.getColor(R.styleable.encryption_input_style_checkedColor, 0xff44ce61);
        mDefaultColor = t.getColor(R.styleable.encryption_input_style_defaultColor, 0xffd0d0d0);
        mBackColor = t.getColor(R.styleable.encryption_input_style_backColor, 0xfff1f1f1);
        mTextColor = t.getColor(R.styleable.encryption_input_style_textColor, 0xFF444444);
        mWaitInputColor = t.getColor(R.styleable.encryption_input_style_waitInputColor, 0xFF444444);
        mIsPwd = t.getBoolean(R.styleable.encryption_input_style_isPwd, true);
        mIsWaitInput = t.getBoolean(R.styleable.encryption_input_style_isWaitInput, true);
        t.recycle();
    }


    /**
     * init.
     */
    private void init() {
        setTextColor(0X00ffffff); //把用户输入的内容设置为透明
        setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        mSidePaint = new Paint();
        mBackPaint = new Paint();
        mTextPaint = new Paint();


        mRectFS = new ArrayList<>();
        mText = "";

        this.setBackgroundDrawable(null);
        setLongClickable(false);
        setTextIsSelectable(false);
        setCursorVisible(false);

    }

    /**
     * @param text         The text the TextView is displaying
     * @param start        The offset of the start of the range of the text that was
     *                     modified
     * @param lengthBefore The length of the former text that has been replaced
     * @param lengthAfter  The length of the replacement modified text
     */
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (mText == null) {
            return;
        }
        //如果字数不超过用户设置的总字数，就赋值给成员变量mText；
        // 如果字数大于用户设置的总字数，就只保留用户设置的几位数字，并把光标制动到最后，让用户可以删除；
        if (text.toString().length() <= mTextLength) {
            mText = text.toString();
        } else {
            setText(mText);
            setSelection(getText().toString().length());  //光标制动到最后
            //调用setText(mText)之后键盘会还原，再次把键盘设置为数字键盘；
            setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        }
        if (onTextChangeListener != null) {
            onTextChangeListener.onTextChange(mText);
        }
    }

    /**
     * @param widthMeasureSpec  horizontal space requirements as imposed by the parent.
     *                          The requirements are encoded with
     *                          {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     *                          The requirements are encoded with
     *                          {@link android.view.View.MeasureSpec}.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                heightSize = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                heightSize = widthSize / mTextLength;
                break;
            default:
                break;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * @param canvas the canvas on which the background will be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //边框画笔
        mSidePaint.setAntiAlias(true); //消除锯齿
        mSidePaint.setStrokeWidth(mStrokeWidth); //设置画笔的宽度
        mSidePaint.setStyle(Paint.Style.STROKE); //设置绘制轮廓
        mSidePaint.setColor(mDefaultColor);
        //背景色画笔
        mBackPaint.setStyle(Paint.Style.FILL);
        mBackPaint.setColor(mBackColor);
        //文字的画笔
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);

        // 方型大小
        int singleSize = getMeasuredHeight();
        // 左右间距
        mSpaceX = (getMeasuredWidth() - singleSize * mTextLength) / (mTextLength - 1);
        RectF rectBg = null;
        for (int i = 0; i < mTextLength; i++) {
            //区分已输入和未输入的边框颜色
            if (mText.length() >= i) {
                mSidePaint.setColor(mCheckedColor);
            } else {
                mSidePaint.setColor(mDefaultColor);
            }
            //RectF的参数(left,  top,  right,  bottom); 画出每个矩形框并设置间距，间距其实是增加左边框距离，缩小上下右边框距离；
            rectBg = new RectF(i * singleSize + mSpaceX * i + mStrokeWidth, mStrokeWidth,
                    (i + 1) * singleSize + mSpaceX * i - mStrokeWidth,
                    singleSize - mStrokeWidth);
            //四个值，分别代表4条线，距离起点位置的线
            canvas.drawRoundRect(rectBg, mRound, mRound, mBackPaint); //绘制背景色
            canvas.drawRoundRect(rectBg, mRound, mRound, mSidePaint); //绘制边框
            mRectFS.add(rectBg);

            if (mIsWaitInput && i == mText.length()) {  //显示待输入的线
                l = new Paint();
                l.setStrokeWidth(3);
                l.setStyle(Paint.Style.FILL);
                l.setColor(mWaitInputColor);
                canvas.drawLine(i * singleSize + singleSize / 2 + (mSpaceX * i),
                        singleSize / 2 - singleSize / 5,
                        i * singleSize + singleSize / 2 + (mSpaceX * i),
                        singleSize / 2 + singleSize / 5, l);
            }
        }
        //画密码圆点
        for (int j = 0; j < mText.length(); j++) {
            if (mIsPwd) {
                canvas.drawCircle(mRectFS.get(j).centerX(), mRectFS.get(j).centerY(), mCircle, mTextPaint);
            } else {
                canvas.drawText(mText.substring(j, j + 1), mRectFS.get(j).centerX() - mTextSize / 2 + mStrokeWidth,
                        mRectFS.get(j).centerY() + mTextSize / 2 - mStrokeWidth, mTextPaint);
            }
        }
    }

    /**
     * input listener.
     */
    public interface OnTextChangeListener {
        /**
         * on text change.
         *
         * @param pwd the pwd
         */
        void onTextChange(String pwd);
    }

    /**
     * OnTextChangeListener.
     */
    private OnTextChangeListener onTextChangeListener;

    /**
     * Sets on text change listener.
     *
     * @param onTextChangeListener the on text change listener
     */
    public void setOnTextChangeListener(OnTextChangeListener onTextChangeListener) {
        this.onTextChangeListener = onTextChangeListener;
    }

    /**
     * clear text.
     */
    public void clearText() {
        setText("");
        setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
    }

    /**
     * set x space.
     *
     * @param space the space
     */
    public void setXSpace(int space) {
        mSpaceX = space;
    }

    /**
     * set input box count.
     *
     * @param mTextLength the m text length
     */
    public void setTextLength(int mTextLength) {
        this.mTextLength = mTextLength;
    }

    /**
     * get input box count.
     *
     * @return text lenght
     */
    public int getTextLength() {
        return this.mTextLength;
    }

    /**
     * set checked color.
     *
     * @param checkedColor the checked color
     */
    public void setCheckedColorColor(int checkedColor) {
        this.mCheckedColor = checkedColor;
    }

    /**
     * set default color.
     *
     * @param defaultColor the default color
     */
    public void setDefaultColorColor(int defaultColor) {
        this.mDefaultColor = defaultColor;
    }

    /**
     * set back color.
     *
     * @param mBackColor the m back color
     */
    public void setBackColor(int mBackColor) {
        this.mBackColor = mBackColor;
    }

    /**
     * set pwd text color.
     *
     * @param textColor the text color
     */
    public void setPwdTextColor(int textColor) {
        this.mTextColor = textColor;
    }

    /**
     * set stoke width.
     *
     * @param width the width
     */
    public void setStrokeWidth(int width) {
        mStrokeWidth = width;
    }

    /**
     * set circle.
     *
     * @param circle the circle
     */
    public void setCircle(int circle) {
        this.mCircle = circle;
    }

    /**
     * set round.
     *
     * @param round the round
     */
    public void setRound(int round) {
        this.mRound = round;
    }

    /**
     * get stoke width.
     *
     * @return stoke width.
     */
    public int getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * get space x.
     *
     * @return spaceX. space x
     */
    public int getSpaceX() {
        return mSpaceX;
    }

    /**
     * get check color.
     *
     * @return checked color.
     */
    public int getCheckedColor() {
        return mCheckedColor;
    }

    /**
     * get default color.
     *
     * @return default color.
     */
    public int getDefaultColor() {
        return mDefaultColor;
    }

    /**
     * get back color.
     *
     * @return back color.
     */
    public int getBackColor() {
        return mBackColor;
    }

    /**
     * get text color.
     *
     * @return text color.
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * get circle.
     *
     * @return circle. circle
     */
    public int getCircle() {
        return mCircle;
    }

    /**
     * get round.
     *
     * @return round. round
     */
    public int getRound() {
        return mRound;
    }

    /**
     * get text size.
     *
     * @return text size.
     */
    public int geTextSize() {
        return mTextSize;
    }

    /**
     * set text size.
     *
     * @param mTextSize the m text size
     */
    public void setTextSize(int mTextSize) {
        this.mTextSize = mTextSize;
    }

    /**
     * get is pwd.
     *
     * @return pwd is pwd
     */
    public boolean getIsPwd() {
        return mIsPwd;
    }

    /**
     * set is pwd.
     *
     * @param mIsPwd the m is pwd
     */
    public void setIsPwd(boolean mIsPwd) {
        this.mIsPwd = mIsPwd;
    }

    /**
     * Gets wait input color.
     *
     * @return wait input color
     */
    public int getWaitInputColor() {
        return mWaitInputColor;
    }

    /**
     * set wait input color.
     *
     * @param mWaitInputColor the m wait input color
     */
    public void setWaitInputColor(int mWaitInputColor) {
        this.mWaitInputColor = mWaitInputColor;
    }

    /**
     * Is is wait input boolean.
     *
     * @return is boolean
     */
    public boolean isIsWaitInput() {
        return mIsWaitInput;
    }

    /**
     * set is wait input.
     *
     * @param mIsWaitInput the m is wait input
     */
    public void setIsWaitInput(boolean mIsWaitInput) {
        this.mIsWaitInput = mIsWaitInput;

    }
}
