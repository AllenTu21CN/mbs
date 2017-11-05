package sanp.mp100.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import sanp.mp100.R;

/**
 * @brief A common dialog, used in confirm, warning, cancel and so on.
 *
 *                   +---------------------+
 *                   |        Title        |
 *                   +---------------------+
 *                   |  image/text/none    |
 *                   +----------+----------+
 *                   | LEFT BTN | RIGHT BTN|
 *                   +----------+----------+
 *
 * @author will@1dao2.com
 * @date 2017-11-2
 */

public class CommonDialog extends Dialog {

    // dialog title view
    private TextView mTitleView;

    private String   mTitle;

    // dialog buttons
    private Button mLeftBtn;
    private Button mRightBtn;

    private String mLeftBtnText;
    private String mRightBtnText;

    private onButtonClickListener  mListener;

    // dialog extra text view
    private TextView mExtraTextView;
    private String   mExtraText;

    // Dialog type
    private int mType = -1;

    public static final int DIALOG_WAITING = 0;
    public static final int DIALOG_WARNING = 1;
    public static final int DIALOG_ERROR   = 2;
    public static final int DIALOG_CONFIRM = 3;

    public static final int LEFT_BUTTON  = 0;
    public static final int RIGHT_BUTTON = 1;

    // Defines dialog buttons click listener interface
    public interface onButtonClickListener {
        void onButtonClick(int button);
    }

    // Constructor
    public CommonDialog(Context context, int userData) {
        super(context, R.style.CourseDialog);
        mType = userData;
    }

    // Sets button click listener
    public void setButtonOnClickListener(String leftBtnText, String rightBtnText,
            onButtonClickListener listener) {
        mListener     = listener;
        mLeftBtnText  = leftBtnText;
        mRightBtnText = rightBtnText;
    }

    // Sets dialog title
    public void setTitle(String title) { mTitle = title; }

//  public void setType(int type) { mType = type; }

    // Gets dialog user data
    public int type() { return mType; }

    // Sets dialog extra text
    public void setExtraText(String extraText) { mExtraText = extraText; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load layout xml: common_dialog.xml
        setContentView(R.layout.common_dialog);

        // Don't close(? dismiss) dialog when touch empty area.
        setCanceledOnTouchOutside(false);

        // init views, data and set listener
        initView();
        initViewData();
        initEvent();
    }

    private void initView() {
        mTitleView = (TextView) findViewById(R.id.dialog_title_view);
        mLeftBtn   = (Button) findViewById(R.id.dialog_left_btn);
        mRightBtn  = (Button) findViewById(R.id.dialog_right_btn);

        mExtraTextView = (TextView) findViewById(R.id.dialog_extra_text_view);
    }

    private void initViewData() {

        // set title text
        if (mTitle != null) mTitleView.setText(mTitle);

        // set button text
        if (mLeftBtnText != null)  mLeftBtn.setText(mLeftBtnText);
        if (mRightBtnText != null) mRightBtn.setText(mRightBtnText);

        // init extra view
        if (mExtraText != null) {
            mExtraTextView.setText(mExtraText);
        } else {
            mExtraTextView.setVisibility(View.GONE);
        }
    }

    private void initEvent() {
        mLeftBtn.setOnClickListener((View v)-> {
            if (mListener != null) mListener.onButtonClick(LEFT_BUTTON);
        });

        mRightBtn.setOnClickListener((View v)-> {
            if (mListener != null) mListener.onButtonClick(RIGHT_BUTTON);
        });
    }
}
