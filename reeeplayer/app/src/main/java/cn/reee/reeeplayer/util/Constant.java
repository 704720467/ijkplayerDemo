package cn.reee.reeeplayer.util;

import android.os.Environment;

/**
 * Create by zp on 2019-12-10
 */
public class Constant {
    public static final String SD_ROOT = Environment.getExternalStorageDirectory().getPath();
    public static final String APP_EXTERNAL_ROOT_PATH = SD_ROOT + "/reeePlayer";
    public static final String TEMP_FILE_PATH = APP_EXTERNAL_ROOT_PATH + "/temp";
    public static final String TEMP_FILTER_PATH = TEMP_FILE_PATH + "/.filter";

}
