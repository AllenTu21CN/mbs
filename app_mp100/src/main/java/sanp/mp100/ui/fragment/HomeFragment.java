package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sanp.mp100.R;
import sanp.mp100.test.ui.fragment.DeviceTestFragment;

/**
 * Created by Tuyj on 2017/10/30.
 *
 * Modified by will@1dao2.com on 2017/10/31
 * 1) implements init view
 * 2) add show course table view
 */

public class HomeFragment extends BaseFragment {

    public static final String TAG = "HomeFragment";
    public static HomeFragment mHomeFragment;

    private View view;

    public static HomeFragment getInstance() {
        if (mHomeFragment == null) {
            mHomeFragment = new HomeFragment();
        }
        return mHomeFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = View.inflate(getActivity(), R.layout.home_fragment, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initView(view);
        return view;
    }

    public void initView(View view) {
        showCourseTable();
        //showDeviceTest();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onBackDown(int keyCode, KeyEvent event) {
        getActivity().onBackPressed();
        return super.onBackDown(keyCode, event);
    }

    // @brief Changes fragment to show course table
    public void showCourseTable() {
        CourseTableFragment fragment = CourseTableFragment.getInstance();
        showFragment(TAG, R.id.fragmentLayout, fragment, CourseTableFragment.TAG);
    }

    public void showDeviceTest() {
        DeviceTestFragment fragment = new DeviceTestFragment();
        showFragment(TAG, R.id.fragmentLayout, fragment, DeviceTestFragment.TAG);
    }
}
