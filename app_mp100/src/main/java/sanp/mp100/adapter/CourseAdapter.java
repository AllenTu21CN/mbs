package com.will.course.coursetable.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.will.course.coursetable.Course;
import com.will.course.coursetable.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hui on 2017/10/14.
 *
 * course adapter, find course for course tables view
 */

public class CourseAdapter extends BaseAdapter {

    private Context m_context;

    private List<Course> m_course_list;

    // just for test
//  private String  m_course_filename;

    public CourseAdapter(Context context) {
        m_context = context;

        initCourseList();
    }

    private void initCourseList() {
        m_course_list = new ArrayList<>();

        m_course_list.add(new Course("1语文", 1));
        m_course_list.add(new Course("1语文", 2));
        m_course_list.add(new Course("1数学", 3));
        m_course_list.add(new Course("1数学", 4));
        m_course_list.add(new Course("1英语", 1));
        m_course_list.add(new Course("1英语", 2));
        m_course_list.add(new Course("1物理", 3));

        m_course_list.add(new Course("2物理", 4));
        m_course_list.add(new Course("2化学", 1));
        m_course_list.add(new Course("2化学", 2));
        m_course_list.add(new Course("2生物", 3));
        m_course_list.add(new Course("2数学", 4));
        m_course_list.add(new Course("2美术", 1));
        m_course_list.add(new Course("2语文", 2));

        m_course_list.add(new Course("3数学", 3));
        m_course_list.add(new Course("3数学", 4));
        m_course_list.add(new Course("3音乐", 1));
        m_course_list.add(new Course("3语文", 2));
        m_course_list.add(new Course("3数学", 3));
        m_course_list.add(new Course("3数学", 4));
        m_course_list.add(new Course("3音乐", 1));

        m_course_list.add(new Course("4语文", 2));
        m_course_list.add(new Course("4数学", 3));
        m_course_list.add(new Course("4数学", 4));
        m_course_list.add(new Course("4音乐", 1));
        m_course_list.add(new Course("4语文", 2));
        m_course_list.add(new Course("4数学", 3));
        m_course_list.add(new Course("4数学", 4));

        m_course_list.add(new Course("5物理", 4));
        m_course_list.add(new Course("5化学", 1));
        m_course_list.add(new Course("5化学", 2));
        m_course_list.add(new Course("5生物", 3));
        m_course_list.add(new Course("5数学", 4));
        m_course_list.add(new Course("5美术", 1));
        m_course_list.add(new Course("5语文", 2));

        m_course_list.add(new Course("6语文", 1));
        m_course_list.add(new Course("6语文", 2));
        m_course_list.add(new Course("6数学", 3));
        m_course_list.add(new Course("6数学", 4));
        m_course_list.add(new Course("6英语", 1));
        m_course_list.add(new Course("6英语", 2));
        m_course_list.add(new Course("6物理", 3));

        m_course_list.add(new Course("7语文", 2));
        m_course_list.add(new Course("7数学", 3));
        m_course_list.add(new Course("7数学", 4));
        m_course_list.add(new Course("7音乐", 1));
        m_course_list.add(new Course("7语文", 2));
        m_course_list.add(new Course("休", 3));
        m_course_list.add(new Course("休", 4));

    }

    @Override
    public int getCount() {
        //return 28; // 4 * 7
        return m_course_list.size(); // 4 * 7
        //TODO: There is 20 courses in the forenoon
        // and only 21 courses in the afternoon
    }

    @Override
    public Course getItem(int position) {
        //int line = position / 7;
        //int column = position % 7;
        //Log.i("[CourseAdapter]", "get item position: " + position + "line: " + line
        //    + "column: " + column);

        //if (line > 4 || column > 5) return null;

        //return m_course_list.get(line * 5 + column);

        return m_course_list.get(position);
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
            convertView = LayoutInflater.from(m_context).inflate(R.layout.course_item, null);

            holder.m_course_view = (TextView) convertView.findViewById(R.id.course_name_view);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // find course and set into the view
        if (m_course_list != null && !m_course_list.isEmpty()) {
            Course course = m_course_list.get(position);
            holder.m_course_view.setText(course.getName());
        }

        return convertView;
    }

    private class ViewHolder {
        TextView m_course_view;
    }
}
