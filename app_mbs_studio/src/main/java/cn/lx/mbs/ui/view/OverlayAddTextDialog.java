package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import cn.lx.mbs.R;

public class OverlayAddTextDialog extends BaseDialog {

    private static final String TAG = OverlayAddImageDialog.class.getSimpleName();

    private View mView;
    private ImageView mSceneEditorBgImageView;
    private Bitmap mSceneEditorBgBitmap;

    public OverlayAddTextDialog(Context context, int width, int height) {
        super(context, width, height);

        // TODO:
        setTitle("Add Text Overlay");
        mView = mInflater.inflate(R.layout.dialog_overlay_add_text, null);
        setContent(mView);

        Utils.adjustAll((ViewGroup) mView);

        mSceneEditorBgImageView = mView.findViewById(R.id.scene_editor_bg);
        mSceneEditorBgImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mSceneEditorBgImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        // Set background image
                        final int CELL_SIZE = Utils.PX(5);
                        mSceneEditorBgBitmap = Utils.generateCheckerBoardBitmap(
                                mSceneEditorBgImageView.getWidth(), mSceneEditorBgImageView.getHeight(),
                                CELL_SIZE, CELL_SIZE);
                        mSceneEditorBgImageView.setImageBitmap(mSceneEditorBgBitmap);
                    }
        });
    }
}
