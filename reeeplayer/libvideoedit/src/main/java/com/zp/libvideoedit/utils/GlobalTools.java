package com.zp.libvideoedit.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 常用工具类
 *
 * @Description:
 */
public class GlobalTools {

    private static PopupWindow mPop;

    /**
     * dip转换成Px
     *
     * @param context
     * @param dipValue
     * @Description:
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static float dip2px_Float(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return dipValue * scale + 0.5f;
    }

    /**
     * px转换成dp
     *
     * @param context
     * @param pxValue
     * @Description:
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static float px2dip_Float(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return pxValue / scale + 0.5f;
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param context
     * @param spValue
     * @return
     */
    public static float sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return spValue * fontScale + 0.5f;
    }

    /**
     * 隐藏软键盘
     *
     * @param context
     * @Description:
     */
    public static boolean hideSoftInput(Activity context) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && context.getCurrentFocus() != null && context.getCurrentFocus().getWindowToken() != null) {
                return inputMethodManager.hideSoftInputFromWindow(context.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 切换输入法显示隐藏状态
     *
     * @param context
     * @Description:
     */
    public static void toggleSoftInput(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

    }

    /**
     * 显示输入法
     *
     * @param editText
     */
    public static void showSoftInput(EditText editText) {
        InputMethodManager inputManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(editText, 0);
    }

    /**
     * 隐藏输入法
     *
     * @param context
     * @param binder  输入法所在控件的token
     * @Description:
     */
    public static void hideSoftInput(Context context, IBinder binder) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, 0);
    }

    /**
     * 存储屏幕高宽的数据
     */
    private static int[] screenSize = null;

    /**
     * 获取屏幕高宽
     *
     * @param context
     * @return 屏幕宽高的数据[0]宽， [1]高
     * @Description:
     */
    public static int[] getScreenSize(Context context) {
        if (screenSize == null) {
            WindowManager manager =
                    (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            screenSize = new int[2];
            screenSize[0] = display.getWidth();
            screenSize[1] = display.getHeight();
        }
        return screenSize;
    }

    /**
     * 清除List内容，并置为null
     *
     * @param list
     * @Description:
     */
    public static void clearList(Collection<?> list) {
        if (list != null) {
            list.clear();
            list = null;
        }
    }

    /**
     * 关闭cursor
     *
     * @param cursor
     * @Description:
     */
    public static void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * 取两个集合的并集
     *
     * @param c1
     * @param c2
     * @Description:
     */
    public static Collection<String> mixedList(Collection<String> c1, Collection<String> c2) {
        Collection<String> tmpBig = new ArrayList<String>();
        Collection<String> tmpSmall = new ArrayList<String>();
        if (c1.size() > c2.size()) {
            tmpBig.addAll(c1);
            tmpSmall.addAll(c2);
        } else {
            tmpBig.addAll(c2);
            tmpSmall.addAll(c1);
        }
        tmpBig.retainAll(tmpSmall);
        return tmpBig;
    }

    static String versionName;

    /**
     * 获取应用版本号
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        if (TextUtils.isEmpty(versionName)) {
            try {
                PackageInfo info =
                        context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                // 当前应用的版本名称
                versionName = info.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return versionName;
    }

    /**
     * 获取应用版本编号
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            PackageInfo info =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            // 当前应用的版本名称
            versionCode = info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private static String imei;

    /**
     * 获取手机imei
     *
     * @param context
     * @return
     */
    public static String getPhoneImei(Context context) {
        try {
            if (TextUtils.isEmpty(imei)) {
                TelephonyManager manager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                imei = manager.getDeviceId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }

    private static String macAddress;
    private static String androidId;

    /**
     * 获取mac地址
     *
     * @return
     */
    public static String getMacAddress() {
        if (TextUtils.isEmpty(macAddress)) {
            String macSerial = "";
            try {
                Process pp = Runtime.getRuntime().exec(
                        "cat /sys/class/net/wlan0/address");
                InputStreamReader ir = new InputStreamReader(pp.getInputStream());
                LineNumberReader input = new LineNumberReader(ir);

                String line;
                while ((line = input.readLine()) != null) {
                    macSerial += line.trim();
                }

                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            macAddress = macSerial;
            return macSerial;
        }
        return macAddress;
    }

    public static String getAndroidId(Context context) {
        if (TextUtils.isEmpty(androidId)) {
            androidId = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return androidId;
    }


    /**
     * 描 述：拨打电话
     */
    public static void callPhone(Context context, String phoneNum) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNum));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 调用系统app，打开pdf
     *
     * @param context
     * @param uri
     */
    public static void openPdf(Context context, Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/pdf");
        openView(context, intent);
    }

    /**
     * 调用系统app，打开文件
     *
     * @param context
     * @param intent
     */
    public static void openView(Context context, Intent intent) {
        List<ResolveInfo> resolveInfo = context.getPackageManager().
                queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() == 0) {
            ToastUtil.showToast(context, "您的手机未安装可以打开此文件的程序");
            return;
        }
        context.startActivity(intent);
    }

}
