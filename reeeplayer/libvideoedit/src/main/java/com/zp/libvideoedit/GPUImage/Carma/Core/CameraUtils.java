package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.hardware.Camera;

import java.util.List;

public class CameraUtils {

    public static Camera.Size getLargePictureSize(Camera camera) {
        if (camera != null) {
            List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size temp = sizes.get(0);
            for (int i = 1; i < sizes.size(); i++) {
                float scale = (float) (sizes.get(i).height) / sizes.get(i).width;
                if (temp.width < sizes.get(i).width && scale < 0.6f && scale > 0.5f)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    public static Camera.Size getLargePreviewSize(Camera camera) {
        if (camera != null) {
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            Camera.Size temp = sizes.get(0);
            for (int i = 1; i < sizes.size(); i++) {
                if (temp.width < sizes.get(i).width)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

//    public static Camera.Size getDefaultPreviewSize(Camera camera, Context context) {
//        if (camera == null)
//            return null;
//        List<Camera.Size> supportedPreviewSizes =
//                CameraEngine.getCamera().getParameters().getSupportedPreviewSizes();
//        Camera.Size previewSize = null;
//        int width = DensityUtil.getDisplayWidth(context);
//        int height = DensityUtil.getDisplayHeight(context);
//        if (supportedPreviewSizes != null) {
//
//            for (int i = 0; i < supportedPreviewSizes.size(); i++) {
//                Camera.Size size = supportedPreviewSizes.get(i);
//                if (size.height == width && size.width == height) {
//                    previewSize = size;
//                    break;
//                }
//            }
//            if (previewSize == null) {
//                for (int i = 0; i < supportedPreviewSizes.size(); i++) {
//                    Camera.Size size = supportedPreviewSizes.get(i);
//                    if (size.height <= width && size.width <= height) {
//                        previewSize = size;
//                        break;
//                    }
//                }
//            }
//            if (previewSize != null && previewSize.height > 1080) {
//                for (int i = 0; i < supportedPreviewSizes.size(); i++) {
//                    Camera.Size size = supportedPreviewSizes.get(i);
//                    if (size.height <= 1080) {
//                        previewSize = size;
//                        break;
//                    }
//                }
//            }
//            if (previewSize == null) {
//                previewSize = supportedPreviewSizes.get(0);
//            }
//        }
//        return previewSize;
//
//    }
}
