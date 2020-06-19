package cn.lx.mbs.ui.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cn.lx.mbs.R;

public class HintDialog extends BaseDialog implements View.OnClickListener {

    private static final String TAG = HintDialog.class.getSimpleName();

    public interface Callback {
        boolean onConfirmed();
    }

    public interface DismissCallback {
        void onDismiss(boolean confirmed);
    }

    private View mView;
    private TextView mMessage;
    private Button mConfirm;
    private Button mCancel;

    private Callback mCallback;
    private DismissCallback mDismissCallback;

    public HintDialog(Context context, int width, int height,
                      String title, String message) {
        this(context, width, height, title, message, false, null);
    }

    public HintDialog(Context context, int width, int height,
                      String title, String message,
                      boolean selectable, Callback callback) {
        super(context, width, height);
        mCallback = callback;

        setTitle(title);
        mView = mInflater.inflate(R.layout.dialog_hint, null);
        setContent(mView);

        mMessage = mView.findViewById(R.id.message);
        mConfirm = mView.findViewById(R.id.confirm);
        mCancel = mView.findViewById(R.id.cancel);

        mMessage.setText(message);

        if (selectable) {
            mConfirm.setOnClickListener(this);
            mCancel.setOnClickListener(this);
        } else {
            mConfirm.setVisibility(View.GONE);
            mCancel.setVisibility(View.GONE);
        }
    }

    public HintDialog setDismissCallback(DismissCallback dismissCallback) {
        mDismissCallback = dismissCallback;
        return this;
    }

    public void show(View parent) {
        showAtLocation(parent, Gravity.CENTER, 0, 0);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm || v.getId() == R.id.cancel) {

            boolean dismiss = true;
            boolean confirmed = false;
            if (mCallback != null && v.getId() == R.id.confirm) {
                confirmed = true;
                dismiss = mCallback.onConfirmed();
            }

            if (dismiss) {
                if (mDismissCallback != null)
                    mDismissCallback.onDismiss(confirmed);
                dismiss();
            }
        } else {
            throw new RuntimeException("unknown action " + v);
        }
    }
}
