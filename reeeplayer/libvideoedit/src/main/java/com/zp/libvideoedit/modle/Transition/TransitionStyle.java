package com.zp.libvideoedit.modle.Transition;

/**
 * Created by gwd on 2018/3/9.
 */

public enum TransitionStyle {
    VNITransitionTypeNone("none", 0), VNITransitionTypeCrossDissolve("CrossDissolve", 1), VNITransitionTypeBlackFade("BlackFade", 2), VNITransitionTypeWhiteFade("WhiteFade", 3), VNITransitionTypeSmoothZoomIn("ZoomIn", 4), VNITransitionTypeSmoothZoomOut("ZoomOut", 5), VNITransitionTypeSmoothRotateLeft("RotateLeft", 6), VNITransitionTypeSmoothRotateRight("RotateRight", 7);

    String name;
    int value;

    TransitionStyle(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static TransitionStyle getStyle(int value) {
        if (value == VNITransitionTypeCrossDissolve.getValue()) {
            return VNITransitionTypeCrossDissolve;
        } else if (value == VNITransitionTypeBlackFade.getValue()) {
            return VNITransitionTypeBlackFade;
        } else if (value == VNITransitionTypeWhiteFade.getValue()) {
            return VNITransitionTypeWhiteFade;
        } else if (value == VNITransitionTypeSmoothZoomIn.getValue()) {
            return VNITransitionTypeSmoothZoomIn;
        } else if (value == VNITransitionTypeSmoothZoomOut.getValue()) {
            return VNITransitionTypeSmoothZoomOut;
        }else if (value == VNITransitionTypeSmoothRotateLeft.getValue()) {
            return VNITransitionTypeSmoothRotateLeft;
        }else if (value == VNITransitionTypeSmoothRotateRight.getValue()) {
            return VNITransitionTypeSmoothRotateRight;
        }else if (value == VNITransitionTypeNone.getValue()) {
            return VNITransitionTypeNone;
        } else {
            return VNITransitionTypeNone;
        }
    }
}
