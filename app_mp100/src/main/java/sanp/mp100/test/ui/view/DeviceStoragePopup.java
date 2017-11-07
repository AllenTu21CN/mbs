package sanp.mp100.test.ui.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import sanp.tools.utils.ScreenUtils;

import sanp.mp100.R;
import sanp.mp100.test.ui.utils.StorageUtils;
import sanp.tools.utils.UsbInfo;

/**
 * Created by zhangxd on 2017/7/22.
 */

public class DeviceStoragePopup extends PopupWindow {

    private Context mContext;

    private View mView;

    private LinearLayout rootView;

    public DeviceStoragePopup(Context context) {
        mContext = context;
        int width = ScreenUtils.getScreenWidth(mContext) / 2;
        int height = ScreenUtils.getScreenWidth(mContext) / 3;
        setWidth(width);
        setHeight(height);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.device_storage_view, null);
        rootView = (LinearLayout) mView.findViewById(R.id.storage_main);
        setContentView(mView);
        showAtLocation(mView, Gravity.CENTER, 0, 0);
    }

    public void queryStrorage(boolean isInternalStorage) {
        if (isInternalStorage) {
            String sdname = StorageUtils.getSDName();
            String romStorageName = StorageUtils.getRomName();
            long sdAvailableSize = StorageUtils.getSDAvailableSize();
            long sdTotalSize = StorageUtils.getSDTotalSize();
            long romStorageAvaSize = StorageUtils.getRomAvailableSize();
            long romStorageTotalSize = StorageUtils.getRomTotalSize();
            StorageViewItem item1 = new StorageViewItem(mContext);
            String sdName = mContext.getString(R.string.sd_name);
            item1.setStorageInfo(sdName, sdname, formatSize(sdAvailableSize), formatSize(sdTotalSize));
            rootView.addView(item1);

            StorageViewItem item2 = new StorageViewItem(mContext);
            String romName = mContext.getString(R.string.rom_name);
            item2.setStorageInfo(romName, romStorageName, formatSize(romStorageAvaSize), formatSize(romStorageTotalSize));
            rootView.addView(item2, getLayoutParams());

        } else {
            // List<UsbInfo> mList = MyApplication.mUNumList; //TODO
            List<UsbInfo> mList = new ArrayList<>();
            String uDiskName = mContext.getString(R.string.u_disk_name);
            for (UsbInfo info : mList) {
                String name = info.getName();
                String remainSize = info.getFreed();
                String totalSize = info.getSize();
                StorageViewItem storageViewItem = new StorageViewItem(mContext);
                storageViewItem.setStorageInfo(uDiskName, name, remainSize, totalSize);
                rootView.addView(storageViewItem, getLayoutParams());
            }
        }
    }

    /**
     * @param size 容量大小
     * @return 格式化后的容量大小
     */
    private String formatSize(long size) {
        DecimalFormat df = new DecimalFormat("0.0");
        StringBuilder formatBuilder = new StringBuilder();
        long locSize = size;
        String formatSize = "";
        String resultSize = "";
        if (size >= 1024) {
            float mSize = (float) locSize / 1024;
            formatSize = df.format(mSize);
            resultSize = formatBuilder.append(formatSize).append(" ").append("GB").toString();
        } else {
            resultSize = formatBuilder.append(locSize).append(" ").append("MB").toString();
        }
        return resultSize;
    }

    private LinearLayout.LayoutParams getLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) mContext.getResources().getDimension(R.dimen.device_storage_item_magrintop);
        return lp;
    }
}
