package cn.lx.mbs.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Space;
import android.widget.TextView;

import org.w3c.dom.Text;

import cn.lx.mbs.R;

public class OverlayAddVideoDialog extends BaseDialog {

    private static final String TAG = OverlayAddVideoDialog.class.getSimpleName();
    private static final int COLUMNS = 2;
    private static final int MAX_POSITIONS = 4;
    private static final String[] POSITION_NAME = new String[] { "A", "B", "C", "D" };
    private static final int LAYOUT_OBJECT_TAG_KEY = 0x336699;

    private View mView;
    private LinearLayout mLayoutsList;
    private LinearLayout mPositionMappingLayout;
    private View[] mPositionMappingView = new View[MAX_POSITIONS];

    private String mCurrentLayoutId;
    private View mCurrentLayoutView;

    class VideoLayoutDescription {
        String name;
        String groupName;
        String shaderSource;
        Bitmap thumbImage;
        int positionCount;
    }

    class VideoLayoutGridAdapter extends BaseAdapter {
        private final Context mContext;
        private final VideoLayoutDescription[] mVideoLayouts;

        public VideoLayoutGridAdapter(Context context, VideoLayoutDescription[] videoLayouts) {
            mContext = context;
            mVideoLayouts = videoLayouts;
        }

        // 2
        @Override
        public int getCount() {
            return mVideoLayouts == null ? 0 : mVideoLayouts.length;
        }

        // 3
        @Override
        public long getItemId(int position) {
            return 0;
        }

        // 4
        @Override
        public Object getItem(int position) {
            return null;
        }

        // 5
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialog_overlay_add_video_listview_item, null);
                //int height = mLayoutsList.getWidth() / COLUMNS * 9 / 16 + 20;
                //GridView.LayoutParams lp = new GridView.LayoutParams(GridView.AUTO_FIT, Utils.PX(height));
                //imageView.setLayoutParams(lp);
            }

            //convertView.setTag(LAYOUT_OBJECT_TAG_KEY, mVideoLayouts[position]);
            ImageView imageView = convertView.findViewById(R.id.thumbnail);
            imageView.setImageBitmap(mVideoLayouts[position].thumbImage);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentLayoutView != null) {
                        View border = mCurrentLayoutView.findViewById(R.id.selected_border);
                        border.setVisibility(View.INVISIBLE);
                    }

                    View border = v.findViewById(R.id.selected_border);
                    border.setVisibility(View.VISIBLE);

                    mCurrentLayoutView = v;

                    // Update position view
                    int availablePos = mVideoLayouts[position].positionCount;
                    for (int i = 0; i < MAX_POSITIONS; i++) {
                        mPositionMappingView[i].setVisibility(i < availablePos ? View.VISIBLE : View.INVISIBLE);
                    }

                    /*RadioButton radioButton = mView.findViewById(R.id.pos_a_in_1);
                    Drawable drawable = radioButton.getButtonDrawable();
                    Rect bounds = drawable.getBounds();
                    drawable.setBounds(0, 0, Utils.PX(48), Utils.PX(48));
                    radioButton.setButtonDrawable(drawable);
                    radioButton.invalidate();*/
                }
            });

            Utils.adjustAll((ViewGroup)convertView);

            return convertView;
        }
    }

    public class VideoLayoutGridView extends GridView {
        public VideoLayoutGridView(Context context) {
            super(context);
        }

        public VideoLayoutGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public VideoLayoutGridView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int heightSpec;
            if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                // The great Android "hackatlon", the love, the magic.
                // The two leftmost bits in the height measure spec have
                // a special meaning, hence we can't use them to describe height.
                heightSpec = MeasureSpec.makeMeasureSpec(
                        Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
            } else {
                // Any other height should be respected as is.
                heightSpec = heightMeasureSpec;
            }

            super.onMeasure(widthMeasureSpec, heightSpec);
        }
    }

    final String[] groupNames = new String[] { "Unused", "Single", "Dual", "Triple", "Four" };
    private VideoLayoutDescription[][] videoLayouts = new VideoLayoutDescription[5][];


    public OverlayAddVideoDialog(Context context, int width, int height) {
        super(context, width, height);

        // TODO:
        setTitle("Add Video Overlay");
        mView = mInflater.inflate(R.layout.dialog_overlay_add_video, null);
        setContent(mView);

        // TODO: TEST
        VideoLayoutDescription single = new VideoLayoutDescription();
        single.name = "Fill";
        single.shaderSource = "<TODO>";
        single.thumbImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_video_layout_single);
        single.positionCount = 1;

        VideoLayoutDescription dual = new VideoLayoutDescription();
        dual.name = "Side-by-Side";
        dual.shaderSource = "<TODO>";
        dual.thumbImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_video_layout_dual);
        dual.positionCount = 2;

        VideoLayoutDescription triple = new VideoLayoutDescription();
        triple.name = "One plus two";
        triple.shaderSource = "<TODO>";
        triple.thumbImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_video_layout_triple);
        triple.positionCount = 3;

        VideoLayoutDescription four = new VideoLayoutDescription();
        four.name = "Two multiply two";
        four.shaderSource = "<TODO>";
        four.thumbImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_video_layout_four);
        four.positionCount = 4;

        // Unused or for recent
        videoLayouts[0] = null;
        // Only 1 position
        videoLayouts[1] = new VideoLayoutDescription[1];
        videoLayouts[1][0] = single;
        // 2 positions
        videoLayouts[2] = new VideoLayoutDescription[2];
        videoLayouts[2][0] = dual;
        videoLayouts[2][1] = dual;
        // 3 positions
        videoLayouts[3] = new VideoLayoutDescription[3];
        videoLayouts[3][0] = triple;
        videoLayouts[3][1] = triple;
        videoLayouts[3][2] = triple;
        // 4 positions
        videoLayouts[4] = new VideoLayoutDescription[4];
        videoLayouts[4][0] = four;
        videoLayouts[4][1] = four;
        videoLayouts[4][2] = four;
        videoLayouts[4][3] = four;

        // Initialize layout thumbs
        mLayoutsList = mView.findViewById(R.id.video_layouts_list);
        for (int i = 0; i < videoLayouts.length; i++) {
            TextView groupLabel = new TextView(context);
            groupLabel.setText(groupNames[i]);
            groupLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utils.PX(28));
            mLayoutsList.addView(groupLabel);

            VideoLayoutGridView gridView = new VideoLayoutGridView(context);
            gridView.setAdapter(new VideoLayoutGridAdapter(context, videoLayouts[i]));
            gridView.setNumColumns(COLUMNS);
            //gridView.setVerticalSpacing(Utils.PX(6));
            //gridView.setHorizontalSpacing(Utils.PX(12));
            mLayoutsList.addView(gridView);

            /*if (null != videoLayouts[i]) {
                for (int j = 0; j < videoLayouts[i].length; ) {
                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setDividerPadding(Utils.PX(6));
                    row.setGravity(Gravity.LEFT);

                    for (int k = 0; k < COLUMNS && j < videoLayouts[i].length; ) {
                        ImageView imageView = new ImageView(mContext);
                        imageView.setImageBitmap(videoLayouts[i][j].thumbImage);
                        //imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                        row.addView(imageView);

                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)imageView.getLayoutParams();
                        lp.weight = 1;
                        lp.gravity = Gravity.LEFT;
                        imageView.setLayoutParams(lp);

                        k++;
                        j++;
                    }

                    while (row.getChildCount() < COLUMNS) {
                        ImageView imageView = new ImageView(mContext);
                        row.addView(imageView);

                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)imageView.getLayoutParams();
                        lp.weight = 1;
                        lp.gravity = Gravity.LEFT;
                        imageView.setLayoutParams(lp);
                    }

                    layoutsList.addView(row);
                    Utils.setSize(row, ViewGroup.LayoutParams.WRAP_CONTENT, Utils.PX(180));
                }
            }*/
        }

        // Initialize position mapping
        mPositionMappingLayout = mView.findViewById(R.id.position_mapping);
        for (int i = 0; i < MAX_POSITIONS; i++) {
            View v = mInflater.inflate(R.layout.dialog_overlay_add_video_position_mapping, null);
            v.setVisibility(View.INVISIBLE);

            TextView label = v.findViewById(R.id.position_label);
            label.setText("Input for " + POSITION_NAME[i]);

            mPositionMappingLayout.addView(v);
            mPositionMappingView[i] = v;
        }

        Utils.adjustAll((ViewGroup)mView);

    }
}
