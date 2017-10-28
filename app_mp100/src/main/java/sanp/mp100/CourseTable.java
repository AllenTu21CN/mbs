package sanp.mp100; 

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.GridView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.adapter.CourseAdapter;
import sanp.mp100.adapter.CourseThread;
import sanp.mp100.integration.BusinessPlatform.TimeTable;

/**
 * @brief Course table activity, It will show courses on course table view.
 * Course will be updated by the course adapter.
 *
 * @author will@1dao2.com
 * @date   2017/10/12
 *
 * @modified 2017/10/28, add this into Monica
 * 
 */

public class CourseTable extends Activity implements CourseThread.Notify {

    private Context  mContext;

    // course table view
    private GridView mCourseTable;

    // course adapter
    private CourseAdapter mCourseAdapter = null;
    // checkout course thread
    private CourseThread  mCourseThread = null;
    private Boolean       mCourseThreadRunning = false;


    // ui thread message handler
    private Handler mHandler = null;

    // message
    private static final int MSG_COURSE_THREAD_READY = 0;
    private static final int MSG_UPDATE_COURSE_TABLE = 1;

    // Implements from CourseThread.Notify
    // - The course thread is Ready
    public void onCourseThreadReady() {
        LogManager.i("CourseTable onCourseThreadReady, send msg to the ui thread");

        // prepare message: ready
        Message msg = Message.obtain();
        msg.what = MSG_COURSE_THREAD_READY;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("CourseTable onCourseThreadReady, send message failed");
            return;
        }

        return;
    }

    // - Something error is happened
    public void onError(int error) {
        //TODO
        mCourseThreadRunning = false;
    }

    // - Checkout courses suc
    public void onCheckoutCourse(List<TimeTable> list) {
        LogManager.i("CourseTable onCheckoutCourse: " + list);

        // prepare message: update course table view
        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_COURSE_TABLE;

        msg.obj  = list;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("CourseTable onCheckoutCourse, send message failed");
            return;
        }

        return;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set screen orientation: landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.course_table);

        mContext = this;

        initUiMessageHandler();

        initView();
    }

    // init course table view
    private void initView() {
        // find the course tables
        mCourseTable  = (GridView) findViewById(R.id.course_table_gird_view);

        // course adapter
        mCourseAdapter = new CourseAdapter(mContext);

        // set adapter into course table view
        mCourseTable.setAdapter(mCourseAdapter);

        // create a course checkout thread
        mCourseThread = new CourseThread(mCourseAdapter, this);
    }

    // init ui message handler
    private void initUiMessageHandler() {
       mHandler = new Handler() {
           @Override
           public void handleMessage(Message msg) {
               switch (msg.what) {
               case MSG_COURSE_THREAD_READY:
                   LogManager.i("Course Thread is ready, try to checkout courses");
                   mCourseThreadRunning = true;
                   checkoutCourseForCurrent();
                   break;
               case MSG_UPDATE_COURSE_TABLE:
                   LogManager.i("Ui Thread need to update course table");
                   //TODO, update course table view
                   break;
               default:
                   LogManager.w("Ui Thread handle unknown message: " + msg.what);
                   break;
               }
           }
       };
    }

    // checkout current time courses
    private void checkoutCourseForCurrent() {
        // current time
        Date date = new Date();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        LogManager.i("Current date is: " +  format.format(date));

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(date);

        // current week: monday's date
        Date monday = getFirstDateOfWeek(date);

        LogManager.i("Current week, Monday date is: " + format.format(monday));

        int week_days = 7;
        // checkout this week's courses
        mCourseThread.checkoutCourse(monday, week_days);
    }


    // @brief Gets Monday's date. Note, First day of week is Monday.
    private Date getFirstDateOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Note, calendar use Sunday as the first day of week.
        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
            calendar.add(Calendar.DATE, -1);
        }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        return calendar.getTime();
    }
}
