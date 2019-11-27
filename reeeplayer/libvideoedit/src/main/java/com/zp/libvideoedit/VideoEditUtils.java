package com.zp.libvideoedit;

import android.content.Context;

/**
 * Create by zp on 2019-11-26
 */
public class VideoEditUtils {
    public static Context instance;

    public static void init(Context context) {
        instance = context;
    }
}
