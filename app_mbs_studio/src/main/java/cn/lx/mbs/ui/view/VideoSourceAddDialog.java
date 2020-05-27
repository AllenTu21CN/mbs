package cn.lx.mbs.ui.view;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import cn.lx.mbs.R;
import mbs.studio.model.VideoSourcesDataModel;

public class VideoSourceAddDialog extends BaseDialog {

    private static final String TAG = VideoSourceAddDialog.class.getSimpleName();

    private static final int[] TYPE_LIST_VALUE = new int[] {
            VideoSourcesDataModel.VideoSourceConfig.TYPE_LOCAL_CAMERA,
            VideoSourcesDataModel.VideoSourceConfig.TYPE_REMOTE_CAMERA,
            VideoSourcesDataModel.VideoSourceConfig.TYPE_RTSP,
            VideoSourcesDataModel.VideoSourceConfig.TYPE_RTMP,
            VideoSourcesDataModel.VideoSourceConfig.TYPE_FILE
    };

    private static final String[] TYPE_LIST_STRING = new String[] {
            "Local Camera",
            "Remote Camera",
            "RTSP Source",
            "RTMP Source",
            "File Source"
    };

    private VideoSourcesManageDialog mParent;
    private View mView;
    private EditText mAliasEditText;
    private Spinner mTypeSpinner;
    private Button mAddButton;

    public VideoSourceAddDialog(Context context, int width, int height, VideoSourcesManageDialog parent) {
        super(context, width, height);
        mParent = parent;

        setTitle("Add Source");
        mView = mInflater.inflate(R.layout.dialog_video_source_add, null);
        setContent(mView);

        mAliasEditText = mView.findViewById(R.id.alias);
        mTypeSpinner = mView.findViewById(R.id.type);
        mAddButton = mView.findViewById(R.id.add_pip_button);

        SimpleArrayAdapter<String> typeAdapter = new SimpleArrayAdapter<>(
                mContext,
                TYPE_LIST_STRING);
        typeAdapter.setTextSize(Utils.PX(28));
        mTypeSpinner.setAdapter(typeAdapter);

        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoSourcesDataModel.VideoSourceConfig config = new VideoSourcesDataModel.VideoSourceConfig();
                config.type = TYPE_LIST_VALUE[mTypeSpinner.getSelectedItemPosition()];
                config.alias = mAliasEditText.getText().toString();
                mParent.addSourceConfig(config);

                dismiss();
            }
        });
    }

    public void reset() {
        mAliasEditText.setText("");
        mTypeSpinner.setSelection(0);
    }
}
