package com.sanbu.tools;

import android.os.Handler;

import com.sanbu.base.Callback;
import com.sanbu.base.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MessageBox {

    private final int mMax;
    private List<Item> mRecords;
    private Handler mHandler;

    public MessageBox(int maxItem) {
        mMax = maxItem;
        mRecords = new LinkedList<>();
    }

    public void init(Handler handler) {
        if (mHandler != null)
            return;

        mHandler = handler;
    }

    public void release() {
        mHandler = null;
    }

    public int getRecordsCount() {
        return mRecords.size();
    }

    public void getRecords(final int max, final Callback/**@see List<Item>*/ callback) {
        if (mHandler == null) {
            callback.done(Result.buildSuccess(Collections.emptyList()));
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int min = Math.min(max, mRecords.size());
                List<Item> records = new ArrayList<>(min);
                for (int i = 0; i < min; ++i)
                    records.add(mRecords.get(i));
                callback.done(Result.buildSuccess(records));
            }
        });
    }

    public void clear() {
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecords.clear();
                }
            });
        }
    }

    public void add(final String type, final String tag, final String error) {
        if (mHandler == null)
            return;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecords.size() == mMax)
                    mRecords.remove(0);

                String date;
                String time = TimeUtil.getCurrentTime(TimeUtil.FORMAT_3);
                String[] times = time.split(" ");
                date = times[0];
                time = times.length > 1 ? times[1] : "N/A";

                mRecords.add(new Item(date, time, type, tag, error));
            }
        });
    }

    public static class Item {
        public String date;     // yyyy-MM-dd
        public String time;     // HH:mm:ss
        public String type;     // 错误/警告/通知
        public String tag;      // 内部模块TAG
        public String message;  // 消息记录

        public Item(String date, String time,
                    String type, String tag, String message) {
            this.date = date;
            this.time = time;
            this.type = type;
            this.tag = tag;
            this.message = message;
        }
    }
}
