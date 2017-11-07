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

    public static class Item {
        public String name;
        public View.OnClickListener listener;
        public Item() {
        }
        public Item(String name, View.OnClickListener listener) {
            this.name = name;
            this.listener = listener;
        }
    }

    public static class ItemGroup {
        public String name;
        public List<Item> items;
        public ItemGroup() {
            name = null;
            items = new ArrayList<>();
        }
        public ItemGroup(String name, List<Item> items) {
            this();
            this.name = name;
            this.items.addAll(items);
        }
    }

    private static DynamicMenuFragment mDynamicMenuFragment;
    public static DynamicMenuFragment getInstance(List<ItemGroup> itemGroups) {
        synchronized (DynamicMenuFragment.class) {
            if (mDynamicMenuFragment == null) {
                if (mDynamicMenuFragment == null) {
                    mDynamicMenuFragment = new DynamicMenuFragment();
                }
            }
            mDynamicMenuFragment.setItems(itemGroups);
            return mDynamicMenuFragment;
        }
    }

    private View mView;
    private LinearLayout mViewGroup;

    private List<ItemGroup> mItemGroups;

    private void setItems(List<ItemGroup> itemGroups) {
        mItemGroups = itemGroups;
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
        for(ItemGroup group: mItemGroups) {
            if(group.name != null && !group.name.isEmpty()) { // add group title
                TextView text = new TextView(mContext);
                text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                text.setTextColor(mContext.getResources().getColor(R.color.white));
                text.setBackground(mContext.getResources().getDrawable(R.drawable.course_table_date_bar_button_selector));
                text.setText(group.name);
                text.getPaint().setFakeBoldText(true);
                mViewGroup.addView(text);
            }

            for(Item item: group.items) {
                Button btn = new Button(mContext);
                btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btn.setTextColor(mContext.getResources().getColor(R.color.white));
                btn.setBackground(mContext.getResources().getDrawable(R.drawable.course_table_date_bar_button_selector));
                btn.setText(item.name);
                btn.setOnClickListener(item.listener);
                mViewGroup.addView(btn);

//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 1);
//            View vi = new View(mContext);
//            vi.setBackgroundColor(mContext.getResources().getColor(R.color.course_dialog_line_color));
//            vi.setLayoutParams(params);
//            mViewGroup.addView(vi);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
