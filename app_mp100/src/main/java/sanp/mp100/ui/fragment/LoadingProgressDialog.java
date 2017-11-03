package sanp.mp100.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import sanp.mp100.R;

/**
 * @brief A loading progress dialog, waiting for process.
 *
 * @author will@1dao2.com
 * @date 2017-11-3
 */

public class LoadingProgressDialog extends Dialog {

    private String  mLoadingProgressTips;
    private Context mContext;

    // Constructor
    public LoadingProgressDialog(Context context, String tips) {
        super(context, R.style.LoadingProgressDialog);
        mLoadingProgressTips = tips;
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load layout
        setContentView(R.layout.loading_progress);
        setCanceledOnTouchOutside(false);

        // set tip text view
        TextView  tipsTextView = findViewById(R.id.loading_progress_text_view);
        tipsTextView.setText(mLoadingProgressTips);

        // set loading progress animation image
        ImageView imageView = findViewById(R.id.loading_progress_image_view);
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.loading_progress_anim);
        imageView.startAnimation(animation);
    }
}
