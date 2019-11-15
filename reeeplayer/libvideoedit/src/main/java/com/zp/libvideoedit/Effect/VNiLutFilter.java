package com.zp.libvideoedit.Effect;

import android.opengl.GLES20;


import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/4/26.
 */

public class VNiLutFilter extends GPUImageTwoInput {
    private static String kGPUImageLookup2FragmentShaderString = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " uniform highp float intensity;\n" +
            " uniform highp float gridsize;\n" +
            " uniform highp float texwidth;\n" +
            "\n" +
            " void main()\n" +
            "{\n" +
            "    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    highp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate);\n" +

            "    highp float gridcount = texwidth / gridsize;\n" +
            "    highp float slice = textureColor.b * (gridsize - 1.0);\n" +
            "    highp float islice0 = floor(slice);\n" +
            "    highp float islice1 = min(gridcount - 1.0, islice0 + 1.0);\n" +
            "    highp float fslice = fract(slice);\n" +
            "    highp float x = textureColor.r * (gridsize - 1.0);\n" +
            "    highp float x1 = x + islice0*gridsize + 0.5;\n" +
            "    highp float x2 = x + islice1*gridsize + 0.5;\n" +
            "    highp float y = textureColor.g * (gridsize - 1.0) + 0.5;\n" +
            "    highp vec2 texPos1 = vec2(x1 / texwidth, y/gridcount);\n" +
            "    highp vec2 texPos2 = vec2(x2 / texwidth, y/gridcount);\n" +
            "    highp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n" +
            "    highp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n" +
            "    highp vec4 newColor = mix(newColor1, newColor2, fslice);\n" +
            "    highp vec4 minColor = textureColor;\n" +
            "    newColor = mix(minColor, newColor, intensity);\n" +
            "    gl_FragColor = newColor;\n" +
            "}";
    protected int intensityUniform;
    protected int gridsizeUniform;
    protected int texwidthUniform;
    protected float intensity;
    protected float texwidth;
    protected float gridsize;


    public VNiLutFilter() {
        super(kGPUImageLookup2FragmentShaderString);
    }

    @Override
    public void init() {
        super.init();
        intensityUniform = mFilterProgram.uniformIndex("intensity");
        gridsizeUniform = mFilterProgram.uniformIndex("gridsize");
        texwidthUniform = mFilterProgram.uniformIndex("texwidth");
        GLES20.glUseProgram(mFilterProgram.getmProgram());
        GLES20.glUniform1f(intensityUniform, 1.0f);
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {

        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(),false);

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
        mFirstInputFramebuffer.unlock();
        secondInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
        setFloat(intensityUniform, this.intensity);
    }

    public void setTexwidth(float texwidth) {
        this.texwidth = texwidth;
        setFloat(texwidthUniform, this.texwidth);
    }

    public void setGridsize(float gridsize) {
        this.gridsize = gridsize;
        setFloat(gridsizeUniform, this.gridsize);

    }
}
