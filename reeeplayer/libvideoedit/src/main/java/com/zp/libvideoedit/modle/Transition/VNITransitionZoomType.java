package com.zp.libvideoedit.modle.Transition;

/**
 * Created by gwd on 2018/7/25.
 */

public enum VNITransitionZoomType {
    VNITransitionZoomTypeIn("zoomin", 0), VNITransitionZoomTypeOut("zoomout", 1);
    String name;
    int value;

    VNITransitionZoomType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static VNITransitionZoomType getStyle(int value) {
        if (value == VNITransitionZoomTypeIn.getValue()) {
            return VNITransitionZoomTypeIn;
        } else if (value == VNITransitionZoomTypeOut.getValue()) {
            return VNITransitionZoomTypeOut;
        } else {
            return VNITransitionZoomTypeIn;
        }
    }

}
