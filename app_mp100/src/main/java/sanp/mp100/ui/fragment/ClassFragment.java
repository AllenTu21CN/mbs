package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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
    public static ClassFragment mClassFragment;

    public static ClassFragment getInstance() {
        if (mClassFragment == null)
            mClassFragment = new ClassFragment();

        return mClassFragment;
    }

    // see: taking_class.xml
    private View mClassViewGroup;

    private TextView mClassStatusView;

    // class info view and info strings
    private TextView mClassNameView;
    private TextView mClassTeacherView;
    private TextView mClassTimeView;
    private TextView mClassContentView;

    // taking class button
    private Button mTakingClassCtrlBtn;
    private Button mHiddenThisView;

    private BusinessPlatform.TimeTable mCourse;

    private Boolean mIsTakingClass;

    // course thread
    private CourseThread mCourseThread;

    private final int MSG_ON_START_CLASS = 0;
    private final int MSG_ON_STOP_CLASS = 1;
    // class fragment message handler
    private Handler mHandler;

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

        mIsTakingClass = false;
        initView();

        return mClassViewGroup;
    }

    private void initView() {
        mClassStatusView  = (TextView) mClassViewGroup.findViewById(R.id.class_status);

        mClassNameView    = (TextView) mClassViewGroup.findViewById(R.id.class_name);
        mClassTeacherView = (TextView) mClassViewGroup.findViewById(R.id.class_teacher);
        mClassTimeView    = (TextView) mClassViewGroup.findViewById(R.id.class_time);
        mClassContentView = (TextView) mClassViewGroup.findViewById(R.id.class_content);

        mTakingClassCtrlBtn = (Button) mClassViewGroup.findViewById(R.id.taking_class_ctrl);
        mHiddenThisView     = (Button) mClassViewGroup.findViewById(R.id.hidden_this_view);

        mTakingClassCtrlBtn.setOnClickListener(this);
        mHiddenThisView.setOnClickListener(this);

        // show class info
        updateClassCourseInfo();
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
                if (!mIsTakingClass) startClass();
                else stopClass();

                break;
            case R.id.hidden_this_view:
                //todo
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

        mIsTakingClass = true;

        String status_text = "正在上课";
        String ctrl_text   = "结束上课";

        if (result != 0) {
            LogManager.w("ClassFragment start class failed");

            mIsTakingClass = false;

            status_text = "课程信息";
            ctrl_text   = "开始上课";
        }

        mClassStatusView.setText(status_text);

        mTakingClassCtrlBtn.setText(ctrl_text);
        mTakingClassCtrlBtn.setEnabled(true);
        mTakingClassCtrlBtn.setFocusable(true);

        return;
    }

    private void onStopClassMsg(int result) {
        LogManager.i("ClassFragment, onStopClass result: " + result);

        mIsTakingClass = false;

        String  status_text = "课程已结束";
        String  ctrl_text   = "开始上课";
        boolean able        = false;

        if (result != 0) {
            LogManager.w("ClassFragment stop class failed");

            mIsTakingClass = true;

            status_text = "正在上课";
            ctrl_text   = "结束上课";
            able        = true;
        }

        mClassStatusView.setText(status_text);

        mTakingClassCtrlBtn.setText(ctrl_text);
        mTakingClassCtrlBtn.setEnabled(able);
        mTakingClassCtrlBtn.setFocusable(able);

        return;
    }

    // @brief Sets current class' course
    public void setClassCourse(BusinessPlatform.TimeTable course) {
        mCourse = course;
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

        // update class card view status
        mClassStatusView.setText("请稍等 正准备开始上课");

        // set class ctrl button disable
        mTakingClassCtrlBtn.setEnabled(false);
        mTakingClassCtrlBtn.setFocusable(false);

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

        // update class card view
        mClassStatusView.setText("请稍等 正准备结束课程");

        // set class ctrl button disable
        mTakingClassCtrlBtn.setEnabled(false);
        mTakingClassCtrlBtn.setFocusable(false);

        return 0;
    }

    // @brief Updates class' course info
    private void updateClassCourseInfo() {
        if (mCourse == null) return;

        mClassNameView.setText(mCourse.subject_name);
        mClassTeacherView.setText(mCourse.teacher_name);
        mClassTimeView.setText(mCourse.date + " 第" + mCourse.section + "节");
        mClassContentView.setText(mCourse.title);

        // set class status and ctrl btn view
        if (mCourse.status.equals("planned")) {
            mIsTakingClass = false;

            mClassStatusView.setText("课程信息");

            mTakingClassCtrlBtn.setText("开始上课");
            mTakingClassCtrlBtn.setEnabled(true);
            mTakingClassCtrlBtn.setFocusable(true);

        } else if (mCourse.status.equals("in_class")) {
            mIsTakingClass = true;

            mClassStatusView.setText("正在上课");

            mTakingClassCtrlBtn.setText("结束课程");
            mTakingClassCtrlBtn.setEnabled(true);
            mTakingClassCtrlBtn.setFocusable(true);

        } else if (mCourse.status.equals("finished")) {
            mClassStatusView.setText("课程已结束");

            mTakingClassCtrlBtn.setEnabled(false);
            mTakingClassCtrlBtn.setFocusable(false);
        }
    }
}
