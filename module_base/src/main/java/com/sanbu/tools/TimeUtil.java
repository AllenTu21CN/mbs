package com.sanbu.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Tom on 2017/3/25.
 */

public class TimeUtil {

    public static final String FORMAT_1 = "yyyy/MM/dd HH:mm";

    public static final String FORMAT_2 = "yyyy-MM-dd HH:mm";

    public static final String FORMAT_3 = "yyyy-MM-dd HH:mm:ss";

    public static final String FORMAT_HH_MM = "HH:mm";

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
    public static String unitFormat(int i) {
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
            LogUtil.e(e);
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
     * @return 得到电话记录的专用时间
     */
    public static String getFormatTime(String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    public static String getHMSTime(long milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(milliseconds);
        return hms;
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  E  yyyy-MM-dd ");
        Date date = new Date();
        return sdf.format(date);
    }

    /*
     * 毫秒转化时分秒
     */
    public static String getHMSTime2(Long ms) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Long hour = ms / hh;
        Long minute = (ms - hour * hh) / mi;
        Long second = (ms - hour * hh - minute * mi) / ss;
        StringBuilder sb = new StringBuilder();
        if (hour > 9) {
            sb.append(hour).append(":");
        } else {
            sb.append("0").append(hour).append(":");
        }
        if (minute > 9) {
            sb.append(minute).append(":");
        } else {
            sb.append("0").append(minute).append(":");
        }
        if (second > 9) {
            sb.append(second);
        } else {
            sb.append("0").append(second);
        }
        return sb.toString();
    }


    /**
     * 得到当前系统时间
     *
     * @param format
     * @return
     */
    public static String getCurrentTime(String format) {
        if (null == format || "".equals(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(String s, String format) {
        return stampToDate(Long.valueOf(s), format);
    }

    public static String stampToDate(long time) {
        return stampToDate(time,null);
    }

    public static String stampToDate(long time, String format) {
        if (null == format)
            format = "yyyy-MM-dd HH:mm:ss";

        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = new Date(time);
        res = simpleDateFormat.format(date);
        return res;
    }

    public static String getDateByStamp(long stamp) {
        String time = stampToDate(stamp, null);
        return time.split(" ")[0];
    }

    public static String getDayTimeByStamp(long stamp) {
        String time = stampToDate(stamp, null);
        return time.split(" ")[1];
    }

    /**
     * 获得指定日期的后一天
     *
     * @param specifiedDay
     * @return
     */
    public static String getSpecifiedDayAfter(String specifiedDay) {
        Calendar c = Calendar.getInstance();
        Date date = null;
        try {
            date = new SimpleDateFormat("yy-MM-dd").parse(specifiedDay);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(date);
        int day = c.get(Calendar.DATE);
        c.set(Calendar.DATE, day + 1);

        String dayAfter = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        return dayAfter;
    }

    /**
     * 获得指定日期的前一天
     *
     * @param specifiedDay
     * @return
     */
    public static String getSpecifiedDayBefore(String specifiedDay) {
        Calendar c = Calendar.getInstance();
        Date date = null;
        try {
            date = new SimpleDateFormat("yy-MM-dd").parse(specifiedDay);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(date);
        int day = c.get(Calendar.DATE);
        c.set(Calendar.DATE, day - 1);

        String dayAfter = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        return dayAfter;
    }

    /*
     * 将时间转换为时间戳
     */
    public static long dateToStamp(String s, String format) {
        if (null == format)
            format = "yyyy-MM-dd HH:mm:ss";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = simpleDateFormat.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long ts = date.getTime();
        return ts;
    }

    /**
     * 查询当前日期是本月的第几周
     *
     * @return
     */
    public static int getCurrentWeekNum() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        //日历式
        int week_of_month = calendar.get(Calendar.WEEK_OF_MONTH);
        //天数式
        //int day_of_week_in_month = calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH);
        return week_of_month;
    }

    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        return month;
    }

    /**
     * 获取当前日期是一周的第几天
     *
     * @return
     */
    public static int getDayWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        //获得当前日期是一个星期的第几天
        int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayWeek;
    }

    /**
     * 获取本周的开始日期
     *
     * @return
     */
    public static String getStartDateOfWeek() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        //获取当前日期是本周的第几天
        int dayWeek = getDayWeek();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.setFirstDayOfWeek(Calendar.MONDAY);//设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        int day = cal.get(Calendar.DAY_OF_WEEK);//获得当前日期是一个星期的第几天
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);//根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        return sdf.format(cal.getTime());
    }

    /**
     * 获取本周的结束日期
     *
     * @return
     */
    public static String getEndDateOfWeek() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //设置时间格式
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(getStartDateOfWeek()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.DATE, 6);
        return sdf.format(cal.getTime());
    }

    /**
     * 根据传入的日期字符串获取当前为本月第几周，以及周起始日期
     *
     * @param time
     * @param fomat
     * @return
     */
    public static Map<String, Object> getWeekDataMap(String time, String fomat) {
        if (null == fomat || "".equals(fomat)) {
            fomat = "yyyy-MM-dd";
        }
        Map<String, Object> result = new HashMap<String, Object>();
        //获取传入日期是当月第几周
        int weekOfMonth = 0;
        SimpleDateFormat sdf = new SimpleDateFormat(fomat);
        Calendar cal = Calendar.getInstance();
        try {
            Date date = sdf.parse(time);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTime(date);
            weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
            result.put("weekOfMonth", weekOfMonth);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //获取传入日期所在周的起始日期
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);//获得当前日期是一个星期的第几天
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.setFirstDayOfWeek(Calendar.MONDAY);//设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        int day = cal.get(Calendar.DAY_OF_WEEK);//获得当前日期是一个星期的第几天
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);//根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        result.put("startDate", sdf.format(cal.getTime()));
        cal.add(Calendar.DATE, 6);
        result.put("endDate", sdf.format(cal.getTime()));
        return result;
    }

    /**
     * 获取time所在的一周的日期
     *
     * @param time
     * @param format
     * @return
     */
    public static List<String> getWeekDateList(String time, String format) {
        if (null == format || "".equals(format)) {
            format = "yyyy-MM-dd";
        }
        List<String> weekList = new ArrayList<String>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format); //设置时间格式
            Calendar cal = Calendar.getInstance();
            Date date = sdf.parse(time);
            cal.setTime(date);
            int dayWeek = cal.get(Calendar.DAY_OF_WEEK);//获得当前日期是一个星期的第几天
            if (1 == dayWeek) {
                cal.add(Calendar.DAY_OF_MONTH, -1);
            }
            cal.setFirstDayOfWeek(Calendar.MONDAY);//设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
            int day = cal.get(Calendar.DAY_OF_WEEK);//获得当前日期是一个星期的第几天
            cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);//根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
            weekList.add(sdf.format(cal.getTime()));
            for (int i = 0; i < 6; i++) {
                cal.add(Calendar.DATE, 1);
                weekList.add(sdf.format(cal.getTime()));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return weekList;
    }

    /**
     * 获取输入时间属于当月第几周
     *
     * @param time
     * @param format
     * @return
     */
    public static Map<String, Integer> getWeekNumByMonth(String time, String format) {
        Map<String, Integer> result = new HashMap<String, Integer>();

        if (null == format || "".equals(format)) {
            format = "yyyy-MM-dd";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            calendar.setTime(date);
            result.put("week", calendar.get(Calendar.WEEK_OF_MONTH));
            result.put("month", calendar.get(Calendar.MONTH) + 1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        //获取当前周次
//        int week = getCurrentWeekNum();
//        System.out.println(week);
        //获取当前周次的开始日期
//        String ww = getStartDateOfWeek();
//        System.out.println(ww);
        //获取当前周次的结束日期
//        String weew = getEndDateOfWeek();
//        System.out.println(weew);
        //综合上述三个结果，可动态传入日期
//        Map map = getWeekDataMap("2018-11-01", "yyyy-MM-dd");
//        System.out.println(map);
        System.out.println(getWeekNumByMonth("2018-10-30", null));
    }

    //true：dateTime大于now，即dateTime在当前时间之后
    public static boolean afterNowTime(long dateTime) {
        return (dateTime - new Date().getTime()) > 0;
    }
}
