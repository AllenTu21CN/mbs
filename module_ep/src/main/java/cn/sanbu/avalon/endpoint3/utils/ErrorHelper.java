package cn.sanbu.avalon.endpoint3.utils;

public class ErrorHelper {

    public static String Error4OpenExtTxStream(int code) {
        switch (code) {
        case 100:
            return "无法打开远端辅流通道";
        default:
            return "打开辅流失败：未知错误#" + code;
        }
    }

}
