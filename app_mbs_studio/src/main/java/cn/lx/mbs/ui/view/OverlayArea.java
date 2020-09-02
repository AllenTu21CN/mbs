package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.content.Context;
import com.sanbu.tools.LogUtil;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import cn.lx.mbs.R;

import cn.lx.mbs.ui.MainActivity;

public class OverlayArea {
    private static final String TAG = OverlayArea.class.getSimpleName();

    private Activity mActivity;
    private Context mContext;

    private View mWrapperView;
    private ImageButton mAddVideoButton;
    private ImageButton mAddImageButton;
    private ImageButton mAddTextButton;
    private ListView mListView;

    private OverlayListViewAdapter mOverlayListViewAdapter;

    private OverlayAddVideoDialog mAddVideoDialog;
    private OverlayAddImageDialog mAddImageDialog;

    public OverlayArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        mWrapperView = mActivity.findViewById(R.id.area_overlay);
        mAddVideoButton = mWrapperView.findViewById(R.id.add_video_button);
        mAddImageButton = mWrapperView.findViewById(R.id.add_image_button);
        mListView = mWrapperView.findViewById(R.id.overlay_list);

        // TEST
        MainActivity ma = (MainActivity) mActivity;
        mContext = (Context) mActivity;

        mOverlayListViewAdapter = new OverlayListViewAdapter(mWrapperView.getContext(), ma.getSceneOverlayDataModel(0));
        mListView.setAdapter(mOverlayListViewAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LogUtil.i("VideoSourcesManageDialog", "Item index " + i + " clicked!");
                // Clear and set highlight
                for (int n = 0; n < adapterView.getChildCount(); n++) {
                    View item = adapterView.getChildAt(n);
                    item.findViewById(R.id.layout).setBackground(
                            mContext.getDrawable(R.drawable.listview_item_normal_bg)
                    );
                    item.findViewById(R.id.selected_border).setVisibility(View.INVISIBLE);
                }

                view.findViewById(R.id.layout).setBackground(mContext.getDrawable(R.drawable.listview_item_highlight_bg));
                view.findViewById(R.id.selected_border).setVisibility(View.VISIBLE);

                mOverlayListViewAdapter.setCurrentIndex(i);
            }
        });

        mAddVideoDialog = new OverlayAddVideoDialog(mContext, Utils.PX(1200), Utils.PX(950));
        mAddImageDialog = new OverlayAddImageDialog(mContext, Utils.PX(1200), Utils.PX(1150));

        mAddVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddVideoDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);
            }
        });
        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddImageDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);
            }
        });
    }
}
