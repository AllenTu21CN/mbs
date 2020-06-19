package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import com.sanbu.tools.LogUtil;
import com.sanbu.tools.ToastUtil;

import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import cn.lx.mbs.ui.MainActivity;
import cn.lx.mbs.R;
import cn.lx.mbs.ui.model.VideoSourcesDataModel;
import cn.sanbu.avalon.media.CameraHelper;

import java.util.List;

public class VideoSourcesManageDialog extends BaseDialog {

    private static final String TAG = VideoSourcesManageDialog.class.getSimpleName();

    private final String[] ALL_TAGS = new String[] {
            "local_camera", "remote_camera", "rtsp", "rtmp", "file" };

    private View mView;
    private ListView mSourcesListView;
    private Button mAddSourceButton;
    private Button mDeleteSourceButton;

    private VideoSourcesDataModel mVideoSourcesDataModel;
    private VideoSourcesListViewAdapter mSourcesListAdapter;
    private String[] mLocalCameraIdList;
    private Size[] mLocalCameraCaptureSizeList;
    private int mCurrentSourceIndex = -1;

    private TextView mTypeTextView;
    private EditText mAliasEditText;
    private Spinner mLocalCameraIdSpinner;
    private Spinner mLocalCameraCaptureSizeSpinner;
    private EditText mRemoteCameraHostEditText;
    private EditText mRemoteCameraPortEditText;
    private EditText mRtspUrlEditText;
    private SwitchButton mRtspUseTCPSwitchButton;
    private EditText mRtspExtraOptionsEditText;
    private EditText mRtmpUrlEditText;
    private EditText mFilePathEditText;
    private SwitchButton mFileLoopSwitchButton;

    private VideoSourceAddDialog mAddSourceDialog;
    private VideoSourceDeleteDialog mDeleteSourceDialog;

    private Button mSaveButton;
    private Button mLoadButton;

    private VideoSourcesArea mParent;
    private int mEntryId;

    public VideoSourcesManageDialog(Context context, int width, int height,
                                    VideoSourcesArea parent, int entryId) {
        super(context, width, height);
        mParent = parent;
        mEntryId = entryId;

        mVideoSourcesDataModel = ((MainActivity)context).getVideoSourcesDataModel();

        setTitle("Sources");
        mView = mInflater.inflate(R.layout.dialog_video_source_manager, null);
        setContent(mView);
        Utils.adjustAll((ViewGroup) mView);

        mSourcesListView = mView.findViewById(R.id.sources_list);
        mSourcesListAdapter = new VideoSourcesListViewAdapter(context, mVideoSourcesDataModel);
        mSourcesListView.setAdapter(mSourcesListAdapter);
        mSourcesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LogUtil.i("VideoSourcesManageDialo", "Item index " + i + " clicked!");
                // Clear and set highlight
                for (int n = 0; n < adapterView.getChildCount(); n++) {
                    adapterView.getChildAt(n).findViewById(R.id.layout).setBackground(
                            mContext.getDrawable(R.drawable.listview_item_normal_bg)
                    );
                }
                view.findViewById(R.id.layout).setBackground(mContext.getDrawable(R.drawable.listview_item_highlight_bg));

                mSourcesListAdapter.setCurrentIndex(i);
                updateDetailsPanel(i);
            }
        });

        mAddSourceButton = mView.findViewById(R.id.add_pip_button);
        mAddSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAddSourceDialog == null) {
                    mAddSourceDialog = new VideoSourceAddDialog(mContext,
                            Utils.PX(600), Utils.PX(300),
                            VideoSourcesManageDialog.this);
                }

                if (mContext instanceof MainActivity) {
                    MainActivity ma = (MainActivity) mContext;
                    mAddSourceDialog.reset();
                    mAddSourceDialog.showAtLocation(ma.findViewById(R.id.main_layout),
                            Gravity.CENTER, 0, 0);
                }
            }
        });

        mDeleteSourceButton = mView.findViewById(R.id.del_button);
        mDeleteSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VideoSourcesDataModel.VideoSourceConfig item = mVideoSourcesDataModel.getItem(mCurrentSourceIndex);
                if (item == null) {
                    return;
                }

                if (mDeleteSourceDialog == null) {
                    mDeleteSourceDialog = new VideoSourceDeleteDialog(mContext,
                            Utils.PX(550), Utils.PX(300),
                            VideoSourcesManageDialog.this);

                    mDeleteSourceDialog.setOnYesListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mVideoSourcesDataModel.remove(mCurrentSourceIndex);
                            mSourcesListAdapter.notifyDataSetChanged();
                            setSourceListSelection(0);
                        }
                    });
                }


                String msg = "Are you sure you want to delete " + item.alias + "?";
                mDeleteSourceDialog.setMessage(msg);

                if (mContext instanceof MainActivity) {
                    MainActivity ma = (MainActivity) mContext;
                    mDeleteSourceDialog.showAtLocation(ma.findViewById(R.id.main_layout),
                            Gravity.CENTER, 0, 0);
                }
            }
        });

        mTypeTextView = mView.findViewById(R.id.type);
        mAliasEditText = mView.findViewById(R.id.alias);
        mLocalCameraIdSpinner = mView.findViewById(R.id.local_camera_id);
        mLocalCameraCaptureSizeSpinner = mView.findViewById(R.id.local_camera_capture_size);
        mRemoteCameraHostEditText = mView.findViewById(R.id.remote_camera_host);
        mRemoteCameraPortEditText = mView.findViewById(R.id.remote_camera_port);
        mRtspUrlEditText = mView.findViewById(R.id.rtsp_url);
        mRtspUseTCPSwitchButton = mView.findViewById(R.id.rtsp_use_tcp);
        mRtspExtraOptionsEditText = mView.findViewById(R.id.rtsp_extra_options);
        mRtmpUrlEditText = mView.findViewById(R.id.rtmp_url);
        mFilePathEditText = mView.findViewById(R.id.file_path);
        mFileLoopSwitchButton = mView.findViewById(R.id.file_loop);

        mSaveButton = mView.findViewById(R.id.save);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        mLoadButton = mView.findViewById(R.id.load);
        mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoSourcesDataModel.VideoSourceConfig item = mVideoSourcesDataModel.getItem(mCurrentSourceIndex);
                if (item == null) {
                    ToastUtil.show("Please select one", false);
                    return;
                }

                if (mParent.loadSource(mEntryId, item))
                    dismiss();
                else
                    ToastUtil.show("Load failed", false);
            }
        });

        // Initialize local camera id spinner
        mLocalCameraIdList = CameraHelper.getInstance().getCameraIdList();
        SimpleArrayAdapter<String> localCameraIdAdapter = new SimpleArrayAdapter<>(
                mContext,
                mLocalCameraIdList);
        localCameraIdAdapter.setTextSize(Utils.PX(28));
        mLocalCameraIdSpinner.setAdapter(localCameraIdAdapter);
        mLocalCameraIdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLocalCameraCaptureSizeSpinner(mLocalCameraIdList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateLocalCameraCaptureSizeSpinner(null);
            }
        });

        setSourceListSelection(0);
    }

    public void addSourceConfig(VideoSourcesDataModel.VideoSourceConfig config) {
        mVideoSourcesDataModel.add(config);

        // Update ListView
        mSourcesListAdapter.notifyDataSetChanged();
        setSourceListSelection(mVideoSourcesDataModel.size() - 1);

        // Save to preferenece
        save();
    }

    private void setSourceListSelection(int index) {
        if (0 <= index && index < mVideoSourcesDataModel.size()) {
            mSourcesListView.performItemClick(
                    mSourcesListView.getAdapter().getView(index, null, null),
                    index, index);
        } else {
            mSourcesListView.setSelection(-1);
            updateDetailsPanel(-1);
        }
    }

    private void hideAllDetails() {
        for (String tag : ALL_TAGS) {
            List<View> views = Utils.getViewsByTag((ViewGroup)mView, tag);
            for (View v : views) {
                v.setVisibility(View.GONE);
            }
        }
    }

    private void updateLocalCameraCaptureSizeSpinner(String selectedCameraId) {
        if (selectedCameraId == null) {
            mLocalCameraCaptureSizeSpinner.setAdapter(null);
            return;
        }

        CameraCharacteristics characteristics = CameraHelper.getInstance().getCameraCharacteristics(selectedCameraId);
        StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //LogUtil.d(TAG, configs.toString());
        mLocalCameraCaptureSizeList = configs.getOutputSizes(SurfaceTexture.class);
        SimpleArrayAdapter<Size> captureSizeAdapter = new SimpleArrayAdapter<>(
                mContext,
                mLocalCameraCaptureSizeList);
        captureSizeAdapter.setTextSize(Utils.PX(28));
        mLocalCameraCaptureSizeSpinner.setAdapter(captureSizeAdapter);

        // Set capture size selection here
        VideoSourcesDataModel.VideoSourceConfig item = mVideoSourcesDataModel.getItem(mCurrentSourceIndex);
        if (item != null) {
            for (int i = 0; i < mLocalCameraCaptureSizeList.length; i++) {
                Size s = mLocalCameraCaptureSizeList[i];
                if (s.getWidth() == item.localCameraConfig.captureWidth
                        && s.getHeight() == item.localCameraConfig.captureHeight) {
                    mLocalCameraCaptureSizeSpinner.setSelection(i, true);
                    break;
                }
            }
        }
    }

    private void updateDetailsPanel(int index) {
        mCurrentSourceIndex = index;
        VideoSourcesDataModel.VideoSourceConfig item = mVideoSourcesDataModel.getItem(index);
        if (item == null) {
            mTypeTextView.setText("");
            mAliasEditText.setText("");
            hideAllDetails();

            return;
        }

        String typeStr;
        String viewTag;
        switch (item.type) {
            case VideoSourcesDataModel.VideoSourceConfig.TYPE_LOCAL_CAMERA :
                typeStr = "Local Camera";
                viewTag = "local_camera";
                // Set camera id selection
                for (int i = 0; i < mLocalCameraIdList.length; i++) {
                    if (mLocalCameraIdList[i].equals(item.localCameraConfig.cameraId)) {
                        mLocalCameraIdSpinner.setSelection(i, true);
                        break;
                    }
                }

                // Set capture size selection in cameraIdSpinner onItemSelectedListener
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_REMOTE_CAMERA :
                typeStr = "Remote Camera";
                viewTag = "remote_camera";
                mRemoteCameraHostEditText.setText(item.remoteCameraConfig.host);
                mRemoteCameraPortEditText.setText(String.valueOf(item.remoteCameraConfig.port));
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_RTSP :
                typeStr = "RTSP Source";
                viewTag = "rtsp";
                mRtspUrlEditText.setText(item.rtspConfig.url);
                mRtspUseTCPSwitchButton.setChecked(item.rtspConfig.useTcp);
                mRtspExtraOptionsEditText.setText(item.rtspConfig.extraOptions);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_RTMP :
                typeStr = "RTMP Source";
                viewTag = "rtmp";
                mRtmpUrlEditText.setText(item.rtmpConfig.url);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_FILE :
                typeStr = "Media File";
                viewTag = "file";
                mFilePathEditText.setText(item.fileConfig.path);
                mFileLoopSwitchButton.setChecked(item.fileConfig.loop);
                break;

            default :
                typeStr = "";
                viewTag = "";
                break;
        }

        if (viewTag.isEmpty()) {
            return;
        }

        mTypeTextView.setText(typeStr);
        mAliasEditText.setText(item.alias);

        // Hide all
        hideAllDetails();

        // Show current type
        List<View> views = Utils.getViewsByTag((ViewGroup)mView, viewTag);
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    private void save() {
        // TODO:
    }
}
