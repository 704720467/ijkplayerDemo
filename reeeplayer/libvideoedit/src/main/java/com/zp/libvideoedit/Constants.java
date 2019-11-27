package com.zp.libvideoedit;

import android.media.AudioFormat;
import android.os.Environment;


public class Constants {
    public static final String TAG = "AE_COMMON";
    /**
     * 是否打印log
     */
    public static final boolean VERBOSE = true;//BuildConfig.LOG_DEBUG;

    public static final boolean VERBOSE_V = VERBOSE && true;
    public static final boolean VERBOSE_LOOP_V = VERBOSE && true;

    public static final boolean VERBOSE_A = VERBOSE && false;
    public static final boolean VERBOSE_LOOP_A = VERBOSE && false;
    public static final boolean VERBOSE_A_MIX = VERBOSE && false;

    //编码encoder
    public static final boolean VERBOSE_EN = VERBOSE && false;
    //frameExctrator
    public static final boolean VERBOSE_FE = VERBOSE && false;
    //transcoder
    public static boolean VERBOSE_TR = VERBOSE && true;

    public static final boolean VERBOSE_GL = VERBOSE && false;
    public static final boolean VERBOSE_UI = VERBOSE && false;
    public static final boolean VERBOSE_M = VERBOSE && false;
    public static final boolean VERBOSE_EDIT = VERBOSE && false;
    public static final boolean VERBOSE_SEEK = VERBOSE && false;
    public static final boolean GL_DEBUG = false;
    /**
     * script
     */
    public static final boolean VERBOSE_SCRIPT = VERBOSE && false;

    public static final String TAG_A = "AE_AUDIO";
    public static final String TAG_V = "AE_VIDEO";
    public static final String TAG_A_MIX = "AE_A_MIX";
    public static final String TAG_EN = "AE_ENCODER";
    public static final String TAG_ENS = "AE_ENCODER_STEP";
    public static final String TAG_FE = "AE_FRAMESEXTRACTOR";
    public static final String TAG_AE = "AE_VIDEOEFFECT";
    public static final String TAG_GL = "AE_VIDEOOPENGL";
    public static final String TAG_M = "AE_MODEL";
    public static final String TAG_EDIT = "AE_VIDEOEDIT";


    public static final String TAG_Re = "AE_CameraReCoder";
    public static final String TAG_TR = "AE_TRANSCODER";
    public static final String TAG_AUDIO_INFO = "AE_AUDIO_INFO";
    public static final String TAG_SEEK = "AE_SEEK";
    public static final String TAG_DRAF = "TAG_DRAF";
    public static final String TAG_SCRIPT = "TAG_DRAF";

    public static final String ASSERT_FILE_PREFIX = "assert://";
    public static final float VIDEOMAXDURATION = 60 * 5;
    public static final String TAILER_H_MP4 = "assert://tailer.mp4";
    public static final String TAILER_V_MP4 = "assert://tailer_v.mp4";

    public static final String EXPORT_VIDOE_ORIGINAL_AUDIO = "_original_audio.mp3";
    public static final String TAG_EXTRA_AUDIO = "AE_ExtratAudio";


    /**
     * 最小时间间隔.两端时间相差MAX_TIME_DIFF，认为相同
     */
    public static final double MAX_TIME_DIFF_SEC = 0.1d;

    /**
     * codec queue 超时时间
     */
    public static final long TIMEOUT_USEC = 10000;// 百分之一秒。单位是US


    public static final String VERSION = "0.9.6"; //

    /**
     * 测试，关闭音频
     */
    public static final boolean PLAY_AUDIO = true;
    public static final boolean PLAY_VIDEO = true;


    /**
     * 一百万。微秒的单位
     */
    public static final long US_MUTIPLE = 1000000;// 百万分之一秒的单位 微秒，
    public static final long MS_MUTIPLE = 1000;// 千分之一秒的单位 毫秒，
    public static final long NS_MUTIPLE = 1000000000; //纳秒

    public static final int ACCECPT_VIDEO_MAX_GOP = 300;
    public static int DEFAULT_AUDIO_CHANNEL_COUNT = 2;
    public static int DEFAULT_AUDIO_SAMPLE_RATE = 44100;
    public static int DEFAULT_AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    public static int DEFAULT_FPS = 25;
    public static int DEFAULT_AUDIO_BUFFER_SIZE = 4096;
    /**
     * PBB*fps*width*height=fps
     */
    public static float DEFAULT_PBB = 0.25f;

    public static float VIDEO_PRE_START_TIME = 0.3f;//提前多少秒唤醒线程

    public static final String SD_ROOT = Environment.getExternalStorageDirectory().getPath();
    public static final String APP_EXTERNAL_ROOT_PATH = SD_ROOT + "/vni";
    public static final String TEMP_FILE_PATH = APP_EXTERNAL_ROOT_PATH + "/temp";
    public static final String TEMP_SPECIAL_PATH = TEMP_FILE_PATH + "/.special/";
    public static final String EXTRACT_AUDIO_PATH = APP_EXTERNAL_ROOT_PATH + "/cts/ExtractAudio/";//从视频中提取出的音乐文件夹
    public static final String TEMP_FILTER_PATH = TEMP_FILE_PATH + "/.filter";
    public static final String TEMP_REVERSE_PATH = TEMP_FILE_PATH + "/reverse";
    public static final String IMG_TRANSCODE_PATH = APP_EXTERNAL_ROOT_PATH + "/cts/1/.vni/img/";


}
