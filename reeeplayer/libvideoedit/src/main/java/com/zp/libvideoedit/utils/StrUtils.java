package com.zp.libvideoedit.utils;

import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IT on 2018/2/2.
 */

public class StrUtils {

    /**
     * 判断字符串为网址, 提取如 www.p-pass.com/123456zx 中的 123456
     *
     * @param content
     * @return
     */
    public static String regexZxContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }

        String src = content.toLowerCase();
        String reg = "(http://|https://|www){1}[\\w\\.\\-/:]+";
        Pattern pt = Pattern.compile(reg);
        Matcher mt = pt.matcher(src);

        String zxBar = "";
        if (mt.find()) {
            zxBar = mt.group();
        }

        if (!TextUtils.isEmpty(zxBar) && zxBar.equals(src)
                && (zxBar.lastIndexOf("/") > 0)
                && zxBar.toLowerCase().endsWith("zx")) {
            try {
                String endStr = zxBar.substring(zxBar.lastIndexOf("/") + 1,
                        zxBar.length() - "zx".length());
                if (!TextUtils.isEmpty(endStr)
                        && endStr.equals(getBeginNumber(endStr))) {
                    return endStr;
                } else {
                    return "";
                }
            } catch (IndexOutOfBoundsException e) {
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        return "";
    }

    /**
     * 匹配数字
     */
    public static String findNum(String str) {
        str = str.trim();
        String str2 = "";
        if (str != null && !"".equals(str)) {
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) >= 48 && str.charAt(i) <= 57) {
                    str2 += str.charAt(i);
                }
            }

        }
        return str2;
    }

    /***
     * gbk 转 utf8
     *
     * @param str
     * @return
     */
    public static String gb2utf8(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }

        String newstr = "";
        try {
            newstr = new String(str.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            newstr = str;
        }

        return newstr;
    }

    /***
     * String 转 int 值
     *
     * @param str
     * @return
     */
    public static int strToInt(String str) {
        int i = 0;
        if (TextUtils.isEmpty(str) || str.equals("null")) {
            return 0;
        } else {
            try {
                i = Integer.parseInt(str);
            } catch (Exception e) {
                i = 0;
            }
        }

        return i;
    }

    /***
     * 去掉空指针或为null情况
     *
     * @param str
     * @return
     */
    public static String tripNull(String str) {
        if (TextUtils.isEmpty(str) || str.equals("null")) {
            return "";
        } else {
            return str;
        }
    }

    /***
     * 将URL中的中文转为UTF-8编码
     *
     * @param str
     * @return
     */
    public static String decodeUrl(String str) {
        return URLDecoder.decode(str);
    }

    /***
     * 将字符串中的中文转为URL编码
     *
     * @param str
     * @return
     */
    public static String encodeUrl(String str) {
        String tstr = "";
        try {
            tstr = URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            tstr = str;
        }

        return tstr;
    }

    /***
     * 将字符串转为md5
     *
     * @param str
     * @return
     */
    public static String string2md5(String str) {
        MessageDigest msgDigest = null;

        try {
            msgDigest = MessageDigest.getInstance("MD5");
            msgDigest.reset();
            msgDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return str;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        byte[] byteArray = msgDigest.digest();
        StringBuffer md5StrBuff = new StringBuffer();

        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
                md5StrBuff.append("0").append(
                        Integer.toHexString(0xFF & byteArray[i]));
            } else {
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
            }
        }

        return md5StrBuff.toString();
    }

    /***
     * 获取字符串内部第一段数字子串
     *
     * @param srcStr
     * @return
     */
    public static String getBeginNumber(String srcStr) {
        String childStr = "";
        if (TextUtils.isEmpty(srcStr)) {
            return childStr;
        }

        String reg = "\\d*";
        Pattern pt = Pattern.compile(reg);
        Matcher mt = pt.matcher(srcStr);
        if (mt.find()) {
            childStr = mt.group();
        }

        if (!(TextUtils.isEmpty(childStr))) {
            if (!(srcStr.startsWith(childStr))) {
                childStr = "";
            }
        }

        return childStr;
    }


    public static String subString(String str, int len) {
        if (str.length() < len)
            return str;
        int size = str.length() / len;
        boolean flag = (str.length() % len == 0);
        StringBuilder mstr = new StringBuilder();
        for (int i = 0; i < size; i++) {
            mstr.append(str.substring(i * len, (i + 1) * len));
            mstr.append("\n");
        }
        if (!flag) {
            mstr.append(str.substring(size * len));
        } else {
            mstr.deleteCharAt(mstr.length() - 1);
        }
        return mstr.toString();
    }

    /**
     * 判断含有非数字
     *
     * @param str
     * @return
     */
    public static boolean isAllNum(String str) {
        Pattern pattern = Pattern.compile("^[0-9]*$");
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static String getDay4Date(String begin, String end) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long date = sdf.parse(end).getTime() - sdf.parse(begin).getTime();
        return date / (1000 * 60 * 60 * 24) + "";
    }

    public static boolean isNullStr(Object obj) {
        if (obj == null)
            return true;
        String str = obj.toString().trim();
        if ((str == null) || ("".equals(str)) || ("null".equals(str)) || ("{}".equals(str))) {
            return true;
        }
        return false;
    }

    public static boolean isNullStr(String str) {
        if ((str == null) || ("".equals(str))) {
            return true;
        }
        return false;
    }

    /**
     * 判断邮箱是否合法
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        if (null == email || "".equals(email)) return false;
        //Pattern p = Pattern.compile("\\w+@(\\w+.)+[a-z]{2,3}"); //简单匹配
        Pattern p = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");//复杂匹配
        Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * 判断是否是数字
     */
    public static boolean isNumber(String s) {
        if (s != null && !"".equals(s.trim()))
            return s.matches("^[0-9]*$");
        else
            return false;
    }

    /**
     * 产生一个随机的字符串
     */
    public static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(62);
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    /**
     * 实现文本复制功能
     * add by wangqianzhou
     *
     * @param content
     */
    public static void copy(String content, Context context) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(content.trim());
    }

    /**
     *
     */

    public static long getNumberForString(String str) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return Long.parseLong(m.replaceAll("").trim());
    }

    public static String getStringForRes(Context context, int resId) {
        return context.getResources().getString(resId);
    }


    public static int compareVersion(String version1, String version2) {
        //0代表相等，1代表version1大于version2，-1代表version1小于version2
        if (version1.equals(version2)) {
            return 0;
        }

        String[] version1Array = version1.split("\\.");
        String[] version2Array = version2.split("\\.");

        int index = 0;
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;

        while (index < minLen && (diff = Integer.parseInt(version1Array[index]) - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }

        if (diff == 0) {
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }

            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();

    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_0L, "w");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    /**
     * 数字格式化 1000-》1k
     */
    public static String format(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    public static String format(String value) {
        return value;
    }


    public static boolean isAscii(String str) {
        String regular = "\\A\\p{ASCII}*\\z";

        return str.matches(regular);
    }

    public static boolean isNickName(String name) {
        String[] chaname = name.split("");
        int nameSize = 0;
        for (int i = 0; i < chaname.length; i++) {
            if (chaname[i].trim().length() == 0) {
                continue;
            }
            if (isAscii(chaname[i])) {
                nameSize += 1;
            } else {
                nameSize += 2;
            }
        }
        return nameSize >= 4 && nameSize <= 16;
    }

    public static boolean isSignature(String sign) {
        String[] chaname = sign.split("");
        int nameSize = 0;
        for (int i = 0; i < chaname.length; i++) {
            if (chaname[i].length() == 0) {
                continue;
            }
            if (isAscii(chaname[i])) {
                nameSize += 1;
            } else {
                nameSize += 1;
            }
        }
        return nameSize <= 30;
    }

    public static List<Double> stringToDouble(String str) {
        List<Double> list = new ArrayList<>();
        String[] strings = stringFormat(str);
        for (int i = 0; i < strings.length; i++) {
            list.add(Double.valueOf(strings[i]));
        }

        return list;
    }

    public static List<Long> stringToLongs(String str) {
        List<Long> list = new ArrayList<>();
        String[] strings = stringFormat(str);
        for (int i = 0; i < strings.length; i++) {
            list.add(Long.valueOf(strings[i]));
        }

        return list;
    }

    private static String[] stringFormat(String str) {
        String str2 = str.replaceAll("\\{", "");
        String str3 = str2.replaceAll("\\}", "");
        String str4 = str3.replaceAll("\\[", "");
        String str5 = str4.replaceAll("\\]", "");
        String str6 = str5.replaceAll(" ", "");
        String[] strings = str6.split(",");
        return strings;
    }


    public static String getQiniuBucket(boolean isImage) {
        if (TimeZone.getDefault().getID().contains("Asia")) {
            if (isImage) {
                return "dong-vni-image";
            } else {
                return "dong-vni-video";
            }
        } else {
            if (isImage) {
                return "us-vni-image";
            } else {
                return "us-vni-video";
            }
        }
    }

    /**
     * 检测字符串中含有几个emoji表情
     *
     * @param str
     * @return
     */
    public static int noContainsEmoji(String str) {//真为不含有表情
        int getSelectNum = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (isEmojiCharacter(str.charAt(i))) {
                getSelectNum++;
            }
        }
        return getSelectNum;
    }

    private static boolean isEmojiCharacter(char codePoint) {
        return !((codePoint == 0x0) ||
                (codePoint == 0x9) ||
                (codePoint == 0xA) ||
                (codePoint == 0xD) ||
                ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
                ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)));
    }

    public static String getWeiboShareString(String canCutString, String shareString) {
        String finalString = canCutString + shareString;
        String temp;
        try {
            if (finalString.getBytes("gbk").length > 140) {
//                temp = canCutString.substring(0,140 - shareString.length());
                temp = substring(canCutString, 140 - shareString.getBytes("gbk").length, "gbk");
                Log.i("debbug", "size====" + (temp + shareString).getBytes("gbk").length);
                return temp + shareString;
            } else {
                return finalString;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return finalString;
        }
    }

    /**
     * @param text   目标字符串
     * @param length 截取长度
     * @param encode 采用的编码方式
     * @return
     * @throws UnsupportedEncodingException
     */

    public static String substring(String text, int length, String encode)
            throws UnsupportedEncodingException {
        if (length < 0) {
            return "";
        }
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        for (char c : text.toCharArray()) {
            currentLength += String.valueOf(c).getBytes(encode).length;
            if (currentLength <= length) {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }

}
