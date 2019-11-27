package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTime;

/**
 * Created by IT on 2018/3/12.
 */

public class VideoTimer {
    private static final int STARTING_STATUS = 1;
    private static final int PAUSEING_STATUS = 2;
    private static final int CANCEL_STATUS = 3;

    private static int status = CANCEL_STATUS;
    private long startTime = 0;
    private long oldTime = 0;
    private final long timeScale = 1000;


    public VideoTimer() {
        status = CANCEL_STATUS;
        startTime = 0;
        oldTime = 0;
    }

    public void start() {
        status = STARTING_STATUS;
        startTime = System.currentTimeMillis();
        oldTime = 0;
    }


    public void pause() {
        oldTime = getCurrentTimeMs();
        startTime = 0;
        status = PAUSEING_STATUS;
    }

    public void resume() {
        status = STARTING_STATUS;
        startTime = System.currentTimeMillis();
    }

    public void seekTime(long time) {
        status = PAUSEING_STATUS;
        oldTime = time;
        startTime = 0;
    }

    public void cancel() {
        status = CANCEL_STATUS;
        startTime = 0;
        oldTime = 0;
    }


    public long getCurrentTimeMs() {
        switch (status) {
            case PAUSEING_STATUS:
                return oldTime;
            case STARTING_STATUS:
                return oldTime + System.currentTimeMillis() - startTime;
        }
        return 0;
    }

    public CMTime getcCurrentTime() {
        return new CMTime(getCurrentTimeMs(),1000);
    }
}
