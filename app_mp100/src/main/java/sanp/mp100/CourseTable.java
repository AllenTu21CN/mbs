package sanp.mp100; 

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.GridView;

import sanp.mp100.adapter.CourseAdapter;

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

public class CourseTable extends Activity {

    private Context  mContext;

    // course table view
    private GridView mCourseTable;

    // course adapter
    CourseAdapter mCourseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set screen orientation: landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.course_table);

        mContext = this;

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
    }

}
