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
 * Created by gwd on 2018/5/10.
 */

public class GPUImageThreeInputFilter extends GPUImageTwoInput {
    protected GPUImageFrameBuffer thirdInputFrameBuffer;
    protected int filterInputTextureUniform3;
    protected int filterThirdTextureCoordinateAttribute;
    protected int filterSourceTexture3;
    protected boolean hasSetSecondTexture, hasReceivedThirdFrame, thirdFrameWasVideo;
    protected boolean thirdFrameCheckDisabled;
    protected static final String kGPUImageThreeInputTextureVertexShaderString =
            "attribute vec4 position;\n" +
                    " attribute vec4 inputTextureCoordinate;\n" +
                    " attribute vec4 inputTextureCoordinate2;\n" +
                    " attribute vec4 inputTextureCoordinate3;\n" +
                    " \n" +
                    " varying vec2 textureCoordinate;\n" +
                    " varying vec2 textureCoordinate2;\n" +
                    " varying vec2 textureCoordinate3;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_Position = position;\n" +
                    "     textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "     textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
                    "     textureCoordinate3 = inputTextureCoordinate3.xy;\n" +
                    " }";


    public GPUImageThreeInputFilter(String fragmentShader) {
        super(kGPUImageThreeInputTextureVertexShaderString,fragmentShader);
    }

    public GPUImageThreeInputFilter(String vertextShader, String fragmentShader) {
        super(vertextShader, fragmentShader);
    }

    @Override
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
        }
        mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
        mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
        mfilterSecondTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate2");
        filterThirdTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate3");
        mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
        mfilterInputTextureUniform2 = mFilterProgram.uniformIndex("inputImageTexture2");
        filterInputTextureUniform3 = mFilterProgram.uniformIndex("inputImageTexture3");

        GPUImageContext.setActiveShaderProgram(mFilterProgram);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glEnableVertexAttribArray(filterThirdTextureCoordinateAttribute);
        GLES20.glEnableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);


    }

    @Override
    protected void initializeAttributes() {
        mFilterProgram.addAttribute("position");
        mFilterProgram.addAttribute("inputTextureCoordinate");
        mFilterProgram.addAttribute("inputTextureCoordinate2");
        mFilterProgram.addAttribute("inputTextureCoordinate3");
    }

    public void disableThirdFrameCheck() {
        this.thirdFrameCheckDisabled = true;
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

        GLES20.glVertexAttribPointer(filterThirdTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(filterThirdTextureCoordinateAttribute);

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
        if (thirdInputFrameBuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, thirdInputFrameBuffer.getTexture());
            GLES20.glUniform1i(filterInputTextureUniform3, 4);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        mFirstInputFramebuffer.unlock();
        secondInputFramebuffer.unlock();
        thirdInputFrameBuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(filterThirdTextureCoordinateAttribute);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public int nextAvailableTextureIndex() {
        if (hasSetSecondTexture) {
            return 2;
        } else if (hasSetFirstTexture) {
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
        } else if (textureIndex == 1) {
            secondInputFramebuffer = newInputFramebuffer;
            hasSetSecondTexture = true;
            if (secondInputFramebuffer != null) {
                secondInputFramebuffer.lock();
            }
        } else {
            thirdInputFrameBuffer = newInputFramebuffer;
            if (thirdInputFrameBuffer != null) {
                thirdInputFrameBuffer.lock();
            }
        }
    }

    @Override
    public void setInputSize(GPUSize newSize, int index) {
        if (index == 0) {
            super.setInputSize(newSize, index);
            if (newSize.width == 0 && newSize.height == 0) {
                hasSetFirstTexture = false;
            }
        } else if (index == 1) {
            if (newSize.width == 0 && newSize.height == 0) {
                hasSetSecondTexture = false;
            }
        }
    }

    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, Constants.NS_MUTIPLE);
        if (hasReceivedFirstFrame && hasReceivedSecondFrame && hasReceivedThirdFrame) {
            return;
        }
        boolean updatedMovieFrameOppositeStillImage = false;
        if (textureIndex == 0) {
            hasReceivedFirstFrame = true;
            if (secondFrameCheckDisabled) {
                hasReceivedSecondFrame = true;
            }
            if (thirdFrameCheckDisabled) {
                hasReceivedThirdFrame = true;
            }
        } else if (textureIndex == 1) {
            hasReceivedSecondFrame = true;
            if (firstFrameCheckDisabled) {
                hasReceivedFirstFrame = true;
            }
            if (thirdFrameCheckDisabled) {
                hasReceivedThirdFrame = true;
            }
        } else {
            hasReceivedThirdFrame = true;
            if (firstFrameCheckDisabled) {
                hasReceivedFirstFrame = true;
            }
            if (secondFrameCheckDisabled) {
                hasReceivedSecondFrame = true;
            }
        }
        if ((hasReceivedFirstFrame && hasReceivedSecondFrame && hasReceivedThirdFrame)) {
            renderToTextureWithVertices(GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices), textureCoordinatesForRotation(GPUImageRotationMode.kGPUImageNoRotation), frameTime);
            informTargetsAboutNewFrameAtTime(frameTime);
            hasReceivedFirstFrame = false;
            hasReceivedSecondFrame = false;
            hasReceivedThirdFrame = false;
        }
    }

    @Override
    public void removeAllTargets() {
        super.removeAllTargets();
        hasReceivedThirdFrame = false;
        hasReceivedSecondFrame = false;
        hasReceivedFirstFrame = false;
    }
}
