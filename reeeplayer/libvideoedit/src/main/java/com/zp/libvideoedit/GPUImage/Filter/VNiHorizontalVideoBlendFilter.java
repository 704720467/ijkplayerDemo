package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;
import com.zp.libvideoedit.Time.CMTime;

import java.nio.FloatBuffer;

/**
 * Created by dapian on 2019/2/25.
 */

public class VNiHorizontalVideoBlendFilter extends GPUImageTwoInput {

    public VNiHorizontalVideoBlendFilter(String fragmentShader) {
        super(fragmentShader);
    }

    public VNiHorizontalVideoBlendFilter() {
        super(VNiHorizontalVideoBlendFilterFragmentShaderStringss);
    }

    @Override
    public void init() {
        super.init();
        maskAlphaUniform = mFilterProgram.uniformIndex("maskAlpha");
        GLES20.glUniform1f(maskAlphaUniform, 1.0f);
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        GPUSize size = sizeOfFBO();
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


    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        if (EditConstants.VERBOSE_GL)
            Log.d("GPUImageTwoInput", "newFrameReadyAtTime  ,textureIndex:  " + textureIndex);
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, EditConstants.NS_MUTIPLE);
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
            hasReceivedSecondFrame = false;
            hasReceivedFirstFrame = false;
            renderAtTime(frameTime, textureIndex);

        }
    }


    public void renderAtTime(long frameTime, int textureIndex) {
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, EditConstants.NS_MUTIPLE);
        this.renderToTextureWithVertices(GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices), GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates), frameTime);
        this.informTargetsAboutNewFrameAtTime(frameTime);
    }

    @Override
    public void removeAllTargets() {
        super.removeAllTargets();
        hasReceivedSecondFrame = false;
        hasReceivedFirstFrame = false;
    }


    protected static final String VNiHorizontalVideoBlendFilterFragmentShaderStringss =
            " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    " varying highp vec2 textureCoordinate3;\n" +

                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " uniform sampler2D inputImageTexture3;\n" +

                    " uniform lowp float maskAlpha;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "    highp vec2 textureCoordinate2Tmp = vec2(textureCoordinate2.x,textureCoordinate2.y/2.0) ;\n" +

                    "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate); //素材视频\n" +
                    "     lowp vec4 textureColor3 = texture2D(inputImageTexture2, textureCoordinate2Tmp);//原来mask视频\n" +
                    "     lowp vec4 textureColor2 = texture2D(inputImageTexture2, vec2(textureCoordinate2Tmp.x,textureCoordinate2Tmp.y+0.5));//黑白视频\n" +
                    "     lowp vec4 tmpColor = vec4(\n" +
                    "                               (textureColor3.r==0.0?0.0:min((textureColor2.r/textureColor3.r), 1.0)),\n" +
                    "                               (textureColor3.g==0.0?0.0:min((textureColor2.g/textureColor3.r), 1.0)),\n" +
                    "                               (textureColor3.b==0.0?0.0:min((textureColor2.b/textureColor3.r), 1.0)),\n" +
                    "                               textureColor3.r );\n" +
                    "     gl_FragColor = mix(textureColor, tmpColor, tmpColor.a) ;\n" +
                    " }";
    private int maskAlphaUniform;

}
