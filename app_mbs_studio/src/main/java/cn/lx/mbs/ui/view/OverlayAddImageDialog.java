package mbs.studio.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.studio.R;

import mbs.studio.MainActivity;

public class OverlayAddImageDialog extends BaseDialog {

    private static final String TAG = OverlayAddImageDialog.class.getSimpleName();

    private View mView;
    private ImageView mSceneEditorBgImageView;
    private OverlayEditableView mOverlayEditableView;
    private TextView mPositionValueTextView;
    private SeekBar mScaleSeekBar;
    private TextView mScaleValueTextView;
    private SeekBar mRotationSeekBar;
    private TextView mRotationValueTextView;
    private SeekBar mOpacitySeekBar;
    private TextView mOpacityValueTextView;

    private Button mOpenButton;

    private Bitmap mSceneEditorBgBitmap;

    public OverlayAddImageDialog(Context context, int width, int height) {
        super(context, width, height);

        // TODO:
        setTitle("Add Image Overlay");
        mView = mInflater.inflate(R.layout.dialog_overlay_add_image, null);
        setContent(mView);

        Utils.adjustAll((ViewGroup)mView);

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

        mOverlayEditableView = mView.findViewById(R.id.scene_editor_overlay);
        mPositionValueTextView = mView.findViewById(R.id.position_value);
        mOverlayEditableView.setOnPositionChangeListener(new OverlayEditableView.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(int left, int top, int right, int bottom, int centerX, int centerY) {
                final int bgWidth = mSceneEditorBgImageView.getWidth();
                final int bgHeight = mSceneEditorBgImageView.getHeight();
                float l = (float)left / (float)bgWidth;
                float t = (float)top / (float)bgHeight;
                float r = (float)right / (float)bgWidth;
                float b = (float)bottom / (float)bgHeight;
                float cx = (float)centerX / (float)bgWidth;
                float cy = (float)centerY / (float)bgHeight;
                mPositionValueTextView.setText(
                        String.format("left=%.3f, top=%.3f, right=%.3f, bottom=%.3f, center=[%.3f, %.3f]",
                        l, t, r, b, cx, cy));
            }
        });

        // TEST ////////////////////////////////////////////////////////////////////////////////////
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap testImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_image_1, options);
        mOverlayEditableView.setImageBitmap(testImg);
        //mOverlayEditableView.setRotateAngle(45);
        // TEST ENDS ///////////////////////////////////////////////////////////////////////////////

        mScaleSeekBar = mView.findViewById(R.id.scale_seek_bar);
        mScaleValueTextView = mView.findViewById(R.id.scale_value);
        mRotationSeekBar = mView.findViewById(R.id.rotation_seek_bar);
        mRotationValueTextView = mView.findViewById(R.id.rotation_value);
        mOpacitySeekBar = mView.findViewById(R.id.opacity_seek_bar);
        mOpacityValueTextView = mView.findViewById(R.id.opacity_value);

        //mScaleSeekBar.setMin(1);
        mScaleSeekBar.setMax(500);
        mScaleSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                mScaleValueTextView.setText(String.format("%d%%", progress));
                final float scale = (float)progress / 100.f;
                mOverlayEditableView.setScaleFactor(scale);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {

            }
        });

        //mRotationSeekBar.setMin(0);
        mRotationSeekBar.setMax(360);
        mRotationSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                mRotationValueTextView.setText(String.format("%dº", progress));
                mOverlayEditableView.setRotateAngle((float)progress);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {

            }
        });

        //mOpacitySeekBar.setMin(0);
        mOpacitySeekBar.setMax(100);
        mOpacitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                mOpacityValueTextView.setText(String.format("%d%%", progress));
                mOverlayEditableView.setOpacity((float)progress / 100.f);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {

            }
        });

        // Initialize value
        mScaleSeekBar.setProgress(100);
        mRotationSeekBar.setProgress(0);
        mOpacitySeekBar.setProgress(100);

        mOpenButton = mView.findViewById(R.id.open_button);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                MainActivity ma = (MainActivity) mContext;
                ma.showPickImageDialog();
            }
        });
    }
}
