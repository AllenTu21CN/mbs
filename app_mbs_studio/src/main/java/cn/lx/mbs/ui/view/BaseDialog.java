package mbs.studio.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.studio.R;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class BaseDialog extends PopupWindow {

    Context mContext;
    LayoutInflater mInflater;
    View mWrapperView;
    TextView mTitleTextView;
    ImageButton mCloseButton;
    FrameLayout mContentView;

    public BaseDialog(Context context, int width, int height) {
        super(width, height);

        mContext = context;
        mInflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        mWrapperView = mInflater.inflate(R.layout.base_dialog_wrapper, null);
        Utils.adjustAll((ViewGroup) mWrapperView);

        mTitleTextView = mWrapperView.findViewById(R.id.title);
        mCloseButton = mWrapperView.findViewById(R.id.close_button);
        mContentView = mWrapperView.findViewById(R.id.content);
        Utils.adjustPaddings(mTitleTextView);
        //Utils.adjustSize(mTitleTextView);
        //Utils.adjustTextSize(mTitleTextView);
        //Utils.adjustSize(mCloseButton);

        setBackgroundDrawable(mContext.getDrawable(R.drawable.common_area_bg));
        setContentView(mWrapperView);
        setElevation(10);
        setOutsideTouchable(true);
        setFocusable(true);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    public void setContent(View content) {
        mContentView.removeAllViews();
        mContentView.addView(content);
    }
}
