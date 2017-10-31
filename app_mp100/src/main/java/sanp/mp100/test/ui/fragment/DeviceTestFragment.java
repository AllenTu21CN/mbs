package sanp.mp100.test.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import sanp.avalon.libs.base.utils.ExecutorThreadUtil;
import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.test.ui.adapter.DeviceTestAdapter;
import sanp.mp100.ui.fragment.BaseFragment;
import sanp.mp100.test.ui.view.DeviceTestGridView;
import sanp.mp100.test.utils.ProductionTesting;

/**
 * Created by zhangxd on 2017/7/14.
 */

public class DeviceTestFragment extends BaseFragment {

    static public final boolean Enabled = true;

    public static final String TAG = "DeviceTestFragment";

    private View mView;

    private DeviceTestGridView mTestGridView;

    private DeviceTestAdapter mTestAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTestAdapter = new DeviceTestAdapter(getActivity());
        ProductionTesting.getInstance().entryTesting();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.device_test_view, container, false);
        initView();
        return mView;
    }

    private void initView() {
        mTestGridView = (DeviceTestGridView) mView.findViewById(R.id.gridview);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTestGridView.getLayoutParams();
        LogManager.d(TAG, "leftMargin: " + lp.leftMargin);
        mTestGridView.setFocusable(true);
        mTestGridView.setAdapter(mTestAdapter);
        mTestGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mTestGridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mTestAdapter.setSelectIndex(i);
                mTestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mTestGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                LogManager.d(TAG, "onItemClick position " + position + "viewholder: " + view.getTag());
                DeviceTestResultFragment mResultFragment = new DeviceTestResultFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("item", position);
                mResultFragment.setArguments(bundle);
                showFragmentAddName(TAG, R.id.fragmentLayout, mResultFragment, DeviceTestResultFragment.TAG);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        ExecutorThreadUtil.submit(new Runnable() {
            @Override
            public void run() {
                //该操作有一定的耗时，不进行线程处理会卡顿
                ProductionTesting.getInstance().exitTesting();
            }
        });
        super.onDestroy();
    }


}