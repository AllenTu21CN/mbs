package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Spinner;

import cn.lx.mbs.R;

public class OverlayAddTextDialog extends BaseDialog {

    private static final Color[] TEXT_COLORS = new Color[] {
            Color.valueOf(0xFFC00000),
            Color.valueOf(0xFFFF0000),
            Color.valueOf(0xFFFFC000),
            Color.valueOf(0xFFFFFF00),
            Color.valueOf(0xFF92D050),
            Color.valueOf(0xFF00B050),
            Color.valueOf(0xFF00B0F0),
            Color.valueOf(0xFF0070C0),
            Color.valueOf(0xFF002060),
            Color.valueOf(0xFFC00000),
            Color.valueOf(0xFF7030A0)
    };

    private static final Color[] BACKGROUND_COLORS = new Color[] {
            Color.valueOf(0xFFFFFF00),
            Color.valueOf(0xFF00FF00),
            Color.valueOf(0xFF00FFFF),
            Color.valueOf(0xFFFF00FF),
            Color.valueOf(0xFF0000FF),
            Color.valueOf(0xFFFF0000),
            Color.valueOf(0xFF000080),
            Color.valueOf(0xFF008080),
            Color.valueOf(0xFF008000),
            Color.valueOf(0xFF800080),
            Color.valueOf(0xFF800000),
            Color.valueOf(0xFF808000),
            Color.valueOf(0xFF808080),
            Color.valueOf(0xFFC0C0C0),
            Color.valueOf(0xFF000000)
    };

    private static final String[] SYSTEM_DFAULT_FONTS = new String[] {
            "Sans serif",
            "Serif",
            "Monospace"
    };

    private static final String TAG = OverlayAddImageDialog.class.getSimpleName();

    private View mView;
    private ImageView mSceneEditorBgImageView;
    private Bitmap mSceneEditorBgBitmap;
    private Spinner mFontSpinner;
    private ColorPickerButton mTextColorPicker;
    private ColorPickerButton mBgColorPicker;

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

        mFontSpinner = mView.findViewById(R.id.font_value);
        SimpleArrayAdapter<String> fontListAdapter = new SimpleArrayAdapter<>(
                mContext,
                SYSTEM_DFAULT_FONTS);
        fontListAdapter.setTextSize(Utils.PX(28));
        mFontSpinner.setAdapter(fontListAdapter);

        mTextColorPicker = mView.findViewById(R.id.text_color_btn);
        mTextColorPicker.updateColorList(TEXT_COLORS);

        mBgColorPicker = mView.findViewById(R.id.bg_color_btn);
        mBgColorPicker.updateColorList(BACKGROUND_COLORS);
    }
}
