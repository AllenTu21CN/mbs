package mbs.studio.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import mbs.studio.MainActivity;
import com.example.studio.R;

public class VideoSourcesArea {
    final static int VIDEO_SOURCE_COUNT = 4;

    MainActivity mActivity;
    VideoSourceItem[] mVideoSourceItems = new VideoSourceItem[VIDEO_SOURCE_COUNT];

    private static class VideoSourceItem {
        final static int DYNAMIC_BUTTON_COUNT = 4;

        int mId;
        ConstraintLayout mSelf;
        TextView mItemNo;
        TextView mSourceType;
        TextView mSourceFormat;
        ImageButton mAddSourceButton;
        SurfaceView mSourceSurfaceView;
        DynamicButton[] mButtons = new DynamicButton[DYNAMIC_BUTTON_COUNT];

        class DynamicButton extends Button {

            static final int TYPE_NONE = 0;
            static final int TYPE_CUT = 1;
            static final int TYPE_PLAY = 2;
            static final int TYPE_PAUSE = 3;
            static final int TYPE_CAM_CONTROL = 4;
            static final int TYPE_CALL = 5;
            static final int TYPE_HANG = 6;
            static final int TYPE_SETTINGS = 7;

            final int[] BUTTON_ICONS = new int[] {
                -1,                                     // NONE
                R.drawable.ic_switch_video_black_24dp,  // CUT
                R.drawable.ic_play_arrow_black_24dp,    // PLAY
                R.drawable.ic_pause_black_24dp,         // PAUSE
                R.drawable.ic_photo_camera_black_24dp,  // CAM_CONTROL
                R.drawable.ic_call_black_24dp,          // CALL
                R.drawable.ic_call_end_black_24dp,      // HANG
                R.drawable.ic_settings_black_24dp,      // SETTINGS
            };

            final String[] BUTTON_TEXTS = new String[] {
                "",         // NONE
                "CUT",      // CUT
                "PLAY",     // PLAY
                "PAUSE",    // PAUSE
                "CTRL",     // CAM_CONTROL
                "CALL",     // CALL
                "HANGUP",   // HANG
                "EDIT",     // SETTINGS
            };

            int mType = -1;

            DynamicButton(Context context, AttributeSet attrs) {
                super(context, attrs);

                setId(View.generateViewId());
                setBackground(getContext().getDrawable(R.drawable.common_button_bg));
                setPadding(0,0, 0, 0);
            }

            int getType() { return mType; }

            void setType(int type) {
                if (type < TYPE_NONE || TYPE_SETTINGS < type) {
                    mType = TYPE_NONE;
                } else {
                    mType = type;
                }

                setText(BUTTON_TEXTS[mType]);
                setTextColor(getContext().getColorStateList(R.color.video_source_dynamic_button_color));
                setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(24));

                Drawable icon = null;
                if (type != TYPE_NONE) {
                    icon = getContext().getDrawable(BUTTON_ICONS[mType]);
                    icon.setBounds(0, Utils.PX(8), Utils.PX(36), Utils.PX(36 + 8));
                }
                setCompoundDrawables(null, icon, null, null);
                setCompoundDrawableTintList(getContext().getColorStateList(R.color.video_source_dynamic_button_tint));
                setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
            }
        }

        VideoSourceItem(int id, ConstraintLayout view) {
            mId = id;
            mSelf = view;
        }

        void init() {
            // Adjust margins
            Utils.setMargins(mSelf, Utils.PX(18), Utils.PX(15),
                    Utils.PX(18), Utils.PX(5));

            mItemNo = mSelf.findViewById(R.id.item_no);
            //mItemNo.setBackground(mSelf.getContext().getDrawable(R.drawable.video_source_item_no_bg));
            mItemNo.setText(String.format("IN-%d", mId + 1));
            mItemNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(24));

            mSourceType = mSelf.findViewById(R.id.source_type);
            //mSourceType.setBackground(mSelf.getContext().getDrawable(R.drawable.video_source_type_bg));
            mSourceType.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(18));

            mSourceFormat = mSelf.findViewById(R.id.source_format);
            mSourceFormat.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(22));

            mAddSourceButton = mSelf.findViewById(R.id.add_source_button);
            Utils.setSize(mAddSourceButton, Utils.PX(120), Utils.PX(120));

            mSourceSurfaceView = mSelf.findViewById(R.id.source_surface_view);
            Utils.setMargins(mSourceSurfaceView, 0, Utils.PX(8), 0, 0);

            // Create dynamic buttons
            for (int i = 0; i < DYNAMIC_BUTTON_COUNT; i++) {
                DynamicButton btn = new DynamicButton(mSelf.getContext(), null);
                btn.setType(DynamicButton.TYPE_NONE);
                btn.setEnabled(false);

                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                        Utils.PX(110), Utils.PX(80));

                lp.topToBottom = mSourceSurfaceView.getId();
                lp.topMargin = Utils.PX(8);

                switch (i) {
                    case 0 :
                        lp.leftToLeft = mSelf.getId();
                        lp.leftMargin = 0;
                        break;

                    case (DYNAMIC_BUTTON_COUNT - 1) :
                        lp.rightToRight = mSelf.getId();
                        lp.rightMargin = 0;
                        break;

                    default :
                        lp.leftToRight = mButtons[i - 1].getId();
                        break;
                }

                btn.setLayoutParams(lp);
                mSelf.addView(btn);
                mButtons[i] = btn;
            }

            // Add constraint right to next button
            for (int i = 1; i < DYNAMIC_BUTTON_COUNT - 1; i++) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mButtons[i].getLayoutParams();
                lp.leftToRight = mButtons[i - 1].getId();
                lp.rightToLeft = mButtons[i + 1].getId();
                mButtons[i].setLayoutParams(lp);
            }
        }
    } // End of class VideoSourceItem

    class NullVideoSource extends VideoSourceItem {
        NullVideoSource(int id, ConstraintLayout view) {
            super(id, view);
        }

        @Override
        void init() {
            super.init();

            mSourceType.setVisibility(View.INVISIBLE);
            mSourceFormat.setVisibility(View.INVISIBLE);

            mAddSourceButton.setVisibility(View.VISIBLE);
            mAddSourceButton.setEnabled(true);
            mAddSourceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mActivity.showVideoSourceManageDialog();

                    // TODO:
                    if (0 == mId % 2) {
                        mVideoSourceItems[mId] = new RtspVideoSource(mId, mSelf);
                    } else {
                        mVideoSourceItems[mId] = new LocalCameraVideoSource(mId, mSelf);
                    }
                    mVideoSourceItems[mId].init();
                }
            });

            for (int i = 0; i < DYNAMIC_BUTTON_COUNT; i++) {
                mButtons[i].setType(DynamicButton.TYPE_NONE);
                mButtons[i].setEnabled(false);
            }
        }
    }

    class RtspVideoSource extends VideoSourceItem {
        RtspVideoSource(int id, ConstraintLayout view) {
            super(id, view);
        }

        @Override
        void init() {
            super.init();

            mSourceType.setText("RTSP");
            mSourceType.setVisibility(View.VISIBLE);

            mSourceFormat.setText("Unknown format");
            mSourceFormat.setVisibility(View.VISIBLE);

            mAddSourceButton.setVisibility(View.INVISIBLE);

            mButtons[0].setType(DynamicButton.TYPE_CUT);
            mButtons[0].setEnabled(true);
            mButtons[1].setType(DynamicButton.TYPE_PAUSE);
            mButtons[1].setEnabled(true);
            mButtons[2].setType(DynamicButton.TYPE_NONE);
            mButtons[2].setEnabled(false);
            mButtons[3].setType(DynamicButton.TYPE_SETTINGS);
            mButtons[3].setEnabled(true);

            mButtons[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO:
                    if (mButtons[1].getType() == DynamicButton.TYPE_PAUSE) {
                        mButtons[1].setType(DynamicButton.TYPE_PLAY);
                    } else if (mButtons[1].getType() == DynamicButton.TYPE_PLAY) {
                        mButtons[1].setType(DynamicButton.TYPE_PAUSE);
                    }
                }
            });
        }
    }

    class LocalCameraVideoSource extends VideoSourceItem {
        LocalCameraVideoSource(int id, ConstraintLayout view) {
            super(id, view);
        }

        @Override
        void init() {
            super.init();

            mSourceType.setText("LOCAL CAM");
            mSourceType.setVisibility(View.VISIBLE);

            mSourceFormat.setText("Unknown format");
            mSourceFormat.setVisibility(View.VISIBLE);

            mAddSourceButton.setVisibility(View.INVISIBLE);
            mButtons[0].setType(DynamicButton.TYPE_CUT);
            mButtons[0].setEnabled(true);
            mButtons[1].setType(DynamicButton.TYPE_PAUSE);
            mButtons[1].setEnabled(true);
            mButtons[2].setType(DynamicButton.TYPE_CAM_CONTROL);
            mButtons[2].setEnabled(true);
            mButtons[3].setType(DynamicButton.TYPE_SETTINGS);
            mButtons[3].setEnabled(true);

            mButtons[1].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO:
                    if (mButtons[1].getType() == DynamicButton.TYPE_PAUSE) {
                        mButtons[1].setType(DynamicButton.TYPE_PLAY);
                    } else if (mButtons[1].getType() == DynamicButton.TYPE_PLAY) {
                        mButtons[1].setType(DynamicButton.TYPE_PAUSE);
                    }
                }
            });
        }
    }

    public VideoSourcesArea(MainActivity activity) {
        mActivity = activity;
    }

    public void init() {
        // Adjust views
        ConstraintLayout[] children = new ConstraintLayout[VIDEO_SOURCE_COUNT];
        children[0] = mActivity.findViewById(R.id.video_source_1);
        children[1] = mActivity.findViewById(R.id.video_source_2);
        children[2] = mActivity.findViewById(R.id.video_source_3);
        children[3] = mActivity.findViewById(R.id.video_source_4);
        for (int i = 0; i < VIDEO_SOURCE_COUNT; i++) {
            mVideoSourceItems[i] = new NullVideoSource(i, children[i]);
            mVideoSourceItems[i].init();
        }
    }
}
