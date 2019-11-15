package com.zp.libvideoedit.utils;


import com.zp.libvideoedit.BuildConfig;

/**
 * function : 日志输出.
 * <p/>
 */
@SuppressWarnings("unused")
public final class LogUtil {

    public static void w(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w(tag, content);
        }
    }

    public static void w(final String tag, Object... objs) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w(tag, getInfo(objs));
        }
    }

    public static void i(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, content);
        }
    }

    public static void i(final String tag, Object... objs) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, getInfo(objs));
        }
    }

    public static void d(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, content);
        }
    }

    public static void d(final String tag, Object... objs) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, getInfo(objs));
        }
    }

    public static void e(String tag, String content) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, content);
        }
    }

    public static void e(String tag, String content, Throwable e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, content, e);
        }
    }

    public static void e(final String tag, Object... objs) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, getInfo(objs));
        }
    }

    private static String getInfo(Object... objs) {
        StringBuilder sb = new StringBuilder();
        if (null == objs) {
            sb.append("no mesage.");
        } else {
            for (Object object : objs) {
                sb.append(object);
            }
            sb.append("-");
        }
        return sb.toString();
    }

    public static void sysOut(Object msg) {
        if (BuildConfig.DEBUG) {
            System.out.println(msg);
        }
    }

    public static void sysErr(Object msg) {
        if (BuildConfig.DEBUG) {
            System.err.println(msg);
        }
    }
}
