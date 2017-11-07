package sanp.mp100.ui.adapter; 

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sanp.tools.utils.LogManager;
import sanp.mp100.integration.BusinessPlatform;
import sanp.mp100.integration.BusinessPlatform.TimeTable;
import sanp.mp100.integration.BusinessPlatformPostman;
import sanp.mp100.integration.RBUtil;

/**
 * @file  CourseThread.java
 * @brief checkout courses thread
 *
 * @author will@1dao2.com
 * @date 2017/10/27
 * */

public class CourseThread implements Runnable, BusinessPlatform.Observer {

    public static final int ERROR_LOST_CONNECTION = 1;

    public interface Notify {
        // The course thread is ready
        void onReady();

        // Something error is happened
        void onError(int error);

        // Checkout courses suc
        void onCheckoutCourse(List<TimeTable> list);
    }

    public interface ClassNotify {
        // Note: @result: 0 is success, or failed

        // Notify the result of start class
        void onStartClass(int result);

        // Notify the result of stop class
        void onStopClass(int result);
    }

    // course call method message
    public static final int MSG_CHECKOUT_COURSE = 0;
    public static final int MSG_STOP_THREAD     = 1;
    public static final int MSG_START_CLASS     = 2;
    public static final int MSG_STOP_CLASS      = 3;

    private Thread mCourseThread = null;
//  private Object mLock = new Object();

    // notify, CourseTable activity
    private Notify      mNotify;

    private ClassNotify mClassNotify;

    // course lesson list
    private List<TimeTable>  mCourseList = null;
    // classroom, which is connect with service
    private BusinessPlatform mClassRoom = null;

    // course message variables
    private Handler mHandler = null;


    public CourseThread(Notify notify) {
        mNotify = notify;

        // init the thread
        init();
    }

    private void init() {
        LogManager.i("CourseThread start a course thread");
        mCourseThread = new Thread(this, "Course Thread");
        mCourseThread.start();

        return;
    }

    private void deinit() {
        //TODO
    }

    // @brief Checkouts courses from service.
    // This is a async method, you can't get course list from this method.
    //
    // @param date [IN] checkout course from date
    // @param days [IN] checkout course for number days
    //
    // @return 0, suc to call; or -1 is failed
    public int checkoutCourse(Date date, int days) {
        LogManager.i("Try to checkout course date: " + date + " for " + days + " days");

        // prepare checkout course message
        Message message = Message.obtain();
        message.what = MSG_CHECKOUT_COURSE;
        message.obj  = date;
        message.arg1 = days;

        if (!mHandler.sendMessage(message)) {
            LogManager.e("Send MSG_CHECKOUT_COURSE message failed");
            return 1;
        }

        return 0;
    }

    // @brief Stops course thread. Async method, send stop message.
    public int stopCourseThread() {
        LogManager.i("Try to stop course thread");

        // prepare stop message
        Message message = Message.obtain();
        message.what = MSG_STOP_THREAD;

        if (!mHandler.sendMessage(message)) {
            LogManager.e("Send MSG_STOP_THREAD message failed");
            return 1;
        }

        return 0;
    }

    // @brief Starts class
    // @param course [IN] start class' course
    public int startClass(TimeTable course) {
        LogManager.i("Try to start class: " + course.subject_name);

        // prepare start class
        Message message = Message.obtain();
        message.what = MSG_START_CLASS;
        message.obj  = course;

        if (!mHandler.sendMessage(message)) {
            LogManager.e("Send MSG_START_CLASS message failed");
            return 1;
        }

        return 0;
    }

    // @brief Stops class
    public int stopClass(TimeTable course) {
        LogManager.i("Try to stop class: " + course.subject_name);

        // prepare stop class
        Message message = Message.obtain();
        message.what = MSG_STOP_CLASS;
        message.obj  = course;

        if (!mHandler.sendMessage(message)) {
            LogManager.i("Send MSG_STOP_CLASS message failed");
            return 1;
        }

        return 0;
    }

    // @brief Sets class notifier
    public void setClassNotify(@Nullable  ClassNotify notify) {
        mClassNotify = notify;
    }

    // @brief course thread entry and process funcion
    @Override 
    public void run() {
        mClassRoom = BusinessPlatform.getInstance();
        if (mClassRoom == null) {
            LogManager.e("CourseThread get classroom from @BusinessPlatform.class");
            return;
        }

        // add notify callback into business platform
        mClassRoom.addStateObserver(this);

        LogManager.i("CourseThread: Enter course table thread process function");

        // notify CourseTable thread is ready.
        if (mClassRoom.connectingState() == BusinessPlatformPostman.State.READY) {
            LogManager.i("CourseThread is ready: business platform is connected");
            mNotify.onReady();
        }

        Looper.prepare();

        // new a message handler
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case MSG_CHECKOUT_COURSE: /* checkout course message */
                    onCheckoutCourseMsg((Date)msg.obj, msg.arg1);
                    break;
                case MSG_STOP_THREAD: /* stop course thread message */
                    Looper.myLooper().quit();
                    break;
                case MSG_START_CLASS:
                    onStartClassMsg((TimeTable)msg.obj);
                    break;
                case MSG_STOP_CLASS:
                    onStopClassMsg((TimeTable)msg.obj);
                    break;
                default:
                    LogManager.w("CourseThread Unknown message: " + msg.what);
                    break;
                }
            }
        };

        // enter message handle loop
        Looper.loop();

        // remove notify callback into business platform
        mClassRoom.removeStateObserver(this);

        // exit
        LogManager.i("CourseThread: Exit course thread ..");
    }

    // process checkout course message
    private void onCheckoutCourseMsg(Date date, int days) {
        Calendar calendar = Calendar.getInstance();

        // calc end of date, add @days
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);

        Date end = calendar.getTime();

        // convert data into string format: "yyyy-MM-dd"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        String date_string = format.format(date);
        String end_string  = format.format(end);

        LogManager.i("CourseThread onCheckoutCourseMsg: try to checkout courses" +
                "(" + date_string + " ~ " + end_string  + ") from service");

        try {
            // checkout courses from classroom(BusinessPlatform)
            mCourseList = mClassRoom.getLessonTimetable(1, date_string, end_string);
        } catch (InternalError|Exception e) {
            e.printStackTrace();
            LogManager.w("onCheckoutCourse checkout courses failed: " + e);
            //TODO, 2017/10/31, need to notify the failed
            return;
        }

        // notify to CourseTable to update table view
        mNotify.onCheckoutCourse(mCourseList);

        return;
    }

    // process start class message
    private void onStartClassMsg(TimeTable course) {
        int result = 0;

        LogManager.i("CourseThread onStartClass: start class");

        try {
            BusinessPlatform.getInstance().startPlanned(course.id);
            LogManager.i("CourseThread success to start class");
        } catch (InternalError|Exception e) {
            e.printStackTrace();
            LogManager.w("onStartClass start class failed: " + e);
            result = -1;
        }

        LogManager.i("CourseThread notify ui start class suc");
        // notify the result of start class
        if (mClassNotify != null) mClassNotify.onStartClass(result);
    }

    // process stop class message
    private void onStopClassMsg(TimeTable course) {
        int result = 0;

        LogManager.i("CourseThread onStopClass: stop class");

        try {
            BusinessPlatform.getInstance().stopPlanned(course.id);
            LogManager.i("CourseThread success to stop class");
        } catch (InternalError|Exception e) {
            e.printStackTrace();
            LogManager.w("onStopClass stop class failed: " + e);
            result = -1;
        }

        LogManager.i("CourseThread notify ui stop class suc");
        // notify the result of stop class
        if (mClassNotify != null) mClassNotify.onStopClass(result);
    }

    // Implements BusinessPlatform.Observer
    public void onConnectingState(boolean connected) {
        if (connected) {
            LogManager.i("CourseThread success to connect with service");
            mNotify.onReady();
        } else {
            LogManager.w("CourseThread lost connection with service");
            mNotify.onError(ERROR_LOST_CONNECTION);
        }
    }

    public void onActivatingState(boolean activated) {
        LogManager.i("CourseThread onActivatingState: " + activated);
        //do nothing
    }

    public void onBindingState(boolean bound) {
        LogManager.i("CourseThread onBindingState: " + bound);
        //do nothing
    }
}
