package com.zp.libvideoedit.GPUImage.FilterCore;

import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.Time.CMTime;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/6/29.
 */

public class VNITailBureFilter extends GPUImageFilter {
    private final static String kVNITailBureFilterVertexShaderString =
            "attribute vec4 position;\n" +
                    " attribute vec4 inputTextureCoordinate;\n" +
                    " \n" +
                    " varying vec2 textureCoordinate;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_Position = position;\n" +
                    "     textureCoordinate = inputTextureCoordinate.xy;\n" +
                    " }";
    private final static String kVNITailBureFilterFragmentShaderString =
            "varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform lowp float mixturePercent;\n" +
                    "\n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate)*mixturePercent;\n" +
                    " }";
    private CMTime durationTime;
    private float bureTime;
    private float burelevel;
    private int blureSlot;

    public VNITailBureFilter() {
        super(kVNITailBureFilterVertexShaderString, kVNITailBureFilterFragmentShaderString);
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinaes);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        float blureValue = calcBurleValue();
        if (EditConstants.VERBOSE_GL)
            Log.d(this.getClass().getSimpleName(), "VNITailBureFilter_Ondraw_renderToTextureWithVertices" + "blureValue:  " + blureValue);
        GLES20.glUniform1f(blureSlot, blureValue);
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.unlock();
        }
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void init() {
        super.init();
        blureSlot = mFilterProgram.uniformIndex("mixturePercent");
        bureTime = 1.0f;
        burelevel = 0.0f;
    }

    public CMTime getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(CMTime durationTime) {
        this.durationTime = durationTime;
    }

    private float calcBurleValue() {
        if (currentTime.getSecond() - durationTime.getSecond() < 0) {
            return 1.0f;
        }
        float k = -((1 - burelevel) / bureTime);
        float b = 1.0f;
        return (float) (k * ((currentTime.getSecond()) - (durationTime.getSecond())) + b);
    }

}
