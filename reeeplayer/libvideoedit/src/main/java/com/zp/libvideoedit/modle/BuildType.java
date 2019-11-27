package com.zp.libvideoedit.modle;

/**
 * build 的类型
 * Created by zp on 2019/7/1.
 */

public enum BuildType {
    BuildType_Default("default", 0),
    BuildType_AUDIO("BuildAudio", 1),
    BuildType_VIDEO("BuildVideo", 2);
    private String name;
    private int value;

    BuildType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static BuildType buildType(int value) {
        if (value == BuildType_AUDIO.getValue()) {
            return BuildType_AUDIO;
        } else if (value == BuildType_VIDEO.getValue()) {
            return BuildType_VIDEO;
        } else {
            return BuildType_Default;
        }
    }

}
