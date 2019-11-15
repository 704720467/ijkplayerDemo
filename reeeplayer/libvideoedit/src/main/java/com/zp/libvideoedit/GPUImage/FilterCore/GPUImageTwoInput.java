package com.zp.libvideoedit.GPUImage.FilterCore;

import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageRotationMode;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.Time.CMTime;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/2/27.
 */

public class GPUImageTwoInput extends GPUImageFilter {

    public GPUImageTwoInput(String fragmentShader) {
        super(kTwoInputGPUImageVertexShaderString, fragmentShader);
    }

    public GPUImageTwoInput(String vertextShader, String fragmentShader) {
        super(vertextShader, fragmentShader);
    }

    public void init() {
        mInputRotation = GPUImageRotationMode.kGPUImageNoRotation;
        mBackgroundColorRed = 0.0f;
        mBackgroundColorGreen = 0.0f;
        mBackgroundColorBlue = 0.0f;
        mBackgroundColorAlpha = 0.0f;
        mFilterProgram = new GLProgram().initWithVertexVShaderStringFShaderString(this.vertextShaderString, this.fragmentShaderString);
        initializeAttributes();
        if (!mFilterProgram.link()) {
            Log.e(TAG, "Program link log: " + mFilterProgram.getmProgramLog());
            Log.e(TAG, "Fragment shader compile log: " + mFilterProgram.getmFragmentShaderLog());
            Log.e(TAG, "Vertex shader compile log: " + mFilterProgram.getmVertexShaderLog());
            mFilterProgram = null;
            return;
        }
        mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
        mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
        mfilterSecondTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate2");
        mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
        mfilterInputTextureUniform2 = mFilterProgram.uniformIndex("inputImageTexture2");
        GPUImageContext.setActiveShaderProgram(mFilterProgram);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
    }

    @Override
    protected void initializeAttributes() {
        mFilterProgram.addAttribute("position");
        mFilterProgram.addAttribute("inputTextureCoordinate");
        mFilterProgram.addAttribute("inputTextureCoordinate2");

    }

    @Override
    public int nextAvailableTextureIndex() {
        if (hasReceivedFirstFrame) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        if (textureIndex == 0) {
            mFirstInputFramebuffer = newInputFramebuffer;
            hasSetFirstTexture = true;
            if (mFirstInputFramebuffer != null) {
                mFirstInputFramebuffer.lock();
            }
        } else {
            secondInputFramebuffer = newInputFramebuffer;
            if (secondInputFramebuffer != null) {
                secondInputFramebuffer.lock();
            }
        }
    }

    @Override
    public void setInputSize(GPUSize newSize, int index) {
        if (index == 0) {
            super.setInputSize(newSize, index);
            if (newSize.width == 0 || newSize.height == 0) {
                hasSetFirstTexture = false;
            }
        }
    }

    @Override
    public void setInputRotation(GPUImageRotationMode newInputRotation, int textureIndex) {
        if (textureIndex == 0) {
        } else {

        }
    }

    public void disableSecondFrameCheck() {
        secondFrameCheckDisabled = true;
    }

    public void disableFirstFrameCheck() {
        firstFrameCheckDisabled = true;
    }

    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        if (Constants.VERBOSE_GL)
            Log.d("GPUImageTwoInput", "newFrameReadyAtTime  ,textureIndex:  " + textureIndex);
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, Constants.NS_MUTIPLE);
        if (hasReceivedFirstFrame && hasReceivedSecondFrame) return;
        if (textureIndex == 0) {
            hasReceivedFirstFrame = true;
            if (secondFrameCheckDisabled) {
                hasReceivedSecondFrame = true;
            }
        } else {
            hasReceivedSecondFrame = true;
            if (firstFrameCheckDisabled) {
                hasReceivedFirstFrame = true;
            }
        }
        if ((hasReceivedFirstFrame && hasReceivedSecondFrame)) {
//            super.newFrameReadyAtTime(frameTime, 0);
//            hasReceivedSecondFrame = false;
            renderAtTime(frameTime,textureIndex);
            hasReceivedFirstFrame = false;
        }
    }


    public void renderAtTime(long frameTime, int textureIndex) {
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, Constants.NS_MUTIPLE);
//       if(Constants.VERBOSE_GL) Log.d(Constants.TAG_GL, "GPUImageFilter CurrentTime: " + CMTime.getSecond(currentTime));
        this.renderToTextureWithVertices(GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices), GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates), frameTime);
        this.informTargetsAboutNewFrameAtTime(frameTime);
    }


    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
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
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, secondInputFramebuffer.getTexture());
            GLES20.glUniform1i(mfilterInputTextureUniform2, 3);

        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        if (mFirstInputFramebuffer != null) mFirstInputFramebuffer.unlock();
        if (secondInputFramebuffer != null) secondInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    protected int mfilterSecondTextureCoordinateAttribute;
    protected int mfilterInputTextureUniform2;
    protected boolean hasSetFirstTexture, hasReceivedFirstFrame, hasReceivedSecondFrame, firstFrameWasVideo, secondFrameWasVideo;
    protected GPUImageFrameBuffer secondInputFramebuffer;
    protected boolean firstFrameCheckDisabled = false, secondFrameCheckDisabled = false;


    protected static final String kTwoInputGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "}";
}
