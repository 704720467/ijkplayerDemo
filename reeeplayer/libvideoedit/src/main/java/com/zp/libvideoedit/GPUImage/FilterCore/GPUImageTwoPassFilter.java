package com.zp.libvideoedit.GPUImage.FilterCore;

import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPURect;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/6/29.
 */

public class GPUImageTwoPassFilter extends GPUImageTwoInput {

    protected GLProgram secondFilterProgram;
    protected int secondFilterPositionAttribute, secondFilterTextureCoordinateAttribute;
    protected int secondFilterInputTextureUniform;
    protected String secondVertextString;
    protected String secondFragmentString;
    protected GPURect firstViewPoint;
    protected GPURect secondViewPoint;

    public GPUImageTwoPassFilter(String firstV, String firstF, String secondV, String secondF) {
        super(firstV, firstF);
        this.secondVertextString = secondV;
        this.secondFragmentString = secondF;
    }

    @Override
    public void init() {
        super.init();
        secondFilterProgram = new GLProgram().initWithVertexVShaderStringFShaderString(secondVertextString, secondFragmentString);
        initializeSecondaryAttributes();
        if (!secondFilterProgram.link()) {
            Log.e(TAG, "Program link log: " + secondFilterProgram.getmProgramLog());
            Log.e(TAG, "Fragment shader compile log: " + secondFilterProgram.getmFragmentShaderLog());
            Log.e(TAG, "Vertex shader compile log: " + secondFilterProgram.getmVertexShaderLog());
            secondFilterProgram = null;
        }
        secondFilterPositionAttribute = secondFilterProgram.attributeIndex("position");
        secondFilterTextureCoordinateAttribute = secondFilterProgram.attributeIndex("inputTextureCoordinate");
        secondFilterInputTextureUniform = secondFilterProgram.uniformIndex("inputImageTexture");
        GPUImageContext.setActiveShaderProgram(secondFilterProgram);
        GLES20.glEnableVertexAttribArray(secondFilterPositionAttribute);
        GLES20.glEnableVertexAttribArray(secondFilterTextureCoordinateAttribute);
    }


    public void initializeSecondaryAttributes() {
        secondFilterProgram.addAttribute("position");
        secondFilterProgram.addAttribute("inputTextureCoordinate");
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

//        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
//        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        //第二路

//        secondOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
//        secondOutputFramebuffer.activeFramebuffer();
        secondFilterProgram.use();
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


    public void setFirstViewPoint(GPURect firstViewPoint) {
        this.firstViewPoint = firstViewPoint;
    }

    public void setSecondViewPoint(GPURect secondViewPoint) {
        this.secondViewPoint = secondViewPoint;
    }
}
