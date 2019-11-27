package com.zp.libvideoedit.modle;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/6/7.
 */

public class AudioMixParam {

    ArrayList<AudioMixInputParameter> inputParameters;

    public AudioMixParam() {
        inputParameters = new ArrayList<AudioMixInputParameter>();
    }

    public static AudioMixParam audioMix() {
        return new AudioMixParam();
    }

    public ArrayList<AudioMixInputParameter> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(ArrayList<AudioMixInputParameter> inputParameters) throws Exception {
        this.inputParameters = inputParameters;
    }

    public ArrayList<AudioMixInputParameter> parametersOfTrackType(TrackType trackType) {
        ArrayList<AudioMixInputParameter> parameters = new ArrayList<AudioMixInputParameter>();
        for (AudioMixInputParameter parameter : inputParameters) {
            if (parameter.getTrackType() == trackType) {
                parameters.add(parameter);
            }
        }
        return parameters;
    }

}
