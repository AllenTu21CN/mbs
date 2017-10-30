package sanp.mp100.ui;

/**
 * Modified by will@1dao2.com on 2017/10/29.
 *
 * see: http://blog.csdn.net/oqihaogongyuan/article/details/50958659
 */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import sanp.mp100.R;

/**
 * 创建自定义的dialog，主要学习其实现原理
 * Created by chengguo on 2016/3/22.
 */
public class CourseDialog extends Dialog {

    private Button mYesBtn;//确定按钮
    private Button mNoBtn;//取消按钮

    // course information text views
    private TextView mCourseNameView;
    private TextView mCourseTeacherView;
    private TextView mCourseTimeView;
    private TextView mCourseContentView;

    // course information: name, teacher, time, content
    private String   mCourseName;
    private String   mCourseTeacher;
    private String   mCourseTime;
    private String   mCourseContent;

    //确定文本和取消文本的显示内容
    private String mYesText, mNoText;

    private onNoOnclickListener noOnclickListener;//取消按钮被点击了的监听器
    private onYesOnclickListener yesOnclickListener;//确定按钮被点击了的监听器

    /**
     * 设置取消按钮的显示内容和监听
     *
     * @param str
     * @param onNoOnclickListener
     */
    public void setNoOnclickListener(String str, onNoOnclickListener onNoOnclickListener) {
        if (str != null) mNoText = str;

        this.noOnclickListener = onNoOnclickListener;
    }

    /**
     * 设置确定按钮的显示内容和监听
     *
     * @param str
     * @param onYesOnclickListener
     */
    public void setYesOnclickListener(String str, onYesOnclickListener onYesOnclickListener) {
        if (str != null) mYesText = str;

        this.yesOnclickListener = onYesOnclickListener;
    }

    public CourseDialog(Context context) {
        super(context, R.style.CourseDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_dialog);
        //按空白处不能取消动画
        setCanceledOnTouchOutside(false);

        //初始化界面控件
        initView();
        //初始化界面数据
        initData();
        //初始化界面控件的事件
        initEvent();

    }

    /**
     * 初始化界面的确定和取消监听器
     */
    private void initEvent() {
        //设置确定按钮被点击后，向外界提供监听
        //mYesBtn.setOnClickListener(new View.OnClickListener() {
        mYesBtn.setOnClickListener((View v) -> {
            if (yesOnclickListener != null) {
                yesOnclickListener.onYesClick();
            }
        });

        //设置取消按钮被点击后，向外界提供监听
        mNoBtn.setOnClickListener((View v) -> {
            if (noOnclickListener != null) {
                noOnclickListener.onNoClick();
            }
        });
    }

    /**
     * 初始化界面控件的显示数据
     */
    private void initData() {
        if (mCourseName != null)    mCourseNameView.setText(mCourseName);
        if (mCourseTeacher != null) mCourseTeacherView.setText(mCourseTeacher);
        if (mCourseTime != null)    mCourseTimeView.setText(mCourseTime);
        if (mCourseContent != null) mCourseContentView.setText(mCourseContent);

        //如果设置按钮的文字
        if (mYesText != null) mYesBtn.setText(mYesText);
        if (mNoText != null)  mNoBtn.setText(mNoText);
    }

    /**
     * 初始化界面控件
     */
    private void initView() {
        mYesBtn = (Button) findViewById(R.id.course_dialog_yes);
        mNoBtn  = (Button) findViewById(R.id.course_dialog_no);

        mCourseNameView    = (TextView) findViewById(R.id.course_name);
        mCourseTeacherView = (TextView) findViewById(R.id.course_teacher);
        mCourseTimeView    = (TextView) findViewById(R.id.course_time);
        mCourseContentView = (TextView) findViewById(R.id.course_content);
    }

    /**
     * Sets course information: name, teacher, time and content
     *
     * @param name/teacher/time/content
     */
    public void setCourseName(String name) { mCourseName = name; }
    public void setCourseTeacher(String teacher) { mCourseTeacher = teacher; }
    public void setCourseTime(String time) { mCourseTime = time; }
    public void setCourseContent(String content) { mCourseContent = content; }


    /**
     * 设置确定按钮和取消被点击的接口
     */
    public interface onYesOnclickListener {
        void onYesClick();
    }

    public interface onNoOnclickListener {
        void onNoClick();
    }
}

