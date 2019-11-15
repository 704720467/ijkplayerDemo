package com.zp.libvideoedit.GPUImage.Filter;


import android.opengl.GLES20;
import android.util.Log;


import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageAlphaBlendFilter;

import java.nio.FloatBuffer;

/**
 * Created by zp on 2018/7/17.
 */

public class VNiImageAlphaBlendFilter extends GPUImageAlphaBlendFilter {
    public VNiImageAlphaBlendFilter() {
        super(VNiImageAlphaBlendFilterFragment);
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
//
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);

        GLES20.glVertexAttribPointer(mfilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates));
        GLES20.glEnableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        if (secondInputFramebuffer != null) {
            if (this.viewPoint != null) {
                GLES20.glViewport((int) viewPoint.getX(), (int) viewPoint.getY(), (int) viewPoint.getWidth(), (int) viewPoint.getHeight());
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, secondInputFramebuffer.getTexture());
            GLES20.glUniform1i(mfilterInputTextureUniform2, 3);

        }
        if (Constants.VERBOSE_GL)
            Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        if (Constants.VERBOSE_GL)
            Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",first: " + mFirstInputFramebuffer + " sec: " + secondInputFramebuffer);
        if (mFirstInputFramebuffer != null)
            mFirstInputFramebuffer.unlock();
        if (secondInputFramebuffer != null)
            secondInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private static final String VNiImageAlphaBlendFilterFragment = " varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            " uniform lowp float mixturePercent;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "     lowp vec4 tmpColor = vec4(\n" +
            "                               (textureColor2.a==0.0?textureColor.r:(textureColor2.r/textureColor2.a)),\n" +
            "                               (textureColor2.a==0.0?textureColor.g:(textureColor2.g/textureColor2.a)),\n" +
            "                               (textureColor2.a==0.0?textureColor.b:(textureColor2.b/textureColor2.a)),\n" +
            "                               textureColor2.a*mixturePercent);\n" +
            "     gl_FragColor = mix(tmpColor,textureColor,1.0-tmpColor.a) ;\n" +
            " }";

}
