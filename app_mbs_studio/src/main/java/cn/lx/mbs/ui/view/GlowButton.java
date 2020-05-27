package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.Button;

import cn.lx.mbs.R;

public class GlowButton extends Button {
    public static final int COLOR_OFF = 0xFF1B1B1B;
    public static final int COLOR_ON = 0xFFF74A00;
    public static final int COLOR_YELLOW = 0xFFF2F200;

    public static final int POSITION_TOP = 1;
    public static final int POSITION_BOTTOM = 2;
    public static final int POSITION_LEFT = 3;
    public static final int POSITION_RIGHT = 4;

    private int mLightWidth = -1;
    private int mLightHeight = -1;
    private int mLightColor;
    private int mLightPosition = POSITION_TOP;
    private Paint mLightPaint;

    public GlowButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackground(context.getDrawable(R.drawable.common_button_bg));

        mLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setLightColor(int color) {
        mLightColor = color;
        invalidate();
    }

    public void setLightPosition(int position) {
        mLightPosition = position;
    }

    public void setLightWidth(int width) {
        mLightWidth = width;
    }

    public void setLightHeight(int height) {
        mLightHeight = height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mLightPaint.setStyle(Paint.Style.FILL);
        mLightPaint.setColor(mLightColor);

        if (mLightHeight <= 0) {
            mLightHeight = Utils.PX(6);
        }

        float radius = mLightHeight;
        float left = Utils.PX(8);
        float right = getWidth() - left;
        float top = Utils.PX(6);
        float bottom = top + mLightHeight;

        switch (mLightPosition) {
            case POSITION_TOP :
                break;

            case POSITION_BOTTOM : {
                bottom = getHeight() - Utils.PX(6);
                top = bottom - mLightHeight;
                break;
            }

            case POSITION_LEFT :
                // TODO:
                break;

            case POSITION_RIGHT :
                // TODO:
                break;

        }

        canvas.drawRoundRect(left, top, right, bottom, radius, radius, mLightPaint);

    }
}
