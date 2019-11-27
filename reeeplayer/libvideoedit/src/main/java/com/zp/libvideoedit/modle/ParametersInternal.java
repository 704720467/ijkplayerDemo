package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTime;

/**
 * Created by gwd on 2018/6/21.
 */

public class ParametersInternal {
    private TrackType trackType;
    private float volume;
    private CMTime atTime;
    private CMTime endTime ;

    public ParametersInternal(TrackType trackType, float volume, CMTime atTime, CMTime endTime) {
        this.trackType = trackType;
        this.volume = volume;
        this.atTime = atTime;
        this.endTime = endTime;
    }

    public ParametersInternal(float volume, CMTime atTime) {
        this.volume = volume;
        this.atTime = atTime;
    }

    public TrackType getTrackType() {
        return trackType;
    }

    public void setTrackType(TrackType trackType) {
        this.trackType = trackType;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public CMTime getAtTime() {
        return atTime;
    }

    public void setAtTime(CMTime atTime) {
        this.atTime = atTime;
    }

    public CMTime getEndTime() {
        return endTime;
    }

    public void setEndTime(CMTime endTime) {
        this.endTime = endTime;
    }
}
