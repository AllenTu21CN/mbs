package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class ColorPicker extends LinearLayout {

    public interface OnColorPickerChangeListener {
        void OnColorSelected(ColorPicker colorPicker);
    }

    private OnColorPickerChangeListener mOnColorPickerChangeListener;

    private ColorButton mSelectedCB;
    private ColorButton[] mColorButtons;

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.HORIZONTAL);
        setPadding(0, 0, 0, 0);

        //updateColorList(STANDARD_COLORS);
    }

    public Color getSelectedColor () {
        if (mSelectedCB != null) {
            return mSelectedCB.getColor();
        }

        return Color.valueOf(Color.TRANSPARENT);
    }

    public void updateColorList(Color[] colors) {
        // TODO:
        removeAllViews();

        mColorButtons = new ColorButton[colors.length];
        for (int i = 0; i < colors.length; i++) {
            ColorButton cb = new ColorButton(getContext(), colors[i]);
            mColorButtons[i] = cb;
            addView(cb);
            Utils.setSize(cb, Utils.PX(48), Utils.PX(48));
            Utils.setMargins(cb, Utils.PX(10), 0, Utils.PX(10), 0);

            cb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (v instanceof ColorButton) {
                        ColorButton btn = (ColorButton) v;
                        if (mSelectedCB != btn) {
                            if (mSelectedCB != null) {
                                mSelectedCB.setSelect(false);
                            }
                            btn.setSelect(true);
                            mSelectedCB = btn;

                            if (mOnColorPickerChangeListener != null) {
                                mOnColorPickerChangeListener.OnColorSelected(ColorPicker.this);
                            }
                        }
                    }
                }
            });
        }

        invalidate();
    }

    public void setSelectedColor(Color color) {
        for (ColorButton cb : mColorButtons) {
            if (cb.getColor().equals(color)) {
                if (cb != mSelectedCB) {
                    if (mSelectedCB != null) {
                        mSelectedCB.setSelect(false);
                    }
                    cb.setSelect(true);
                    mSelectedCB = cb;
                }
                break;
            }
        }
    }

    public void setOnColorPickerChangeListener(OnColorPickerChangeListener listener) {
        mOnColorPickerChangeListener = listener;
    }

    public boolean showCustomDialog() {
        // TODO:
        return false;
    }

    private class ColorButton extends Button {
        public static final int COLOR_SELECTED = 0xFFF74A00;

        public final int BORDER_WIDTH = Utils.PX(8);
        public final int PADDING_WIDTH = Utils.PX(0);

        private Color mColor;
        private boolean mIsSelected = false;

        private Paint mBgImagePaint;
        private Paint mBorderPaint;
        private Paint mPaddingPaint;

        public ColorButton(Context context, Color color) {
            super(context, null);

            mColor = color;
            setBackgroundColor(mColor.toArgb());

            mBgImagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setColor(COLOR_SELECTED);
            mBorderPaint.setStrokeWidth(BORDER_WIDTH);

            mPaddingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaddingPaint.setStyle(Paint.Style.STROKE);
            mPaddingPaint.setColor(Color.WHITE);
            mPaddingPaint.setStrokeWidth(PADDING_WIDTH);
        }

        public Color getColor() { return mColor; }

        public boolean isSelected() { return mIsSelected; }

        public void setSelect(boolean selected) {
            mIsSelected = selected;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mColor.toArgb() == Color.TRANSPARENT) {
                final int CELL_SIZE = Utils.PX(5);
                Bitmap bg = Utils.generateCheckerBoardBitmap(getWidth(), getHeight(), CELL_SIZE, CELL_SIZE);
                canvas.drawBitmap(bg, 0, 0, mBgImagePaint);
            }

            if (!mIsSelected) {
                return;
            }

            float left = 0;
            float right = getWidth();
            float top = 0;
            float bottom = getHeight();

            canvas.drawRect(left, top, right, bottom, mBorderPaint);

            if (PADDING_WIDTH > 0) {
                left += BORDER_WIDTH;
                right -= BORDER_WIDTH;
                top += BORDER_WIDTH;
                bottom -= BORDER_WIDTH;

                canvas.drawRect(left, top, right, bottom, mPaddingPaint);
            }
        }
    }
}
