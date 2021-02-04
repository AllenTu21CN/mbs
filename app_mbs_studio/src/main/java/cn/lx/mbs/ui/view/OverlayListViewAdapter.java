package cn.lx.mbs.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import cn.lx.mbs.R;

import cn.lx.mbs.ui.model.SceneOverlayDataModel;

public class OverlayListViewAdapter extends RecyclerView.Adapter<OverlayListViewAdapter.ViewHolder> {

    public interface OnSelectListener {
        void onItemSelected(int position);
    }

    private Context mContext;
    private SceneOverlayDataModel mDataModel;
    private static LayoutInflater mInflater = null;
    private ViewHolder mSelectedViewHolder;

    private OnSelectListener mOnSelectListener;

    public OverlayListViewAdapter(Context context, SceneOverlayDataModel data) {
        mContext = context;
        mDataModel = data;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public SceneOverlayDataModel getModel() {
        return mDataModel;
    }

    public void setOnSelectListener(OnSelectListener listener) {
        mOnSelectListener = listener;
    }

    @Override
    public int getItemCount() {
        return mDataModel != null ? mDataModel.size() : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.area_overlay_listview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO:
        SceneOverlayDataModel.Overlay item = mDataModel.getItem(position);

        holder.position = position;

        holder.nameTextView.setText(item.getTitle());

        holder.thumbnailImageView.setImageBitmap(item.getThumbnail(Utils.PX(160), Utils.PX(90)));

        switch (item.type) {
            case SceneOverlayDataModel.Overlay.TYPE_VIDEO :
                holder.typeIconImageView.setImageResource(R.drawable.ic_picture_in_picture_black_24dp);
                break;

            case SceneOverlayDataModel.Overlay.TYPE_IMAGE :
                holder.typeIconImageView.setImageResource(R.drawable.ic_image_black_24dp);
                break;

            case SceneOverlayDataModel.Overlay.TYPE_TEXT :
                holder.typeIconImageView.setImageResource(R.drawable.ic_title_black_24dp);
                break;

            default :
                break;
        }

        holder.visibilityIconImageView.setImageResource(
                item.isVisiable
                        ? R.drawable.ic_visibility_black_24dp
                        : R.drawable.ic_visibility_off_black_24dp);

        holder.lockIconImageView.setImageResource(
                item.isLocked
                        ? R.drawable.ic_lock_black_24dp
                        : R.drawable.ic_lock_open_black_24dp);

        // Highlight
        holder.layout.setBackground(mContext.getDrawable(
                holder == mSelectedViewHolder
                        ? R.drawable.listview_item_highlight_bg
                        : R.drawable.listview_item_normal_bg));

        holder.selectedBorderView.setVisibility(
                holder == mSelectedViewHolder
                    ? View.VISIBLE : View.INVISIBLE);
    }

    //public void setCurrentIndex(int index) {
    //    mCurrentIndex = index;
    //}

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        int position;

        ConstraintLayout layout;
        TextView nameTextView;
        ImageView thumbnailImageView;
        ImageView typeIconImageView;
        ImageView visibilityIconImageView;
        ImageView lockIconImageView;
        View selectedBorderView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.layout);
            nameTextView = itemView.findViewById(R.id.name);
            thumbnailImageView = itemView.findViewById(R.id.thumbnail);
            typeIconImageView = itemView.findViewById(R.id.type_icon);
            visibilityIconImageView = itemView.findViewById(R.id.visibility_icon);
            lockIconImageView = itemView.findViewById(R.id.lock_icon);
            selectedBorderView = itemView.findViewById(R.id.selected_border);

            Utils.adjustPaddings(layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Clear and set highlight
            if (mSelectedViewHolder != null) {
                mSelectedViewHolder.layout.setBackground(
                        mContext.getDrawable(R.drawable.listview_item_normal_bg)
                );
                mSelectedViewHolder.selectedBorderView.setVisibility(View.INVISIBLE);
            }

            mSelectedViewHolder = (ViewHolder) view.getTag();
            mSelectedViewHolder.layout.setBackground(mContext.getDrawable(R.drawable.listview_item_highlight_bg));
            mSelectedViewHolder.selectedBorderView.setVisibility(View.VISIBLE);

            if (mOnSelectListener != null) {
                mOnSelectListener.onItemSelected(mSelectedViewHolder.position);
            }
        }
    }
}
