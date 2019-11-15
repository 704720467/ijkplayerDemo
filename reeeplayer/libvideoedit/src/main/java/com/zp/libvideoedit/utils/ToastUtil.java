package com.zp.libvideoedit.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * ToastUtil
 * Created by pc on 2016/5/27.
 */
public class ToastUtil {

    private static Toast mToast = null;

    public static void showToast(Context context, String text, int duration) {
        if (null == mToast) {
            mToast = Toast.makeText(context.getApplicationContext(), null, duration);
        } else {
            mToast.setDuration(duration);
        }
        mToast.setText(text);
        mToast.show();
    }

    public static void showToast(Context context, String text) {
        showToast(context, text, Toast.LENGTH_SHORT);
    }


}
