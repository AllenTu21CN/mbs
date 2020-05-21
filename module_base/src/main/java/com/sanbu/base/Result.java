package com.sanbu.base;

public class Result {

    public static final Result SUCCESS = new Result(BaseError.SUCCESS, null);

    public static Result buildSuccess(Object data) {
        return new Result(SUCCESS.code, SUCCESS.message, data);
    }

    public int code;
    public String message;
    public Object data;

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccessful() {
        return code == SUCCESS.code;
    }

    public String getError() {
        if (code == SUCCESS.code)
            return null;

        String error = message;
        if (message == null && data != null) {
            if (data instanceof Throwable) {
                error = ((Throwable) data).getMessage();
            } else {
                error = data.toString();
            }
        }
        return "code=" + code + ", error: " + error;
    }

    public String getMessage() {
        if (code == SUCCESS.code)
            return "";

        String error = message;
        if (message == null && data != null) {
            if (data instanceof Throwable) {
                error = ((Throwable) data).getMessage();
            } else {
                error = data.toString();
            }
        }
        return error;
    }

    public Throwable getThrowable() {
        if (code == SUCCESS.code)
            return null;
        if (data instanceof Throwable)
            return (Throwable) data;
        return new Exception(getError());
    }
}
