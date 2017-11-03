package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.ui.adapter.CourseThread;

/**
 * @brief Created by will@1dao2.com on 2017/10/30.
 * Take class card view overlap on view surface view.
 *
 * @author will@1dao2.com
 * @date   2017/10/30
 */

public class ClassFragment extends BaseFragment implements View.OnClickListener, CourseThread.ClassNotify {

    public static final String TAG = "ClassFragment";
    public static ClassFragment mClassFragment = null;

    // class fragment view: a single instance.
    public static ClassFragment getInstance() {
        if (mClassFragment == null) mClassFragment = new ClassFragment();

        return mClassFragment;
    }

    // see: taking_class.xml
    private View mClassViewGroup;

    private TextView mClassStatusView;

    // class info view and info strings
    private TextView mClassNameView;
    private TextView mClassTeacherView;
    private TextView mClassTimeView;
    private String   mClassTime;
    private TextView mClassContentView;

    // taking class button
    private Button mClassCtrlBtn;
    private Button mHiddenThisBtn;

    private BusinessPlatform.TimeTable mCourse;

    private Boolean mInClass;

    // course thread
    private CourseThread mCourseThread;

    private final int MSG_ON_START_CLASS = 0;
    private final int MSG_ON_STOP_CLASS = 1;
    // class fragment message handler
    private Handler mHandler;

    // class waiting, warning, error confirm dialog
    private CommonDialog mDialog;

    private static final int BUTTON_ENABLE_TEXT_COLOR  = 0xffffffff;
    private static final int BUTTON_DISABLE_TEXT_COLOR = 0x646a6a6b;

    private static final String CLASS_STATE_FINISHED   = "finished";
    private static final String CLASS_STATE_IN_CLASS   = "in_class";
    private static final String CLASS_STATE_PLANNED    = "planned";
    private static final String CLASS_STATE_STARTING   = "starting";
    private static final String CLASS_STATE_STOPPING   = "stopping";

    // @brief Implements Fragment
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClassViewGroup = View.inflate(getActivity(), R.layout.course_class, null);

        LogManager.i("ClassFragment onCreate: init view group(layout/course_class.xml)");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        LogManager.i("ClassFragment: onCreateView: init all views(Text, Btn, ..)");

        initMessageHandler();

        initView();

        return mClassViewGroup;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogManager.i("ClassFragment: onKeyDown, key code: " + keyCode);

        // show the class fragment if it isn't visibility
        if (mClassViewGroup.getVisibility() == View.GONE) {
            mClassViewGroup.setVisibility(View.VISIBLE);
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            LogManager.i("ClassFragment: on key is back");

            showConfirmDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        mClassStatusView  = (TextView) mClassViewGroup.findViewById(R.id.class_status);

        mClassNameView    = (TextView) mClassViewGroup.findViewById(R.id.class_name);
        mClassTeacherView = (TextView) mClassViewGroup.findViewById(R.id.class_teacher);
        mClassTimeView    = (TextView) mClassViewGroup.findViewById(R.id.class_time);
        mClassContentView = (TextView) mClassViewGroup.findViewById(R.id.class_content);

        mClassCtrlBtn  = (Button) mClassViewGroup.findViewById(R.id.taking_class_ctrl);
        mHiddenThisBtn = (Button) mClassViewGroup.findViewById(R.id.hidden_this_view);

        mClassCtrlBtn.setOnClickListener(this);
        mHiddenThisBtn.setOnClickListener(this);

        // update fragment view
        updateClassFragmentView();
    }

    // @brief Init class fragment message handler
    private void initMessageHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_ON_START_CLASS:
                    onStartClassMsg(msg.arg1);
                    break;
                case MSG_ON_STOP_CLASS:
                    onStopClassMsg(msg.arg1);
                    break;
                default:
                    LogManager.i("ClassFragment unknown msg " + msg.what);
                    break;
                }
            }
        };
    }

    // @brief Implements View.onClickListener
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.taking_class_ctrl:
                if (!mInClass) startClass();
                else stopClass();

                break;
            case R.id.hidden_this_view:
                LogManager.i("ClassFragment hidden this fragment view");
                mClassViewGroup.setVisibility(view.GONE);

                break;
            default:
                break;
        }
    }

    // @brief Implements CourseThread.ClassNotify
    public void onStartClass(int result) {
        LogManager.i("ClassFragment Notify onStartClass, result: " + result);

        // prepare message
        Message msg = Message.obtain();
        msg.what = MSG_ON_START_CLASS;
        msg.arg1 = result;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("ClassFragment send MSG_ON_START_CLASS failed");
            return;
        }

        return;
    }

    public void onStopClass(int result) {
        LogManager.i("ClassFragment Notify onStopClass, result: " + result);

        // prepare message
        Message msg = Message.obtain();
        msg.what = MSG_ON_STOP_CLASS;
        msg.arg1 = result;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("ClassFragment send MSG_ON_STOP_CLASS failed");
            return;
        }

        return;
    }

    private void onStartClassMsg(int result) {
        LogManager.i("ClassFragment, onStartClass result: " + result);

        if (result == 0) {
            mCourse.status = CLASS_STATE_IN_CLASS;
        } else {
            LogManager.w("ClassFragment start class failed");
            mCourse.status = CLASS_STATE_PLANNED;
            //TODO: show a warning dialog
        }

        // update class fragment view
        updateClassFragmentView();

        return;
    }

    private void onStopClassMsg(int result) {
        LogManager.i("ClassFragment, onStopClass result: " + result);

        if (result == 0) {
            mCourse.status = CLASS_STATE_FINISHED;
        } else {
            LogManager.w("ClassFragment stop class failed");
            mCourse.status = CLASS_STATE_IN_CLASS;
            //TODO: show a waning dialog
        }

        // update class fragment view
        updateClassFragmentView();

        if (result == 0 && mDialog != null && mDialog.type() == CommonDialog.DIALOG_CONFIRM) {
            mDialog.dismiss();
            mDialog = null;
            popFragment();
        }

        return;
    }

    // @brief Sets current class' course
    public void setClassCourse(BusinessPlatform.TimeTable course) {
        mCourse = course;
        // calc class time: date + week-day
        mClassTime = CourseTableFragment.dateWithWeekDay(course.date) + " 第" + course.section + "节";
    }

    // @brief Sets course thread handler
    public void setCourseThread(CourseThread thread) {
        mCourseThread = thread;
    }

    // @brief Starts class
    private int startClass() {
        if (mCourse == null || mCourseThread == null) {
            LogManager.i("ClassFragment start class: No course or course thread");
            return 1;
        }

        mCourseThread.setClassNotify(this);
        // start
        mCourseThread.startClass(mCourse);

        // change course state
        mCourse.status = CLASS_STATE_STARTING;

        // update view
        updateClassFragmentView();

        return 0;
    }

    // @brief Stops class
    private int stopClass() {
        if (mCourse == null || mCourseThread == null) {
            LogManager.i("ClassFragment stop class: No course or course thread");
            return 1;
        }

        mCourseThread.setClassNotify(this);
        // stop
        mCourseThread.stopClass(mCourse);

        // change course state
        mCourse.status = CLASS_STATE_STOPPING;

        // update class card(fragment) view
        updateClassFragmentView();

        return 0;
    }

    // @brief Updates class fragment from class' course info
    private void updateClassFragmentView() {
        if (mCourse == null) return;

        mClassNameView.setText(mCourse.subject_name);
        mClassTeacherView.setText(mCourse.teacher_name);
        mClassTimeView.setText(mClassTime);
        mClassContentView.setText(mCourse.title);

        // set class status and ctrl btn view
        if (mCourse.status.equals(CLASS_STATE_PLANNED)) {
            mInClass = false;

            mClassStatusView.setText("课程信息");

            mClassCtrlBtn.setText("开始上课");
            mClassCtrlBtn.setTextColor(BUTTON_ENABLE_TEXT_COLOR);

            mClassCtrlBtn.setEnabled(true);
            mClassCtrlBtn.setFocusable(true);

        } else if (mCourse.status.equals(CLASS_STATE_IN_CLASS)) {
            mInClass = true;

            mClassStatusView.setText("正在上课");

            mClassCtrlBtn.setText("结束课程");
            mClassCtrlBtn.setTextColor(BUTTON_ENABLE_TEXT_COLOR);

            mClassCtrlBtn.setEnabled(true);
            mClassCtrlBtn.setFocusable(true);

        } else if (mCourse.status.equals(CLASS_STATE_FINISHED)) {
            mInClass = false;

            mClassStatusView.setText("课程已结束");

            mClassCtrlBtn.setText("开始上课");
            mClassCtrlBtn.setTextColor(BUTTON_DISABLE_TEXT_COLOR);

            mClassCtrlBtn.setEnabled(false);
            mClassCtrlBtn.setFocusable(false);

        } else if (mCourse.status.equals(CLASS_STATE_STARTING)) {

            // update class card view status
            mClassStatusView.setText("正在开始上课");

            // set class ctrl button disable
            mClassCtrlBtn.setText("开始上课");
            mClassCtrlBtn.setTextColor(BUTTON_DISABLE_TEXT_COLOR);

            mClassCtrlBtn.setEnabled(false);
            mClassCtrlBtn.setFocusable(false);

        } else if (mCourse.status.equals(CLASS_STATE_STOPPING)) {

            // update class card view
            mClassStatusView.setText("正在结束课程");

            // set class ctrl button disable
            mClassCtrlBtn.setText("结束课程");
            mClassCtrlBtn.setTextColor(BUTTON_DISABLE_TEXT_COLOR);

            mClassCtrlBtn.setEnabled(false);
            mClassCtrlBtn.setFocusable(false);
        }
    }


    /// Confirm, Warning, Error, Waiting dialog

    public void showConfirmDialog() {
        mDialog = new CommonDialog(mContext, CommonDialog.DIALOG_CONFIRM);

        mDialog.setTitle("确认退出");

        if (mInClass) mDialog.setExtraText("正在上课, 退出会结束课程");

        // set dialog button on click listener
        mDialog.setButtonOnClickListener("取消", "退出", (int button)-> {
            switch (button) {
            case CommonDialog.LEFT_BUTTON:
                mDialog.dismiss();
                mDialog = null;
                break;
            case CommonDialog.RIGHT_BUTTON:
                if (mInClass) {
                    LogManager.i("ClassFragment Confirm dialog in-class, stop class");
                    stopClass();
                } else {
                    mDialog.dismiss();
                    mDialog = null;
                    popFragment();
                }
                break;
            default:
                break;
            }
        });

        mDialog.show();
    }
}
