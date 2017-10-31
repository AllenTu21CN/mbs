package sanp.mp100.ui.adapter; 

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.ui.CourseDialog;
import sanp.mp100.ui.CourseTable;
import sanp.mp100.integration.BusinessPlatform.TimeTable;

/**
 * @brief Course adapter. Checkouts course and show them on the view. 
 *
 * @author will@1dao2.com
 * @date   2017/10/14
 *
 * @modified 2017/10/28, add this into Monica
 * 
 */

public class CourseAdapter extends BaseAdapter {

    // The course table view context
    private Context mContext;

    // The course list which is checked from service
    // 8 courses(sections) each day
    private List<TimeTable> mCourseList;

    // 12 sections each day
//  private List<TimeTable> m12CourseList;

    public CourseAdapter(Context context) {
        mContext = context;

        initCourseAdapter();
    }

    private void initCourseAdapter() {
        mCourseList = new ArrayList<>();

        // the total course number: 8courses * 7days = 49
        for (int iday = 0; iday < 7; iday ++) {
            for (int isection = 0; isection < 8; isection ++) {
                TimeTable course = new TimeTable();

                course.id           = -1; 
                course.section      = String.valueOf(isection);
                course.subject_name = "";

                mCourseList.add(course);
            }
        }
    }

    // @brief Reset course list
    private void resetCourseList() {
        for(TimeTable it : mCourseList) {
            it.id           = -1;
            it.subject_name = "";
        }
    }

    // @brief Update adapter course list
    public void updateCourseList(List<TimeTable> list, Date monday) {
        LogManager.i("CourseAdapter update course list");

        // reset the old course list
        resetCourseList();

        // update new course list
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        for(TimeTable it : list) {
            Date date = null;
            try {
                date = format.parse(it.date);
            } catch (Exception e) {
                 LogManager.w("Course: " + it.subject_name  + ", Section: " +
                     it.section + " has invalid date: " + it.date);
                 continue;
            }

            // calc days from monday in week
            int days = CourseTable.daysBetween(monday, date);
            if (days < 0 || days >= 7) {
                LogManager.w("Course: " + it.subject_name  + ", Section: " +
                    it.section + ", date: " + it.date + " is not in this week");
                continue;
            }

            int section = Integer.parseInt(it.section);
            if (section <= 0 || section > 12) {
                LogManager.w("Course: " + it.subject_name  + ", Section: " +
                    it.section + ", date: " + it.date + " is invalid section");
                continue;
            }

            LogManager.i("Id: " + it.id +
                    "; Type: " + it.type +
                    "; Subject Name: " + it.subject_name +
                    "; Title:  " + it.title +
                    "; Date: " + it.date + "; days: " + days +
                    "; Section: "+ it.section + "\n");

            // find the course and update course
            //int index = days * 7 + (section - 1);
            int index = (section - 1) * 7 + days;
            if (index < 0 || index > 8 * 7) {
                LogManager.w("Course index: " + index + "is out range");
                return;
            }

            TimeTable course = mCourseList.get(index);

            course.id           = it.id;
            course.type         = it.type;
            course.subject_id   = it.subject_id;
            course.subject_name = it.subject_name;
            course.title        = it.title;
            course.teacher_id   = it.teacher_id;
            course.teacher_name = it.teacher_name;
            course.date         = it.date;
            course.section      = it.section;
            course.duration     = it.duration;
            course.status       = it.status;
        }

        return;
    }

    @Override
    public int getCount() { return mCourseList.size(); }

    @Override
    public TimeTable getItem(int position) {
        return mCourseList.get(position);
    }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();

            convertView = LayoutInflater.from(mContext).inflate(R.layout.course_item, null);
            holder.mCourseView = (TextView) convertView.findViewById(R.id.course_name_view);

            //convertView = LayoutInflater.from(mContext).inflate(R.layout.course_item_bak, null);
            //holder.mCourseView = (TextView) convertView.findViewById(R.id.course_name_view_bak);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // find course and set into the view
        if (mCourseList != null && !mCourseList.isEmpty()) {
            TimeTable course = mCourseList.get(position);
            holder.mCourseView.setText(course.subject_name);

            /* TODO, disable item if the course is null
            // check whether the course is valid
            if (course.id != -1) {
                //LogManager.i("Course: " + course.subject_name + " is valid");
                convertView.setEnabled(true);
                convertView.setFocusable(true);
            } else {
                //LogManager.i("Course[" + position + "] is invalid");
                convertView.setEnabled(false);
                convertView.setFocusable(false);
            }
            */

            setCourseViewClickHandler(convertView, course);
        }

        return convertView;
    }

    // @brief Sets course view item click handler
    private void setCourseViewClickHandler(View view, TimeTable course) {

        if (course.id == -1) return;

        // set on click listener: show a course dialog
        view.setOnClickListener((View v) -> { showCourseDialog(course); });

        return;
    }

    // @brief Shows course dialog
    private void showCourseDialog(TimeTable course) {
        CourseDialog dialog = new CourseDialog(mContext);

        dialog.setCourseName(course.subject_name);
        dialog.setCourseTeacher(course.teacher_name);
        dialog.setCourseTime(course.date + " 第" + course.section + "节");
        dialog.setCourseContent(course.title);

        dialog.setYesOnclickListener("确定", () -> {
            LogManager.i("CourseAdapter: start course[" + course.subject_name + "]");
            //TODO, start class, 2017/10/29
            dialog.dismiss();
        });

        dialog.setNoOnclickListener("取消", () -> {
            LogManager.i("CourseAdapter: CANCEL");
            // do nothing
            dialog.dismiss();
        });

        dialog.show();
    }

    // @brief Shows course dialog according to item position
    public void showCourseDialog(int position) {
        TimeTable course = mCourseList.get(position);

        if (course.id == -1) return;

        showCourseDialog(course);

        return;
    }

    private class ViewHolder {
        TextView mCourseView;
    }
}