package com.sanbu.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sanbu.tools.R;

public class LoadingDialog {
    private static final String TAG = LoadingDialog.class.getSimpleName();
    public static Dialog dialogs;

    private static Dialog createLoadingDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dlg_loading, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);// 加载布局
        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
        TextView tipTextView = (TextView) v.findViewById(R.id.tv_tip);// 提示文字

        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
                context, R.anim.dlg_loading);

        spaceshipImage.startAnimation(hyperspaceJumpAnimation);
        tipTextView.setText(msg);

        Dialog loadingDialog = new Dialog(context, R.style.DialogLoading);

        loadingDialog.setCancelable(false);// 是否可以用“返回键”取消
        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));// 设置布局
        return loadingDialog;
    }

    public static void show(Context context, String msg)
    {
        dialogs = createLoadingDialog(context, msg);
        dialogs.show();
        dialogs.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static void dismiss()
    {
        dialogs.dismiss();
    }
}
