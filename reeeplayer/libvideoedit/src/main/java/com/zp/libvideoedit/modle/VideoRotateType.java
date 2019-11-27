package com.zp.libvideoedit.modle;

/**
 * Created by gwd on 2018/6/11.
 */

public enum VideoRotateType {
    VideoRotateTypeNone("none", 0), VideoRotateTypeHFlip("flip", 1), VideoRotateTypeVFlip("vflip", 2), VideoRotateTypeLeft("leftrote", 3), VideoRotateTypeRight("rightrote", 4);
    String name;
    int value;

    VideoRotateType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static VideoRotateType getrotateType(int value) {
        if (value == VideoRotateTypeNone.getValue()) {
            return VideoRotateTypeNone;
        } else if (value == VideoRotateTypeHFlip.getValue()) {
            return VideoRotateTypeHFlip;
        } else if (value == VideoRotateTypeVFlip.getValue()) {
            return VideoRotateTypeVFlip;
        } else if (value == VideoRotateTypeLeft.getValue()) {
            return VideoRotateTypeLeft;
        } else if (value == VideoRotateTypeRight.getValue()) {
            return VideoRotateTypeRight;

        }
        return null;
    }


    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

}
