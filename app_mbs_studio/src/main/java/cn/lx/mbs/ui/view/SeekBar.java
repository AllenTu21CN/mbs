package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.Gravity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class SeekBar extends android.widget.SeekBar {
    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        setProgressBarHeight(8);
        setThumbSize(30);
    }

    public void setProgressBarHeight(int height) {
        int progressBarHeight = height;
        /**
         * <item android:id="@android:id/background"
         *         android:gravity="center_vertical|fill_horizontal">
         *         <shape android:shape="rectangle"
         *             android:tint="#555555">
         *             <corners android:radius="10px"/>
         *             <size android:height="10px" />
         *             <solid android:color="#555555" />
         *         </shape>
         *     </item>
         * <item android:id="@android:id/progress"
         *         android:gravity="center_vertical|fill_horizontal">
         *         <scale android:scaleWidth="100%">
         *             <selector>
         *                 <item android:state_enabled="false"
         *                     android:drawable="@android:color/transparent" />
         *                 <item>
         *                     <shape android:shape="rectangle"
         *                         android:tint="#2ecc71">
         *                         <corners android:radius="10px"/>
         *                         <size android:height="10px" />
         *                         <solid android:color="#2ecc71" />
         *                     </shape>
         *                 </item>
         *             </selector>
         *         </scale>
         *     </item>
         */
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(0xff555555);
        background.setCornerRadius(progressBarHeight);
        background.setSize(MATCH_PARENT, progressBarHeight);
        background.setTint(0xff555555);

        GradientDrawable checked = new GradientDrawable();
        checked.setShape(GradientDrawable.RECTANGLE);
        checked.setColor(0xff2ecc71);
        checked.setCornerRadius(progressBarHeight);
        checked.setSize(MATCH_PARENT, progressBarHeight);
        checked.setTint(0xff2ecc71);

        StateListDrawable selector = new StateListDrawable();
        selector.addState(new int[] { -android.R.attr.state_enabled }, getContext().getDrawable(android.R.color.transparent));
        selector.addState(StateSet.WILD_CARD, checked);

        ScaleDrawable progress = new ScaleDrawable(selector, Gravity.LEFT, 1.0f, -1);

        LayerDrawable drawable = new LayerDrawable(new Drawable[]{ background, progress });
        drawable.setId(0, android.R.id.background);
        drawable.setId(1, android.R.id.progress);
        drawable.setLayerGravity(0, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
        drawable.setLayerGravity(1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);

        setProgressDrawable(drawable);
    }

    public void setThumbSize(int size) {
        int thumbSize = size;
        /**
         * <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
         *     <item>
         *         <shape android:shape="oval">
         *             <solid android:color="@color/primaryTextColor"/>
         *             <size android:width="30px" android:height="30px"/>
         *         </shape>
         *     </item>
         * </layer-list>
         */
        ShapeDrawable thumb = new ShapeDrawable(new OvalShape());
        thumb.getPaint().setStyle(Paint.Style.FILL);
        thumb.getPaint().setColor(0xffffffff);
        thumb.setIntrinsicWidth(thumbSize);
        thumb.setIntrinsicHeight(thumbSize);

        LayerDrawable drawable = new LayerDrawable(new Drawable[]{ thumb });
        setThumb(drawable);
    }
}
