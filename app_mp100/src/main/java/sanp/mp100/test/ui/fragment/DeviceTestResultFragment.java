package sanp.mp100.test.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.test.utils.ProductionTesting;
import sanp.mp100.ui.fragment.BaseFragment;
import sanp.mp100.test.ui.view.DeviceNetWorkPopup;
import sanp.mp100.test.ui.view.DeviceStoragePopup;
import sanp.mp100.test.ui.view.MediaCodingPopup;
import sanp.mp100.test.ui.view.MediaInputTestPopup;

/**
 * Created by zhangxd on 2017/7/17.
 */

public class DeviceTestResultFragment extends BaseFragment {

    public static final String TAG = "DeviceTestResultFragment";

    private View mView;

    private MediaCodingPopup multiMediaPopup;

    private MediaInputTestPopup mediaInputTestPopup;

    private DeviceStoragePopup mStoragePopup;

    private DeviceNetWorkPopup mNetWorkPopup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.d(TAG, "onCrete()");
        multiMediaPopup = new MediaCodingPopup(mContext);
        mediaInputTestPopup = new MediaInputTestPopup(mContext);
    }

    /**
     * 处理点击的具体测试项
     */
    private void parseArguments() {
        Bundle bundle = getArguments();
        int position = bundle.getInt("item");
        setItemSelected(position);
    }

    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.device_test_result, container, false);
        parseArguments();
        return mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        LogManager.d(TAG, "onDestroy()");
        if (multiMediaPopup != null) {
            multiMediaPopup.dismissPopup();
        }
        mediaInputTestPopup.dismissPopup();
        if (mStoragePopup != null) {
            mStoragePopup.dismiss();
        }
        if (mNetWorkPopup != null) {
            mNetWorkPopup.dismissPopup();
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    /**
     * 设备检查的参数配置
     *
     * @param item 具体的某个位置
     */
    private void setItemSelected(int item) {
        switch (item) {
            case 0:
                mediaInputTestPopup.startMediaInputTest(ProductionTesting.CAPTURE_DEVICE_CAMERA0);
                break;
            case 1:
                mediaInputTestPopup.startMediaInputTest(ProductionTesting.CAPTURE_DEVICE_CAMERA1);
                break;
            case 2:
                multiMediaPopup.startEnCodingTest();
                break;
            case 3:
                showFragment(TAG, R.id.fragmentLayout, new CodecChoiceFragment(), CodecChoiceFragment.TAG);
                break;
            default:
                break;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            multiMediaPopup.showPopup();
        }
        super.onHiddenChanged(hidden);
    }


}