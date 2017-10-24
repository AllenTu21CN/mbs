package sanp.avalon.libs.base.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import sanp.avalon.libs.R;

/**
 * Created by Tom on 2017/1/23.
 * Dialog 管理基础类：创建Dialog，一种是 menu 类型，一种是居中类型
 */

public class DialogManager {


    public static Dialog initMenuDialogView(View v, Context context) {
        Dialog dialog = new Dialog(context, R.style.ActionSheetDialogStyle);
        //将布局设置给Dialog
        dialog.setContentView(v);
        //获取当前Activity所在的窗体
        Window dialogWindow = dialog.getWindow();
        //设置Dialog从窗体底部弹出
        dialogWindow.setGravity(Gravity.BOTTOM);
        //获得窗体的属性
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = 20;//设置Dialog距离底部的距离
        lp.width = ScreenUtils.getScreenWidth(context) - 120;
        dialogWindow.setAttributes(lp);// 将属性设置给窗体
        return dialog;
    }


    public static Dialog initCenterDialogView(View v, Context context) {
        Dialog dialog = new Dialog(context, R.style.ActionSheetDialogStyle);
        //将布局设置给Dialog
        dialog.setContentView(v);
        //获取当前Activity所在的窗体
        Window dialogWindow = dialog.getWindow();
        //设置Dialog从窗体底部弹出
        dialogWindow.setGravity(Gravity.CENTER);
        //获得窗体的属性
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = 20;//设置Dialog距离底部的距离
        dialogWindow.setAttributes(lp);// 将属性设置给窗体
        return dialog;
    }
}
