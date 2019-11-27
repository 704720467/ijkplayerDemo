package com.zp.libvideoedit.modle;

/**
 * Created by gwd on 2018/3/17.
 */

public enum TrackType {
    TrackType_Video_Main("V_MAIN", 0), TrackType_Video_Second("V_SEC", 1),
    TrackType_Video_Mask("V_MASK", 2), TrackType_Video_Mask_Ext("V_EXT", 3),
    TrackType_Video_Transition("V_Trans", 4), TrackType_Main_Audio("A_MAIN", 5),
    TrackType_Audio_BackGround("A_BGM", 6), TrackType_Audio_Recoder("A_REC", 7),
    TrackType_Audio_BackGround_Daemon("A_BGM_DAEMON", 8),
    TrackType_Audio_SOUND_EFFECT("A_OUND_EFFECT", 9);

    //    public static final int TrackType_Video_Main = 0;
//    public static final int TrackType_Video_Second = 1;
//    public static final int TrackType_Video_Mask = 2;
//    public static final int TrackType_Video_Mask_Ext = 3;
//    public static final int TrackType_Video_Transition = 4;
//    public static final int TrackType_Main_Audio = 5;
//    public static final int TrackType_Audio_BackGround = 6;
    String name;
    int value;

    TrackType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static TrackType getTrackType(int value) {
        if (value == TrackType_Video_Main.getValue()) {
            return TrackType_Video_Main;
        } else if (value == TrackType_Video_Second.getValue()) {
            return TrackType_Video_Second;
        } else if (value == TrackType_Video_Mask.getValue()) {
            return TrackType_Video_Mask;
        } else if (value == TrackType_Video_Mask_Ext.getValue()) {
            return TrackType_Video_Mask_Ext;
        } else if (value == TrackType_Video_Transition.getValue()) {
            return TrackType_Video_Transition;
        } else if (value == TrackType_Main_Audio.getValue()) {
            return TrackType_Main_Audio;
        } else if (value == TrackType_Audio_BackGround.getValue()) {
            return TrackType_Audio_BackGround;
        } else if (value == TrackType_Audio_Recoder.getValue()) {
            return TrackType_Audio_Recoder;
        } else if (value == TrackType_Audio_BackGround_Daemon.getValue()) {
            return TrackType_Audio_BackGround_Daemon;
        } else if (value == TrackType_Audio_SOUND_EFFECT.getValue()) {
            return TrackType_Audio_SOUND_EFFECT;
        }
        return null;
    }


}
