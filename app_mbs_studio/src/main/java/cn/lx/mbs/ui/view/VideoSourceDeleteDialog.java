package mbs.studio.view;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.studio.R;

public class VideoSourceDeleteDialog extends BaseDialog {

    private static final String TAG = VideoSourceDeleteDialog.class.getSimpleName();

    private VideoSourcesManageDialog mParent;
    private View mView;
    private TextView mMessageTextView;
    private Button mYesButton;
    private Button mNoButton;

    private View.OnClickListener mOnYesListener;
    private View.OnClickListener mOnNoListener;

    public VideoSourceDeleteDialog(Context context, int width, int height, VideoSourcesManageDialog parent) {
        super(context, width, height);
        mParent = parent;

        setTitle("Delete Source");
        mView = mInflater.inflate(R.layout.dialog_video_source_delete, null);
        setContent(mView);

        mMessageTextView = mView.findViewById(R.id.message);
        mYesButton = mView.findViewById(R.id.yes_button);
        mNoButton = mView.findViewById(R.id.no_button);

        mYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnYesListener != null) {
                    mOnYesListener.onClick(v);
                }

                dismiss();
            }
        });

        mNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnNoListener != null) {
                    mOnNoListener.onClick(v);
                }

                dismiss();
            }
        });
    }

    public void setMessage(String message) {
        mMessageTextView.setText(message);
    }

    public void setOnYesListener(View.OnClickListener listener) {
        mOnYesListener = listener;
    }

    public void setOnNoListener(View.OnClickListener listener) {
        mOnNoListener = listener;
    }
}
