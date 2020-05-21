package com.sanbu.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.sanbu.base.BaseError;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AlarmHelper {
    public static final String TAG = AlarmHelper.class.getSimpleName();

    public enum Period {
        NoLoop(0, 0),

        ByMinute(60 * 1000, Calendar.MINUTE),
        ByHour(AlarmManager.INTERVAL_HOUR, Calendar.HOUR),
        ByDay(AlarmManager.INTERVAL_DAY, Calendar.DAY_OF_MONTH),
        ByWeek(7 * ByDay.intervalMS, Calendar.WEEK_OF_YEAR),

        ByMonth(0, Calendar.MONTH),
        ByYear(0, Calendar.YEAR);

        public final long intervalMS;
        public final int unit;

        Period(long intervalMS, int unit) {
            this.intervalMS = intervalMS;
            this.unit = unit;
        }
    }

    private static volatile int gReqCodeIndex = 10;

    private Context mContext;
    private Map<String, Task> mTasks;
    private Object mLock = new Object();

    public AlarmHelper(Context context) {
        mContext = context;
        mTasks = new LinkedHashMap<>();
    }

    public void release() {
        synchronized (mLock) {
            List<String> names = new LinkedList<>(mTasks.keySet());
            for (String name: names)
                cancel(name);
            mContext = null;
        }
    }

    public int add(String uniqueName, Runnable runnable, Calendar first, final Period periodUnit, int periodNumber) {
        if (mContext == null)
            return BaseError.ACTION_CANCELED;

        if (periodUnit != Period.NoLoop && periodNumber == 0)
            return BaseError.INVALID_PARAM;

        synchronized (mLock) {
            if (mTasks.containsKey(uniqueName))
                return BaseError.ACTION_ILLEGAL;
        }

        // check and adjust first time
        while (System.currentTimeMillis() > first.getTimeInMillis()) {
            if (periodUnit == Period.NoLoop) {
                return BaseError.INVALID_PARAM;
            } else {
                LogUtil.d(TAG, "adjust first time, add " + periodNumber + " " + periodUnit.name());
                first.add(periodUnit.unit, periodNumber);
            }
        }

        // init receiver
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                synchronized (mLock) {
                    Task task = mTasks.get(action);
                    if (task != null) {
                        LogUtil.i(TAG, "on " + action);
                        try {
                            task.runnable.run();
                        } catch (Exception e) {
                            LogUtil.e(TAG, "do timer runnable error", e);
                        }
                        if (task.periodUnit == Period.NoLoop) {
                            cancel(action);
                        } else if (periodUnit == Period.ByMonth || periodUnit == Period.ByYear) {
                            cancel(action);
                            add(action, task.runnable, task.first, task.periodUnit, task.periodNumber);
                        }
                    }
                }
            }
        };

        // register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(uniqueName);
        mContext.registerReceiver(receiver, filter);

        // init a timer
        Intent intent = new Intent(uniqueName);
        PendingIntent timer = PendingIntent.getBroadcast(mContext, gReqCodeIndex++, intent, 0);

        // backup
        synchronized (mLock) {
            Task task = new Task(runnable, receiver, timer, first, periodUnit, periodNumber);
            mTasks.put(uniqueName, task);
        }

        // deploy the timer
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (periodUnit == Period.NoLoop || periodUnit == Period.ByMonth || periodUnit == Period.ByYear) {
            am.set(AlarmManager.RTC_WAKEUP, first.getTimeInMillis(), timer);
        } else {
            am.setRepeating(AlarmManager.RTC_WAKEUP, first.getTimeInMillis(), periodUnit.intervalMS * periodNumber, timer);
        }

        LogUtil.d(TAG, "success to add alarm: " + uniqueName);
        return 0;
    }

    public void cancel(String uniqueName) {
        if (mContext == null)
            return;

        synchronized (mLock) {
            Task task = mTasks.remove(uniqueName);
            if (task != null) {
                if (task.receiver != null)
                    mContext.unregisterReceiver(task.receiver);
                if (task.timer != null) {
                    AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    am.cancel(task.timer);
                }
                LogUtil.d(TAG, "success to cancel alarm: " + uniqueName);
            }
        }
    }

    private class Task {
        public final Runnable runnable;
        public final BroadcastReceiver receiver;
        public final PendingIntent timer;

        public final Calendar first;
        public final Period periodUnit;
        public final int periodNumber;

        public Task(Runnable runnable, BroadcastReceiver receiver, PendingIntent timer,
                    Calendar first, Period periodUnit, int periodNumber) {
            this.runnable = runnable;
            this.receiver = receiver;
            this.timer = timer;
            this.first = first;
            this.periodUnit = periodUnit;
            this.periodNumber = periodNumber;
        }
    }
}
