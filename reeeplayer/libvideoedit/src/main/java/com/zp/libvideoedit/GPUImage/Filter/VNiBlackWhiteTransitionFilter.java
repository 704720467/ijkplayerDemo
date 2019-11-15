package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageThreeInputFilter;
import com.zp.libvideoedit.Time.CMTime;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/6/7.
 */

public class VNiBlackWhiteTransitionFilter extends GPUImageThreeInputFilter {
    public VNiBlackWhiteTransitionFilter(String fragmentShader) {
        super(fragmentShader);
    }

    public VNiBlackWhiteTransitionFilter() {
        this(kVNIBlackWhiteTransitionFilterFragmentShaderString);
    }


    @Override
    public void init() {
        super.init();
        halfFlagSlot = mFilterProgram.uniformIndex("halfTime");
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);

        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);

        GLES20.glVertexAttribPointer(mfilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);

        GLES20.glVertexAttribPointer(filterThirdTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(filterThirdTextureCoordinateAttribute);
        int halfTime = checkHalfTime();
        GLES20.glUniform1i(halfFlagSlot, halfTime);
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

    private int checkHalfTime() {
        float duration = (float) CMTime.getSecond(CMTime.subTime(transitionEnd, transitionStart));
        int flag = 0;
        if (CMTime.getSecond(currentTime) > CMTime.getSecond(transitionStart) && CMTime.getSecond(currentTime) - CMTime.getSecond(transitionStart) < duration / 2.f) {
            flag = 0;
        } else if (CMTime.getSecond(currentTime) > CMTime.getSecond(transitionStart) && CMTime.getSecond(currentTime) - CMTime.getSecond(transitionStart) > duration / 2.f) {
            flag = 1;
        }
        return flag;
    }

    private int halfFlagSlot;
    private static final String kVNIBlackWhiteTransitionFilterFragmentShaderString =
            "uniform lowp int halfTime;\n" +
                    " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    " varying highp vec2 textureCoordinate3;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " uniform sampler2D inputImageTexture3;\n" +
                    " void main()\n" +
                    " {\n" +
                    "     highp vec4 color  = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "     highp vec4 color2  = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "     highp vec4 color3  = texture2D(inputImageTexture3, textureCoordinate3);\n" +
                    "     if(halfTime ==0)\n" +
                    "     {\n" +
                    "         gl_FragColor = color*color3.r;\n" +
                    "     }else\n" +
                    "     {\n" +
                    "         gl_FragColor = color2*color3.r;\n" +
                    "     }\n" +
                    "     \n" +
                    " }";
}
