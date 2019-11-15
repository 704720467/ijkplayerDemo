package com.zp.libvideoedit.GPUImage.FilterCore;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/6/29.
 */

public class GPUImageAlphaTwoPassInput extends GPUImageTwoPassFilter {
    public static final String kGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String kGPUImagePassthroughFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
//        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        if (firstViewPoint != null) {
            GLES20.glViewport((int) firstViewPoint.getX(), (int) firstViewPoint.getY(), (int) firstViewPoint.getWidth(), (int) firstViewPoint.getHeight());
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinaes);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        //第二路

//        secondOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
//        secondOutputFramebuffer.activeFramebuffer();
        secondFilterProgram.use();
//        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_DST_ALPHA);
        if (!secondFilterProgram.ismInitialized()) {
            return;
        }
        if (secondViewPoint != null) {
            GLES20.glViewport((int) secondViewPoint.getX(), (int) secondViewPoint.getY(), (int) secondViewPoint.getWidth(), (int) secondViewPoint.getHeight());
        }
        GLES20.glVertexAttribPointer(secondFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(secondFilterPositionAttribute);
        GLES20.glVertexAttribPointer(secondFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinaes);
        GLES20.glEnableVertexAttribArray(secondFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (secondInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, secondInputFramebuffer.getTexture());
            GLES20.glUniform1i(secondFilterInputTextureUniform, 3);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.unlock();
        }
        if (secondInputFramebuffer != null) {
            secondInputFramebuffer.unlock();
        }
        GLES20.glDisableVertexAttribArray(secondFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(secondFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public GPUImageAlphaTwoPassInput() {
        super(kGPUImageVertexShaderString, kGPUImagePassthroughFragmentShaderString, kGPUImageVertexShaderString, kGPUImagePassthroughFragmentShaderString);
    }

}
