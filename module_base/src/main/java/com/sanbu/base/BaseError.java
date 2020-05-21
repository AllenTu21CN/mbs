package com.sanbu.base;

public class BaseError {
    public static final int SUCCESS = 0;

    public static final int UNKNOWN             = -1;
    private static final int START              = -2000;
    private static final int END                = -3000;

    public static final int INVALID_PARAM       = START - 1; // 无效的参数
    public static final int TARGET_NOT_FOUND    = START - 2; // 目标不存在

    public static final int ACTION_ILLEGAL      = START - 3; // 操作非法
    public static final int ACTION_TIMEOUT      = START - 4; // 操作超时
    public static final int ACTION_CANCELED     = START - 5; // 操作被取消
    public static final int ACTION_UNSUPPORTED  = START - 6; // 不支持的操作
    public static final int LOGICAL_ERROR       = START - 7; // 逻辑错误

    public static final int INTERNAL_ERROR      = START - 10;// 内部错误
    public static final int REMOTE_SYSTEM_ERROR = START - 11;// 远端系统错误
}
