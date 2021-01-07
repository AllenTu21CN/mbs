package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import cn.lx.mbs.R;

public class ColorPickerButton extends AppCompatButton {

    public ColorPickerButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundDrawable(getContext().getDrawable(R.drawable.common_button_bg));
        setPadding(0, 0, 0, 0);
        setBackgroundColor(0xff336699);
    }

}
