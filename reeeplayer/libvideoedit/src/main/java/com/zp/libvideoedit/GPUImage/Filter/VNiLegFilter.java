package com.zp.libvideoedit.GPUImage.Filter;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.Carma.Core.GPUSurfaceBaseView;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by gwd on 2018/5/15.
 */

public class VNiLegFilter extends GPUImageFilter {
    private FloatBuffer vertextBuffer;
    private FloatBuffer textureBuffer;
    private boolean mNeedGetPic;
    private GPUSurfaceBaseView.CreatePicCallBack mCallBack;

    public VNiLegFilter() {
        super(kGPUImageVertexShaderString, kVNiPinchDistortionFragmentShaderString);
        vertextBuffer = GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices);
        textureBuffer = GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates);
    }

    @Override
    public void init() {
        super.init();
        scaleUniform = mFilterProgram.uniformIndex("scale");
        centerUniform = mFilterProgram.uniformIndex("center");
        rotateUniform = mFilterProgram.uniformIndex("rotate");
        setScaleValue(0.0f);
        setCenterValue(0.4f);
        setRotateVaule(1.f);
    }

    public void setScaleValue(float scaleValue) {
        this.scaleValue = scaleValue;
    }

    public void setCenterValue(float centerValue) {
        this.centerValue = centerValue;
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

        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinaes);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        GLES20.glUniform1f(rotateUniform, rotateVaule);
        GLES20.glUniform1f(scaleUniform, scaleValue);
        GLES20.glUniform1f(centerUniform, centerValue);
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        mFirstInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    public int renderToTextureWithVertices(int textureId) {

        if (mOutputFramebuffer == null) {
            mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(),false);
        }
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return -1;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertextBuffer);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        GLES20.glUniform1f(rotateUniform, rotateVaule);
        GLES20.glUniform1f(scaleUniform, scaleValue);
        GLES20.glUniform1f(centerUniform, centerValue);
        if (textureId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        if (mNeedGetPic && mCallBack != null) {
            mNeedGetPic = false;
            mOutputFramebuffer.activeFramebuffer();
            IntBuffer ib = IntBuffer.allocate(sizeOfFBO().width * sizeOfFBO().height * 4);
            GLES20.glReadPixels(0, 0, sizeOfFBO().width, sizeOfFBO().height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
            Bitmap result = Bitmap.createBitmap(sizeOfFBO().width, sizeOfFBO().height, Bitmap.Config.ARGB_8888);
            result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
            mCallBack.complete(result);
        }
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mOutputFramebuffer.getTexture();
    }

    public void getCurrentPic(boolean needGetPic, GPUSurfaceBaseView.CreatePicCallBack callBack) {
        this.mNeedGetPic = needGetPic;
        this.mCallBack = callBack;
    }


    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        mFirstInputFramebuffer = newInputFramebuffer;
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.lock();
        }
    }

    public void release() {
        if (mOutputFramebuffer != null) {
            mOutputFramebuffer.destoryFramebuffer();
            mOutputFramebuffer = null;
        }

    }

    public void setRotateVaule(float rotateVaule) {
        this.rotateVaule = rotateVaule;
    }

    private static final String kVNiPinchDistortionFragmentShaderString =
            " varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " \n" +
                    " uniform highp float scale;\n" +
                    " uniform highp float center;\n" +
                    " uniform highp float rotate;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     if(scale==0.0) {\n" +
                    "         gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "     } else {\n" +
                    "         if(rotate!=0.0){\n" +
                    "             if(textureCoordinate.y>center) {\n" +
                    "                 highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * (0.85+0.15*(1.0-scale)) + center - center * (0.85+0.15*(1.0-scale))));\n" +
                    "                 gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );\n" +
                    "             } else {\n" +
                    "                 highp vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * (0.96+0.04*(1.0-scale)) + center - center * (0.96+0.04*(1.0-scale))));\n" +
                    "                 gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );\n" +
                    "             }\n" +
                    "         } else {\n" +
                    "             if((textureCoordinate.x<center&&rotate==1.0)||(textureCoordinate.x>center&&rotate==-1.0)) {\n" +
                    "                 highp vec2 textureCoordinateToUse = vec2((textureCoordinate.x * (0.85+0.15*(1.0-scale)) + center - center * (0.85+0.15*(1.0-scale))), textureCoordinate.y);\n" +
                    "                 gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );\n" +
                    "             } else {\n" +
                    "                 highp vec2 textureCoordinateToUse = vec2((textureCoordinate.x * (0.96+0.04*(1.0-scale)) + center - center * (0.96+0.04*(1.0-scale))), textureCoordinate.y);\n" +
                    "                 gl_FragColor = texture2D(inputImageTexture, textureCoordinateToUse );\n" +
                    "             }\n" +
                    "         }\n" +
                    "     }\n" +
                    " }";
    private int scaleUniform = -1;
    private int centerUniform = -1;
    private int rotateUniform = -1;
    private float scaleValue;
    private float centerValue;
    private float rotateVaule;

}
