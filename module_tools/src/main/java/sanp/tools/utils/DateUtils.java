package sanp.tools.utils;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;

public class DateUtils {

    private static String[] monthArray = {"01月", "02月", "03月", "04月", "05月", "06月", "07月", "08月", "09月", "10月", "11月", "12月"};
    private static String[] hourArray = {"01时", "02时", "03时", "04时", "05时", "06时", "07时", "08时", "09时", "10时", "11时", "12时", "13时", "14时", "15时", "16时", "17时", "18时", "19时", "20时", "21时", "22时", "23时", "00时"};
    private static String[] miniArray = {"00分", "01分", "02分", "03分", "04分", "05分", "06分", "07分", "08分", "09分", "10分", "11分", "12分", "13分", "14分", "15分", "16分", "17分", "18分", "18分", "20分", "21分", "22分", "23分", "24分", "25分", "26分", "27分", "28分", "29分", "30分", "31分", "32分", "33分", "34分", "35分", "36分", "37分", "38分", "39分", "40分", "41分", "42分", "43分", "44分", "45分", "46分", "47分", "48分", "49分", "50分", "51分", "52分", "53分", "54分", "55分", "56分", "57分", "58分", "59分"};


    public static SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    public static SimpleDateFormat sdfWithoutHour = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.CHINA);

    public static SimpleDateFormat sdfWithoutDay = new SimpleDateFormat(
            "yyyy-MM", Locale.CHINA);

    /**
     * 字符年份
     *
     * @param t
     * @return
     */
    public static List<String> getAllYearList(int t) {
        List<String> infoList = new ArrayList<>();
        for (int i = t - 20; i < t + 20; i++) {
            infoList.add(i + "年");
        }
        return infoList;
    }

    /**
     * 获取月份
     *
     * @return
     */
    public static List<String> getAllMonthList() {
        return Arrays.asList(monthArray);
    }

    /**
     * 获取月份
     *
     * @return
     */
    public static List<String> getAllHourList() {
        return Arrays.asList(hourArray);
    }

    /**
     * 获取月份
     *
     * @return
     */
    public static List<String> getAllMinuList() {
        return Arrays.asList(miniArray);
    }

    /**
     * 获取每月天数
     * 时间格式 2013-04
     *
     * @param
     * @return
     */
    public static int getAllDaysList(String yearStr,String monthStr) {
        int daysNum = 0;
        if (yearStr.indexOf("年") != -1) {
            yearStr = yearStr.substring(0,yearStr.length()-1);
        }
        if (monthStr.indexOf("月") != -1) {
            monthStr = monthStr.substring(0,monthStr.length()-1);
        }
        int year = Integer.parseInt(yearStr);
        int month = Integer.parseInt(monthStr);
        if (month == 2) {
            if (isleapyear(year)) {
                daysNum = 29;  // 29天
            } else {
                daysNum = 28;  // 28天
            }
        } else {
            switch (month) {
                case 1:
                    daysNum = 31;
                    break;
                case 3:
                    daysNum = 31;
                    break;
                case 4:
                    daysNum = 30;
                    break;
                case 5:
                    daysNum = 31;
                    break;
                case 6:
                    daysNum = 30;
                    break;
                case 7:
                    daysNum = 31;
                    break;
                case 8:
                    daysNum = 31;
                    break;
                case 9:
                    daysNum = 30;
                    break;
                case 10:
                    daysNum = 31;
                    break;
                case 11:
                    daysNum = 30;
                    break;
                case 12:
                    daysNum = 31;
                    break;
            }
        }
        return daysNum;
    }


    // 得到某年第一月到十二月
    @SuppressLint("SimpleDateFormat")
    public static String getAllMonthOfYear(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        return year + "-01~" + year + "-12";// 输出格式：2015-01~2015~12
    }

    public static String getNowDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        return str;
    }

    /**
     * 把"yyyy-MM-dd HH:mm:ss"的时间转为"yyyy-MM-dd"的时间
     *
     * @param time
     * @return
     */
    public static String formatTimeWithoutHour(String time) {
        try {
            Date date = sdf.parse(time);
            time = sdfWithoutHour.format(date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
           LogManager.e(e);
        }
        return time;
    }

    /**
     * 把年月日时分秒转成年月日
     *
     * @return
     */
    public static String transDate2DateWithHour(String time) {
        try {
            Date date = sdf.parse(time);
            time = sdfWithoutHour.format(date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
           LogManager.e(e);
        }
        return time;
    }

    /**
     * 把年月日时分秒转成年月日
     *
     * @return
     */
    public static Date transDate1DateWithHour(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(time, pos);
        return strtodate;
    }

    // 判断收款时间是周几
    public static String getWeek(Date data) {
        String Week = "星期";
        Calendar c = Calendar.getInstance();
        c.setTime(data);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                Week += "日";
                break;
            case 2:
                Week += "一";
                break;
            case 3:
                Week += "二";
                break;
            case 4:
                Week += "三";
                break;
            case 5:
                Week += "四";
                break;
            case 6:
                Week += "五";
                break;
            case 7:
                Week += "六";
                break;
            default:
                break;
        }
        return Week;
    }

    // 得到某年第一月
    @SuppressLint("SimpleDateFormat")
    public static String getFirstMonthOfYear(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        return year + "-01";// 输出格式：2015-01
    }

    // 得到某年最后一月
    @SuppressLint("SimpleDateFormat")
    public static String getLastMonthOfYear(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        return year + "-12";// 输出格式：2015-01
    }

    // 得到某年
    @SuppressLint("SimpleDateFormat")
    public static int getYear(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        return year;// 输出格式：2015-01
    }

    // 得到某月第一天的日期
    @SuppressLint("SimpleDateFormat")
    public static String getFirstDayOfMonth(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        int month = Integer.parseInt(time.substring(5, 7));
        return year + "-" + formatTime(month) + "-01";// 输出格式：2015-04-01
    }

    // 若时间是2013-1-1，返回2013-01-01
    public static String formatTime(int t) {
        return t >= 10 ? "" + t : "0" + t;// 三元运算符 t>10时取 ""+t
    }

    // 得到某月最后一天的日期
    @SuppressLint("SimpleDateFormat")
    public static String getLastDayOfMonth(String time) {// time格式：2015-04-14
        int year = Integer.parseInt(time.substring(0, 4));
        int month = Integer.parseInt(time.substring(5, 7));
        if (month == 2) {
            if (isleapyear(year)) {
                return year + "-02-29";// 输出格式：2015-02-29
            } else {
                return year + "-02-28";// 输出格式：2015-02-28
            }
        }
        // 计算一个月有多少天
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);// 要计算6月的日期，month-1要等于5
        int maxDate = cal.getActualMaximum(Calendar.DATE);

        return year + "-" + formatTime(month) + "-" + maxDate;// 输出格式：2015-04-31
    }

    // 判断年数是否是闰年
    private static boolean isleapyear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) ? true
                : false;
    }

    @SuppressLint("SimpleDateFormat")
    // 得到某周末周一的日期
    public static String getFirstDayOfWeek(int week) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.WEEK_OF_MONTH, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return dateformat.format(cal.getTime());// 输出格式：2015-04-31
    }

    // 得到某周末周末的日期
    @SuppressLint("SimpleDateFormat")
    public static String getLastDayOfWeek(int week) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        // cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.add(Calendar.WEEK_OF_MONTH, week + 1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return dateformat.format(cal.getTime());// 输出格式：2015-04-31
    }

    // 某一天
    @SuppressLint("SimpleDateFormat")
    public static String getCurrentDay(int day) {// 0表示当天，-1表示前一天，1表示后一天。。。
        Date date = new Date();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        date = calendar.getTime();
        return dateformat.format(date);// 输出格式：2015-04-31
    }

}
