package sanp.mp100.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sanp.mp100.R;

public class DynamicMenuFragment extends BaseFragment {
    public static final String TAG = "DynamicMenuFragment";

    private static DynamicMenuFragment mDynamicMenuFragment;
    public static DynamicMenuFragment getInstance(List<Pair<String/*itemName*/, View.OnClickListener>> items) {
        synchronized (DynamicMenuFragment.class) {
            if (mDynamicMenuFragment == null) {
                if (mDynamicMenuFragment == null) {
                    mDynamicMenuFragment = new DynamicMenuFragment();
                }
            }
            mDynamicMenuFragment.setItems(items);
            return mDynamicMenuFragment;
        }
    }

    private View mView;
    private LinearLayout mViewGroup;

    private List<Pair<String/*itemName*/, View.OnClickListener>> mItems;

    private void setItems(List<Pair<String/*itemName*/, View.OnClickListener>> items) {
        mItems = new ArrayList<>();
        mItems.addAll(items);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = View.inflate(getActivity(), R.layout.dynamic_menu_view, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initView();
        return mView;
    }

    public void initView() {
        mViewGroup = (LinearLayout) mView.findViewById(R.id.view_group);
        initChilView();
    }

    private void initChilView() {
        for(Pair<String/*itemName*/, View.OnClickListener> item: mItems) {
            String name = item.first;
            View.OnClickListener listener = item.second;
            Button btn = new Button(mContext);
            btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btn.setTextColor(mContext.getResources().getColor(R.color.white));
            btn.setBackground(mContext.getResources().getDrawable(R.drawable.course_table_date_bar_button_selector));
            btn.setText(name);
            btn.setOnClickListener(listener);
            mViewGroup.addView(btn);

//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 1);
//            View vi = new View(mContext);
//            vi.setBackgroundColor(mContext.getResources().getColor(R.color.course_dialog_line_color));
//            vi.setLayoutParams(params);
//            mViewGroup.addView(vi);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
