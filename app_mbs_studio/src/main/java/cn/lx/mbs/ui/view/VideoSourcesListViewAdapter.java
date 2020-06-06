package cn.lx.mbs.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.lx.mbs.R;
import cn.lx.mbs.ui.model.VideoSourcesDataModel;

public class VideoSourcesListViewAdapter extends BaseAdapter {

    private Context mContext;
    private VideoSourcesDataModel mDataModel;
    private static LayoutInflater mInflater = null;
    private int mCurrentIndex;

    public VideoSourcesListViewAdapter(Context context, VideoSourcesDataModel data) {
        mContext = context;
        mDataModel = data;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mDataModel.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataModel.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.dialog_video_source_listview_item, null);
        }

        LinearLayout layout = view.findViewById(R.id.layout);
        Utils.adjustPaddings(layout);

        // Highlight
        layout.setBackground(mContext.getDrawable(
                position == mCurrentIndex
                        ? R.drawable.listview_item_highlight_bg
                        : R.drawable.listview_item_normal_bg));

        TextView alias = view.findViewById(R.id.alias);
        TextView details = view.findViewById(R.id.details);

        VideoSourcesDataModel.VideoSourceConfig item = mDataModel.getItem(position);
        alias.setText(item.alias);
        Utils.adjustTextSize(alias);

        String summary;
        switch (item.type) {
            case VideoSourcesDataModel.VideoSourceConfig.TYPE_LOCAL_CAMERA :
                summary = String.format("Local CAM: #%s", item.localCameraConfig.cameraId);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_REMOTE_CAMERA :
                summary = String.format("RCAM: %s:%d", item.remoteCameraConfig.host, item.remoteCameraConfig.port);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_RTSP :
                summary = String.format("rtsp://%s", item.rtspConfig.url);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_RTMP :
                summary = String.format("rtmp://%s", item.rtmpConfig.url);
                break;

            case VideoSourcesDataModel.VideoSourceConfig.TYPE_FILE :
                summary = String.format("file://%s", item.fileConfig.path);
                break;

            default :
                summary = "N/A";
                break;
        }

        details.setText(summary);
        Utils.adjustTextSize(details);

        return view;
    }

    public void setCurrentIndex(int index) {
        mCurrentIndex = index;
    }
}
