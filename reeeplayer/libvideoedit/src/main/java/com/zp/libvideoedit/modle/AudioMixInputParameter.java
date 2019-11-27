package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTime;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/6/7.
 */

public class AudioMixInputParameter {
    private TrackType trackType;
    private ArrayList<ParametersInternal> parametersInternals;

    public AudioMixInputParameter() {
        parametersInternals = new ArrayList<>();
    }

    public static AudioMixInputParameter audioMixInputParametersWithTrack(TrackType trackType) {
        AudioMixInputParameter parameter = new AudioMixInputParameter();
        parameter.setTrackType(trackType);
        return parameter;
    }

    public void setVolumeAtTime(float volume, CMTime atTime) {
        ParametersInternal parametersInternal = new ParametersInternal(volume, atTime);
        parametersInternals.add(parametersInternal);
    }

    public ArrayList<ParametersInternal> getParametersInternals() {
        return parametersInternals;
    }

    public void setTrackType(TrackType trackType) {
        this.trackType = trackType;
    }

    public TrackType getTrackType() {
        return trackType;
    }
}
