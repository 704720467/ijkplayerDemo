package com.zp.libvideoedit.GPUImage.Filter;

import android.renderscript.Matrix4f;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTransformFilter;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/7/12.
 */

public class VNiTailterTextTransitionFilter extends GPUImageTransformFilter {
    private float startTime = 0;
    private Matrix4f transformMatrix;
    private float transformDuration = 3.f;

    public VNiTailterTextTransitionFilter() {
        super();
        transformMatrix = new Matrix4f();
        transformMatrix.loadIdentity();
    }


    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        calcScale();
        super.renderToTextureWithVertices(vertices, textureCoordinaes, frameIndex);
    }


    private void calcScale() {
        if (currentTime.getSecond() - startTime < transformDuration) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.loadIdentity();
            float scaleValue = ((1.0f-0.93f) / 3.f) * ((float) currentTime.getSecond() - startTime) + 0.93f;
            if (Constants.VERBOSE_GL)
                Log.d(Constants.TAG_GL, "VNiTailerBlendFilter_calcScale:  " + scaleValue+" currentTime: "+currentTime.getSecond()+ "  start: "+startTime);
            matrix4f.scale(scaleValue, scaleValue, 1.f);
            setTransform3D(matrix4f.getArray());
            return;
        }
        setTransform3D(transformMatrix.getArray());
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }
}
