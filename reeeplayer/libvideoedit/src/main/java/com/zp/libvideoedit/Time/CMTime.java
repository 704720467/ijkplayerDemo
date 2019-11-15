package com.zp.libvideoedit.Time;

import android.util.Log;


import androidx.annotation.NonNull;

import java.io.Serializable;

import static com.zp.libvideoedit.Constants.MS_MUTIPLE;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;

/**
 * Created by gwd on 2018/3/8.
 */

public class CMTime implements Serializable {
    private long value;
    private long timeScale;

    public CMTime(long value, long timeScale) {
        this.timeScale = US_MUTIPLE;
        this.value = Math.round(((double) value) * (US_MUTIPLE / ((double) timeScale)));
    }

    public static CMTime ofSec(double second) {
        return new CMTime(second, US_MUTIPLE);
    }

    public CMTime(double second, long timeScale) {
        this.timeScale = US_MUTIPLE;
        this.value = Math.round(second * US_MUTIPLE);
    }

    public CMTime(float second, long timeScale) {
//        this.timeScale = timeScale;
//        double value = second * timeScale;
//        this.value = (long) Math.floor(value);
        this.timeScale = US_MUTIPLE;
        this.value = Math.round(second * US_MUTIPLE);
    }

    //    @NonNull
    public static CMTime convertTimeScale(CMTime time, long timeScale) {
        timeScale = US_MUTIPLE;
        long value = (long) Math.round((double) (time.getValue() * (timeScale / (1.0 * time.getTimeScale()))));
        long timescale = timeScale;
        return new CMTime(value, timescale);
    }

    public CMTime(float second) {
        this.timeScale = US_MUTIPLE;
        double value = second * this.timeScale;
        this.value = (long) Math.round(value);
    }

    public CMTime(double second) {
        this.timeScale = US_MUTIPLE;
        double value = second * timeScale;
        this.value = (long) Math.round(value);
    }

    public CMTime(long us) {
        this.timeScale = US_MUTIPLE;
        this.value = us;
    }


    @NonNull
    public static CMTime addTime(CMTime time1, CMTime time2) {
        CMTime tmpTime = null;
        if (time1.getTimeScale() >= time2.getTimeScale()) {
            return addTime(time1, time2, time1.getTimeScale());
        } else {
            return addTime(time1, time2, time2.getTimeScale());
        }
    }

    @NonNull
    public static CMTime addTime(CMTime time1, CMTime time2, long timeScale) {
        CMTime tmptime1 = CMTime.convertTimeScale(time1, timeScale);
        CMTime tmptime2 = CMTime.convertTimeScale(time2, timeScale);
        return new CMTime(tmptime1.getValue() + tmptime2.getValue(), timeScale);
    }

    @NonNull
    public static CMTime subTime(CMTime time1, CMTime time2) {
        CMTime tmpTime = null;
        if (time1.getTimeScale() > time2.getTimeScale()) {
            return subTime(time1, time2, time1.getTimeScale());
        } else {
            return subTime(time1, time2, time2.getTimeScale());

        }
    }

    public static CMTime subTime(CMTime time1, CMTime time2, long timeScale) {
        CMTime tmptime1 = CMTime.convertTimeScale(time1, timeScale);
        CMTime tmptime2 = CMTime.convertTimeScale(time2, timeScale);
        if (tmptime1.getValue() - tmptime2.getValue() < 0) return zeroTime();
        return new CMTime(tmptime1.getValue() - tmptime2.getValue(), timeScale);
    }

    public static CMTime divide(CMTime time, double divide) {
        if (divide == 0) return CMTime.zeroTime();
        long timeScale = time.getTimeScale();
        long value = (long) (time.getValue() / divide);
        return new CMTime(value, timeScale);
    }

    public static CMTime divide(CMTime time, double divide, long timeScale) {
        if (divide == 0) return CMTime.zeroTime();
        CMTime tmptime1 = CMTime.convertTimeScale(time, timeScale);
        long value = (long) (tmptime1.getValue() / divide);
        return new CMTime(value, timeScale);

    }

    public static CMTime multiply(CMTime time, float multip) {
        return new CMTime((long) (time.getValue() * multip), time.getTimeScale());
    }

    public static int compare(CMTime time1, CMTime time2) {
        CMTime tmpTime = CMTime.convertTimeScale(time2, time1.getTimeScale());
        if (time1.getValue() > tmpTime.getValue()) {
            return 1;
        } else if (time1.getValue() == time2.getValue()) {
            return 0;
        } else {
            return -1;
        }
    }

    public static int compare(CMTime time1, CMTime time2, long timeScale) {
        CMTime tmpTime1 = CMTime.convertTimeScale(time1, timeScale);
        CMTime tmpTime2 = CMTime.convertTimeScale(time2, timeScale);
        if (tmpTime1.getValue() > tmpTime2.getValue()) {
            return 1;
        } else if (tmpTime1.getValue() == tmpTime2.getValue()) {
            return 0;
        } else {
            return -1;
        }

    }

    public static CMTime zeroTime() {
        return new CMTime(0, US_MUTIPLE);
    }

    public static long timeToUs(CMTime time) {
        long value = (long) Math.round((double) (time.getValue() * US_MUTIPLE / (1.0 * time.getTimeScale())));
        return value;

    }

    public static long timeToMs(CMTime time) {
        long value = (long) Math.round((double) (time.getValue() * MS_MUTIPLE / (1.0 * time.getTimeScale())));
        return value;
    }

    public static double getSecond(CMTime time) {
        if (time.value == 0) return 0;
        return (double) time.value / (double) time.timeScale;
    }

    public long getValue() {
        return value;
    }

    public long getTimeScale() {
        return timeScale;
    }

    /**
     * @return 返回单位秒
     */
    public double getSecond() {
        return getSecond(this);
    }

    /**
     * @return 返回单位毫秒
     */
    public long getMs() {
        return timeToMs(this);
    }

    /**
     * @return 返回单位微秒
     */
    public long getUs() {
        return timeToUs(this);
    }


    public void print() {
        Log.d(this.getClass().getSimpleName(), "time :" + CMTime.getSecond(this));
    }

    public static CMTime Minimum(CMTime time1, CMTime time2) {
        if (CMTime.compare(time1, time2) > 0) {
            return time2;
        } else {
            return time1;
        }
    }

    public static CMTime Maxmum(CMTime time1, CMTime time2) {
        if (CMTime.compare(time1, time2) > 0) {
            return time1;
        } else {
            return time2;
        }
    }

    public String prettyString() {
        return String.format("%.2f", getSecond());
    }

//    public CMTimeVo timeVo() {
//        CMTimeVo vo = new CMTimeVo();
//        vo.setValue(getValue());
//        vo.setTimeScale(getTimeScale());
//        return vo;
//    }

    public static CMTime timeFromString(String time) {
        return null;
    }

    @Override
    public String toString() {
        return "CMTime{" + "value=" + value + ", timeScale=" + timeScale + '}';
    }

}
