package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;
import android.renderscript.Matrix4f;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/7/11.
 */

public class VNiTailerBlendFilter extends GPUImageTwoInput {

    private int alphaSlot;
    private int blureSlot;
    private int bgAlphaSlot;
    private float startTime;
    private float alphaduration;
    private float bgAlphaStartTime;
    private float bgAlphaDuration;
    private int transformMatrixSlot;
    private Matrix4f transformMatrix;
    private float transformDuration;


    public VNiTailerBlendFilter() {
        this(VNiTailerBlendFilterMattingFragmentShaderStringss);
        transformMatrix = new Matrix4f();
        transformMatrix.loadIdentity();
    }

    public VNiTailerBlendFilter(String fragmentShader) {
        super(fragmentShader);
    }

    @Override
    public void init() {
        super.init();
        blureSlot = mFilterProgram.uniformIndex("blurevalue");
        alphaSlot = mFilterProgram.uniformIndex("alphavalue");
        bgAlphaSlot = mFilterProgram.uniformIndex("bgalphaValue");
        transformMatrixSlot = mFilterProgram.uniformIndex("transformMatrix");
        mFilterProgram.use();
        GLES20.glUniform1f(blureSlot, 1.0f);
        GLES20.glUniform1f(alphaSlot, 1.0f);
        GLES20.glUniform1f(bgAlphaSlot, 0.f);
        GLES20.glUniformMatrix4fv(transformMatrixSlot, 1, false, transformMatrix.getArray(), 0);

    }


    private float calcAlpha() {
        if (currentTime.getSecond() - startTime < 0) return 1.f;
        if (currentTime.getSecond() - startTime <= alphaduration) {
            return (float) ((-(1.0f / 0.4f)) * (currentTime.getSecond() - startTime) + 1.f);
        }
        return 0.f;
    }

    private float calcblure() {
        if (currentTime.getSecond() - startTime < 0) return 1.f;
        if (currentTime.getSecond() - startTime <= alphaduration) {
            return (float) ((-(1.0f / alphaduration)) * (currentTime.getSecond() - startTime) + 1.f);
        }
        return 0;
    }

    private float calcBgAlpha() {
        if (currentTime.getSecond() - startTime < 0) return 0.f;
        if (currentTime.getSecond() - startTime <= bgAlphaDuration) {
            return (1.0f / bgAlphaDuration) * ((float) currentTime.getSecond() - startTime);
        }
        return 1.f;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
        alphaduration = 0.4f;
        bgAlphaStartTime = startTime;
        bgAlphaDuration = 0.8f;
        transformDuration = 3.0f;

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
        GLES20.glUniform1f(alphaSlot, calcAlpha());
        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "VNiTailerBlendFilter_calcAlpha_alpha:  " + calcAlpha());
        GLES20.glUniform1f(blureSlot, calcblure());
        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "VNiTailerBlendFilter_calcblure_blure :  " + calcblure());
        GLES20.glUniform1f(bgAlphaSlot, calcBgAlpha());
        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "VNiTailerBlendFilter_calcBgAlpha_alpha:  " + calcBgAlpha());
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


    protected static final String VNiTailerBlendFilterMattingFragmentShaderStringss =
            " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " uniform lowp float alphavalue;\n" +
                    " uniform lowp float bgalphaValue;\n" +
                    " uniform lowp float blurevalue;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate)*blurevalue; \n" +
                    "     lowp vec4 videoColor = vec4(textureColor.r,textureColor.g,textureColor.b,alphavalue); \n" +

                    "     lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "     lowp vec4 bgTextureColor = vec4(textureColor2.r,textureColor2.g,textureColor2.b,bgalphaValue); \n" +

//                    "     lowp vec4 textureColor3 = texture2D(inputImageTexture3, textureCoordinate3);\n" +
//                    "     lowp vec4 textColor = vec4(textureColor3.r,textureColor3.g,textureColor3.b ,textureColor3.a*(1.0-alphavalue));\n" +

                    "\t   lowp vec4 secondLayer = vec4(mix(textureColor.rgb, bgTextureColor.rgb, bgTextureColor.a ), (1.0-alphavalue));\n" +

//
//                    "\t   lowp vec4 Color = vec4(mix(secondLayer.rgb, textColor.rgb, textColor.a ), 1.0);\n" +

                    "     gl_FragColor = secondLayer ;\n" +

                    " }";
}