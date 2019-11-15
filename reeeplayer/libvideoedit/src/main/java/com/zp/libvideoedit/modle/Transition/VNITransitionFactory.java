package com.zp.libvideoedit.modle.Transition;


import com.zp.libvideoedit.GPUImage.Filter.GPUImageTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNIColorFadeTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNICrossDissolveTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNISmoothRotateTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNISmoothZoomTransitionFilter;
import com.zp.libvideoedit.Time.CMTime;

/**
 * Created by gwd on 2018/7/25.
 */

public class VNITransitionFactory {

    public static CMTime maxTransitionDuration() {
        return new CMTime(0.8f);
    }

    public static GPUImageTransitionFilter transitionFilterWithType(TransitionStyle type, Origentation origentation) {
        switch (type) {
            case VNITransitionTypeNone: {
                return null;
            }
            case VNITransitionTypeCrossDissolve: {
                VNICrossDissolveTransitionFilter filter = new VNICrossDissolveTransitionFilter();
                return filter;
            }
            case VNITransitionTypeBlackFade: {
                VNIColorFadeTransitionFilter filter = new VNIColorFadeTransitionFilter();
                float[] blackColor = {0.0f, 0.0f, 0.0f, 1.0f};
                filter.setFadeColor(blackColor);
                return filter;
            }
            case VNITransitionTypeWhiteFade: {
                VNIColorFadeTransitionFilter filter = new VNIColorFadeTransitionFilter();
                float[] blackColor = {1.0f, 1.0f, 1.0f, 1.0f};
                filter.setFadeColor(blackColor);
                return filter;
            }
            case VNITransitionTypeSmoothZoomIn: {
                VNISmoothZoomTransitionFilter filter = new VNISmoothZoomTransitionFilter();
                filter.setType(VNITransitionZoomType.VNITransitionZoomTypeIn);
                if (origentation == Origentation.kVideo_Vertical) {
                    filter.setRatio(9.f / 16.f);
                } else if (origentation == Origentation.kVideo_Horizontal) {
                    filter.setRatio(16.f / 9.f);
                }
                return filter;
            }
            case VNITransitionTypeSmoothZoomOut: {
                VNISmoothZoomTransitionFilter filter = new VNISmoothZoomTransitionFilter();
                filter.setType(VNITransitionZoomType.VNITransitionZoomTypeOut);
                if (origentation == Origentation.kVideo_Vertical) {
                    filter.setRatio(9.f / 16.f);
                } else if (origentation == Origentation.kVideo_Horizontal) {
                    filter.setRatio(16.f / 9.f);
                }
                return filter;
            }
            case VNITransitionTypeSmoothRotateLeft: {
                VNISmoothRotateTransitionFilter filter = new VNISmoothRotateTransitionFilter();
                filter.setType(VNITransitionRotateType.VNITransitionRotateTypeLeft);
                if (origentation == Origentation.kVideo_Vertical) {
                    filter.ratio = 9.f / 16.f;
                } else if (origentation == Origentation.kVideo_Horizontal) {
                    filter.ratio = 16.f / 9.f;
                }
                return filter;
            }
            case VNITransitionTypeSmoothRotateRight: {
                VNISmoothRotateTransitionFilter filter = new VNISmoothRotateTransitionFilter();
                filter.setType(VNITransitionRotateType.VNITransitionRotateTypeRight);
                if (origentation == Origentation.kVideo_Vertical) {
                    filter.ratio = 9.f / 16.f;
                } else if (origentation == Origentation.kVideo_Horizontal) {
                    filter.ratio = 16.f / 9.f;
                }
                return filter;
            }
            default:
                break;
        }
        return null;
    }


}
