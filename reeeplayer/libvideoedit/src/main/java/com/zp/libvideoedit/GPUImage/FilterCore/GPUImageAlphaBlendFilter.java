package com.zp.libvideoedit.GPUImage.FilterCore;

import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/5/21.
 */

public class GPUImageAlphaBlendFilter extends GPUImageTwoInput {

    public GPUImageAlphaBlendFilter() {
        super(AlphaBlendFilterFragment);
    }

    public GPUImageAlphaBlendFilter(String fragmentString) {
        super(fragmentString);
    }

    protected GPURect viewPoint;

    @Override
    public void init() {
        super.init();
        mixturePercentSlot = mFilterProgram.uniformIndex("mixturePercent");
        GLES20.glUseProgram(mFilterProgram.getmProgram());
        mixturePercent = 1.0f;
        setFloat(mixturePercentSlot, mixturePercent);
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

        GLES20.glVertexAttribPointer(mfilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
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
        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",first: " + mFirstInputFramebuffer + " sec: " + secondInputFramebuffer);
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

    public void setViewPoint(GPURect viewPoint) {
        this.viewPoint = viewPoint;
    }

    private static final String AlphaBlendFilterFragment =
            " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    "\n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " \n" +
                    " uniform lowp float mixturePercent;\n" +
                    "\n" +
                    " void main()\n" +
                    " {\n" +
                    "\t lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "\t lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "\t \n" +
                    "\t gl_FragColor = vec4(mix(textureColor.rgb, textureColor2.rgb, textureColor2.a * mixturePercent), textureColor.a);\n" +

                    " }";
    private int mixturePercentSlot;
    private float mixturePercent;
}
