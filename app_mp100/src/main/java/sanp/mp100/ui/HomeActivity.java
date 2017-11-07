package sanp.mp100.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

import sanp.tools.utils.LogManager;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.integration.RBUtil;
import sanp.mp100.test.ui.fragment.DeviceTestFragment;
import sanp.mp100.ui.fragment.BaseFragment;
import sanp.mp100.ui.fragment.HomeFragment;

import sanp.mp100.R;

/**
 * Created by Tom on 2017/10/28.
 */

public class HomeActivity extends FragmentActivity {

    private Context mContext;
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private HomeFragment mHomeFragment;
    private SurfaceView mSurfaceView;
    private SurfaceHolder.Callback mSurfaceCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_view);
        initData();
        initView();
        initFragment();
    }

    private void initData() {
        mContext = this;
    }

    public void initView() {
        mSurfaceCallback = new SurfaceHolderCallback();
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(mSurfaceCallback);
    }

    private void initFragment() {
        mHomeFragment = HomeFragment.getInstance();
        mFragmentManager = getSupportFragmentManager();
        mFragmentTransaction = mFragmentManager.beginTransaction();
    }

    @Override
    protected void onDestroy() {
        LogManager.w("!!!HomeActivity onDestroy!!!");
        BusinessPlatform.getInstance().release();
        RBUtil.getInstance().release();
        super.onDestroy();
    }

    private BaseFragment getVisibleFragment() {
        List<Fragment> fragments = mFragmentManager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    try {
                        return (BaseFragment) fragment;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        BaseFragment mFragmentManager = getVisibleFragment();
        if (mFragmentManager != null)
            mFragmentManager.onkeyUp(keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (BaseFragment.mFragmentList.size() > 0)
            if(BaseFragment.mFragmentList.get(0).onKeyDown(keyCode, event))
                return true;
        return super.onKeyDown(keyCode, event);
    }

    class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            RBUtil rb = RBUtil.getInstance();
            rb.init(mContext, holder);
            if(!DeviceTestFragment.Enabled) {
                rb.addSources();
                rb.setScene(RBUtil.Scene.Home);
            }
            BusinessPlatform.getInstance().init(mContext);

            mFragmentTransaction.add(R.id.fragmentLayout, mHomeFragment, HomeFragment.TAG);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            RBUtil.getInstance().changeSurface(holder, format, width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

}