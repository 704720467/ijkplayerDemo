package cn.reee.reeeplayer.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;

public class ScreenUtil {

    private static ScreenUtil utils;

    //这里是设计稿参考宽高
    private static final float STANDARD_WIDTH = 750;
    private static final float STANDARD_HEIGHT = 1334;

    //这里是屏幕显示宽高 单位像素
    private int mDisplayWidth;
    private int mDisplayHeight;

    private ScreenUtil(Context context) {
        //获取屏幕的宽高
        if (mDisplayWidth == 0 || mDisplayHeight == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(displayMetrics);
                if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                    //横屏
                    mDisplayWidth = displayMetrics.heightPixels;
                    mDisplayHeight = displayMetrics.widthPixels;
                } else {
                    mDisplayWidth = displayMetrics.widthPixels;
                    mDisplayHeight = displayMetrics.heightPixels - getStatusBarHeight(context);
                }
            }
        }

    }

    public int getStatusBarHeight(Context context) {
        int resID = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resID > 0) {
            return context.getResources().getDimensionPixelSize(resID);
        }
        return 0;
    }

    public static ScreenUtil getInstance(Context context) {
        if (utils == null) {
            utils = new ScreenUtil(context.getApplicationContext());
        }
        return utils;
    }

    //获取水平方向的缩放比例
    public float getHorizontalScale() {
        return mDisplayWidth / STANDARD_WIDTH;
    }

    //获取垂直方向的缩放比例
    public float getVerticalScale() {
        return mDisplayHeight / STANDARD_HEIGHT;
    }

    public int getmDisplayWidth() {
        return mDisplayWidth;
    }

    public int getmDisplayHeight() {
        return mDisplayHeight;
    }


    public static int dp2px(Context context, int dp) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        display.getMetrics(displaymetrics);

        return (int) (dp * displaymetrics.density + 0.5f);
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    public static int getStatusBarHeight(Activity activity) {

        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return activity.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            //LogHelper.e("Exception", "*****EXCEPTION*****\n", e);  
        }

        return 0;

    }

    public static int getViewLocationY(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        return location[1];
    }

    /**
     * 获取屏幕高单位是像素
     *
     * @param context
     * @return
     */
    public static int getScreenHeightSize(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    /**
     * 检测底部状态栏是否展示
     * 条件：activity 全屏状态
     * 测试通过机型： 小米手机
     *
     * @param activity
     * @param rootLayout
     * @return true 展示了状态栏 false 没有展示
     */
    public static boolean isNavigationAtBottom(Activity activity, View rootLayout) {
        if (rootLayout == null) return false;
        int rootLayoutHeight = rootLayout.getHeight();
        int screenCanUseHeight = getScreenHeightSize(activity);
        return rootLayoutHeight == screenCanUseHeight;
    }

    /**
     * 获取屏幕宽单位是像素
     *
     * @param context
     * @return
     */
    public static int getScreenWidthSize(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }


}
