package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sanp.mp100.R;

/**
 * Created by Tom on 2017/4/20.
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
        view = View.inflate(getActivity(), R.layout.video_test_view, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initView(view);
        return view;
    }

    public void initView(View view) {
        // showLessonTable();
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
}
