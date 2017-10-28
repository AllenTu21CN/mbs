package sanp.mp100.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.ui.adapter.CourseAdapter;
import sanp.mp100.ui.adapter.CourseThread;
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

public class CourseTable extends Activity implements View.OnClickListener, CourseThread.Notify {

    private Context  mContext;

    // course table view
    private GridView mCourseTable;
    // course table Monday's date
    private Date     mCourseTableMonday;

    // course table date line bar
    private Button   mPrevWeekBtn;
    private Button   mNextWeekBtn;
    private TextView mMondayDateView;
    private TextView mSundayDataView;

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
    @Override
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
    @Override
    public void onError(int error) {
        //TODO
        mCourseThreadRunning = false;
    }

    // - Checkout courses suc
    @Override
    public void onCheckoutCourse(List<TimeTable> list) {
        LogManager.i("CourseTable onCheckoutCourse, update courses");

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

        // course date line bar
        mPrevWeekBtn = (Button) findViewById(R.id.prev_week_btn);
        mNextWeekBtn = (Button) findViewById(R.id.next_week_btn);

        mPrevWeekBtn.setOnClickListener(this);
        mNextWeekBtn.setOnClickListener(this);

        mMondayDateView = (TextView) findViewById(R.id.monday_date_view);
        mSundayDataView = (TextView) findViewById(R.id.sunday_date_view);

        // init date
        initDate();

        // create a course checkout thread
        mCourseThread = new CourseThread(mCourseAdapter, this);
    }

    // @brief Init date
    private void initDate() {
        // current time
        Date date = new Date();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(date);

        // current week: monday's date
        mCourseTableMonday = getFirstDateOfWeek(date);

        LogManager.i("Today is " +  format.format(date) + ", Monday is " +
            format.format(mCourseTableMonday));

        // set current date into date line bar
        updateDateLineBarView();
    }

    private void updateDateLineBarView() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        // set monday's date
        mMondayDateView.setText(format.format(mCourseTableMonday));

        Calendar calendar = Calendar.getInstance();

        // calc sunday's date
        calendar.setTime(mCourseTableMonday);
        calendar.add(Calendar.DAY_OF_YEAR, 6);

        Date sunday = calendar.getTime();

        // set sunday's date
        mSundayDataView.setText(format.format(sunday));
    }

    // init ui message handler
    private void initUiMessageHandler() {
       mHandler = new Handler() {
           @Override
           public void handleMessage(Message msg) {
               switch (msg.what) {
               case MSG_COURSE_THREAD_READY:
                   LogManager.i("Ui Thread handle MSG_COURSE_THREAD_READY");
                   mCourseThreadRunning = true;
                   checkoutCourseForCurrent();
                   break;
               case MSG_UPDATE_COURSE_TABLE:
                   LogManager.i("Ui Thread handle MSG_UPDATE_COURSE_TABLE");
                   updateCourseTable((List<TimeTable>)msg.obj);
                   break;
               default:
                   LogManager.w("Ui Thread handle unknown message: " + msg.what);
                   break;
               }
           }
       };
    }

    // @brief Updates course table
    private void updateCourseTable(List<TimeTable> list) {
        LogManager.i("CourseTable need to update course table");

        /* Debug output course list. Don't output here,
         * it'll be output in CourseAdapter.updateCourseList()
        String courses = "Table:\n";
        for (TimeTable it : list) {
            courses += "Id: " + it.id +
                "; Type: " + it.type +
                "; Subject Name: " + it.subject_name +
                "; Title:  " + it.title +
                "; Date: " + it.date +
                "; Section: "+ it.section + "\n";
        }

        LogManager.i(courses);
        */

        //TODO, update course table view
        //2017/10/28

        // update course adapter course-list
        mCourseAdapter.updateCourseList(list, mCourseTableMonday);

        // notify to adapter update course table
        mCourseAdapter.notifyDataSetChanged();

        return;
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

    // @brief Calculates days between date1 and date2: date2 - date1
    public static int daysBetween(Date date1,Date date2)
    {
        /*
        Calendar cal = Calendar.getInstance();

        cal.setTime(date1);
        long time1 = cal.getTimeInMillis();

        cal.setTime(date2);
        long time2 = cal.getTimeInMillis();

        long between_days=(time2-time1)/(1000*3600*24);

        return Integer.parseInt(String.valueOf(between_days));
        */

        java.util.Calendar calst = java.util.Calendar.getInstance();
        java.util.Calendar caled = java.util.Calendar.getInstance();
        calst.setTime(date1);
        caled.setTime(date2);

        calst.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calst.set(java.util.Calendar.MINUTE, 0);
        calst.set(java.util.Calendar.SECOND, 0);
        caled.set(java.util.Calendar.HOUR_OF_DAY, 0);
        caled.set(java.util.Calendar.MINUTE, 0);
        caled.set(java.util.Calendar.SECOND, 0);

        // calculate
        int days = ((int) (caled.getTime().getTime() / 1000) - (int) (calst
                .getTime().getTime() / 1000)) / 3600 / 24;

        return days;
    }

    // @brief Handles button click
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.prev_week_btn:
                checkoutPrevWeekCourse();
                break;
            case R.id.next_week_btn:
                checkoutNextWeekCourse();
                break;

            default:
                break;
        }
    }

    // @brief Checkouts current time courses
    private void checkoutCourseForCurrent() {
        LogManager.i("CourseTable checkout current week's courses");

        // checkout this week's courses
        // days: 6 + include monday = 7 days
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }

    // @brief Checkouts prev week courses
    private void checkoutPrevWeekCourse() {
        LogManager.i("CourseTable checkout prev week's courses");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        LogManager.i("CourseTable table date: " + format.format(mCourseTableMonday));

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(mCourseTableMonday);

        // calculate prev week
        calendar.add(Calendar.DAY_OF_YEAR, -7);

        mCourseTableMonday = calendar.getTime();
        LogManager.i("CourseTable prev week: " + format.format(mCourseTableMonday));

        // update date line text view
        updateDateLineBarView();

        // checkout this week's courses
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }

    // @brief Checkouts next week courses
    private void checkoutNextWeekCourse() {
        LogManager.i("CourseTable checkout next week's courses");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        LogManager.i("CourseTable table date: " + format.format(mCourseTableMonday));

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(mCourseTableMonday);

        // calculate prev week
        calendar.add(Calendar.DAY_OF_YEAR, 7);

        mCourseTableMonday = calendar.getTime();
        LogManager.i("CourseTable next week: " + format.format(mCourseTableMonday));

        // update date line text view
        updateDateLineBarView();

        // checkout this week's courses
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }
}

