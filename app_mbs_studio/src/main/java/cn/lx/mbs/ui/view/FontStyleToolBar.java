package cn.lx.mbs.ui.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import cn.lx.mbs.R;

public class FontStyleToolBar extends LinearLayout {

    public interface OnFontStyleChangeListener {
         void OnFontStyleChanged(FontStyleToolBar fontStyleToolBar);
    }

    private OnFontStyleChangeListener mOnFontStyleChangeListener;

    private static final int BOLD = 0;
    private static final int ITALIC = 1;
    private static final int UNDERLINED = 2;
    private static final int ALIGN_LEFT = 3;
    private static final int ALIGN_CENTER = 4;
    private static final int ALIGN_RIGHT = 5;

    private static final int[] ICONS_RES_ID = new int[] {
        R.drawable.ic_format_bold_black_24dp,
        R.drawable.ic_format_italic_black_24dp,
        R.drawable.ic_format_underlined_black_24dp,
        R.drawable.ic_format_align_left_black_24dp,
        R.drawable.ic_format_align_center_black_24dp,
        R.drawable.ic_format_align_right_black_24dp,
    };

    private FontStyleButton[] mButtons = new FontStyleButton[ICONS_RES_ID.length];

    public FontStyleToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.HORIZONTAL);
        setPadding(0, 0, 0, 0);

        for (int i = 0; i < mButtons.length; i++) {
            FontStyleButton fb = new FontStyleButton(context, context.getDrawable(ICONS_RES_ID[i]));

            mButtons[i] = fb;
            if (i < ALIGN_LEFT) {
                // Bold, Italic and Underlined
                fb.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (v instanceof FontStyleButton) {
                            FontStyleButton btn = (FontStyleButton) v;
                            btn.toggle();

                            if (mOnFontStyleChangeListener != null) {
                                mOnFontStyleChangeListener.OnFontStyleChanged(FontStyleToolBar.this);
                            }
                        }
                    }
                });
            } else {
                // Alignment
                fb.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (v instanceof FontStyleButton) {
                            FontStyleButton btn = (FontStyleButton) v;
                            for (int j = 3; j < mButtons.length; j++) {
                                mButtons[j].setChecked(false);
                            }
                            btn.setChecked(true);

                            if (mOnFontStyleChangeListener != null) {
                                mOnFontStyleChangeListener.OnFontStyleChanged(FontStyleToolBar.this);
                            }
                        }
                    }
                });
            }

            addView(fb);
            //Utils.setSize(fb, Utils.PX(48), Utils.PX(48));
            Utils.setMargins(fb, Utils.PX(12), 0, Utils.PX(12), 0);
        }
    }

    public boolean isBold() {
        return mButtons[BOLD].isChecked();
    }

    public boolean isItalic() {
        return mButtons[ITALIC].isChecked();
    }

    public boolean isUnderlined() {
        return mButtons[UNDERLINED].isChecked();
    }

    public Layout.Alignment getAlignment() {
        if (mButtons[ALIGN_LEFT].isChecked()) {
            return Layout.Alignment.ALIGN_NORMAL;
        }

        if (mButtons[ALIGN_CENTER].isChecked()) {
            return Layout.Alignment.ALIGN_CENTER;
        }

        if (mButtons[ALIGN_RIGHT].isChecked()) {
            return Layout.Alignment.ALIGN_OPPOSITE;
        }

        return Layout.Alignment.ALIGN_CENTER;
    }

    public void setOnFontStyleChangeListener(OnFontStyleChangeListener listener) {
        mOnFontStyleChangeListener = listener;
    }

    private class FontStyleButton extends ImageButton {
        private boolean mChecked = false;

        FontStyleButton(Context context, Drawable src) {
            super(context, null);

            setImageDrawable(src);
            setBackgroundColor(0x00000000);
            setImageTintList(
                    ColorStateList.valueOf(context.getColor(R.color.commonButtonNormalTextColor)));
            setImageTintMode(PorterDuff.Mode.SRC_IN);
            setScaleType(ScaleType.FIT_XY);
        }

        void setChecked(boolean checked) {
            mChecked = checked;

            setImageTintList(
                    ColorStateList.valueOf(
                            getContext().getColor(
                                    mChecked ? R.color.colorPrimaryOrange
                                             : R.color.commonButtonNormalTextColor)));

            invalidate();
        }

        void toggle() {
            setChecked(!mChecked);
        }

        boolean isChecked() {
            return mChecked;
        }
    }
}
