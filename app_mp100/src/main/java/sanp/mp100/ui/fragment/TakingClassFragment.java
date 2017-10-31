package sanp.mp100.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import sanp.mp100.R;

/**
 * @brief Created by will@1dao2.com on 2017/10/30.
 * Take class card view overlap on view surface view.
 *
 * @author will@1dao2.com
 * @date   2017/10/30
 */

public class TakingClassFragment extends BaseFragment implements View.OnClickListener  {

    public static final String TAG = "TakingClassFragment";
    public static TakingClassFragment mTakingClassFragment;

    public static TakingClassFragment getInstance() {
        if (mTakingClassFragment == null)
            mTakingClassFragment = new TakingClassFragment();

        return mTakingClassFragment;
    }

    // see: taking_class.xml
    private View mTakingClassViewGroup;

    private TextView mClassStatusView;

    // class info view and info strings
    private TextView mClassNameView;
    private TextView mClassTeacherView;
    private TextView mClassTimeView;
    private TextView mClassContentView;

    private String   mClassName;
    private String   mClassTeacher;
    private String   mClassTime;
    private String   mClassContent;

    // taking class button
    private Button mTakingClassCtrlBtn;
    private Button mHiddenThisView;

    // @brief Implements Fragment
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTakingClassViewGroup = View.inflate(getActivity(), R.layout.taking_class, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        initView();

        return mTakingClassViewGroup;
    }

    private void initView() {
        mClassStatusView  = (TextView) mTakingClassViewGroup.findViewById(R.id.taking_class_status);

        mClassNameView    = (TextView) mTakingClassViewGroup.findViewById(R.id.class_name);
        mClassTeacherView = (TextView) mTakingClassViewGroup.findViewById(R.id.class_teacher);
        mClassTimeView    = (TextView) mTakingClassViewGroup.findViewById(R.id.class_time);
        mClassContentView = (TextView) mTakingClassViewGroup.findViewById(R.id.class_content);

        mTakingClassCtrlBtn = (Button) mTakingClassViewGroup.findViewById(R.id.taking_class_ctrl);
        mHiddenThisView     = (Button) mTakingClassViewGroup.findViewById(R.id.hidden_this_view);

        mTakingClassCtrlBtn.setOnClickListener(this);
        mHiddenThisView.setOnClickListener(this);
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
}
