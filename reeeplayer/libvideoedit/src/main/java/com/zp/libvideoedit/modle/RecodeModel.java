package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;

/**
 * Created by gwd on 2018/5/29.
 */

public class RecodeModel {
    private CMTimeRange timeRange;
    private CMTime atTime;
    private String filePath;

    public RecodeModel(String filePath) {
        this.filePath = filePath;
    }

    public RecodeModel(CMTimeRange timeRange, CMTime atTime, String filePath) {
        this.timeRange = timeRange;
        this.atTime = atTime;
        this.filePath = filePath;
    }

    public CMTimeRange getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(CMTimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public CMTime getAtTime() {
        return atTime;
    }

    public void setAtTime(CMTime atTime) {
        this.atTime = atTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
