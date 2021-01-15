package com.sanbu.modules;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.google.gson.reflect.TypeToken;
import com.sanbu.base.BaseEvents;
import com.sanbu.base.Callback;
import com.sanbu.base.Result;
import com.sanbu.discarded.tools.LooperWorker;
import com.sanbu.discarded.tools.SPUtil;
import com.sanbu.tools.EventPub;
import com.sanbu.tools.ExtractZip;
import com.sanbu.tools.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class UpgradeManager {

    private static final String TAG = UpgradeManager.class.getSimpleName();

    private static final long FLUSH_TASK_INTERVAL_MS = 15000;

    public enum ActionType {
        Unknown("unknown"),
        Apk("apk"),
        Script("script");

        private String dsp;

        ActionType(String dsp) {
            this.dsp = dsp;
        }

        public String toString() {
            return dsp;
        }

        public static ActionType fromString(String dsp) {
            if (dsp == null)
                return Unknown;

            if (dsp.equals(Apk.dsp))
                return Apk;
            else if (dsp.equals(Script.dsp))
                return Script;
            else
                return Unknown;
        }
    }

    public enum ActionCode {
        Unknown(-1),
        RightNow(0),
        IdleTime(1),
        BootTime(2);

        private int value;

        ActionCode(int value) {
            this.value = value;
        }

        public int toValue() {
            return value;
        }

        public static ActionCode fromValue(int value) {
            if (value == RightNow.value)
                return RightNow;
            else if (value == IdleTime.value)
                return IdleTime;
            else if (value == BootTime.value)
                return BootTime;
            else
                return Unknown;
        }
    }

    public static class Config {
        public final String SPNamespace;
        public final String SPKey;
        public final String upgradeDirectory;
        public final String tempDirectory;

        public Config(String SPNamespace, String SPKey, String upgradeDirectory, String tempDirectory) {
            this.SPNamespace = SPNamespace;
            this.SPKey = SPKey;
            this.upgradeDirectory = upgradeDirectory;
            this.tempDirectory = tempDirectory;
        }
    }

    private static class Task {
        public final ActionType type;
        public final ActionCode code;
        public final String packageFile;
        public final String scriptFile;
        public final Callback callback;

        public Task(ActionType type, ActionCode code, String packageFile, String scriptFile, Callback callback) {
            this.type = type;
            this.code = code;
            this.packageFile = packageFile;
            this.scriptFile = scriptFile;
            this.callback = callback;
        }
    }

    private Context mContext;
    private SPUtil mSPUtil;
    private String mSPNamespace;
    private String mSPKey;
	private File mTempDirectory;
    private File mUpgradeDirectory;
    private LooperWorker mNonRealTimeWorker;
    private boolean mWorkerOwner = false;

    private Object mLock = new Object();
    private List<Task> mTasks;

    private Runnable mFlushTask = new Runnable() {
        @Override
        public void run() {
            UpgradeManager.this.flushTaskLoop();
        }
    };

    public int init(Context context, Config config, LooperWorker nonRealTimeWorker) {
        if (mContext == null) {
            mContext = context;
            mSPKey = config.SPKey;
            mSPNamespace = config.SPNamespace;
            mSPUtil = SPUtil.getInstance();
            mNonRealTimeWorker = nonRealTimeWorker;
            mWorkerOwner = false;
            if (mNonRealTimeWorker == null) {
                mNonRealTimeWorker = new LooperWorker("Worker@" + TAG);
                mWorkerOwner = true;
            }

            mTempDirectory = new File(config.tempDirectory);
            mUpgradeDirectory = new File(config.upgradeDirectory);
            mTempDirectory.mkdirs();
            mUpgradeDirectory.mkdirs();

            loadPreferences();

            initWorker();

            mNonRealTimeWorker.postDelayed(mFlushTask, FLUSH_TASK_INTERVAL_MS);
        }

        return 0;
    }

    public void release() {
        synchronized (mLock) {
            if (mContext != null) {

                mNonRealTimeWorker.removeCallbacks(mFlushTask);

                if (mWorkerOwner)
                    mNonRealTimeWorker.stopTheLoopThreadUntilOver(false);
                mNonRealTimeWorker = null;
                mWorkerOwner = false;

                mContext = null;
                mSPUtil = null;
                mSPNamespace = null;
                mTempDirectory = null;
                mUpgradeDirectory = null;
                mTasks = null;
            }
        }
    }

    public void recovery() {
        synchronized (mLock) {
            if (mTasks != null) {
                mTasks.clear();
                mSPUtil.remove(mSPNamespace, mSPKey);
            }
        }
    }

    public Result pushTask(ActionType type, ActionCode code, String packageFile, String scriptFile, Callback callback) {
        if (type == ActionType.Unknown) {
            String message = "not support to update " + type.dsp;
            LogUtil.w(TAG, "reject the action: " + message);
            return new Result(-1, message);
        }
        if (packageFile == null || !new File(packageFile).exists()) {
            String message = "packageFile not exists: " + packageFile;
            LogUtil.w(TAG, "reject the action: " + message);
            return new Result(-1, message);
        }
        if (scriptFile != null && !new File(scriptFile).exists()) {
            String message = "scriptFile not exists: " + scriptFile;
            LogUtil.w(TAG, "reject the action: " + message);
            return new Result(-1, message);
        }

        final Task task = new Task(type, code, packageFile, scriptFile, callback);
        if (code == ActionCode.RightNow) {
            mNonRealTimeWorker.postDelayed(new Runnable() {
                @Override
                public void run() {
                    UpgradeManager.this.doTask(task);
                }
            }, 500);
            return Result.SUCCESS;
        } else if (code == ActionCode.BootTime || code == ActionCode.IdleTime) {
            synchronized (mLock) {
                mTasks.add(task);
                mSPUtil.putObject(mSPNamespace, mSPKey, mTasks);
            }
            LogUtil.w(TAG, String.format("do task later: type-%s code-%d package-%s script-%s",
                    type.dsp, code.value, packageFile, scriptFile));
            return Result.SUCCESS;
        } else {
            String message = "unknown action code: " + code.value;
            LogUtil.w(TAG, message);
            return new Result(-1, message);
        }
    }

    protected abstract boolean isReady();

    protected abstract Result doUpgrade(String apkPath);

    protected abstract Result doUpgrade(String packageFile, String scriptFile);

    protected abstract List<String> getSupportingPackageNames(); // sort by priority

    private void initWorker() {
        if (mWorkerOwner) {
            LogUtil.w(TAG, "create Worker in a new thread");
            mNonRealTimeWorker.startLoopInNewThreadUntilReady();
        }
    }

    private void loadPreferences() {
        mTasks = mSPUtil.getObject(mSPNamespace, mSPKey, new TypeToken<List<Task>>() {}.getType(), null);
        if (mTasks == null)
            mTasks = new ArrayList<>();
    }

    private void flushTaskLoop() {
        LogUtil.v("UpgradeManager, try to flushTask");
        flushTask();
        mNonRealTimeWorker.postDelayed(mFlushTask, FLUSH_TASK_INTERVAL_MS);
    }

    private void flushTask() {
        if (!isReady())
            return;

        // try to get task
        Task task = null;
        synchronized (mLock) {
            if (mTasks.size() > 0) {
                task = mTasks.remove(0);
                mSPUtil.putObject(mSPNamespace, mSPKey, mTasks);
            }
        }
		
        // do task
        if (task != null) {
            final Task t = task;
            mNonRealTimeWorker.postDelayed(new Runnable() {
                @Override
                public void run() {
                    UpgradeManager.this.doTask(t);
                }
            }, 500);
        }
    }

    private void doTask(Task task) {
        Result result;
        if (task.type == ActionType.Apk)
            result = doApkUpgrade(task); // pre-process for apk upgrading
        else
            result = doUpgrade(task.packageFile, task.scriptFile);

        if (task.callback != null) {
            task.callback.done(result);
        } else {
            if (!result.isSuccessful()) {
                LogUtil.w(TAG, result.message);
                EventPub.getDefaultPub().post(BaseEvents.buildEvt4UserHint("更新失败: " + result.message, true));
            }
        }
    }

    private Result doApkUpgrade(Task task) {
        // check and sort upgrade apks
        List<String> sortedApks = new ArrayList<>();
        File upgradeFile = new File(task.packageFile);
        Result result = checkAndPreprocessUpgradeFile(upgradeFile, sortedApks, null);
        if (!result.isSuccessful())
            return result;

        // do upgrading
        for (String apkPath: sortedApks) {
            result = doUpgrade(apkPath);
            if (!result.isSuccessful())
                break;
        }
        return result;
    }

    public Result checkAndPreprocessUpgradeFile(File upgradeFile, List<String> sortedApks, List<String> sortedPackageNames) {
        if (sortedApks != null)
            sortedApks.clear();
        if (sortedPackageNames != null)
            sortedPackageNames.clear();
        if (!upgradeFile.exists())
            return new Result(-1, "upgradeFile not exists");

        if (isApk(upgradeFile.getName())) {

            // move the upgrade file to upgrade directory
            String path = upgradeFile.getParent();
            if (path == null || !path.equals(mUpgradeDirectory.getAbsolutePath())) {
                File newFile  = new File(mUpgradeDirectory.getAbsolutePath(), upgradeFile.getName());
                try {
                    if (upgradeFile.renameTo(newFile))
                        upgradeFile = newFile;
                } catch (Exception e){
                    LogUtil.w(TAG, "move the upgrade file to upgrade directory failed:");
                    e.printStackTrace();
                }
            }

            return checkAndSortApksByPackageName(Arrays.asList(upgradeFile.getAbsolutePath()), sortedApks, sortedPackageNames);

        } else if (isZip(upgradeFile.getName())) {

            // extract zip files firstly
            List<String> files = ExtractZip.extract(upgradeFile.getAbsolutePath(), mUpgradeDirectory.getAbsolutePath());

            // check file list
            if (files.size() == 0) {
                String message = upgradeFile.getName() + " is empty";
                return new Result(-1, message);
            }

            // get the apk filename
            List<String> apkFiles = new ArrayList<>();
            for (String filename: files) {
                if (isApk(filename)) {
                    apkFiles.add(filename);
                }
            }

            // skip empty task
            if (apkFiles.size() == 0) {
                String message = upgradeFile.getName() + " not contains a apk file";
                return new Result(-1, message);
            }

            return checkAndSortApksByPackageName(apkFiles, sortedApks, sortedPackageNames);

        } else {
            String message = upgradeFile.getName() + " is unknown file type";
            return new Result(-1, message);
        }
    }

    private Result checkAndSortApksByPackageName(final List<String> oriApks,
                                                 List<String> sortedApks, List<String> sortedPackageNames) {
        // check apk package-name validity
        List<String> packageNames = new ArrayList<>();
        Result result = checkPackageName(oriApks, packageNames);
        if (!result.isSuccessful())
            return result;

        // resort apk list by user priority
        sortByPackageName(oriApks, packageNames, sortedApks, sortedPackageNames);
        return result;
    }

    private Result checkPackageName(final List<String> apkPaths, List<String> packageNames) {
        int code = 0;
        String message = null;

        for (String apk: apkPaths) {
            // get apk package name
            String apkPackageName = null;
            PackageInfo pi = mContext.getPackageManager().getPackageArchiveInfo(apk, 0);
            if (pi != null)
                apkPackageName = pi.applicationInfo.packageName;

            // check apk package name
            if (apkPackageName == null || !getSupportingPackageNames().contains(apkPackageName)) {
                if (code == 0) {
                    code = -1;
                    message = "non supported apks: ";
                }
                message += "[" + apkPackageName + "@" + new File(apk).getName() + "],";
            } else {
                packageNames.add(apkPackageName);
            }
        }

        return new Result(code, message);
    }

    private void sortByPackageName(final List<String> oriApks, final List<String> packageNames,
                                   List<String> sortedApks, List<String> sortedPackageNames) {
        for (String packageName: getSupportingPackageNames()) {
            int index = packageNames.indexOf(packageName);
            if (index >= 0) {
                if (sortedApks != null)
                    sortedApks.add(oriApks.get(index));
                if (sortedPackageNames != null)
                    sortedPackageNames.add(packageName);
            }
        }
    }

    private static boolean isApk(String filename) {
        if (filename == null || filename.length() <= 4)
            return false;
        return filename.substring(filename.length()-4, filename.length()).equals(".apk");
    }

    private static boolean isZip(String filename) {
        if (filename == null || filename.length() <= 4)
            return false;
        return filename.substring(filename.length()-4, filename.length()).equals(".zip");
    }
}
