package com.zp.libvideoedit.modle;

/**
 * Created by gwd on 2018/3/8.
 */

public enum MediaType {
    MEDIA_TYPE_Video("VIDEO", 0), MEDIA_TYPE_Audio("AUDIO", 1), MEDIA_TYPE_Picture("IMAGE", 2), MEDIA_TYPE_Unknow("UNKNOW", 3);

    String name;
    int value;

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    MediaType(String name, int value) {
        this.name = name;
        this.value = value;

    }

    public static MediaType getmusicModel(int value) {
        if (value == MEDIA_TYPE_Video.getValue()) {
            return MEDIA_TYPE_Video;
        } else if (value == MEDIA_TYPE_Audio.getValue()) {
            return MEDIA_TYPE_Audio;
        } else if (value == MEDIA_TYPE_Picture.getValue()) {
            return MEDIA_TYPE_Picture;
        }
        return null;
    }

}
