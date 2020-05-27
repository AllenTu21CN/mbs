package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.StateSet;
import android.widget.Switch;

public class SwitchButton extends Switch {
    public SwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        setThumbSize(32);
    }

    public void setThumbSize(int size) {
        int thumbSize = size;
        int padding = Utils.PX(4);
        int trackRadius = thumbSize + padding * 2;

        /**
         * <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
         *     <item android:left="4px" android:top="4px" android:right="4px" android:bottom="4px">
         *         <shape android:shape="oval">
         *             <solid android:color="#ffffff"/>
         *             <size android:width="32px" android:height="32px"/>
         *         </shape>
         *     </item>
         * </layer-list>
         */
        ShapeDrawable thumbDrawable = new ShapeDrawable(new OvalShape());
        thumbDrawable.getPaint().setStyle(Paint.Style.FILL);
        thumbDrawable.getPaint().setColor(0xffffffff);
        thumbDrawable.setIntrinsicWidth(thumbSize);
        thumbDrawable.setIntrinsicHeight(thumbSize);

        LayerDrawable switchThumbDrawable = new LayerDrawable(new Drawable[]{ thumbDrawable });
        switchThumbDrawable.setLayerInset(0, padding, padding, padding, padding);

        setThumbDrawable(switchThumbDrawable);

        /**
         * <selector xmlns:android="http://schemas.android.com/apk/res/android">
         *     <!-- Checked -->
         *     <item android:state_checked="true">
         *         <shape android:shape="rectangle">
         *             <corners android:radius="40px" />
         *             <solid android:color="#2ecc71" />
         *         </shape>
         *     </item>
         *
         *     <!-- Unchecked -->
         *     <item android:state_checked="false">
         *         <shape android:shape="rectangle">
         *             <corners android:radius="40px" />
         *             <solid android:color="#555555" />
         *         </shape>
         *     </item>
         * </selector>
         */
        GradientDrawable checkedTrackDrawable = new GradientDrawable();
        checkedTrackDrawable.setShape(GradientDrawable.RECTANGLE);
        checkedTrackDrawable.setColor(0xff2ecc71);
        checkedTrackDrawable.setCornerRadius(trackRadius);

        GradientDrawable uncheckedTrackDrawable = new GradientDrawable();
        uncheckedTrackDrawable.setShape(GradientDrawable.RECTANGLE);
        uncheckedTrackDrawable.setColor(0xff555555);
        uncheckedTrackDrawable.setCornerRadius(trackRadius);

        StateListDrawable trackDrawable = new StateListDrawable();
        trackDrawable.addState(new int[] { android.R.attr.state_checked }, checkedTrackDrawable);
        trackDrawable.addState(StateSet.WILD_CARD, uncheckedTrackDrawable);

        setTrackDrawable(trackDrawable);
    }
}
