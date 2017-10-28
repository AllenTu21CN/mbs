package com.will.course.coursetable;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.GridView;

import com.will.course.coursetable.adapter.CourseAdapter;

/**
 * Created by hui on 2017/10/12.
 *
 * course table activity, read courses and show them on the table
 */

public class CourseTable extends Activity {

    private Context  m_context;

    // course table view
    private GridView m_course_table;

    // course adapter
    CourseAdapter m_course_adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set screen orientation: landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.course_table);

        m_context = this;

        initView();
    }

    // init course table view
    private void initView() {
        // find the course tables
        m_course_table  = (GridView) findViewById(R.id.course_table_gird_view);

        // course adapter
        m_course_adapter = new CourseAdapter(m_context);

        // set adapter into course table view
        m_course_table.setAdapter(m_course_adapter);
    }

}
