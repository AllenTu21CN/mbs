package sanp.mp100.adapter; 

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sanp.mp100.integration.BusinessPlatform;

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

    // An empty course list to show before checkout courses.
    private List<TimeTable> mEmptyCourseList;

    // The course list which is checked from service
    private List<TimeTable> mCourseList;

    public CourseAdapter(Context context) {
        mContext = context;

        initCourseAdapter();
    }

    private void initCourseAdapter() {
        mEmptyCourseList = new ArrayList<>();

        // the total course number: 7courses * 7days = 49
        for (int iday = 0; iday < 7; iday ++) {
            for (int isection = 0; isection < 7; isection ++) {
                TimeTable course = new TimeTable();

                course.id           = -1; 
                course.section      = new String(isection);
                course.subject_name = new String("");

                mEmptyCourseList.add(course);
            }
        }
    }

    @Override
    public int getCount() {
        return mEmptyCourseList.size(); // 4 * 7
    }

    @Override
    public Course getItem(int position) {
        return mEmptyCourseList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.course_item, null);

            holder.mCourseView = (TextView) convertView.findViewById(R.id.course_name_view);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // find course and set into the view
        if (mEmptyCourseList != null && !mEmptyCourseList.isEmpty()) {
            Course course = mEmptyCourseList.get(position);
            holder.mCourseView.setText(course.getName());
        }

        return convertView;
    }

    private class ViewHolder {
        TextView mCourseView;
    }
}
