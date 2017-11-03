package sanp.mp100.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

public class CourseTableFragment extends BaseFragment implements View.OnClickListener, CourseThread.Notify {

    public static final String TAG = "CourseTableFragment";

    // course table layout view: see course_table.xml
    private View     mCourseTableViewGroup;

    // course table view: see course_table.xml:gridview
    private GridView mCourseTable;
    // course table Monday's date
    private Date     mCourseTableMonday;

    // course table date line bar
    private Button   mPrevWeekBtn;
    private Button   mNextWeekBtn;
    private Button   mRefreshBtn;
    private TextView mMondayDateView;
    private TextView mSundayDataView;

    // course adapter
    private CourseAdapter mCourseAdapter = null;
    // checkout course thread
    private CourseThread  mCourseThread = null;


    // course table message handler
    private Handler mHandler = null;

    // message
    private static final int MSG_COURSE_TABLE_READY = 0;
    private static final int MSG_UPDATE_COURSE_TABLE = 1;

    // course table fragment single instance
    private static CourseTableFragment mCourseTableFragment = null;
    public static CourseTableFragment getInstance() {
        if (mCourseTableFragment == null)
            mCourseTableFragment = new CourseTableFragment();

        return mCourseTableFragment;
    }

    // @brief Implements from CourseThread.Notify
    // - The course thread is Ready
    @Override
    public void onReady() {
        LogManager.i("CourseTableFragment onReady, send msg to ui thread");

        // prepare message: ready
        Message msg = Message.obtain();
        msg.what = MSG_COURSE_TABLE_READY;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("CourseTableFragment onReady, send message failed");
            return;
        }

        return;
    }

    // - Something error is happened
    @Override
    public void onError(int error) {
        //TODO
    }

    // - Checkout courses suc
    @Override
    public void onCheckoutCourse(List<TimeTable> list) {
        LogManager.i("CourseTableFragment onCheckoutCourse, update courses");

        // prepare message: update course table view
        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_COURSE_TABLE;

        msg.obj  = list;

        if (!mHandler.sendMessage(msg)) {
            LogManager.e("CourseTableFragment onCheckoutCourse, send message failed");
            return;
        }

        return;
    }

    @Override
    public void onAttach(Context context) {
        LogManager.i("CourseTableFragment onAttach");

        super.onAttach(context);
    }

    // @brief Implements, method is defined in Fragment
    // - Fragment onCreate
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogManager.i("CourseTableFragment onCreate, load course_table.xml");

        super.onCreate(savedInstanceState);

        // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // course layout view
        mCourseTableViewGroup = View.inflate(getActivity(), R.layout.course_table, null);

        return;
    }

    // - Fragment onCreateView
    @Nullable
    @Override 
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        LogManager.i("CourseTableFragment onCreateView, init message handler and views");

        // init message handler
        initMessageHandler();

        // load all view: date line bar, course table gridview and so on.
        initView();

        return mCourseTableViewGroup;
    }

    // - Fragment onDestroyView
    @Override
    public void onDestroyView() {
        LogManager.i("CourseTableFragment onDestroyView");

        super.onDestroyView();
        // stop course thread
        mCourseThread.stopCourseThread();
    }

    // - Fragment onDestroy
    @Override
    public void onDestroy() {
        LogManager.i("CourseTableFragment onDestroy");
        super.onDestroy();
        //TODO: release all resources
    }

    @Override
    public void onStart() {
        LogManager.i("CourseTableFragment onStart");
        super.onStart();
    }

    @Override
    public void onPause() {
        LogManager.i("CourseTableFragment onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        LogManager.i("CourseTableFragment onStop");
        super.onStop();
    }

    @Override
    public void onResume(){
        LogManager.i("CourseTableFragment onResume");
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            LogManager.i("CourseTableFragment: on key is back");

            CommonDialog dialog = new CommonDialog(mContext, CommonDialog.DIALOG_ERROR);
            dialog.setTitle("确认退出");
            dialog.setButtonOnClickListener("取消", "退出", (int button)-> {
                switch (button) {
                    case CommonDialog.LEFT_BUTTON:
                        dialog.dismiss();
                        break;
                    case CommonDialog.RIGHT_BUTTON:
                        dialog.dismiss();
                        popFragment();
                        break;
                    default:
                        break;
                }
            });

            dialog.show();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // @brief Init course table view
    private void initView() {
        // find the course tables
        mCourseTable  = mCourseTableViewGroup.findViewById(R.id.course_table_grid_view);

        // course date line bar
        mPrevWeekBtn = mCourseTableViewGroup.findViewById(R.id.prev_week_btn);
        mNextWeekBtn = mCourseTableViewGroup.findViewById(R.id.next_week_btn);
        mRefreshBtn  = mCourseTableViewGroup.findViewById(R.id.refresh_btn);

        //TODO: enable after course thread is running
        //mRefreshBtn.setEnabled(false);
        //mRefreshBtn.setFocusable(false);


        mPrevWeekBtn.setOnClickListener(this);
        mNextWeekBtn.setOnClickListener(this);
        mRefreshBtn.setOnClickListener(this);

        mMondayDateView = mCourseTableViewGroup.findViewById(R.id.monday_date_view);
        mSundayDataView = mCourseTableViewGroup.findViewById(R.id.sunday_date_view);

        // init course table gridview
        initCourseTableGridView();

        // init date
        initDate();

        // create a course checkout thread
        mCourseThread = new CourseThread(this);
    }

    // @brief Init course table grid view
    private void initCourseTableGridView() {
        // course adapter
        mCourseAdapter = new CourseAdapter(mContext);

        // set adapter into course table view
        mCourseTable.setAdapter(mCourseAdapter);

        // set grid view item click handler
        mCourseTable.setOnItemClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    mCourseAdapter.showCourseDialog(position);
                });
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

    // init course table message handler
    private void initMessageHandler() {
       mHandler = new Handler() {
           @Override
           public void handleMessage(Message msg) {
               switch (msg.what) {
               case MSG_COURSE_TABLE_READY:
                   LogManager.i("CourseTable handle MSG_COURSE_TABLE_READY");
                   checkoutCourseForCurrent();
                   break;
               case MSG_UPDATE_COURSE_TABLE:
                   LogManager.i("CourseTable handle MSG_UPDATE_COURSE_TABLE");
                   updateCourseTable((List<TimeTable>)msg.obj);
                   break;
               default:
                   LogManager.w("CourseTable handle unknown message: " + msg.what);
                   break;
               }
           }
       };
    }

    // @brief Updates course table
    private void updateCourseTable(List<TimeTable> list) {
        LogManager.i("CourseTableFragment need to update course table");

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

        //2017/10/28
        // update course adapter course-list
        mCourseAdapter.updateCourseList(list, mCourseTableMonday);

        // notify to adapter update course table
        mCourseAdapter.notifyDataSetChanged();

        // set default focus on 1st course
        mCourseTable.requestFocusFromTouch();
        mCourseTable.setSelection(0);

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

    public static String dateWithWeekDay(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        Date time;
        try { time = format.parse(date); } catch(Exception e) {
            return date;
        }

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(time);
        String day = "";

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    day = "星期一"; break;
            case Calendar.TUESDAY:   day = "星期二"; break;
            case Calendar.WEDNESDAY: day = "星期三"; break;
            case Calendar.THURSDAY:  day = "星期四"; break;
            case Calendar.FRIDAY:    day = "星期五"; break;
            case Calendar.SATURDAY:  day = "星期六"; break;
            case Calendar.SUNDAY:    day = "星期日"; break;
        }

        return day;
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

            case R.id.refresh_btn:
                checkoutCourseForCurrent();
                break;

            default:
                break;
        }
    }

    // @brief Checkouts current time courses
    private void checkoutCourseForCurrent() {
        LogManager.i("CourseTableFragment checkout current week's courses");

        // checkout this week's courses
        // days: 6 + include monday = 7 days
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }

    // @brief Checkouts prev week courses
    private void checkoutPrevWeekCourse() {
        LogManager.i("CourseTableFragment checkout prev week's courses");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        LogManager.i("CourseTableFragment table date: " + format.format(mCourseTableMonday));

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(mCourseTableMonday);

        // calculate prev week
        calendar.add(Calendar.DAY_OF_YEAR, -7);

        mCourseTableMonday = calendar.getTime();
        LogManager.i("CourseTableFragment prev week: " + format.format(mCourseTableMonday));

        // update date line text view
        updateDateLineBarView();

        // checkout this week's courses
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }

    // @brief Checkouts next week courses
    private void checkoutNextWeekCourse() {
        LogManager.i("CourseTableFragment checkout next week's courses");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        LogManager.i("CourseTableFragment table date: " + format.format(mCourseTableMonday));

        Calendar calendar= Calendar.getInstance();
        calendar.setTime(mCourseTableMonday);

        // calculate prev week
        calendar.add(Calendar.DAY_OF_YEAR, 7);

        mCourseTableMonday = calendar.getTime();
        LogManager.i("CourseTableFragment next week: " + format.format(mCourseTableMonday));

        // update date line text view
        updateDateLineBarView();

        // checkout this week's courses
        mCourseThread.checkoutCourse(mCourseTableMonday, 6);
    }

    //@brief Enter course's course_class to decide taking course_class
    public void enterTakingClassFragment(@NonNull TimeTable course) {
        ClassFragment fragment = ClassFragment.getInstance();

        // set course thread
        fragment.setCourseThread(mCourseThread);
        // set current course
        fragment.setClassCourse(course);

        // show class fragment
        showFragment(TAG, R.id.fragmentLayout, fragment, ClassFragment.TAG);
    }
}

