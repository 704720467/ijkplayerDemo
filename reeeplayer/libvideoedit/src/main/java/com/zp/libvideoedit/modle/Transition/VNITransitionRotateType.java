package com.zp.libvideoedit.modle.Transition;

/**
 * Created by gwd on 2018/7/25.
 */

public enum VNITransitionRotateType {
    VNITransitionRotateTypeRight("Rotateright", 1), VNITransitionRotateTypeLeft("Rotateleft", 0);
    String name;
    int value;

    VNITransitionRotateType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static VNITransitionRotateType getStyle(int value) {
        if (value == VNITransitionRotateTypeRight.getValue()) {
            return VNITransitionRotateTypeRight;
        } else if (value == VNITransitionRotateTypeLeft.getValue()) {
            return VNITransitionRotateTypeLeft;
        } else {
            return VNITransitionRotateTypeRight;
        }
    }
}
