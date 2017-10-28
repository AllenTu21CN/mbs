package com.will.course.coursetable.adapter;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.BaseAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.LogManager;

/**
 * @file  CourseThread.java
 * @brief checkout courses thread
 *
 * @author will@1dao2.com
 * @date 2017/10/27
 * */

public class CourseThread implements Runnable {

    // course call method message
    public static final int MSG_CHECKOUT_COURSE = 0;
    public static final int MSG_STOP = 1;

    private Thread mCourseThread = null;
//  private Object mLock = new Object();

    // course table view adapter
    private BaseAdapter mCourseAdapter;
    // course lesson list
    private List<TimeTable>  mCourseList = null;
    // classroom, which is connect with service
    private BusinessPlatform mClassRoom = null;

    // course message variables
    private Handler mHandler = null;


    public CourseThread(BaseAdapter adapter) {
        mCourseAdapter = adapter;

        // init the thread
        init();
    }

    private void init() {
        LogManager.i("CourseThread start a course thread");
        mCourseThread = new Thread(this, "Course Thread");
        mCourseThread.start();

        //TODO synchronized, wait thread runnning
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
        LogManager.i("Try to checkout course date: " + date + "for " + days + " days");

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
        message.what = MSG_STOP;

        if (!mHandler.sendMessage(message)) {
            LogManager.e("Send MSG_STOP message failed");
            return 1;
        }

        return 0;
    }

    // @brief Obtains course list directly
    // Obtains course list after mCourseAdapter.notifyDataSetChanged()
    public List<TimeTable> obtainCourseList() { return mCourseList;}

    // @brief course thread entry and process funcion
    @Override 
    public void run() {
        mClassRoom = BusinessPlatform.getInstance();
        if (mClassRoom == null) {
            LogManager.e("CourseTable get classroom from @BusinessPlatform.class");
            return;
        }

        LogManager.i("Enter course table thread process function");

        Looper.prepare();

        // new a message handler
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case MSG_CHECKOUT_COURSE: /* checkout course message */
                    onCheckoutCourseMsg((Date)msg.obj, msg.arg1);
                    break;
                case MSG_STOP: /* stop course thread message */
                    Looper.myLooper().quit();
                    break;
                default:
                    LogManager.w("Unknown message: " + msg.what);
                    break;
                }
            }
        };

        // enter message handle loop
        Looper.loop();

        // exit
        LogManager.i("Exit course thread process function");
    }

    // process checkout course message
    private void onCheckoutCourseMsg(Date date, int days) {
        Calendar calendar = Calendar.getInstance();

        // calc end of date, add @days
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);

        Date end = calendar.getTime();

        // convert data into string format: "yyyy-MM-dd"
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");

        String date_string = date_format.format(date);
        String end_string  = date_format.format(date);

        // checkout courses from classroom(BusinessPlatform) 
        mCourseList = mClassRoom.getLessonTimetable(0, date_string, end_string);

        // notify to adapter update course table
        mCourseAdapter.notifyDataSetChanged();
        return;
    }
}
