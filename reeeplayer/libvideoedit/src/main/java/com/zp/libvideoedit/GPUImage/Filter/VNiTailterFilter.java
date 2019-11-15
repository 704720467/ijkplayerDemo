package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/7/11.
 */

public class VNiTailterFilter extends GPUImageTwoInput {

    private int alphaSlot;
    private int scaleSlot;


    public VNiTailterFilter() {
        this(VNiTailterFilterFragment);
    }

    public VNiTailterFilter(String fragmentShader) {
        super(fragmentShader);
    }

    @Override
    public void init() {
        super.init();
        alphaSlot = mFilterProgram.uniformIndex("alphavalue");
        scaleSlot = mFilterProgram.uniformIndex("scaleValue");
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glVertexAttribPointer(mfilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        setTextureDefaultConfig();
//        GLES20.glUniform1f(alphaSlot, calcAlpha());
//        GLES20.glUniform1f(scaleSlot, calcScale());
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

    private float calcAlpha() {

        return 0;
    }

    private float calcScale() {
        return 0;
    }


    private static final String VNiTailterFilterFragment =
            " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    "\n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " \n" +
                    " uniform lowp float alphavalue;\n" +
                    " uniform lowp float scaleValue;\n" +

                    "\n" +
                    " void main()\n" +
                    " {\n" +
                    "\t lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "\t lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "\t \n" +
                    "\t gl_FragColor = vec4(mix(textureColor.rgb, textureColor2.rgb, textureColor2.a), textureColor.a);\n" +

                    " }";

}
