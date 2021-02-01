package cn.lx.mbs.ui.view;

import android.app.Activity;
import android.content.Context;

import android.graphics.Color;
import android.graphics.RectF;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

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
    private ImageButton mToggleLockButton;
    private ImageButton mToggleVisibilityButton;
    private ImageButton mEditButton;
    private ImageButton mDeleteButton;

    private RecyclerView mOverlayListView;
    private OverlayListViewAdapter mOverlayListViewAdapter;

    private OverlayAddVideoDialog mAddVideoDialog;
    private OverlayAddImageDialog mAddImageDialog;
    private OverlayAddTextDialog mAddTextDialog;

    public OverlayArea(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        setupUi();
        setupListener();
    }

    private void setupUi() {
        // TODO:
        mWrapperView = mActivity.findViewById(R.id.area_overlay);

        mAddVideoButton = mWrapperView.findViewById(R.id.add_video_button);
        mAddImageButton = mWrapperView.findViewById(R.id.add_image_button);
        mAddTextButton = mWrapperView.findViewById(R.id.add_text_button);
        mToggleLockButton = mWrapperView.findViewById(R.id.toggle_lock_button);
        mToggleVisibilityButton = mWrapperView.findViewById(R.id.toggle_visibility_button);
        mEditButton = mWrapperView.findViewById(R.id.edit_button);
        mDeleteButton = mWrapperView.findViewById(R.id.delete_button);

        mOverlayListView = mWrapperView.findViewById(R.id.overlay_list);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mWrapperView.getContext(), DividerItemDecoration.VERTICAL);
        mOverlayListView.addItemDecoration(dividerItemDecoration);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP
                        | ItemTouchHelper.DOWN
                        | ItemTouchHelper.START
                        | ItemTouchHelper.END, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                SceneOverlayDataModel model = ((OverlayListViewAdapter) recyclerView.getAdapter()).getModel();
                model.swap(fromPosition, toPosition);
                recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mOverlayListView);

        // TEST
        MainActivity ma = (MainActivity) mActivity;
        mContext = (Context) mActivity;

        mAddVideoDialog = new OverlayAddVideoDialog(mContext, Utils.PX(1200), Utils.PX(950));
        mAddImageDialog = new OverlayAddImageDialog(mContext, Utils.PX(1200), Utils.PX(1250));
        mAddTextDialog = new OverlayAddTextDialog(mContext, Utils.PX(1200), Utils.PX(1300));
    }

    private void setupListener() {
        // TODO:
        mAddVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddVideoDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);
            }
        });

        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOverlayListViewAdapter == null) {
                    return; // No scene selected
                }

                // Create an empty overlay item and show editing dialog
                SceneOverlayDataModel.ImageOverlay o = new SceneOverlayDataModel.ImageOverlay();
                o.originalFilePath = null;
                o.orignalBitmap = null;
                o.dstRect = new RectF(0.f, 0.f, 1.f, 1.f);
                o.opacity = 1.0f;

                mOverlayListViewAdapter.getModel().add(o);
                mOverlayListViewAdapter.notifyDataSetChanged();

                mAddImageDialog.showAtLocation(mWrapperView, Gravity.CENTER, 0, 0);
            }
        });

        mAddTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOverlayListViewAdapter == null) {
                    return; // No scene selected
                }

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

            mOverlayListViewAdapter = null;
        } else {
            mAddVideoButton.setEnabled(true);
            mAddImageButton.setEnabled(true);
            mAddTextButton.setEnabled(true);

            mOverlayListViewAdapter = new OverlayListViewAdapter(mWrapperView.getContext(), model);
            mOverlayListViewAdapter.setOnSelectListener(new OverlayListViewAdapter.OnSelectListener() {
                @Override
                public void onItemSelected(int position) {
                    SceneOverlayDataModel.Overlay item = mOverlayListViewAdapter.getModel().getItem(position);
                    onOverlayItemSelected(item);
                }
            });
        }

        mOverlayListView.setAdapter(mOverlayListViewAdapter);
    }

    private void onOverlayItemSelected(SceneOverlayDataModel.Overlay item) {
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
    }

}
