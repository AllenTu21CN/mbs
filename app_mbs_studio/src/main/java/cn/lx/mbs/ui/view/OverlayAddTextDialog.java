package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import cn.lx.mbs.R;
import cn.sanbu.avalon.media.TextRenderer;

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

    private static final String[] SYSTEM_DEFAULT_FONTS = new String[] {
            "Sans serif",
            "Serif",
            "Monospace"
    };

    private static final String TAG = OverlayAddImageDialog.class.getSimpleName();

    private View mView;
    private ImageView mSceneEditorBgImageView;
    private Bitmap mSceneEditorBgBitmap;
    private EditText mTextEditText;
    private Spinner mFontSpinner;
    private FontStyleButtonGroup mFontStyleButtonGroup;
    private ColorPickerButton mTextColorPicker;
    private ColorPickerButton mBgColorPicker;
    private SeekBar mBgOpacitySeekBar;
    private TextView mBgOpacityTextView;
    private EditText mBgBorderRadiusEditText;

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

        mTextEditText = mView.findViewById(R.id.text_value);
        mFontSpinner = mView.findViewById(R.id.font_value);
        SimpleArrayAdapter<String> fontListAdapter = new SimpleArrayAdapter<>(
                mContext,
                SYSTEM_DEFAULT_FONTS);
        fontListAdapter.setTextSize(Utils.PX(28));
        mFontSpinner.setAdapter(fontListAdapter);

        mFontStyleButtonGroup = mView.findViewById(R.id.text_style_button_group);
        mTextColorPicker = mView.findViewById(R.id.text_color_btn);
        mTextColorPicker.updateColorList(TEXT_COLORS);

        mBgColorPicker = mView.findViewById(R.id.bg_color_btn);
        mBgColorPicker.updateColorList(BACKGROUND_COLORS);

        mBgOpacitySeekBar = mView.findViewById(R.id.bg_opacity_seek_bar);
        mBgOpacitySeekBar.setMin(0);
        mBgOpacitySeekBar.setMax(100);
        mBgOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                mBgOpacityTextView.setText(String.format("%d%%", progress));
                // TODO:
                updatePreview();
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {

            }
        });
        mBgOpacityTextView = mView.findViewById(R.id.bg_opacity_value);

        mBgBorderRadiusEditText = mView.findViewById(R.id.bg_border_radius_value);
    }

    public void updatePreview() {
        // TODO:
        String text = mTextEditText.getText().toString();
        String fontFamily = (String) mFontSpinner.getSelectedItem();
        boolean isBold = mFontStyleButtonGroup.isBold();
        boolean isItalic = mFontStyleButtonGroup.isItalic();
        boolean isUnderlined = mFontStyleButtonGroup.isUnderlined();
        Layout.Alignment alignment = mFontStyleButtonGroup.getAlignment();
        Color textColor = mTextColorPicker.getSelectedColor();
        Color bgColor = mBgColorPicker.getSelectedColor();
        float bgOpacity = (float) mBgOpacitySeekBar.getProgress() / 100.f;
        int bgBorderRadius = Integer.valueOf(mBgBorderRadiusEditText.getText().toString());

        Bitmap img = TextRenderer.renderTextAsBitmap(
                text,
                fontFamily, textColor.toArgb(),
                isBold, isItalic, isUnderlined, alignment,
                bgColor.toArgb(), bgBorderRadius, 1920, 1080);
        mSceneEditorBgImageView.setImageBitmap(img);
    }
}
