package cn.lx.mbs.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import cn.lx.mbs.R;

import cn.lx.mbs.ui.model.SceneOverlayDataModel;

public class OverlayListViewAdapter extends BaseAdapter {

    private Context mContext;
    private SceneOverlayDataModel mDataModel;
    private static LayoutInflater mInflater = null;
    private int mCurrentIndex;

    public OverlayListViewAdapter(Context context, SceneOverlayDataModel data) {
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
            view = mInflater.inflate(R.layout.area_overlay_listview_item, null);
        }

        ConstraintLayout layout = view.findViewById(R.id.layout);
        Utils.adjustPaddings(layout);

        // Highlight
        layout.setBackground(mContext.getDrawable(
                position == mCurrentIndex
                        ? R.drawable.listview_item_highlight_bg
                        : R.drawable.listview_item_normal_bg));

        View selectedBorderView = view.findViewById(R.id.selected_border);
        selectedBorderView.setVisibility(position == mCurrentIndex ? View.VISIBLE : View.INVISIBLE);

        SceneOverlayDataModel.Overlay item = mDataModel.getItem(position);

        // TODO:
        TextView nameTextView = view.findViewById(R.id.name);
        nameTextView.setText(item.name);

        ImageView thumbnailImageView = view.findViewById(R.id.thumbnail);
        item.updateThumbnail(Utils.PX(160), Utils.PX(90));
        thumbnailImageView.setImageBitmap(item.thumbnailBitmap);

        ImageView typeIconImageView = view.findViewById(R.id.type_icon);
        switch (item.type) {
            case SceneOverlayDataModel.Overlay.TYPE_VIDEO :
                typeIconImageView.setImageResource(R.drawable.ic_picture_in_picture_black_24dp);
                break;

            case SceneOverlayDataModel.Overlay.TYPE_IMAGE :
                typeIconImageView.setImageResource(R.drawable.ic_image_black_24dp);
                break;

            case SceneOverlayDataModel.Overlay.TYPE_TEXT :
                typeIconImageView.setImageResource(R.drawable.ic_title_black_24dp);
                break;

            default :
                break;
        }

        ImageView visibilityIconImageView = view.findViewById(R.id.visibility_icon);
        visibilityIconImageView.setImageResource(
                item.isVisiable
                        ? R.drawable.ic_visibility_black_24dp
                        : R.drawable.ic_visibility_off_black_24dp);

        ImageView lockIconImageView = view.findViewById(R.id.lock_icon);
        lockIconImageView.setImageResource(
                item.isLocked
                        ? R.drawable.ic_lock_black_24dp
                        : R.drawable.ic_lock_open_black_24dp);


        return view;
    }

    public void setCurrentIndex(int index) {
        mCurrentIndex = index;
    }
}
