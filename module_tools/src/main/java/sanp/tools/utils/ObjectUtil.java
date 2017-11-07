package sanp.tools.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ObjectUtil {
	private static DecimalFormat fmtDbl = new DecimalFormat("#.##");
	
	public static final String EMPTY_STR = "";

	/** 空对象判断 */
	public static boolean isNullOrEmpty(Object o) {
		return ((o == null) || (String.valueOf(o).trim().length() == 0)||"null".equals(o));
	}

	/** 空记录判断 */
	public static boolean isNullOrEmpty(List<?> l) {
		return ((l == null) || (l.isEmpty()));
	}

	/** 空集合判断 */
	public static boolean isNullOrEmpty(Collection<?> c) {
		return ((c == null) || (c.isEmpty()));
	}
	
	/** 非空对象判断 */
	public static boolean isNotNullOrEmpty(Object o) {
		return ((o != null) && (String.valueOf(o).trim().length() != 0)&&"null".equals(o)==false);
	}
	

	/** IP校验 */
	public static Boolean isIp(String s) {
		s = s.trim();
		if (ObjectUtil.isNullOrEmpty(s) || s.length() < 7 || s.length() > 15 ) {
			return false;
		}
		String regex = "(((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))[.](((2[0-4]\\d)|(25[0-5]))|(1\\d{2})|([1-9]\\d)|(\\d))";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(s);
		return m.matches();
	}

	/** Object转为int */
	public static int intOf(Object o) {
		int i = 0;
		try {
			i = Integer.parseInt(o.toString());
		} catch (Exception e) {

		}
		return i;
	}

	/** Object转为long */
	public static long longOf(Object o) {
		long l = 0L;
		try {
			l = Long.parseLong(o.toString());
		} catch (Exception e) {

		}
		return l;
	}

	/** Object转为double */
	public static double doubleOf(Object o) {
		double d = 0;
		try {
			d = Double.parseDouble(o.toString());
			String s = fmtDbl.format(d);
			d = Double.parseDouble(s);
		} catch (Exception e) {

		}
		return d;
	}

	/** Object转为String */
	public static String stringOf(Object o) {
		String s = "";
		if (!isNullOrEmpty(o)) {
			s = o.toString();
		}
		return s;
	}

	/** Object转为Boolean */
	public static boolean booleanOf(Object o) {
		boolean f = false;
		try {
			f = Boolean.parseBoolean(o.toString());
		} catch (Exception e) {

		}
		return f;
	}

	/**
	 * Object转为日期
	 */
	public static Date dateOf(Object o) throws ParseException {
		Date date = null;
		if (!isNullOrEmpty(o)) {
			String sdate = o.toString();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			date = df.parse(sdate);
		}
		return date;
	}
	
	/**
	 * Object转为日期
	 */
	public static Date datetimeOf(Object o) throws ParseException {
		Date date = null;
		if (!isNullOrEmpty(o)) {
			String sdate = o.toString();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			date = df.parse(sdate);
		}
		return date;
	}
	
	 /** 大写数字 */ 
	 private static final String[] NUMBERS = { "零", "壹", "贰", "叁", "肆", "伍", "陆", 
	 "柒", "捌", "玖" }; 
	 /** 整数部分的单位 */ 
	 private static final String[] IUNIT = { "元", "拾", "佰", "仟", "万", "拾", "佰", 
	 "仟", "亿", "拾", "佰", "仟", "万", "拾", "佰", "仟" }; 
	 /** 小数部分的单位 */ 
	 private static final String[] DUNIT = { "角", "分"}; 
	 /**
	  * 得到大写金额。
	  */ 
	 public static String toChinese(String str) { 
	     str = str.replaceAll(",", "");// 去掉","
	     String integerStr;// 整数部分数字
	     String decimalStr;// 小数部分数字
	 // 初始化：分离整数部分和小数部分
	 if (str.indexOf(".") > 0) { 
	       integerStr = str.substring(0, str.indexOf(".")); 
	       decimalStr = str.substring(str.indexOf(".") + 1); 
	     } else if (str.indexOf(".") == 0) { 
	       integerStr = ""; 
	       decimalStr = str.substring(1); 
	     } else { 
	       integerStr = str; 
	       decimalStr = ""; 
	     } 
	 // integerStr去掉首0，不必去掉decimalStr的尾0(超出部分舍去)
	 if (!integerStr.equals("")) { 
	       integerStr = Long.toString(Long.parseLong(integerStr)); 
	 if (integerStr.equals("0")) { 
	         integerStr = ""; 
	       } 
	     } 
	 // overflow超出处理能力，直接返回
	 if (integerStr.length() > IUNIT.length) { 
	       LogManager.e(str + ":超出处理能力");
	 return str; 
	     } 
	 int[] integers = toArray(integerStr);// 整数部分数字
	 boolean isMust5 = isMust5(integerStr);// 设置万单位
	 int[] decimals = toArray(decimalStr);// 小数部分数字
	 return getChineseInteger(integers, isMust5) + getChineseDecimal(decimals); 
	   } 
	 /**
	  * 整数部分和小数部分转换为数组，从高位至低位
	  */ 
	 private static int[] toArray(String number) { 
	 int[] array = new int[number.length()]; 
	 for (int i = 0; i < number.length(); i++) { 
	       array[i] = Integer.parseInt(number.substring(i, i + 1)); 
	     } 
	 return array; 
	   } 
	 /**
	  * 得到中文金额的整数部分。
	  */ 
	 private static String getChineseInteger(int[] integers, boolean isMust5) { 
	     StringBuffer chineseInteger = new StringBuffer(""); 
	 int length = integers.length; 
	 for (int i = 0; i < length; i++) { 
	 // 0出现在关键位置：1234(万)5678(亿)9012(万)3456(元)
	 // 特殊情况：10(拾元、壹拾元、壹拾万元、拾万元)
	       String key = ""; 
	 if (integers[i] == 0) { 
	 if ((length-i) == 13)// 万(亿)(必填)
	           key = IUNIT[4]; 
	 else if ((length-i) == 9)// 亿(必填)
	           key = IUNIT[8]; 
	 else if ((length-i) == 5 && isMust5)// 万(不必填)
	           key = IUNIT[4]; 
	 else if ((length-i) == 1)// 元(必填)
	           key = IUNIT[0]; 
	 // 0遇非0时补零，不包含最后一位
	 if ((length-i) > 1 && integers[i + 1] != 0) 
	           key += NUMBERS[0]; 
	       } 
	       chineseInteger.append(integers[i] == 0 ? key 
	           : (NUMBERS[integers[i]] + IUNIT[length - i - 1])); 
	     } 
	 return chineseInteger.toString(); 
	   } 
	 /**
	  * 得到中文金额的小数部分。
	  */ 
	 private static String getChineseDecimal(int[] decimals) { 
	     StringBuffer chineseDecimal = new StringBuffer(""); 
	 for (int i = 0; i < decimals.length; i++) { 
	 // 舍去2位小数之后的
	 if (i == 2) 
	 break; 
	       chineseDecimal.append(decimals[i] == 0 ? "" 
	           : (NUMBERS[decimals[i]] + DUNIT[i])); 
	     } 
	 return chineseDecimal.toString(); 
	   } 
	 /**
	  * 判断第5位数字的单位"万"是否应加。
	  */ 
	private static boolean isMust5(String integerStr) { 
		int length = integerStr.length(); 
		if (length > 4) { 
			String subInteger = ""; 
			if (length > 8) { 
				// 取得从低位数，第5到第8位的字串
				subInteger = integerStr.substring(length-8, length-4); 
			} else { 
				subInteger = integerStr.substring(0, length-4); 
			} 
			return Integer.parseInt(subInteger) > 0; 
		} else { 
			return false; 
		} 
	}
	
	/**
	 * 将包含有14位数字的字符串或纯14位数字字符串转Date
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date parseStringToDate(String date) throws ParseException{ 
        Date result=null;  
        String parse=date; 
        if(!parse.matches("^\\d+$")){
	        parse=parse.replaceFirst("^[0-9]{4}([^0-9]?)", "yyyy$1");  
	        parse=parse.replaceFirst("^[0-9]{2}([^0-9]?)", "yy$1");  
	        parse=parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1MM$2");  
	        parse=parse.replaceFirst("([^0-9]?)[0-9]{1,2}( ?)", "$1dd$2");  
	        parse=parse.replaceFirst("( )[0-9]{1,2}([^0-9]?)", "$1HH$2");  
	        parse=parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1mm$2");  
	        parse=parse.replaceFirst("([^0-9]?)[0-9]{1,2}([^0-9]?)", "$1ss$2"); 
        }
        DateFormat format=new SimpleDateFormat(parse);  
        result=format.parse(date);  
        return result;  
    }
}
