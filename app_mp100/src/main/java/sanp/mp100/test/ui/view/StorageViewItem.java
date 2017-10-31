package sanp.mp100.test.ui.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import sanp.mp100.R;

/**
 * Created by zhangxd on 2017/7/24.
 */

public class StorageViewItem extends LinearLayout {

    private TextView mNameType;

    private TextView mName;

    private TextView mRemainSize;

    private TextView mTotalSize;

    private View mView;

    private static final String TAG = "UDiskStorage";

    public StorageViewItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StorageViewItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StorageViewItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public StorageViewItem(Context context) {
        super(context);
        mView = LayoutInflater.from(context).inflate(R.layout.usb_disk_storage_item, null);
        setOrientation(VERTICAL);
        addView(mView);
        initView();
    }

    private void initView() {
        mNameType=(TextView)mView.findViewById(R.id.disk_name);
        mName = (TextView) mView.findViewById(R.id.storage_name);
        mRemainSize = (TextView) mView.findViewById(R.id.available_size);
        mTotalSize = (TextView) mView.findViewById(R.id.total_size);
    }

    public void setStorageInfo(String nameType,String name, String remainSize, String totalSize) {
        mNameType.setText(nameType);
        mName.setText(name);
        mRemainSize.setText(remainSize);
        mTotalSize.setText(totalSize);
    }


}
