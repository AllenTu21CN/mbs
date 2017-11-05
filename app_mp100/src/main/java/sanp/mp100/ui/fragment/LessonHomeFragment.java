package sanp.mp100.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.avalon.libs.base.utils.ToastManager;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.integration.RBUtil;
import sanp.mp100.ui.adapter.CourseThread;

public class LessonHomeFragment extends BaseFragment implements View.OnClickListener, CourseThread.ClassNotify {

    public static final String TAG = "LessonHomeFragment";

    public interface ChildFragment {
        void onStatusChanged();
    }

    private static final String CLASS_STATE_FINISHED   = "finished";
    private static final String CLASS_STATE_IN_CLASS   = "in_class";
    private static final String CLASS_STATE_PLANNED    = "planned";
    private static final String CLASS_STATE_STARTING   = "starting";
    private static final String CLASS_STATE_STOPING    = "stopping";

    public static LessonHomeFragment mLessonHomeFragment = null;

    public static LessonHomeFragment getInstance() {
        synchronized (LessonHomeFragment.class) {
            if (mLessonHomeFragment == null)
                mLessonHomeFragment = new LessonHomeFragment();
            return mLessonHomeFragment;
        }
    }

    enum State {
        None("未开课"),
        Starting("课程启动中"),
        Started("上课中"),
        Stopping("下课中"),
        Stopped("已下课");

        private String dsp;
        private State(String dsp) {
            this.dsp = dsp;
        }
        public String toString() {
            return dsp;
        }
    }

    private View mView;

    private TextView mTextLessonStatus;
    private TextView mTextLessonInfo;

    private State mState;
    private BusinessPlatform.TimeTable mLessonInfo;

    private final int MSG_UPDATE_VIEW = 0;

    private Handler mHandler;
    private CourseThread mCourseThread;

    private Object mLock = new Object();
    private ChildFragment mChildFragment = null;

    public void reset(BusinessPlatform.TimeTable timetable, CourseThread thread) {
        mLessonInfo = timetable;
        mCourseThread = thread;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = View.inflate(getActivity(), R.layout.lesson_home_view, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initData();
        initView();
        initHandler();
        return mView;
    }

    @Override
    public void onDestroyView() {
        RBUtil.getInstance().setScene(RBUtil.Scene.ShowTimeTable);
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        showFragmentTrans(R.id.fragmentLayout, LessonMenuFragment.getInstance(), LessonMenuFragment.TAG);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showFragmentTrans(R.id.fragmentLayout, LessonMenuFragment.getInstance(), LessonMenuFragment.TAG);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mState == State.Started) {
                showFragmentTrans(R.id.fragmentLayout, LessonMenuFragment.getInstance(), LessonMenuFragment.TAG);
                ToastManager.showToast(mContext, "请先下课");
                return true;
            } else if(mState == State.Starting || mState == State.Stopping) {
                ToastManager.showToast(mContext, "操作中,请稍等");
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void setChild(ChildFragment child) {
        synchronized (mLock) {
            mChildFragment = child;
        }
    }

    private void initData() {
        if(mLessonInfo == null) {
            throw new RuntimeException("LessonHomeFragment logical error: reset first");
        }

        if (mLessonInfo.status.equals(CLASS_STATE_PLANNED)) {
            mState = State.None;
        } else if (mLessonInfo.status.equals(CLASS_STATE_IN_CLASS)) {
            mState = State.Started;
        } else if (mLessonInfo.status.equals(CLASS_STATE_FINISHED)) {
            mState = State.Stopped;
        } else if (mLessonInfo.status.equals(CLASS_STATE_STARTING)) {
            mState = State.Starting;
        } else if (mLessonInfo.status.equals(CLASS_STATE_STOPING)) {
            mState = State.Stopping;
        }

        RBUtil.getInstance().setScene(RBUtil.Scene.InClass);
    }

    private void initView() {
        mTextLessonStatus = (TextView) mView.findViewById(R.id.text_lesson_status);
        mTextLessonInfo = (TextView) mView.findViewById(R.id.text_lesson_info);

        mTextLessonStatus.setOnClickListener(this);
        mTextLessonInfo.setOnClickListener(this);

        updateView();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_VIEW:
                        updateView();
                        break;
                    default:
                        LogManager.e("LessonHomeFragment unknown msg " + msg.what);
                        break;
                }
            }
        };
    }

    private void updateView() {
        String time = CourseTableFragment.date2WeekDay(mLessonInfo.date) + " 第" + mLessonInfo.section + "节";
        mTextLessonInfo.setText(String.format("%s %s: %s", time, mLessonInfo.teacher_name, mLessonInfo.title));
        mTextLessonStatus.setText(mState.toString());

        synchronized (mLock) {
            if(mChildFragment != null)
                mChildFragment.onStatusChanged();
        }
    }

    public State state() {
        return mState;
    }

    public void switchLesson() {
        if (mLessonInfo == null || mCourseThread == null) {
            throw new RuntimeException("LessonHomeFragment logical error: reset first");
        }

        if(mState == State.None) {
            mState = State.Starting;
            mLessonInfo.status = CLASS_STATE_STARTING;
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIEW));

            mCourseThread.setClassNotify(this);
            mCourseThread.startClass(mLessonInfo);
        } else if(mState == State.Started) {
            mState = State.Stopping;
            mLessonInfo.status = CLASS_STATE_STOPING;
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIEW));

            mCourseThread.setClassNotify(this);
            mCourseThread.stopClass(mLessonInfo);
        }
    }

    public void onStartClass(int result) {
        LogManager.i("LessonHomeFragment onStartClass, result: " + result);
        if (result == 0) {
            mState = State.Started;
            mLessonInfo.status = CLASS_STATE_IN_CLASS;
        } else {
            mState = State.None;
            mLessonInfo.status = CLASS_STATE_PLANNED;
            LogManager.w("LessonHomeFragment start class failed");
            ToastManager.showToast(mContext, "课程启动失败");
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIEW));
    }

    public void onStopClass(int result) {
        LogManager.i("LessonHomeFragment onStopClass, result: " + result);
        if (result != 0) {
            LogManager.w("LessonHomeFragment stop class failed");
            ToastManager.showToast(mContext, "下课失败");
        }
        mState = State.Stopped;
        mLessonInfo.status = CLASS_STATE_FINISHED;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIEW));
    }

}
