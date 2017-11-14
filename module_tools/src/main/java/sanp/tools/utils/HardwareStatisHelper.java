package sanp.tools.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Tuyj on 2017/7/19.
 */

public class HardwareStatisHelper {

    public static long getTotalCpuTime()
    { // 获取系统总CPU使用时间
        String[] cpuInfos = null;
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        long totalCpu = Long.parseLong(cpuInfos[2])
                + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
                + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
                + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
        return totalCpu;
    }

    public static long getAppCpuTime()
    { // 获取应用占用的CPU时间
        String[] cpuInfos = null;
        try
        {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        long appCpuTime = Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]);
        return appCpuTime;
    }

    private static long getAvailableMemory(Context context) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.availMem;
    }

    public static float getUsedPercentValue(Context context) {
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            long totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
            long availableSize = getAvailableMemory(context) / 1024;
            float percent = (totalMemorySize - availableSize) / (float) totalMemorySize * 100;
            return percent;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static float getDeviceTemperature() {
        File file = new File("/sys/devices/virtual/thermal/thermal_zone0/temp");
        if (!file.exists())
            file = new File("/sys/class/thermal/thermal_zone0/temp");
        if (!file.exists())
            return 0;

        try {
            FileReader fr4 = new FileReader(file);
            BufferedReader br4 = new BufferedReader(fr4);
            String value = br4.readLine();
            br4.close();
            if(value.length() == 0)
                return 0;
            else if(value.length() > 2)
                value = value.substring(0, 2) + "." + value.substring(2, 3);
            return Float.valueOf(value);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /*
    public static class TempSensor implements SensorEventListener {
        private final SensorManager mSensorManager;
        private final Sensor mTempSensor;

        public TempSensor(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }

        public void start() {
            mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
            LogManager.w("TempSensor start");
        }

        public void stop() {
            mSensorManager.unregisterListener(this);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            LogManager.w("onAccuracyChanged:" + accuracy);
        }

        public void onSensorChanged(SensorEvent event) {
            float temp = event.values[0];
            LogManager.w("onSensorChanged: " + temp);
        }
    }
    //*/
}
