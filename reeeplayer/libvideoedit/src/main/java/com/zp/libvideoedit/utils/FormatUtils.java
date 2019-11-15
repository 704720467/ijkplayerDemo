package com.zp.libvideoedit.utils;


import android.os.Build;

/**
 * Created by guoxian on 2018/5/4.
 */

public class FormatUtils {
    public static final String EMPTY = "";
    private static final int PAD_LIMIT = 8192;


    public static String rightPad(final String str, final int length) {
        if (str == null) return null;
        if (str.length() >= length) return str;
        return rightPad(str, length, ' ');
    }

    public static String rightPad(final String str, final int length, final char padChar) {
        if (str == null) {
            return null;
        }
        if (str.length() >= length) return str;

        int pads = length - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            pads = PAD_LIMIT;
        }
        String padString = repeat(padChar, pads);
        return str.concat(padString);
    }


    public static String leftPad(final String str, final int length) {
        if (str == null) return null;
        if (str.length() >= length) return str;
        return leftPad(str, length, ' ');
    }

    public static String leftPad(final String str, final int length, final char padChar) {
        if (str == null) {
            return null;
        }
        if (str.length() >= length) return str;

        int pads = length - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            pads = PAD_LIMIT;
        }
        String padString = repeat(padChar, pads);
        return padString.concat(str);
    }

    public static String pad(final String str, final int length) {
        if (str == null) return null;
        if (str.length() >= length) return str;
        return pad(str, length, ' ');
    }

    /**
     * 两边补充
     *
     * @param str
     * @param length
     * @param padChar
     * @return
     */
    public static String pad(final String str, final int length, final char padChar) {
        if (str == null) {
            return null;
        }
        if (str.length() >= length) return str;

        int pads = length - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            pads = PAD_LIMIT;
        }
        int rightPad = pads / 2;
        int leftPad = pads - rightPad;
        return repeat(padChar, leftPad).concat(str).concat(repeat(padChar, rightPad));
    }

    /**
     * 中间填充
     *
     * @param str
     * @param str2
     * @param length
     * @return
     */
    public static String fill(final String str, final String str2, final int length) {
        return fill(str, str2, length, ' ');
    }

    public static String fill(final String str, final String str2, final int length, final char padChar) {
        if (str == null && str2 == null) return null;
        int strLength = (str != null ? str.length() : 0) + (str2 != null ? str2.length() : 0);
        if (strLength >= length) return str + str2;

        int pads = length - strLength;
        if (pads <= 0) {
            return str + str2; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            pads = PAD_LIMIT;
        }
        return str.concat(repeat(padChar, pads).concat(str2));
    }

    public static String repeat(final char ch, final int repeat) {
        if (repeat <= 0) {
            return EMPTY;
        }
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    public static String generateStackTraceTag(String customTagPrefix) {
        StackTraceElement caller = new Throwable().getStackTrace()[2];
        String tag = "%s.%s(L:%d)";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(), caller.getLineNumber());
        tag = customTagPrefix + "_" + tag;
        return tag;
    }
    public static String caller() {
        StackTraceElement caller = new Throwable().getStackTrace()[1];
        String tag = "|%s_%d|%s.%s(L:%d)___";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, Thread.currentThread().getName(), Thread.currentThread().getId(),callerClazzName, caller.getMethodName(), caller.getLineNumber());
        return tag;
    }

    public static String generateCallStack() {
        StackTraceElement[] traces = new Throwable().getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("|"+ Thread.currentThread().getName()+"|");
        for (int i = 0; i < traces.length; i++) {
            if (i < 2) continue;
            StackTraceElement caller = traces[i];
            String callerClazzName = caller.getClassName();
            callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
            sb.append(callerClazzName + "." + caller.getMethodName() + "<-");
        }

        return sb.toString();
    }

    public static String deviceInfo(){
        return Build.BRAND+"|"+ Build.MODEL+"|"+ Build.VERSION.RELEASE+"|"+ Build.VERSION.SDK_INT+"|";
    }

    public static void main(String[] a) {
        System.out.println("a".substring(2));
        System.out.println(pad("s", 9));
        System.out.println(pad("s", 4));
        System.out.println(pad("s", 3));
        System.out.println(pad("s", 2));
        System.out.println(pad("s", 1));
        System.out.println(pad("s", 0));


        System.out.println(rightPad("s", 9));
        System.out.println(rightPad("s", 4));
        System.out.println(rightPad("s", 3));
        System.out.println(rightPad("s", 2));
        System.out.println(rightPad("s", 1));
        System.out.println(rightPad("s", 0));

        System.out.println(leftPad("s", 9));
        System.out.println(leftPad("s", 4));
        System.out.println(leftPad("s", 3));
        System.out.println(leftPad("s", 2));
        System.out.println(leftPad("s", 1));
        System.out.println(leftPad("s", 0));


        System.out.println(pad("s", 9, '-'));
        System.out.println(pad("s", 4, '-'));
        System.out.println(pad("s", 3, '-'));
        System.out.println(pad("s", 2, '-'));
        System.out.println(pad("s", 1, '-'));
        System.out.println(pad("s", 0, '-'));


        System.out.println(rightPad("s", 9, '-'));
        System.out.println(rightPad("s", 4, '-'));
        System.out.println(rightPad("s", 3, '-'));
        System.out.println(rightPad("s", 2, '-'));
        System.out.println(rightPad("s", 1, '-'));
        System.out.println(rightPad("s", 0, '-'));

        System.out.println(leftPad("s", 9, '-'));
        System.out.println(leftPad("s", 4, '-'));
        System.out.println(leftPad("s", 3, '-'));
        System.out.println(leftPad("s", 2, '-'));
        System.out.println(leftPad("s", 1, '-'));
        System.out.println(leftPad("s", 0, '-'));
    }

}
