package sanp.tools.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Tom on 2017/3/25.
 */

public class TimeUtils {

    /**
     * 得到秒数累计时间
     *
     * @param str_sec 秒数int值
     * @return "HH:mm:ss"
     */
    public static String getTimeString(int str_sec) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (str_sec <= 0) {
            return "00:00:00";
        } else {
            minute = str_sec / 60;
            hour = minute / 60;
            if (hour > 99) {
                return "99:59:59";
            }
            minute = minute % 60;
            second = str_sec - hour * 3600 - minute * 60;
            timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
        }
        return timeStr;
    }

    /**
     * 不足10前面补0
     *
     * @param i 数值int
     * @return "22"||"02"
     */
    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }

    /**
     * 获得日历类
     *
     * @param strdate 2017-04-18
     * @return
     */
    public static Calendar getCalendar(String strdate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date date = format.parse(strdate);
            Calendar cd = Calendar.getInstance();
            cd.setTime(date);
            return cd;
        } catch (ParseException e) {
            LogManager.e(e);
            return null;
        }
    }

    //获得日历类
    public static Calendar getCalendar(long longdate) {
        Date date = new Date(longdate);
        Calendar cd = Calendar.getInstance();
        cd.setTime(date);
        return cd;
    }

    //时间long值转format
    public static String longToStr(long l, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(l);
    }

    //时间long值转format
    public static String dateToStr(Date d, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(d);
    }

    /**
     * 毫秒值转换成星期
     */
    public static int longToDay(long millis) {
        Calendar cal = getCalendar(millis);
        int week = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return week;
    }

    /**
     * 获得一周周一周日时间
     *
     * @param l
     * @return ["2017-04-17 00:00:00", "2017-04-23 23:59:59"]
     */
    public static String[] getMondaySunday(long l) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date monday;
        Date sunday;
        Calendar cal = getCalendar(l);
        //周日~周六 -- 1~7
        int w = cal.get(Calendar.DAY_OF_WEEK);
        if (w != 1) {
            cal.add(Calendar.DATE, -w + 2);
        } else {
            cal.add(Calendar.DATE, -6);
        }
        monday = cal.getTime();

        cal = getCalendar(l);
        //周日~周六 -- 1~7
        w = cal.get(Calendar.DAY_OF_WEEK);
        if (w != 1) {
            cal.add(Calendar.DATE, +8 - w);
        }
        sunday = cal.getTime();

        return new String[]{format.format(monday) +
                " 00:00:00", format.format(sunday) + " 23:59:59"};
    }

    /**
     * 根据日期字符得到周一时间
     *
     * @param strdate 2017-04-18
     * @return "2017-04-17"
     * @throws Exception
     */
    public static String getMonday(String strdate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = getCalendar(strdate);
        //周日~周六 -- 1~7
        int w = cal.get(Calendar.DAY_OF_WEEK);
        if (w != 1) {
            cal.add(Calendar.DATE, -w + 2);
        } else {
            cal.add(Calendar.DATE, -6);
        }
        return format.format(cal.getTime());
    }

    public static long getMondayLong(long longdate) {
        Calendar cal = getCalendar(longdate);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        //周日~周六 -- 1~7
        int w = cal.get(Calendar.DAY_OF_WEEK);
        if (w != 1) {
            cal.add(Calendar.DATE, -w + 2);
        } else {
            cal.add(Calendar.DATE, -6);
        }
        return cal.getTimeInMillis();
    }


    /**
     * 根据日期字符串判断第几月第几周
     *
     * @param strdate 2017-04-18
     * @return "4月第3周"
     * @throws Exception
     */
    public static String getMonthWeek(String strdate) {
        String month = strdate.split("-")[1] + "月第";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = getCalendar(strdate);
        //第几周，从1开始计
        int week = calendar.get(Calendar.WEEK_OF_MONTH);
        //第几天，从周日开始1~7
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        //周日算上周
        if (day == 1) {
            return month + (week - 1) + "周";
        }
        return month + week + "周";
    }

    /**
     *
     * @return 得到电话记录的专用时间
     */
    public static String getHistroyCallTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date date = new Date();
        String day = format.format(date);
        return day;
    }

}
