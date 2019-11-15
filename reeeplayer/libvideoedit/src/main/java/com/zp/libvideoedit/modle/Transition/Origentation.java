package com.zp.libvideoedit.modle.Transition;

/**
 * Created by gwd on 2018/3/9.
 */

public enum Origentation {
    kVideo_Horizontal("h", 1), kVideo_Vertical("v", 0), kVideo_Unknow("unknow", -1);
    String name;
    int value;

    Origentation(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static Origentation getOrigentation(int value) {
        if (value == kVideo_Horizontal.getValue()) {
            return kVideo_Horizontal;
        } else if (value == kVideo_Vertical.getValue()) {
            return kVideo_Vertical;
        } else {
            return kVideo_Unknow;
        }
    }
}
