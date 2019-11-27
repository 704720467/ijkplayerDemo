package com.zp.libvideoedit.utils;

import android.graphics.Point;
import android.renderscript.Matrix4f;

import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * function 字符串工具类
 */
@SuppressWarnings("unused")
public class StringUtil {

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmptyOrNull(String str) {
        boolean result = false;
        if ("".equals(str) || null == str || "null".equals(str)) {
            result = true;
        }
        return result;
    }


    /**
     * 获取字符串的长度
     */
    public static int getStringLength(String str) {
        int result = 0;
        if (!isEmptyOrNull(str)) {
            result = str.length();
        }

        return result;
    }

    /**
     * 格式化字符串
     */
    public static String format(int strResId, Object... args) {
        return null;
    }

    /**
     * 格式化字符串
     */
    public static String format(String formatStr, Object... args) {
        return String.format(formatStr, args);
    }

    /**
     * 保留2位小数，默认四舍五入
     *
     * @param data
     * @return
     */
    public static String formatString(double data) {
        BigDecimal bd = new BigDecimal(data);
        return bd.setScale(2).toString();
    }

    public static String spilt(double d, int len) {
        return spilt(String.valueOf(d), len);
    }

    /**
     * 数字格式化
     *
     * @param s   要格式化的数字
     * @param len 保留的小数位 四舍五入
     * @return
     */
    public static String spilt(String s, int len) {
        if (s == null || s.equals("") || s.equals("null")) {
            s = "0";
        }
        NumberFormat formater = null;
        double num = Double.parseDouble(s);
        if (len == 0) {
            formater = new DecimalFormat("###,###");
        } else {
            StringBuffer buff = new StringBuffer();
            buff.append("###,##0.");
            for (int i = 0; i < len; i++) {
                buff.append("0");
            }
            formater = new DecimalFormat(buff.toString());
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);

        return formater.format(num);

    }

    /**
     * 数字去格式化
     *
     * @param s
     * @return
     */
    public static String delComma(String s) {
        String formatString = "";
        if (s != null && s.length() >= 1) {
            formatString = s.replaceAll(",", "");
        }

        return formatString;
    }

    /**
     * 返回脱敏手机号
     *
     * @param phone
     * @return
     */
    public static String getPhone(String phone) {
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }


    /**
     * 保留两位小数
     * 描 述：两位后舍
     * 作 者：qin
     *
     * @param d
     * @return
     */
    public static double decimalDown(double d) {
        return new BigDecimal(d).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
    }

    public static double decimalUp(double d, int len) {
        return new BigDecimal(d).setScale(len, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static double decimalDown(double d, int len) {
        return new BigDecimal(d).setScale(len, BigDecimal.ROUND_DOWN).doubleValue();
    }

    public static String numberFormat(double d) {
        return d % 1 == 0 ? String.valueOf((int) d) : String.valueOf(d);
    }

    public static String numberFormat(double d, int maxLen) {
        String n = numberFormat(d);
        if (n.contains(".")) {
            String m = n.split("[.]")[0];
            String s = n.split("[.]")[1];
            if (s.length() > maxLen) {
                s = s.substring(0, maxLen);
                n = m + "." + s;
            }
        }
        return n;
    }

    public static String numberFormat1(double d, int maxLen) {
        String n = numberFormat(d);
        if (n.contains(".")) {
            String m = n.split("[.]")[0];
            String s = n.split("[.]")[1];
            if (s.length() > maxLen) {
                s = s.substring(0, maxLen);
                n = m + "." + s;
            }
        } else {
            n = n + ".0";
        }
        return n;
    }

    public static String numberFormat2(double d, int maxLen) {
        String n = String.valueOf(d);
        if (n.contains(".")) {
            String m = n.split("[.]")[0];
            String s = n.split("[.]")[1];
            if (s.length() > maxLen) {
                s = s.substring(0, maxLen);
                n = m + "." + s;
            }
        }
        return n;
    }

    public static String numberFormat(double d, int maxLen, boolean half_up) {
        if (half_up) {
            d = new BigDecimal(d).setScale(maxLen, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return numberFormat(d);
    }

    //{{0, 100000}, {189864, 100000}}
    public static String stringFromCMTimeRange(CMTimeRange range) {
        return String.format("{{%d,%d},{%d,%d}}", range.getStartTime().getValue(), range.getStartTime().getTimeScale(), range.getDuration().getValue(), range.getDuration().getTimeScale());
    }

    //{{0, 0}, {315, 311}}
    public static String stringFromRect(GPURect rect) {
        return String.format("{{%f,%f},{%f,%f}}", rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    //{189864, 100000}
    public static String stringFromCMTime(CMTime time) {

        return String.format("{%d,%d}", time.getValue(), time.getTimeScale());
    }

    //{187.5, 333.5}
    public static String stringFromPoint(Point point) {
        return String.format("{%f,%f}", point.x, point.y);
    }

    //[1,2,3,4,5,6]
    public static String stringFromMatrix4f(Matrix4f matrix4f) {
        if (matrix4f == null) {
            Matrix4f mat = new Matrix4f();
            mat.loadIdentity();
            matrix4f = mat;
        }
        float[] mat = new float[6];
        mat[0] = matrix4f.get(0, 0);
        mat[1] = matrix4f.get(0, 1);
        mat[2] = matrix4f.get(1, 0);
        mat[3] = matrix4f.get(1, 1);
        mat[4] = matrix4f.get(3, 0);
        mat[5] = matrix4f.get(3, 2);
        return String.format("[%f,%f,%f,%f,%f,%f]", mat[0], mat[1], mat[2], mat[3], mat[4], mat[5]);
    }

    //{{0, 100000}, {189864, 100000}}
    public static String stringFromBound(double width, double height) {
        return String.format("{{0,0},{%f,%f}}", width, height);
    }

    public static String stringFromCenter(double left, double top) {
        return String.format("{%f,%f}", left, top);
    }

}
