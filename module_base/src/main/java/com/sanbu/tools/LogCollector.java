package com.sanbu.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;

import com.sanbu.base.BaseError;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.board.Rockchip;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LogCollector {

    private static final String TAG = LogCollector.class.getSimpleName();

    private static final String ON_NEW_DAY = "com.sanbu.base.log_new_day";

    private static final SimpleDateFormat LOG_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HHmmss");

    private static final long LOG_CLEAR_PATH_INTERVAL_MS = 10 * 60 * 1000; // 10 min

    private static final String PACK_ZIP_SUFFIX = ".tar.gz";

    public static class Config {
        public String logPath;
        public String logPrefix;
        public String logSuffix;
        public String packageName;
        public long maxFileMB;
        public long maxPathMB;
        public int expiredDays;
        public String tagExpr;
        public List<String> included;
        public List<String> excluded;

        public static Config buildDefault(String logPath, String packageName) {
            return new Config(logPath, "", ".log", packageName,
                    50, 1000, 7, "*:I",
                    null, null);
        }

        public Config(String logPath, String logPrefix, String logSuffix, String packageName,
                      long maxFileMB, long maxPathMB, int expiredDays, String tagExpr,
                      List<String> included, List<String> excluded) {
            this.logPath = logPath;
            this.logPrefix = logPrefix;
            this.logSuffix = logSuffix;
            this.packageName = packageName;
            this.maxFileMB = maxFileMB;
            this.maxPathMB = maxPathMB;
            this.expiredDays = expiredDays;
            this.tagExpr = tagExpr;
            this.included = included;
            this.excluded = excluded;
        }

        public Config(Config other) {
            this.logPath     = other.logPath;
            this.logPrefix   = other.logPrefix;
            this.logSuffix   = other.logSuffix;
            this.packageName = other.packageName;
            this.maxFileMB   = other.maxFileMB;
            this.maxPathMB   = other.maxPathMB;
            this.expiredDays = other.expiredDays;
            this.tagExpr     = other.tagExpr;
            this.included    = other.included;
            this.excluded    = other.excluded;
        }

        public boolean isValid() {
            return !StringUtil.isEmpty(logPath) && !StringUtil.isEmpty(logSuffix) &&
                    maxFileMB > 0 && maxPathMB > 0 && maxPathMB >= maxFileMB &&
                    expiredDays > 0 && !StringUtil.isEmpty(tagExpr);
        }

        public String getFullPath() {
            String timestamp = LOG_NAME_DATE_FORMAT.format(new Date());
            return logPath + File.separator + logPrefix + timestamp + logSuffix;
        }

        public String toLogcatCmd() {
            return toLogcatCmd(getFullPath());
        }

        public String toLogcatCmd(String fullPath) {
            // e.g.:
            // logcat -f /sdcard/2019-10-10-101010.log -r 50000 -n 20 -v threadtime
            //        --pid=$(pidof -s com.sanbu.app_tsbox) *:I
            //        -e "^(?!.*(avalon|VideoEngine))(.*(epGetStatistics|event)).*$"

            long maxCount = maxPathMB / maxFileMB;

            // specify the pid if package name is not none
            String pid = StringUtil.isEmpty(packageName) ? "" : ("--pid=$(pidof -s " + packageName + ")");

            // gen filter in regex
            String regex = "";
            if (excluded != null && excluded.size() > 0)
                regex += String.format("(?!.*(%s))", StringUtil.join("|", excluded));
            if (included != null && included.size() > 0)
                regex += String.format("(.*(%s))", StringUtil.join("|", included));
            if (!regex.isEmpty())
                regex = "-e \"^" + regex + ".*$\"";

            return String.format("logcat -f %s -r %d -n %d -v threadtime %s %s %s",
                    fullPath, maxFileMB * 1024, maxCount, pid, tagExpr, regex);
        }
    }

    private Context mContext;
    private Handler mNRTHandler;
    private PowerManager.WakeLock mWakeLock;

    private Config mConfig;
    private String mCurrentFile;
    private Process mCollection;
    private BroadcastReceiver mCheckingNewDay;
    private PendingIntent mCheckingNewDayTimer;
    private Runnable mOnNewDay = new Runnable() {
        @Override
        public void run() {
            LogCollector.this.checkNewDayLoop();
        }
    };
    private Runnable mClearPath = new Runnable() {
        @Override
        public void run() {
            LogCollector.this.clearPathLoop();
        }
    };

    public void init(Context context, Handler nrtHandler, Config config) {
        if (!config.isValid())
            throw new RuntimeException("LogCollector.init, config is invalid");

        if (mContext == null) {
            mContext = context;
            mNRTHandler = nrtHandler;

            updateConfigImpl(config);

            // get WakeLock
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
    }

    public void release() {
        final AsyncResult ar = new AsyncResult();
        stopCollection(new Callback() {
            @Override
            public void done(Result result) {
                ar.notify2(result);
            }
        });
        ar.wait2(1000, new Result(BaseError.ACTION_TIMEOUT, "stopCollection timeout"));

        mWakeLock = null;
        mContext = null;
        mNRTHandler = null;
        mConfig = null;
    }

    public void updateConfig(final Config config, final Callback callback) {
        if (!config.isValid()) {
            callbackError(callback, "updateConfig", BaseError.INVALID_PARAM, "config is invalid");
            return;
        }
        if (mContext == null) {
            callbackError(callback, "updateConfig", BaseError.ACTION_ILLEGAL, "init LogCollector first");
            return;
        }

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                LogCollector.this.updateConfigImpl(config);
                callbackSuccess(callback);
            }
        });
    }

    public void startCollection(final Callback callback) {
        if (mContext == null) {
            callbackError(callback, "startCollection", BaseError.ACTION_ILLEGAL, "init LogCollector first");
            return;
        }

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                Result result = LogCollector.this.startCollectionImpl();

                // register a receiver to calendar to waiting an new day
                if (result.isSuccessful()) {
                    LogCollector.this.registerCheckingNewDay();
                    mNRTHandler.postDelayed(mClearPath, LOG_CLEAR_PATH_INTERVAL_MS);
                }

                callbackResult(callback, result);
            }
        });
    }

    public void stopCollection(final Callback callback) {
        if (mContext == null) {
            callbackSuccess(callback);
            return;
        }

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                LogCollector.this.unregisterCheckingNewDay();
                mNRTHandler.removeCallbacks(mOnNewDay);
                mNRTHandler.removeCallbacks(mClearPath);

                LogCollector.this.stopCollectionImpl();
                callbackSuccess(callback);
            }
        });
    }

    public Config getConfig() {
        return mConfig;
    }

    public boolean isRunning() {
        return mCollection != null;
    }

    ///////////////// log action

    public void cleanLogs(final boolean today, final Callback callback) {
        if (mContext == null || mNRTHandler == null) {
            callbackError(callback, "cleanLogs", BaseError.ACTION_ILLEGAL, "init first");
            return;
        }

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                List<String> files;
                if (today)
                    files = getTodayFileList(mConfig.logPath, mConfig.logSuffix);
                else
                    files = getSortedFileList(mConfig.logPath, mConfig.logSuffix);
                if (files == null) {
                    callbackError(callback, "cleanLogs", BaseError.INTERNAL_ERROR, "getXXFileList failed");
                    return;
                }

                String current = LogCollector.this.isRunning() ? mCurrentFile : null;

                LogUtil.i(TAG, "cleanLogs: ");
                for (String file : files) {
                    // skip current log file which is in writing
                    if (CompareHelper.isEqual(file, current))
                        continue;

                    File target = new File(file);
                    if (!target.exists() || !target.isFile())
                        continue;

                    LogUtil.i(TAG, target.getName());
                    target.delete();
                }

                callbackSuccess(callback);
            }
        });
    }

    public void zipLogs(final boolean today, final Callback callback) {
        if (mContext == null || mNRTHandler == null) {
            callbackError(callback, "zipLogs", BaseError.ACTION_ILLEGAL, "init first");
            return;
        }

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                String timestamp = LOG_NAME_DATE_FORMAT.format(new Date());
                String zipPrefix = mConfig.logPath + File.separator + mConfig.logPrefix + timestamp;
                Result result = zipLogs(mConfig.logPath, mConfig.logSuffix, today ? "-mtime -1" : "", zipPrefix);
                callbackResult(callback, result);
            }
        });
    }

    ///////////////// private functions

    private void updateConfigImpl(Config config) {
        mConfig = config;
        new File(config.logPath).mkdirs();
    }

    private Result startCollectionImpl() {
        if (mContext == null)
            return new Result(BaseError.ACTION_ILLEGAL, "init first");
        if (mCollection != null)
            return new Result(BaseError.ACTION_ILLEGAL, "had been in collecting");

        Result result;
        mWakeLock.acquire();

        do {
            try {
                // kill all other collectors
                result = killAllLogcat4CurrentUser();
                if (!result.isSuccessful())
                    break;

                // clear path, and expired log
                clearPath();
                clearExpiredLog();

                printLogcatInfo();

                // start process to collect log
                mCurrentFile = mConfig.getFullPath();
                String cmd = mConfig.toLogcatCmd(mCurrentFile);
                LogUtil.i(TAG, "try to collect log: sh -c " + cmd);

                mCollection = Runtime.getRuntime().exec(new String[] {"sh", "-c", cmd});

                try {
                    Thread.sleep(500);

                    int ret = mCollection.exitValue();
                    String msg = "startCollectionImpl failed: " + ret;
                    LogUtil.w(TAG, msg);
                    result = new Result(BaseError.INTERNAL_ERROR, msg);

                    mCollection.destroy();
                    mCollection = null;
                    mCurrentFile = null;
                } catch (IllegalThreadStateException e) {
                    result = Result.SUCCESS;
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "startCollectionImpl error", e);
                result = new Result(BaseError.INTERNAL_ERROR, "startCollectionImpl error", e);
            }
        } while (false);

        mWakeLock.release();
        return result;
    }

    private void stopCollectionImpl() {
        if (mCollection != null) {
            mCollection.destroy();
            mCollection = null;
            mCurrentFile = null;
        }
        killAllLogcat4CurrentUser();
    }

    private void registerCheckingNewDay() {
        // init receiver
        mCheckingNewDay = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ON_NEW_DAY.equals(action)) {
                    LogUtil.i(TAG, "on new day");
                    mNRTHandler.postDelayed(mOnNewDay, 5000);
                }
            }
        };

        // register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ON_NEW_DAY);
        mContext.registerReceiver(mCheckingNewDay, filter);

        // init calendar
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);
        if (System.currentTimeMillis() > calendar.getTimeInMillis())
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        // deploy timer
        Intent intent = new Intent(ON_NEW_DAY);
        mCheckingNewDayTimer = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mCheckingNewDayTimer);
        LogUtil.i(TAG, "registerCheckingNewDay success");
    }

    private void unregisterCheckingNewDay() {
        if (mCheckingNewDay != null) {
            mContext.unregisterReceiver(mCheckingNewDay);
            mCheckingNewDay = null;
        }
        if (mCheckingNewDayTimer != null) {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mCheckingNewDayTimer);
            mCheckingNewDayTimer = null;
        }
    }

    private void clearPath() {
        // delete all log zip files
        List<String> files = getSortedFileList(mConfig.logPath, PACK_ZIP_SUFFIX);
        if (files != null) {
            for (String file: files) {
                File zip = new File(file);
                if (!zip.exists() || !zip.isFile())
                    continue;
                LogUtil.i(TAG, "delete log zip: " + zip.getName());
                zip.delete();
            }
        }

        // check used space
        long usingMB = getPathMB(mConfig.logPath);
        long targetMB = (mConfig.maxPathMB - mConfig.maxFileMB) * 2 / 3;
        if (usingMB < targetMB)
            return;

        LogUtil.i(TAG, "log path is full(" + usingMB + "/" + mConfig.maxPathMB + "MB), try to delete some old log files");
        files = getSortedFileList(mConfig.logPath, mConfig.logSuffix);
        if (files == null)
            return;

        String current = isRunning() ? mCurrentFile : null;

        for (String target: files) {

            // skip current log file which is in writing
            if (CompareHelper.isEqual(target, current))
                continue;

            File oldest = new File(target);
            if (!oldest.exists() || !oldest.isFile())
                continue;

            long sizeMB = oldest.length() / 1024 / 1024;
            if (oldest.delete()) {
                LogUtil.i(TAG, "delete oldest log: " + oldest.getName());
                usingMB -= sizeMB;
                if (usingMB < targetMB)
                    break;
            }
        }

        LogUtil.i(TAG, "now, log path has used " + getPathMB(mConfig.logPath) + "MB");
    }

    private void clearExpiredLog() {
        List<String> files = getExpiredFileList(mConfig.logPath, mConfig.logSuffix, mConfig.expiredDays);
        if (files == null || files.size() == 0)
            return;

        LogUtil.i(TAG, "delete expired logs: ");
        for (String file: files) {
            File expired = new File(file);
            if (!expired.exists() || !expired.isFile())
                continue;
            LogUtil.i(TAG, expired.getName());
            expired.delete();
        }
    }

    private void checkNewDayLoop() {
        if (mContext == null || mNRTHandler == null)
            return;

        mNRTHandler.post(new Runnable() {
            @Override
            public void run() {
                LogCollector.this.stopCollectionImpl();

                // clear logcat cache to avoid repetitive recording
                clearLogCache();
                LogUtil.i(TAG, "clearLogCache...");

                LogCollector.this.startCollectionImpl();
            }
        });
    }

    private void clearPathLoop() {
        if (mContext == null || mNRTHandler == null)
            return;

        clearPath();
        mNRTHandler.postDelayed(mClearPath, LOG_CLEAR_PATH_INTERVAL_MS);
    }

    ///////////////// static utils

    private static void clearLogCache() {
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult("logcat -c");
        if (result.code != 0)
            LogUtil.w(TAG, "clearLogCache failed: " + result.AllToString());
    }

    private static Result killAllLogcat4CurrentUser() {
        String cmd = Rockchip.isProduct() ? "ps" : "ps -ef";
        cmd += "| egrep logcat | grep -v grep | busybox awk '{print $2}'";
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd);
        LogCmd("killAllLogcat4CurrentUser", cmd, result);
        if (result.code != 0)
            return new Result(BaseError.INTERNAL_ERROR, "get all logcat process failed: " + result.AllToString());

        cmd = "";
        List<String> pids = result.stdOut;
        for (String pid: pids) {
            String tmp = pid.trim();
            if (tmp.isEmpty())
                continue;
            try {
                int id = Integer.valueOf(tmp);
                cmd += id + " ";
            } catch (Exception e) {
                continue;
            }
        }

        if (cmd.isEmpty())
            return Result.SUCCESS;

        cmd = "kill -9 " + cmd;
        result = LocalLinuxUtil.doShellWithResult(cmd);
        LogCmd("killAllLogcat4CurrentUser", cmd, result);
        if (result.code == 0)
            return Result.SUCCESS;
        else
            return new Result(BaseError.INTERNAL_ERROR, "kill all logcat process failed: " + result.AllToString());
    }

    private static long getPathMB(String path) {
        String cmd = String.format("du -sm %s | busybox awk '{print $1}'", path);
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd) ;
        LogCmd("getPathMB", cmd, result);

        if (result.code != 0) {
            LogUtil.w(TAG, "getPathMB failed: " + result.AllToString());
            return 0;
        }
        if (result.stdOut.size() == 0) {
            LogUtil.w(TAG, "getPathMB failed with unknown error");
            return 0;
        }
        String tmp = result.stdOut.get(0).trim();
        if (tmp.isEmpty()) {
            LogUtil.w(TAG, "getPathMB failed with unknown error2");
            return 0;
        }

        try {
            return Long.valueOf(tmp);
        } catch (Exception e) {
            LogUtil.w(TAG, "getPathMB error", e);
            return 0;
        }
    }

    private static List<String> getSortedFileList(String path, String suffix) {
        String cmd = String.format("files=`find %s -type f -name \"*%s*\"`; [ -n \"$files\" ] && ls -rt $files", path, suffix);
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd) ;
        LogCmd("getSortedFileList", cmd, result);

        if (result.code == 1) { // not found
            return new ArrayList<>();
        } else if (result.code != 0) { // with error
            LogUtil.w(TAG, "getSortedFileList failed: " + result.AllToString());
            return null;
        } else { // found
            return result.stdOut;
        }
    }

    private static List<String> getExpiredFileList(String path, String suffix, int expiredDays) {
        String cmd = String.format("find %s -type f -name \"*%s*\" -mtime +%d", path, suffix, expiredDays);
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd) ;
        LogCmd("getExpiredFileList", cmd, result);

        if (result.code != 0) {
            LogUtil.w(TAG, "getExpiredFileList failed: " + result.AllToString());
            return null;
        }
        return result.stdOut;
    }

    private static List<String> getTodayFileList(String path, String suffix) {
        String cmd = String.format("find %s -type f -name \"*%s*\" -mtime -1", path, suffix);
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd) ;
        LogCmd("getTodayFileList", cmd, result);

        if (result.code != 0) {
            LogUtil.w(TAG, "getTodayFileList failed: " + result.AllToString());
            return null;
        }
        return result.stdOut;
    }

    private static Result zipLogs(String path, String suffix, String options, String zipPrefix) {
        String zipFile = zipPrefix + ".tar.gz";
        String cmd = String.format("cd %s && find * -type f -name \"*%s*\" %s |xargs tar zcf %s", path, suffix, options, zipFile);
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult(cmd) ;
        LogCmd("zipLogs", cmd, result);
        if (result.code != 0) {
            String message = "zipLogs failed: " + result.AllToString();
            LogUtil.w(TAG, message);
            return new Result(BaseError.INTERNAL_ERROR, message);
        }

        if (result.stdErr.size() != 0)
            return new Result(BaseError.INTERNAL_ERROR, result.ErrorToString());
        else
            return Result.buildSuccess(zipFile);
    }

    private static void printLogcatInfo() {
        LocalLinuxUtil.Result result = LocalLinuxUtil.doShellWithResult("logcat -g");
        LogCmd("printLogcatInfo", "logcat -g", result);
    }

    private static void LogCmd(String action, String cmd, LocalLinuxUtil.Result result) {
        LogUtil.v(TAG, String.format(action + ", cmd: \n%s \nresult: \n%s", cmd, result.AllToString()));
    }

    private static void callbackError(Callback callback, String action, int code, String message) {
        String error = String.format("%scode: %d message: %s", (action == null ? "" : action + ", "), code, message);
        LogUtil.w(TAG, error);
        if (callback != null)
            callback.done(new Result(code, message));
    }

    private static void callbackResult(Callback callback, Result result) {
        if (callback != null)
            callback.done(result);
    }

    private static void callbackSuccess(Callback callback) {
        if (callback != null)
            callback.done(Result.SUCCESS);
    }
}