package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import sanp.avalon.libs.base.utils.LogManager;
import sanp.mp100.R;
import sanp.mp100.integration.BusinessPlatform;

/**
 * @brief Created by will@1dao2.com on 2017/10/30.
 * Take class card view overlap on view surface view.
 *
 * @author will@1dao2.com
 * @date   2017/10/30
 */

public class ClassFragment extends BaseFragment implements View.OnClickListener  {

    public static final String TAG = "ClassFragment";
    public static ClassFragment mClassFragment;

    public static ClassFragment getInstance() {
        if (mClassFragment == null)
            mClassFragment = new ClassFragment();

        return mClassFragment;
    }

    // see: taking_class.xml
    private View mClassViewGroup;

    private TextView mClassStatusView;

    // class info view and info strings
    private TextView mClassNameView;
    private TextView mClassTeacherView;
    private TextView mClassTimeView;
    private TextView mClassContentView;

    // taking class button
    private Button mTakingClassCtrlBtn;
    private Button mHiddenThisView;

    private BusinessPlatform.TimeTable mCourse;

    private Boolean mIsTakingClass;

    // @brief Implements Fragment
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClassViewGroup = View.inflate(getActivity(), R.layout.course_class, null);

        LogManager.i("ClassFragment onCreate: init view group(layout/course_class.xml)");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogManager.i("ClassFragment: onCreateView: init all views(Text, Btn, ..)");

        mIsTakingClass = false;
        initView();

        return mClassViewGroup;
    }

    private void initView() {
        mClassStatusView  = (TextView) mClassViewGroup.findViewById(R.id.class_status);

        mClassNameView    = (TextView) mClassViewGroup.findViewById(R.id.class_name);
        mClassTeacherView = (TextView) mClassViewGroup.findViewById(R.id.class_teacher);
        mClassTimeView    = (TextView) mClassViewGroup.findViewById(R.id.class_time);
        mClassContentView = (TextView) mClassViewGroup.findViewById(R.id.class_content);

        mTakingClassCtrlBtn = (Button) mClassViewGroup.findViewById(R.id.taking_class_ctrl);
        mHiddenThisView     = (Button) mClassViewGroup.findViewById(R.id.hidden_this_view);

        mTakingClassCtrlBtn.setOnClickListener(this);
        mHiddenThisView.setOnClickListener(this);

        // set class status and ctrl btn view according to @mIsTakingClass
        if (mIsTakingClass) {
            mClassStatusView.setText("正在上课");
            mTakingClassCtrlBtn.setText("结束上课");
        } else {
            mClassStatusView.setText("课程信息");
            mTakingClassCtrlBtn.setText("开始上课");
        }

        // show class info
        updateClassCourseInfo();
    }

    // @brief Implements View.onClickListener
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.taking_class_ctrl:
                mClassStatusView.setText("课程结束");
                mTakingClassCtrlBtn.setText("结束上课");
                //todo
                break;
            case R.id.hidden_this_view:
                //todo
                break;
            default:
                break;
        }
    }

    // @brief Sets current class' course
    public void setClassCourse(BusinessPlatform.TimeTable course) {
        mCourse = course;
    }

    // @brief Starts taking class
    private int startTakingClass() {
        //todo
        return 0;
    }

    // @brief Stops taking class
    private int StopTakingClass() {
        //todo
        return 0;
    }

    // @brief Updates class' course info
    private void updateClassCourseInfo() {
        if (mCourse == null) return;

        mClassNameView.setText(mCourse.subject_name);
        mClassTeacherView.setText(mCourse.teacher_name);
        mClassTimeView.setText(mCourse.date + " 第" + mCourse.section + "节");
        mClassContentView.setText(mCourse.title);
    }
}
