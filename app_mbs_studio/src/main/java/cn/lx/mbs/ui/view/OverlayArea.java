package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.content.Context;
import com.sanbu.tools.LogUtil;

import android.graphics.Color;
import android.graphics.RectF;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import cn.lx.mbs.R;

import cn.lx.mbs.ui.MainActivity;
import cn.lx.mbs.ui.model.SceneOverlayDataModel;

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
    private OverlayAddTextDialog mAddTextDialog;

    public OverlayArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        mWrapperView = mActivity.findViewById(R.id.area_overlay);
        mAddVideoButton = mWrapperView.findViewById(R.id.add_video_button);
        mAddImageButton = mWrapperView.findViewById(R.id.add_image_button);
        mAddTextButton = mWrapperView.findViewById(R.id.add_text_button);
        mListView = mWrapperView.findViewById(R.id.overlay_list);

        // TEST
        MainActivity ma = (MainActivity) mActivity;
        mContext = (Context) mActivity;

        mAddVideoDialog = new OverlayAddVideoDialog(mContext, Utils.PX(1200), Utils.PX(950));
        mAddImageDialog = new OverlayAddImageDialog(mContext, Utils.PX(1200), Utils.PX(1150));
        mAddTextDialog = new OverlayAddTextDialog(mContext, Utils.PX(1200), Utils.PX(1300));

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
        mAddTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an empty overlay item and show editing dialog
                SceneOverlayDataModel.TextOverlay o = new SceneOverlayDataModel.TextOverlay();
                o.text = "No title";
                o.fontFamily = "Sans-serif";
                o.isBold = o.isItalic = o.isUnderlined = false;
                o.alignment = Layout.Alignment.ALIGN_CENTER;
                o.textColor = Color.valueOf(0xFFFFFFFF);
                o.backgroundColor = Color.valueOf(0xFF000000);
                o.backgroundOpacity = 0.5f;
                o.backgroundRadius = 0.05f;

                mOverlayListViewAdapter.getModel().add(o);
                mOverlayListViewAdapter.notifyDataSetChanged();

                mAddTextDialog.updateFields(o.text, o.fontFamily,
                        o.isBold, o.isItalic, o.isUnderlined, o.alignment,
                        o.textColor, o.backgroundColor, o.backgroundOpacity, o.backgroundRadius,
                        o.dstRect, o.rotateAngle);
                mAddTextDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);

                // TODO:
                mAddTextDialog.setOnSaveListener(new OverlayAddTextDialog.OnSaveListener() {
                    @Override
                    public void onSave(String text, String fontFamily,
                                       boolean isBold, boolean isItalic, boolean isUnderlined,
                                       Layout.Alignment alignment,
                                       Color textColor, Color backgroundColor,
                                       float backgroundOpacity, float backgroundRadius,
                                       RectF dstRect, float rotateAngle) {
                        o.text = text;
                        o.fontFamily = fontFamily;
                        o.isBold = isBold;
                        o.isItalic = isItalic;
                        o.isUnderlined = isUnderlined;
                        o.alignment = alignment;
                        o.textColor = textColor;
                        o.backgroundColor = backgroundColor;
                        o.backgroundOpacity = backgroundOpacity;
                        o.backgroundRadius = backgroundRadius;

                        mOverlayListViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public OverlayAddVideoDialog getAddVideoDialog() { return mAddVideoDialog; }
    public OverlayAddImageDialog getAddImageDialog() { return mAddImageDialog; }
    public OverlayAddTextDialog getAddTextDialog() { return mAddTextDialog; }

    public void updateSceneOverlayDataModel(SceneOverlayDataModel model) {
        // TODO:
        if (model == null) {
            mAddVideoButton.setEnabled(false);
            mAddImageButton.setEnabled(false);
            mAddTextButton.setEnabled(false);
        } else {
            mAddVideoButton.setEnabled(true);
            mAddImageButton.setEnabled(true);
            mAddTextButton.setEnabled(true);
        }

        mOverlayListViewAdapter = new OverlayListViewAdapter(mWrapperView.getContext(), model);
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

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                LogUtil.i("VideoSourcesManageDialog", "Item index " + position + " long clicked!");
                Object item = parent.getItemAtPosition(position);

                // TODO: Show editing dialog
                if (item instanceof SceneOverlayDataModel.TextOverlay) {
                    SceneOverlayDataModel.TextOverlay o = (SceneOverlayDataModel.TextOverlay) item;
                    mAddTextDialog.updateFields(o.text, o.fontFamily,
                            o.isBold, o.isItalic, o.isUnderlined, o.alignment,
                            o.textColor, o.backgroundColor, o.backgroundOpacity, o.backgroundRadius,
                            o.dstRect, o.rotateAngle);
                    mAddTextDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);
                    mAddTextDialog.setOnSaveListener(new OverlayAddTextDialog.OnSaveListener() {
                        @Override
                        public void onSave(String text, String fontFamily,
                                           boolean isBold, boolean isItalic, boolean isUnderlined,
                                           Layout.Alignment alignment,
                                           Color textColor, Color backgroundColor,
                                           float backgroundOpacity, float backgroundRadius,
                                           RectF dstRect, float rotateAngle) {
                            o.text = text;
                            o.fontFamily = fontFamily;
                            o.isBold = isBold;
                            o.isItalic = isItalic;
                            o.isUnderlined = isUnderlined;
                            o.alignment = alignment;
                            o.textColor = textColor;
                            o.backgroundColor = backgroundColor;
                            o.backgroundOpacity = backgroundOpacity;
                            o.backgroundRadius = backgroundRadius;

                            mOverlayListViewAdapter.notifyDataSetChanged();
                        }
                    });
                } else if (item instanceof SceneOverlayDataModel.ImageOverlay) {
                    // TODO:
                } else if (item instanceof SceneOverlayDataModel.VideoOverlay) {
                    // TODO:
                }

                return false;
            }
        });
    }

}
