package app.dinus.com.loadingdrawable;

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

public class DSLoadingDialog {
    private String TAG = DSLoadingDialog.class.getSimpleName();
    public static Dialog dialogs;

    private static Dialog createLoadingDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dlg_loading_ds, null);// 得到加载view
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);// 加载布局

        Dialog loadingDialog = new Dialog(context);

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
