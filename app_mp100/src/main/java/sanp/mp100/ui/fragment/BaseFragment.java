package sanp.mp100.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sanp.tools.db.DbSettingsManager;
import sanp.tools.utils.PreferencesUtils;
import sanp.tools.utils.ScreenUtils;
import sanp.mp100.R;

/**
 * Created by Tom on 2017/3/1.
 *
 * Modified by will@1dao2.com, 2017/10/31
 */

public abstract class BaseFragment extends Fragment {

    /*上下文*/
    public Activity mContext;

    /*配置文件存储器*/
    public PreferencesUtils mPreferenceUtils;

    public static boolean mDialogShowing = false;

    /**
     * Fragment 管理器
     */
    private static FragmentManager mFragmentManager;


    public DbSettingsManager mDbSettingsManager;

    /**
     * 获取焦点的view
     */
    public View mFocusView;

    /**
     * 字体大小
     */
    public int textSize_24;
    public int textSize_18;
    public int contentTop;

    /**
     * 字体颜色
     */
    public final int WORD_WHITE = 0xFFFFFFFF;

    public final int WORD_WHITE_GRAY = 0xFFDDDDDD;

    public static BaseFragment mCurrentFragment;

    public static List<BaseFragment> mFragmentList = new ArrayList<>();

    public String mUrl;
    public HashMap<String, String> mParams = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPreferenceUtils = PreferencesUtils.getInstance(mContext);
        mFragmentManager = getFragmentManager();
        mDbSettingsManager = new DbSettingsManager(mContext);
        textSize_24 = (int) mContext.getResources().getDimension(R.dimen.text_size_24);
        textSize_18 = (int) mContext.getResources().getDimension(R.dimen.text_size_22);
        contentTop = (int) mContext.getResources().getDimension(R.dimen.contents_top);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentFragment = this;
        mFragmentList.add(0, this);
    }

    /**
     * 添加新 Fragment
     *
     * @param oldTag   当前fragment的Tag
     * @param layout   fragment容器
     * @param fragment 新 Fragment
     * @param newTag   新 Fragment 的Tag
     */
    public void showFragment(String oldTag, int layout, BaseFragment fragment, String newTag) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (newTag.equals("CourseTableFragment TODO")) { //TODO
            ft.setCustomAnimations(R.anim.actionsheet_dialog_in, R.anim.actionsheet_dialog_out);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        if (fragment != null) {
            ft.hide(mFragmentManager.findFragmentByTag(oldTag));
        }
        ft.add(layout, fragment, newTag);
        ft.addToBackStack(null);
        ft.commit();
    }


    /**
     * 添加新 透明Fragment
     *
     * @param layout   fragment容器
     * @param fragment 新 Fragment
     * @param newTag   新 Fragment 的Tag
     */
    public void showFragmentTrans(int layout, BaseFragment fragment, String newTag) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.add(layout, fragment, newTag);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void showFragmentAddName(String oldTag, int layout, BaseFragment fragment, String newTag){
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (newTag.equals("LessonTableFragment")) {
            ft.setCustomAnimations(R.anim.actionsheet_dialog_in, R.anim.actionsheet_dialog_out);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        if (fragment != null) {
            ft.hide(mFragmentManager.findFragmentByTag(oldTag));
        }
        ft.add(layout, fragment, newTag);
        ft.addToBackStack(newTag);
        ft.commit();
    }

    /**
     *
     * @param name the simplename of fragment
     * @param flags 1 include itself,0--not
     */
    public void popFrament(String name,int flags){
       mFragmentManager.popBackStackImmediate(name,flags);
    }

    public void popFragment() {
        mFragmentManager.popBackStack();
    }

    /**
     * 获取屏幕宽度
     *
     * @return
     */
    public int getWidth() {
        return ScreenUtils.getScreenWidth(mContext);
    }

    /**
     * 获取屏幕高度
     *
     * @return
     */
    public int getHeight() {
        return ScreenUtils.getScreenHeight(mContext);
    }

    /**
     * 截获点击事件
     *
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 截获松开按键事件---------------------------------------
     */
    public boolean onkeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 截获返回事件
     *
     * @return
     */
    public boolean onBackDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getUserVisibleHint()) {
            if (mFocusView != null) {
                mFocusView.requestFocus();
                mFocusView.requestFocusFromTouch();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFragmentList.size() > 0)
            mFragmentList.remove(this);
    }

    public void setFocusView(View view) {
        mFocusView = view;
    }

    public interface OnFragmentBackListen {
        void onBackListen(Object obj);
    }
}
