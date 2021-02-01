package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cn.lx.mbs.R;

import cn.lx.mbs.ui.MainActivity;

public class OverlayAddImageDialog extends BaseDialog {

    public interface OnSaveListener {
        void onSave(String originalFilePath, Bitmap originalBitmap,
                    RectF dstRect, float rotateAngle, float opacity);
    }

    private static final String TAG = OverlayAddImageDialog.class.getSimpleName();

    private View mView;
    private ImageView mSceneEditorBgImageView;
    private OverlayEditableView mOverlayEditableView;
    private TextView mFilePathTextView;
    private TextView mPositionValueTextView;
    private SeekBar mScaleSeekBar;
    private TextView mScaleValueTextView;
    private SeekBar mRotationSeekBar;
    private TextView mRotationValueTextView;
    private SeekBar mOpacitySeekBar;
    private TextView mOpacityValueTextView;

    private Button mOpenButton;
    private Button mSaveButton;

    private Bitmap mSceneEditorBgBitmap;
    private Bitmap mOriginalBitmap;

    private OnSaveListener mOnSaveListener;

    public OverlayAddImageDialog(Context context, int width, int height) {
        super(context, width, height);

        setupUi();
        setupListener();

        // TEST ////////////////////////////////////////////////////////////////////////////////////
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap testImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_image_1, options);
        mOverlayEditableView.setImageBitmap(testImg);
        //mOverlayEditableView.setRotateAngle(45);
        // TEST ENDS ///////////////////////////////////////////////////////////////////////////////

    }

    private void setupUi() {
        setTitle("Add Image Overlay");

        mView = mInflater.inflate(R.layout.dialog_overlay_add_image, null);
        setContent(mView);

        Utils.adjustAll((ViewGroup)mView);

        mSceneEditorBgImageView = mView.findViewById(R.id.scene_editor_bg);
        mOverlayEditableView = mView.findViewById(R.id.scene_editor_overlay);

        mFilePathTextView = mView.findViewById(R.id.file_path_value);
        mPositionValueTextView = mView.findViewById(R.id.position_value);
        mScaleSeekBar = mView.findViewById(R.id.scale_seek_bar);
        mScaleValueTextView = mView.findViewById(R.id.scale_value);
        mRotationSeekBar = mView.findViewById(R.id.rotation_seek_bar);
        mRotationValueTextView = mView.findViewById(R.id.rotation_value);
        mOpacitySeekBar = mView.findViewById(R.id.opacity_seek_bar);
        mOpacityValueTextView = mView.findViewById(R.id.opacity_value);
        mOpenButton = mView.findViewById(R.id.open_button);
        mSaveButton = mView.findViewById(R.id.save_button);

        //mScaleSeekBar.setMin(1);
        mScaleSeekBar.setMax(500);
        mScaleSeekBar.setProgress(100);

        //mRotationSeekBar.setMin(0);
        mRotationSeekBar.setMax(360);
        mRotationSeekBar.setProgress(0);

        //mOpacitySeekBar.setMin(0);
        mOpacitySeekBar.setMax(100);
        mOpacitySeekBar.setProgress(100);
    }

    private void setupListener() {
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

        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                MainActivity ma = (MainActivity) mContext;
                ma.showImageOverlayPickDialog();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                if (mOnSaveListener != null) {
                    String originalFilePath = mFilePathTextView.getText().toString();
                    RectF dstRect = null;
                    float rotateAngle = mRotationSeekBar.getProgress();
                    float opacity = (float) mOpacitySeekBar.getProgress() / 100.f;

                    mOnSaveListener.onSave(originalFilePath, mOriginalBitmap, dstRect, rotateAngle, opacity);
                }

                dismiss();
            }
        });
    }

    public void setExternalImageUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            mOverlayEditableView.setImageBitmap(bitmap);
            // Center in scene and keep aspect ratio
            // TODO:
            int maxWidth = mSceneEditorBgImageView.getWidth();
            int maxHeight = mSceneEditorBgImageView.getHeight();
            int srcWidth = bitmap.getWidth();
            int srcHeight = bitmap.getHeight();

            int tgtWidth = srcWidth;
            int tgtHeight = srcHeight;

            if (srcWidth > maxWidth || srcHeight > maxHeight) {
                float ratioOfWidth = (float)srcWidth / (float)maxWidth;
                float ratioOfHeight = (float)srcHeight / (float)maxHeight;
                if (ratioOfWidth > ratioOfHeight) {
                    tgtWidth /= ratioOfWidth;
                    tgtHeight /= ratioOfWidth;
                    mOverlayEditableView.setScaleFactor(ratioOfWidth);
                } else {
                    tgtWidth /= ratioOfHeight;
                    tgtHeight /= ratioOfHeight;
                    mOverlayEditableView.setScaleFactor(ratioOfHeight);
                }
            }

            mOverlayEditableView.setX(mSceneEditorBgImageView.getX() + (maxWidth - tgtWidth) / 2.f);
            mOverlayEditableView.setY(mSceneEditorBgImageView.getY() + (maxHeight - tgtHeight) / 2.f);

        } catch (FileNotFoundException e) {
            // TODO: File not found
        } catch (IOException e) {
            // TODO: IO exception
        }
    }

    public void updateFields(String originalFilePath, Bitmap originalBitmap,
                             RectF dstRect, float rotateAngle, float opacity) {
        // TODO:
        mFilePathTextView.setText(originalFilePath);
        mOriginalBitmap = originalBitmap;
        mRotationSeekBar.setProgress((int)rotateAngle);
        mOpacitySeekBar.setProgress((int)(opacity * 100.f));
    }
}
