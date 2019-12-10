package com.zp.libvideoedit.Time;

import android.text.TextUtils;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.utils.StrUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Created by gwd on 2018/3/8.
 */

public class CMTimeRange implements Serializable {
    private CMTime startTime;
    private CMTime duration;

    /**
     * 将String类型的Timerange转为CMTimeTange
     *
     * @param timeRangeStr
     */
    public CMTimeRange(String timeRangeStr) {
        this.startTime = CMTime.zeroTime();
        this.duration = CMTime.zeroTime();
        if (TextUtils.isEmpty(timeRangeStr)) return;
        List<Long> timeRangeList = StrUtils.stringToLongs(timeRangeStr);
        if (timeRangeList == null || timeRangeList.size() < 4) return;

        CMTime newStartTime = new CMTime(timeRangeList.get(0), timeRangeList.get(1));
        CMTime newDuration = new CMTime(timeRangeList.get(2), timeRangeList.get(3));
        adjustmentData(newStartTime, newDuration);

    }

    public CMTimeRange(CMTime startTime, CMTime duration) {
        adjustmentData(startTime, duration);
    }

    /**
     * 调整数据
     *
     * @param startTime
     * @param duration
     */
    private void adjustmentData(CMTime startTime, CMTime duration) {
        if (startTime.getTimeScale() == 0 && duration.getTimeScale() == 0) {
            this.startTime = CMTime.zeroTime();
            this.duration = CMTime.zeroTime();
        } else {
            if (startTime.getTimeScale() > duration.getTimeScale()) {
                this.startTime = startTime;
                this.duration = CMTime.convertTimeScale(duration, startTime.getTimeScale());
            } else {
                this.duration = duration;
                this.startTime = CMTime.convertTimeScale(startTime, duration.getTimeScale());
            }
        }
    }

    public CMTimeRange(double startTime, double duration) {
        this.startTime = new CMTime(startTime, EditConstants.US_MUTIPLE);
        this.duration = new CMTime(duration, EditConstants.US_MUTIPLE);
    }

    public static CMTimeRange RangeFromTimeToTime(CMTime time, CMTime endTime) {
        CMTime duration = null;
        if (endTime != null && time != null) {
            duration = CMTime.subTime(endTime, time);
            return new CMTimeRange(time, duration);
        }
        return null;
    }

    public static boolean CMTimeRangeContains(CMTimeRange timeRange, CMTime time) {
        float tmpTime = (float) CMTime.getSecond(time);
        float start = (float) CMTime.getSecond(timeRange.getStartTime());
        float end = (float) CMTime.getSecond(timeRange.getEnd());
        if (tmpTime >= start && tmpTime < end) {
            return true;
        }
        return false;
    }

    public static CMTimeRange zeroTimeRange() {
        return new CMTimeRange(CMTime.zeroTime(), CMTime.zeroTime());
    }

    public static CMTimeRange CMTimeRangeTimeToTime(CMTime startTime, CMTime endTime) {
        return new CMTimeRange(startTime, CMTime.subTime(endTime, startTime));
    }

    public CMTime getStartTime() {
        return startTime;
    }

    public CMTime getDuration() {
        return duration;
    }

    public CMTime getEnd() {
        return CMTime.addTime(startTime, duration);
    }

    public void print() {
        Log.d(this.getClass().getSimpleName(), " start:" + CMTime.getSecond(this.startTime) + " :End:" + CMTime.getSecond(this.duration));
    }

    public void offset(double sec) {
        this.startTime = new CMTime(this.startTime.getSecond() + sec, this.startTime.getTimeScale());
    }

    public void offset(long us) {
        this.startTime = new CMTime(this.startTime.getUs() + us, this.startTime.getTimeScale());
    }

    public void resize(double sec) {
        this.duration = new CMTime(sec, this.startTime.getTimeScale());
    }

    public void resize(long us) {
        this.duration = new CMTime(us);
    }

    public void stretch(double second) {
        this.duration = new CMTime(second + duration.getSecond(), this.startTime.getTimeScale());
    }


    public boolean containMs(long ms) {
        long startMs = startTime.getMs();
        long durationMs = duration.getMs();
        if (ms >= startMs && ms <= startMs + durationMs) return true;
        else return false;
    }

    public boolean containUs(long us) {
        long startUs = startTime.getUs();
        long durationUs = duration.getUs();
        if (us >= startUs && us <= startUs + durationUs) return true;
        else return false;
    }

//    public CMTimeRangeVo timeRangeVo() {
//        CMTimeVo s = new CMTimeVo();
//        s.setValue(startTime.getValue());
//        s.setTimeScale(startTime.getTimeScale());
//        CMTimeVo e = new CMTimeVo();
//        e.setValue(duration.getValue());
//        e.setTimeScale(duration.getTimeScale());
//        CMTimeRangeVo rangeVo = new CMTimeRangeVo();
//        rangeVo.setStartTime(s);
//        rangeVo.setDuration(e);
//        return rangeVo;
//    }

    public static CMTimeRange rangeFromString(String range) {
        return null;
    }

    public static String CMtimeRangeToString(CMTimeRange range) {
        return null;
    }

    @Override
    public String toString() {
        return "CMTimeRange{" + "startTime=" + startTime + ", duration=" + duration + '}';

    }
}


