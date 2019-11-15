package com.zp.libvideoedit.utils;

import android.renderscript.Matrix4f;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.modle.ViewportRange;

import java.util.List;

/**
 * Created by gwd on 2018/5/13.
 */

public class MatrixUtils {
    private static final String TAG = "MatrixUtils";

    public static float[] rotationMatrixToEulerAngles(float[] matrixArray) {

        return null;
    }

    public static float[] rotationMatrixToEulerAngles(Matrix4f matrix) {
        return null;
    }

    public static boolean isRotationMatrix(Matrix4f matrix) {
        return true;
    }

    public static GPURect AVMakeRectWithAspectRatioInsideRect(GPUSize size, GPURect rect, ViewportRange viewportRange) {
        float imageRatio = size.width / (1.0f * size.height);
        float viewRectRatio = rect.getWidth() / (1.0f * rect.getHeight());
        if (imageRatio < viewRectRatio) {
            float scale = rect.getHeight() / (1.0f * size.height);
            float width = scale * size.width;
            float xoffset = (rect.getWidth() - width) * 0.5f;
//            return new GPURect(xoffset, 0, width, rect.getHeight());
            float rs = Math.max((rect.getWidth() / width), 1.0f);
            //移动到顶部
            float yoffsetToTop = (rect.getHeight() / (rs * 2)) * (rs - 1);
            //y方向可移动的最大位置
            float totalY = yoffsetToTop * 2;
            //计算移动距离的百分比
            float offsetScale = 0.5f;
            if (viewportRange != null && !viewportRange.isVerticalShowCenter())
                offsetScale = viewportRange.getTop() / (1 - (viewportRange.getBottom() - viewportRange.getTop()));
            //计算和移动的真实距离
            float yOffset = totalY * offsetScale;
            return new GPURect(xoffset, -yoffsetToTop + yOffset, width, rect.getHeight());
        } else {
            float scale = rect.getWidth() / size.width;
            float height = scale * size.height;
//            float yoffset = (rect.getHeight() - height) * 0.5f;
//            return new GPURect(0, yoffset, rect.getWidth(), height);
            //此值将垂直方向居中，以便于缩放
            float yoffset = (rect.getHeight() - height) * 0.5f;
            //计算真实的缩放值
            float rs = Math.max((rect.getHeight() / height), 1.0f);
            //首先移动到最左边
            float xoffsetToLeft = (rect.getWidth() / (rs * 2)) * (rs - 1);
            //x方向可移动的最大位置
            float totalX = xoffsetToLeft * 2;
            //计算移动距离的百分比
            float offsetScale = 0.5f;
            if (viewportRange != null && !viewportRange.isHorizontalShowCenter())
                offsetScale = viewportRange.getLeft() / (1 - (viewportRange.getRight() - viewportRange.getLeft()));
            float xOffset = totalX * offsetScale;
            return new GPURect(xoffsetToLeft - xOffset, yoffset, rect.getWidth(), height);
        }
    }

    public static GPUSize SizeApplyAffineTransform2D(GPUSize size, Matrix4f matrix) {
        if (Constants.VERBOSE_GL)
            Log.d(MatrixUtils.TAG, "size = " + size.width + "  " + size.height + "  " + matrix);
        float a = matrix.get(0, 0);
        float b = matrix.get(0, 1);
        float c = matrix.get(1, 0);
        float d = matrix.get(1, 1);
        float width = a * size.width + c * size.height;
        float height = b * size.width + d * size.height;
        return new GPUSize((int) width, (int) height);
    }

    public static Matrix4f Matrix4fMakeAffineTransform(float[] matrixArray) {
        if (matrixArray == null) return null;
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        matrix4f.set(0, 0, matrixArray[0]);
        matrix4f.set(0, 1, matrixArray[1]);
        matrix4f.set(1, 0, matrixArray[2]);
        matrix4f.set(1, 1, matrixArray[3]);
        matrix4f.set(3, 0, matrixArray[4]);
        matrix4f.set(3, 1, matrixArray[5]);
        return matrix4f;
    }

    public static Matrix4f Matrix4fMakeAffineTransform(List<Double> matrixArray) {
        if (matrixArray == null) return null;
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        float a = matrixArray.get(0).floatValue();
        matrix4f.set(0, 0, matrixArray.get(0).floatValue());
        matrix4f.set(0, 1, matrixArray.get(1).floatValue());
        matrix4f.set(1, 0, matrixArray.get(2).floatValue());
        matrix4f.set(1, 1, matrixArray.get(3).floatValue());
        matrix4f.set(3, 0, matrixArray.get(4).floatValue());
        matrix4f.set(3, 1, matrixArray.get(5).floatValue());
        return matrix4f;
    }
}
