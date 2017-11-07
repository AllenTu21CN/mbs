package sanp.mp100.ui.fragment;

import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.ToastManager;
import sanp.mp100.R;
import sanp.mp100.integration.RBUtil;

/**
 * Created by Tuyj on 2017/11/3.
 */

public class LessonMenuFragment extends BaseFragment implements View.OnClickListener, LessonHomeFragment.ChildFragment {
    public static final String TAG = "LessonMenuFragment";

    public static LessonMenuFragment mClassMenuFragment = null;

    public static LessonMenuFragment getInstance() {
        synchronized (LessonMenuFragment.class) {
            if (mClassMenuFragment == null)
                mClassMenuFragment = new LessonMenuFragment();
            return mClassMenuFragment;
        }
    }

    private View mMenuView;

    private Button mBtnSwitchClass;
    private Button mBtnSwitchContent;
    private Button mBtnSwitchSubScreen;

    private LessonHomeFragment mParent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMenuView = View.inflate(getActivity(), R.layout.lesson_memu_view, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initData();
        initView();
        return mMenuView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_switch_class:
                mParent.switchLesson();
                enableBtn(mBtnSwitchClass, false);
                break;
            case R.id.button_switch_content:
                showContentMenu();
                break;
            case R.id.button_switch_subscreen:
                showSubScreenMenu();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroyView() {
        mParent.setChild(null);
        super.onDestroyView();
    }

    private void initData() {
        mParent = LessonHomeFragment.getInstance();
        mParent.setChild(this);
    }

    private void initView() {
        mBtnSwitchClass = mMenuView.findViewById(R.id.button_switch_class);
        mBtnSwitchContent = mMenuView.findViewById(R.id.button_switch_content);
        mBtnSwitchSubScreen = mMenuView.findViewById(R.id.button_switch_subscreen);

        mBtnSwitchClass.setOnClickListener(this);
        mBtnSwitchContent.setOnClickListener(this);
        mBtnSwitchSubScreen.setOnClickListener(this);

        updateMenuItemsAsStatus();
    }

    private void updateMenuItemsAsStatus() {
        LessonHomeFragment.State state = mParent.state();
        if(state == LessonHomeFragment.State.None) {
            mBtnSwitchClass.setText("上课");
            enableBtn(mBtnSwitchClass, true);
        } else if(state == LessonHomeFragment.State.Started) {
            mBtnSwitchClass.setText("下课");
            enableBtn(mBtnSwitchClass, true);
        } else if(state == LessonHomeFragment.State.Stopped || state == LessonHomeFragment.State.Stopping) {
            mBtnSwitchClass.setText("上课");
            enableBtn(mBtnSwitchClass, false);
        } else if(state == LessonHomeFragment.State.Starting) {
            mBtnSwitchClass.setText("下课");
            enableBtn(mBtnSwitchClass, false);
        }
    }

    @Override
    public void onStatusChanged() {
        updateMenuItemsAsStatus();
    }

    private void enableBtn(Button btn, boolean able) {
        if(able) {
            btn.setTextColor(getResources().getColor(R.color.white));
        } else {
            btn.setTextColor(getResources().getColor(R.color.word_gray));
        }
        btn.setEnabled(able);
    }

    private void showContentMenu() {
        DynamicMenuFragment.ItemGroup itemGroup = new DynamicMenuFragment.ItemGroup();
        itemGroup.name = null; // has no title name

        RBUtil rb = RBUtil.getInstance();
        RBUtil.Scene scene = rb.currentScene();
        List<String> names = rb.getSceneContentNames();
        for(String name: names) {
            itemGroup.items.add(new DynamicMenuFragment.Item(name, ((v) -> {
                rb.setScene(scene, name);
            })));
        }

        List<DynamicMenuFragment.ItemGroup> itemGroups = new ArrayList<>();
        itemGroups.add(itemGroup);

        showFragment(TAG, R.id.fragmentLayout, DynamicMenuFragment.getInstance(itemGroups), DynamicMenuFragment.TAG);
    }

    private void showSubScreenMenu() {
        List<DynamicMenuFragment.ItemGroup> itemGroups = new ArrayList<>();

        RBUtil rb = RBUtil.getInstance();
        RBUtil.Content content = rb.currentContent();

        int subscreenCnt = content.roleCandidates.size();
        Integer[] idxs = content.roleCandidates.keySet().toArray(new Integer[subscreenCnt]);
        List<Integer> indexs = java.util.Arrays.asList(idxs);
        Collections.sort(indexs);
        for(int index: indexs) {
            List<RBUtil.Role> roles = content.roleCandidates.get(index);
            DynamicMenuFragment.ItemGroup itemGroup = new DynamicMenuFragment.ItemGroup();
            if(index == 0) {
                itemGroup.name = "主分屏";
            } else {
                if(subscreenCnt == 2) {
                    itemGroup.name = "子分屏";
                } else {
                    itemGroup.name = "第" + index + "子分屏";
                }
            }
            for(RBUtil.Role role: roles) {
                itemGroup.items.add(new DynamicMenuFragment.Item(role.toString(), ((v) -> {
                    int ret = rb.selectSubScreenRole(index, role);
                    if(ret != 0) {
                        if(ret == -1) {
                            ToastManager.showToast(mContext, "该角色尚未绑定任何源");
                        } else {
                            ToastManager.showToast(mContext, "切换角色(" + role.toString() + ")失败: " + ret);
                        }
                    }
                })));
            }
            itemGroups.add(itemGroup);
        }

        showFragment(TAG, R.id.fragmentLayout, DynamicMenuFragment.getInstance(itemGroups), DynamicMenuFragment.TAG);
    }
}
