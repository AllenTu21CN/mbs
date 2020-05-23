package mbs.studio.view;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import mbs.studio.MainActivity;
import com.example.studio.R;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class SettingsDialog extends PopupWindow {

    private static final String TAG = SettingsDialog.class.getSimpleName();

    private static final String[] FRAGMENT_NAMES = new String[] {
            "General", "Source", "Encoding", "Streaming", "Recording", "About"
    };

    Context mContext;
    LayoutInflater mInflater;
    View mWrapperView;
    TabLayout mTabLayout;
    ImageButton mCloseButton;
    ViewPager2 mViewPager;

    public SettingsDialog(Context context, int width, int height) {
        super(width, height);

        mContext = context;
        mInflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        mWrapperView = mInflater.inflate(R.layout.settings_dialog_wrapper, null);

        mTabLayout = mWrapperView.findViewById(R.id.tab_layout);
        mCloseButton = mWrapperView.findViewById(R.id.close_button);
        mViewPager = mWrapperView.findViewById(R.id.view_pager);

        setBackgroundDrawable(mContext.getDrawable(R.drawable.common_area_bg));
        setContentView(mWrapperView);
        setElevation(10);
        setOutsideTouchable(true);
        setFocusable(true);

        //mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        //mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        mTabLayout.setSelectedTabIndicatorHeight(Utils.PX(5));
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        MainActivity ma = (MainActivity) mContext;
        DemoCollectionAdapter demoCollectionAdapter = new DemoCollectionAdapter(
                ma.getSupportFragmentManager(), ma.getLifecycle());
        mViewPager.setAdapter(demoCollectionAdapter);
        new TabLayoutMediator(mTabLayout, mViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(FRAGMENT_NAMES[position]);
                int childCount = tab.view.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View v = tab.view.getChildAt(i);
                    if (v instanceof TextView) {
                        ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(32));
                    }
                }
            }
        }).attach();

        Utils.adjustAll((ViewGroup) mWrapperView);
    }

    public class DemoCollectionAdapter extends FragmentStateAdapter {
        public DemoCollectionAdapter(@NonNull FragmentManager fragmentManager,
                                     @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return a NEW fragment instance in createFragment(int)
            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();
            // Our object is just an integer :-P
            args.putInt(DemoObjectFragment.ARG_OBJECT, position + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return FRAGMENT_NAMES.length;
        }
    }

    // Instances of this class are fragments representing a single
    // object in our collection.
    public static class DemoObjectFragment extends Fragment {
        public static final String ARG_OBJECT = "object";

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.settings_dialog_general_fragment, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            ((TextView) view.findViewById(R.id.text1))
                    .setText(Integer.toString(args.getInt(ARG_OBJECT)));
        }
    }
}
