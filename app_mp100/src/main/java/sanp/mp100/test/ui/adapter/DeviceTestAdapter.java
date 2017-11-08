package sanp.mp100.test.ui.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import sanp.tools.utils.LogManager;
import sanp.tools.utils.ScreenUtils;
import sanp.mp100.R;

/**
 * Created by zhangxd on 2017/7/14.
 */

public class DeviceTestAdapter extends BaseAdapter {

    private String[] mTestName;

    private Context mContext;

    private int selectIndex;

    public DeviceTestAdapter(Context context) {
        this.mContext = context;
        loadData();
    }

    public void setSelectIndex(int index) {
        this.selectIndex = index;
    }

    private void loadData() {
        mTestName = mContext.getResources().getStringArray(R.array.device_test_array);
    }

    @Override
    public int getCount() {
        return mTestName.length;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.test_name, null);
            holder.textView = (TextView) view.findViewById(R.id.name);
            scaleScreenSize(holder.textView);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (selectIndex == i) {
            holder.textView.setBackgroundResource(R.mipmap.bg_fang_blue_icon);
        } else {
            holder.textView.setBackgroundResource(R.mipmap.bg_fang_btm_small_icon);
        }

        holder.textView.setText(mTestName[i]);
        return view;
    }

    public class ViewHolder {
        public TextView textView;
        // public VideoCodec mVideoCodeEc;
    }

    //    class VideoCodec {
//        public MediaTesting.VideoFormat mDecodeFormat;
//        public int decodeNum;
//        public MediaTesting.VideoFormat mEncodeFormat;
//        public int encodeNum;
//
//        public VideoCodec(MediaTesting.VideoFormat decodeFormat, int decodeNum, MediaTesting.VideoFormat encodeFormat, int encodeNum) {
//            this.mDecodeFormat = decodeFormat;
//            this.decodeNum = decodeNum;
//            this.mEncodeFormat = encodeFormat;
//            this.encodeNum = encodeNum;
//        }
//    }
    private void scaleScreenSize(TextView view) {
        int screenWith = ScreenUtils.getScreenWidth(mContext);
        if (screenWith < 1920) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
            lp.height = ScreenUtils.getScreenHeight(mContext) / 9;
            float textSize = ScreenUtils.getScreenWidth(mContext) / 55;
            LogManager.d("DeviceTestFragment", "textSize: " + textSize);
            view.setLayoutParams(lp);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }
}
