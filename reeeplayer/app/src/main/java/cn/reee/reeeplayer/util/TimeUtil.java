package cn.reee.reeeplayer.util;

import android.text.TextUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by zp on 2016/4/22.
 */
public class TimeUtil {
    private final static ThreadLocal<SimpleDateFormat> dateFormater = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    public static String getDate(Date date) {
        if (isSameDay(date, System.currentTimeMillis())) {
            return "今天";
        }
        SimpleDateFormat format = new SimpleDateFormat("M月d日");
        return format.format(date);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        return date1.equals(date2);
    }

    public static boolean isSameDay(Date date1, long date2) {
        return date1.getTime() == date2;
    }

    public static String getTime(Date createTime) {
        SimpleDateFormat format = new SimpleDateFormat("MM'月'dd'日' HH:mm");
        return format.format(createTime);
    }

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    public static String toIso8601(Date createTime) {
        return buildIso8601Format().format(createTime);
    }

    public static String getStartDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("M月d日");
        return format.format(date);
    }

    public static String dateToString(Date time) {
        // 获取时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String ctime = formatter.format(time);
        return ctime;
    }

    public static String getStartDateWithYear(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年M月d日");
        return format.format(date);
    }

    public static String getYearToMinute(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年M月d日 HH:mm");
        return format.format(date);
    }

    /**
     * 中间连接处是点
     *
     * @param date
     * @return
     */
    public static String getStartDateWithYearByDot(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        return format.format(date);
    }

    public static String getStartDateWithYearStyle2(Date date) {
        String timeString = "";
        try {
            timeString = friendlyTime(date.getTime() / 1000);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return timeString;

    }

    public static String getTime(Date start, Date end) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(start) + "-" + format.format(end);
    }

    public static String getTime(long start, long end) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(start) + "-" + format.format(end);
    }

    public static String getTwoTypeDate(Date date) {
        if (isSameDay(date, System.currentTimeMillis())) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            return format.format(date);
        }
        SimpleDateFormat format = new SimpleDateFormat("M月d日");
        return format.format(date);
    }

    public static boolean sameDay(long timestamp, Calendar cal) {
        // 判断是否是同一天
        String curDate = dateFormater.get().format(cal.getTime());
        String paramDate = dateFormater.get().format(timestamp * 1000);
        boolean equals = curDate.equals(paramDate);
        return equals;
    }

    /**
     * 以友好的方式显示时间
     *
     * @param timestamp 单位秒
     * @return
     */
    public static String friendlyTime(long timestamp) {
        Date date = new Date(timestamp * 1000);

        String ftime = "";
        Calendar cal = Calendar.getInstance();

        boolean equals = sameDay(timestamp, cal);
        if (equals) {
            int hour = (int) ((cal.getTimeInMillis() - date.getTime()) / 3600000);
            if (hour == 0) {//小于一小时
                int minute = (int) ((cal.getTimeInMillis() - date.getTime()) / 60000);
                if (minute == 0) {//小于一分钟
                    ftime = "刚刚";
                } else {//大于一分钟
                    ftime = Math.max((cal.getTimeInMillis() - date.getTime()) / 60000, 1) + "分钟前";
                }
            } else {//大于一小时
                ftime = hour + "小时前";
            }
            return ftime;
        }

        long lt = date.getTime() / 86400000;
        long ct = cal.getTimeInMillis() / 86400000;
        int days = (int) (ct - lt);
        if (days == 0) {
            int hour = (int) ((cal.getTimeInMillis() - date.getTime()) / 3600000);
            if (hour == 0) {//小于一小时
                int minute = (int) ((cal.getTimeInMillis() - date.getTime()) / 60000);
                if (minute == 0) {//小于一分钟
                    ftime = "刚刚";
                } else {//大于一分钟
                    ftime = Math.max((cal.getTimeInMillis() - date.getTime()) / 60000, 1) + "分钟前";
                }
            } else {//大于一小时
                ftime = hour + "小时前";
            }
        } else if (days == 1) {
            ftime = "昨天";
        } else if (days == 2) {
            ftime = "前天";
        } else {
            ftime = dateFormater.get().format(timestamp * 1000);
        }
        return ftime;
    }

    public static Date stringToDate(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日");
        Date date = null;
        try {
            date = sdf.parse(s);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return date;
    }

    public static String millisToString(float millis) {
        return millisToString(millis, false);
    }

    public static String millisToText(long millis) {
        return millisToString(millis, true);
    }

    /**
     * 从时间(毫秒)中提取出时间(时:分) 时间格式: 时:分
     *
     * @param millisecond
     * @return
     */
    public static String getTimeFromMillisecond(Long millisecond) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        Date date = new Date(millisecond);
        String timeStr = simpleDateFormat.format(date);
        return timeStr;
    }

    // millins转换为mm:ss
    static String millisToString(float millis, boolean text) {
        boolean negative = millis < 0;
        millis = Math.abs(millis);
        if (millis <= 1000 && millis >= 500) {
            millis = 1000;
        }

        millis /= 1000;
        int sec = Math.round(millis % 60);
        if (sec == 60) {
            sec = 0;
            millis++;
        }
        millis /= 60;
        //方案二：以下计算分钟，没有小时
        int min = (int) millis;
        millis = 0;
        int hours = Math.round((int) millis);
        //方案一：以下计算分钟加小时
        //        int min = Math.round((int) millis % 60);
        //        if (min == 60) {
        //            min = 0;
        //            millis++;
        //        }
        //        millis /= 60;
        //        int hours = Math.round((int) millis);
        //        millis = Math.round((int) millis);

        String time;
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        format.applyPattern("00");
        if (text) {
            if (millis > 0)
                time = (negative ? "-" : "") + hours + "h" + format.format(min) + "min";
            else if (min > 0)
                time = (negative ? "-" : "") + min + "min";
            else
                time = (negative ? "-" : "") + sec + "s";
        } else {
            if (millis > 0)// 有时
                time = (negative ? "-" : "") + format.format(hours) + ":" + format.format(min) + ":" + format.format(sec);
            else if (min > 0)// 无时有分
                time = (negative ? "-" : "") + format.format(min) + ":" + format.format(sec);
            else
                // 无时无分有秒
                time = (negative ? "-" : "") + format.format(min) + ":" + format.format(sec);
        }
        return time;
    }

    // date类型转换为String类型
    // formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
    // data Date类型的时间
    public static String dateToString(Date data, String formatType) {
        if (data == null)
            return null;
        return new SimpleDateFormat(formatType).format(data);
    }

    /**
     * long类型转换为String类型
     *
     * @param currentTime
     * @param formatType  要转换的string类型的时间格式
     * @return String类型
     * @throws ParseException
     */
    public static String longToString(long currentTime, String formatType) throws ParseException {
        Date date = longToDate(currentTime, formatType); // long类型转成Date类型
        String strTime = dateToString(date, formatType); // date类型转成String
        return strTime;
    }

    /**
     * long类型转换为String类型
     *
     * @param currentTime
     * @param formatType  要转换的string类型的时间格式
     * @return String类型
     * @throws ParseException
     */
    public static String longToString2(long currentTime, String formatType) {
        try {
            Date date = longToDate(currentTime, formatType); // long类型转成Date类型
            String strTime = dateToString(date, formatType); // date类型转成String
            return strTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * string类型转换为date类型，strTime的时间格式必须要与formatType的时间格式相同
     *
     * @param strTime
     * @param formatType 要转换的格式yyyy-MM-dd HH:mm:ss或者yyyy年MM月dd日
     * @return
     * @throws ParseException
     */
    public static Date stringToDate(String strTime, String formatType) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(formatType);
        Date date = null;
        date = formatter.parse(strTime);
        return date;
    }

    /**
     * long转换为Date类型
     *
     * @param currentTime
     * @param formatType  要转换的时间格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
     * @return
     * @throws ParseException
     */
    public static Date longToDate(long currentTime, String formatType) throws ParseException {
        Date dateOld = new Date(currentTime); // 根据long类型的毫秒数生命一个date类型的时间
        String sDateTime = dateToString(dateOld, formatType); // 把date类型的时间转换为string
        Date date = stringToDate(sDateTime, formatType); // 把String类型转换为Date类型
        return date;
    }

    /**
     * string类型转换为long类型 注意：strTime的时间格式和formatType的时间格式必须相同
     *
     * @param strTime
     * @param formatType
     * @return
     * @throws ParseException
     */
    public static long stringToLong(String strTime, String formatType) throws ParseException {
        Date date = stringToDate(strTime, formatType); // String类型转成date类型
        if (date == null) {
            return 0;
        } else {
            long currentTime = dateToLong(date); // date类型转成long类型
            return currentTime;
        }
    }

    public static long stringToLong2(String strTime, String formatType) {
        long time = 0;
        try {
            time = stringToLong(strTime, formatType);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return time;
        }
    }

    /**
     * date类型转换为long类型
     *
     * @param date 要转换的date类型的时间
     * @return
     */
    public static long dateToLong(Date date) {
        return date.getTime();
    }

    public static String getStartWithMinute(long ms) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String hms = formatter.format(ms);

        return hms;
    }


    //时间戳转时间
    public static String timeStamp2Date(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(time));
    }

    /**
     * 转换为指定格式
     *
     * @param time
     * @param dataFormat
     * @return
     */
    public static String timeStamp2Date(long time, String dataFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dataFormat);
        return sdf.format(new Date(time));
    }


    //时间戳转时间
    public static String timeStamp2Date2(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(time));
    }

    //时间格式字符串转时间戳
    public static long date2TimeStamp(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return Long.parseLong(String.valueOf(sdf.parse(date).getTime()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    //时间戳转时间
    public static String timeStamp2Date3(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd  HH:mm");
        return sdf.format(new Date(time));
    }


    //年月日 时分秒 毫秒 转成 时分秒
    public static String changeTimeType(String time) {
        long timeStamp = date2TimeStamp(time);
        return timeStamp2Date(timeStamp);
    }


    //年月日 时分秒 毫秒 转成 月日 时分
    public static String changeTimeType2(String time) {
        long timeStamp = date2TimeStamp(time);
        return timeStamp2Date3(timeStamp);
    }

    /**
     * 时间转换
     *
     * @param date          时间
     * @param nowDateFormat 当前时间格式
     * @param tagDateFormat 目标时间格式
     * @return 返回指定格式 时间
     */
    public static String changeTimeType(String date, String nowDateFormat, String tagDateFormat) {
        String tagData = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(nowDateFormat);
            long timeTmp = Long.parseLong(String.valueOf(sdf.parse(date).getTime()));
            sdf = new SimpleDateFormat(tagDateFormat);
            tagData = sdf.format(new Date(timeTmp));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tagData;
    }

    /**
     * 根据日期获取当天是周几
     *
     * @param datetime 日期
     * @return 周几
     */
    public static String dateToWeek(String datetime, String dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        Date date;
        try {
            date = sdf.parse(datetime);
            cal.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return weekDays[w];
    }

    /**
     * 检测时间是否是今天  或者是 昨天
     *
     * @param time
     * @param dateFormat
     * @return
     */
    public static boolean dataIsTodayOYesterday(String time, String dateFormat) {
        boolean isTodayOYesterday = false;
        try {
            long timeDate = stringToLong2(time, dateFormat);
            long now = new Date().getTime();
            long day = (now + (8 * 60 * 1000)) / (24 * 60 * 60 * 1000) - (timeDate + (8 * 60 * 1000)) / (24 * 60 * 60 * 1000);
            isTodayOYesterday = day <= 2;
        } catch (Exception e) {
            Log.e("TimeUtil", "检测是否时间最近两天：" + e.getMessage());
            isTodayOYesterday = false;
        }
        return isTodayOYesterday;
    }

    /**
     * 把时间转换为：时分秒格式。
     *
     * @param second ：秒，传入单位为秒
     * @return
     */
    public static String getTimeString(long second) {
        long miao = second % 60;
        long fen = second / 60;
        long hour = 0;
        if (fen >= 60) {
            hour = fen / 60;
            fen = fen % 60;
        }
        String timeString = "";
        String miaoString = "";
        String fenString = "";
        String hourString = "";
        if (miao < 10) {
            miaoString = "0" + miao;
        } else {
            miaoString = miao + "";
        }
        if (fen < 10) {
            fenString = "0" + fen;
        } else {
            fenString = fen + "";
        }
        if (hour < 10) {
            hourString = "0" + hour;
        } else {
            hourString = hour + "";
        }
        if (hour != 0) {
            timeString = hourString + ":" + fenString + ":" + miaoString;
        } else {
            timeString = fenString + ":" + miaoString;
        }
        return timeString;
    }

}
