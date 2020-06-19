package cn.lx.mbs.support.base;

import cn.lx.mbs.LXConst;

import com.sanbu.tools.LogUtil;

/**
 * 功能：
 * # 基本信息
 *   . 获取应用基本信息（包含：序列号、版本号、应用名称。。。。）
 *   . ...
 *
 * # 授权/购买
 *
 * # 应用数据管理(查看/修改/清除/导入/导出)
 *
 * # 日志搜集
 *   . 获取日志搜集配置
 *   . 设置日志搜集配置
 *   . 清除日志
 *   . 下载日志
 *
 * # 版本管理(查看/在线升级)
 *
 * # 其他信息
 *   . 获取网络信息
 *   . 获取磁盘信息
 * */

public class Basics {

    private static final String TAG = Basics.class.getSimpleName();

    public Basics() {

    }

    public void init() {
        LogUtil.i(LXConst.TAG, TAG, "TODO：init");
    }

    public void release() {

    }

    /////////////////////////////// callbacks

    public void onNetworkChanged() {

    }
}
