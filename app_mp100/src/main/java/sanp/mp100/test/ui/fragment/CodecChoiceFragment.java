package sanp.mp100.test.ui.fragment;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sanp.test.MediaTesting;
import sanp.tools.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.test.ui.view.MediaCodingPopup;
import sanp.mp100.ui.fragment.BaseFragment;

/**
 * Created by zhangxd on 2017/8/14.
 */

public class CodecChoiceFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener
        , View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    public static final String TAG = "CodecChoiceFragment";

    private View mView;

    private CheckBox mRightCheckbox;

    private CheckBox mLeftcheckBox;

    private Spinner mDecodingSpinner;

    private Spinner mEnCodingSpinner1;

    private Spinner mEnCodingSpinner2;

    private Spinner mEnCodingSpinner3;

    private Spinner mEnCodingSpinner4;

    private Button mConfirm;

    private Button mCancel;

    private ArrayAdapter<CharSequence> mArrayAdapter;

    private List<Spinner> mSPinnerList = new ArrayList<>();

    private static Map<Integer, MediaTesting.VideoFormat> mEncodingFormatMap = new HashMap<>();

    private int decodingNum = 0;

    private RadioGroup mRadioGroup;

//    private LinearLayout mCaptureLayout;
//
//    private LinearLayout mDecodingNumLayout;
//
//    private LinearLayout

    private MediaTesting.VideoFormat decodingFormat = MediaTesting.DEFAULT_1080P_FORMAT;

    static {
        mEncodingFormatMap.put(0, MediaTesting.DEFAULT_1080P_FORMAT);
        mEncodingFormatMap.put(1, MediaTesting.DEFAULT_720P_FORMAT);
        mEncodingFormatMap.put(2, MediaTesting.DEFAULT_480P_FORMAT);
        mEncodingFormatMap.put(3, MediaTesting.VideoFormat.FORMAT_1080P_1M_10FPS);
        mEncodingFormatMap.put(4, MediaTesting.VideoFormat.FORMAT_720P_1M_10FPS);
    }

    private static Map<Integer, MediaTesting.VideoFormat> mSelectedEncodingMap = new HashMap<>();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogManager.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mArrayAdapter = ArrayAdapter.createFromResource(mContext, R.array.encoding_px, android.R.layout.simple_spinner_item);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LogManager.d(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.codec_choice, container, false);
        initView();
        initDecodingSpinner();
        initEndoingSpinner();
        return mView;
    }

    private void initView() {
        mLeftcheckBox = (CheckBox) mView.findViewById(R.id.checkbox_zero);
        mRightCheckbox = (CheckBox) mView.findViewById(R.id.checkbox_one);
        mLeftcheckBox.setOnCheckedChangeListener(this);
        mRightCheckbox.setOnCheckedChangeListener(this);
        mDecodingSpinner = (Spinner) mView.findViewById(R.id.decoding_spinner);
        mEnCodingSpinner1 = (Spinner) mView.findViewById(R.id.endcoing_spinner_one);
        mEnCodingSpinner2 = (Spinner) mView.findViewById(R.id.endcoing_spinner_two);
        mEnCodingSpinner3 = (Spinner) mView.findViewById(R.id.endcoing_spinner_three);
        mEnCodingSpinner4 = (Spinner) mView.findViewById(R.id.endcoing_spinner_four);
        mSPinnerList.add(mEnCodingSpinner1);
        mSPinnerList.add(mEnCodingSpinner2);
        mSPinnerList.add(mEnCodingSpinner3);
        mSPinnerList.add(mEnCodingSpinner4);
        mConfirm = (Button) mView.findViewById(R.id.choice_ok);
        mConfirm.setOnClickListener(this);
        mCancel = (Button) mView.findViewById(R.id.choice_cancel);
        mCancel.setOnClickListener(this);
        mRadioGroup = (RadioGroup) mView.findViewById(R.id.radio_group);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    private void initSize(){

    }


    private void initDecodingSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, R.array.docoing_num, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDecodingSpinner.setAdapter(adapter);
        mDecodingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                decodingNum = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initEndoingSpinner() {
        ArrayAdapter<CharSequence> mArrayAdapter = ArrayAdapter.createFromResource(mContext, R.array.encoding_px, android.R.layout.simple_spinner_item);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Spinner spinner : mSPinnerList) {
            spinner.setAdapter(mArrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        return;
                    }
                    MediaTesting.VideoFormat format = mEncodingFormatMap.get(position - 1);
                    switch (parent.getId()) {
                        case R.id.endcoing_spinner_one:
                            mSelectedEncodingMap.put(0, format);
                            break;
                        case R.id.endcoing_spinner_two:
                            mSelectedEncodingMap.put(1, format);
                            break;
                        case R.id.endcoing_spinner_three:
                            mSelectedEncodingMap.put(2, format);
                            break;
                        case R.id.endcoing_spinner_four:
                            mSelectedEncodingMap.put(3, format);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }


    }


    @Override
    public void onResume() {
        LogManager.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            popFrament(DeviceTestResultFragment.TAG, 1);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onkeyUp(int keyCode, KeyEvent event) {
        return super.onkeyUp(keyCode, event);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroy() {
        LogManager.d(TAG, "onDestroy");
        mSPinnerList.clear();
        mSPinnerList = null;
        mSelectedEncodingMap.clear();
        super.onDestroy();
    }


    @Override
    public void onDestroyView() {
        LogManager.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choice_ok:
                startMediaCompTest();
                popFrament(DeviceTestResultFragment.TAG, 0);
                break;
            case R.id.choice_cancel:
                popFrament(DeviceTestResultFragment.TAG, 1);
                break;
            default:
                break;
        }

    }

    private void startMediaCompTest() {
        int captureFormat = 0;
        if (mLeftcheckBox.isChecked() && mRightCheckbox.isChecked()) {
            captureFormat = MediaTesting.CAPTURE_DEVICE_BOTH;
        } else if (mLeftcheckBox.isChecked()) {
            captureFormat = MediaTesting.CAPTURE_DEVICE_CAMERA0;
        } else if (mRightCheckbox.isChecked()) {
            captureFormat = MediaTesting.CAPTURE_DEVICE_CAMERA1;
        } else {
            captureFormat = MediaTesting.CAPTURE_DEVICE_NONE;
        }
        List<MediaTesting.VideoFormat> mTempList = new ArrayList<>(mSelectedEncodingMap.values());
        MediaCodingPopup.startMediaCodingTest(captureFormat, decodingFormat, decodingNum, mTempList);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.high_px_radio) {
            decodingFormat = MediaTesting.DEFAULT_1080P_FORMAT;
        } else {
            decodingFormat = MediaTesting.DEFAULT_720P_FORMAT;
        }
    }
}
