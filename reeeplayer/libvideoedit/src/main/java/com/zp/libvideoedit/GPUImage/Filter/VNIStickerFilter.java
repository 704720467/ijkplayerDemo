package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;
import android.renderscript.Matrix4f;
import android.util.Log;


import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageRotationMode;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;
import com.zp.libvideoedit.modle.StickerConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 贴纸
 * Created by zp on 2019/5/6.
 */

public class VNIStickerFilter extends GPUImageTwoInput {
    protected GPURect mSecondViewPoint;
    private StickerConfig stickerConfig;
    private GPUSize bitMapSize;

    protected static final String AlphaBlendFilterVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform mat4 positionMatrix;\n" +
            "uniform mat4 textureCoordinateMatrix;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = positionMatrix*position;\n" +
            "    textureCoordinate = (textureCoordinateMatrix * inputTextureCoordinate).xy;\n" +
            "}";

    private static final String AlphaBlendFilterFragment =
            " varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " \n" +
                    " uniform lowp float mixturePercent;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    " }";

    private int mixturePercentSlot;
    private float mixturePercent;
    private int positionMatrix;
    private int textureCoordinateMatrix;
    private int textureCoordinate2Matrix;
    private float[] positionMatrixArray;
    private float[] textureCoordinateMatrixArray;
    private float[] textureCoordinate2MatrixArray;
    private FloatBuffer mFilterPositionAttributeBuffer = null;

    protected boolean filterInited;


    public VNIStickerFilter() {
        super(AlphaBlendFilterVertexShaderString, AlphaBlendFilterFragment);
//        textureCoordinate2MatrixArray = new float[9];
//        Matrix matrix3f = new Matrix();
//        matrix3f.setTranslate(0.5f, 0.5f);
//        matrix3f.postScale(1f, 4f);
//        matrix3f.postRotate(90);
//        matrix3f.getValues(textureCoordinate2MatrixArray);
//        Matrix3f matrix3f = new Matrix3f();
//        matrix3f.translate(0, 100000f);
//        matrix3f.scale(1f, 4f);
//        textureCoordinate2MatrixArray = matrix3f.getArray();
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
//        matrix4f.translate(0.5f, 0.5f, 0f);
//        matrix4f.scale(1f, 4f, 1f);
//        matrix4f.rotate(-90, 0f, 0f, 1f);
//
//        matrix4f.translate(-0.75f, 0.125f, 0f);
        positionMatrixArray = matrix4f.getArray();
        textureCoordinateMatrixArray = matrix4f.getArray();
        textureCoordinate2MatrixArray = matrix4f.getArray();


    }

    @Override
    public void init() {
        if (!filterInited) {
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
            mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
            GPUImageContext.setActiveShaderProgram(mFilterProgram);
            GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
            GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);

            positionMatrix = mFilterProgram.uniformIndex("positionMatrix");
            setUniformMatrix4f(positionMatrix, positionMatrixArray);
            textureCoordinateMatrix = mFilterProgram.uniformIndex("textureCoordinateMatrix");
            setUniformMatrix4f(textureCoordinateMatrix, textureCoordinateMatrixArray);
            filterInited = true;
        } else {
            Log.e("VNIStickerFilter ", "VNIStickerFilter inited!");
        }
    }


    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {

        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);


        if (mFirstInputFramebuffer != null) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.loadIdentity();
            positionMatrixArray = matrix4f.getArray();
            setUniformMatrix4f(positionMatrix, positionMatrixArray);
            setTextureDefaultConfig();

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
            if (Constants.VERBOSE_GL)
                Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        initDrowData(stickerConfig);
        if (secondInputFramebuffer != null && mSecondViewPoint != null) {
            GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mFilterPositionAttributeBuffer);
            GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
            GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates));
            GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
            setUniformMatrix4f(positionMatrix, positionMatrixArray);
            setTextureDefaultConfig();
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glViewport(
                    (int) (mSecondViewPoint.getX()),
                    (int) (mSecondViewPoint.getY()),
                    (int) mSecondViewPoint.getWidth(), (int) mSecondViewPoint.getHeight());

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, secondInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);

            if (Constants.VERBOSE_GL)
                Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES20.glFlush();
        if (Constants.VERBOSE_GL)
            Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",first: " + mFirstInputFramebuffer + " sec: " + secondInputFramebuffer);
        if (mFirstInputFramebuffer != null)
            mFirstInputFramebuffer.unlock();
        if (secondInputFramebuffer != null)
            secondInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glDisableVertexAttribArray(mfilterSecondTextureCoordinateAttribute);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setViewPoint(GPURect viewPoint) {
        this.mSecondViewPoint = viewPoint;
    }

    /**
     * 初始化绘制数据
     *
     * @param stickerConfig
     */
    public void initDrowData(StickerConfig stickerConfig) {
        if (stickerConfig == null) {
            mSecondViewPoint = null;
            return;
        }
        calculatePositionAttributeBuffer(stickerConfig);
        calculateSecondViewPoint(stickerConfig);
        calculatePositionMatrixArray(stickerConfig);
    }

    private void calculateSecondViewPoint(StickerConfig stickerConfig) {
        float differenceX = (stickerConfig.getRight() - stickerConfig.getLeft()) * sizeOfFBO().width;
        float differenceY = (stickerConfig.getBottom() - stickerConfig.getTop()) * sizeOfFBO().height;
        float width = (float) Math.sqrt(differenceX * differenceX + differenceY * differenceY);
        width = width * stickerConfig.getScale();

        float x = (stickerConfig.getLeft() + stickerConfig.getRight()) / 2 * sizeOfFBO().width;
        float y = sizeOfFBO().height - (stickerConfig.getTop() + stickerConfig.getBottom()) / 2 * sizeOfFBO().height;
        float newX = x - (width) / 2;
        float newY = y - (width) / 2;
        mSecondViewPoint = new GPURect(newX, newY, width, width);
    }

    private void calculatePositionMatrixArray(StickerConfig stickerConfig) {
        Matrix4f matrix4f1 = new Matrix4f();
        matrix4f1.loadIdentity();
        matrix4f1.rotate(-stickerConfig.getRotationAngle(), 0, 0, 1f);
        positionMatrixArray = matrix4f1.getArray();
    }

    public void setStickerConfig(StickerConfig stickerConfig, GPUSize bitMapSize) {
        this.stickerConfig = stickerConfig;
        this.bitMapSize = bitMapSize;
    }

    /**
     * 计算顶点坐标
     */
    public void calculatePositionAttributeBuffer(StickerConfig stickerConfig) {
        float differenceMaxX = (stickerConfig.getRight() - stickerConfig.getLeft()) * sizeOfFBO().width;
        float differenceMaxY = (stickerConfig.getBottom() - stickerConfig.getTop()) * sizeOfFBO().height;
        float outerWidth = (float) Math.sqrt(differenceMaxX * differenceMaxX + differenceMaxY * differenceMaxY);
        float adjustWidth = differenceMaxX;
        float adjustHeight = differenceMaxY;

        //真实图片的宽高大于给定的最大尺寸，需要调整比例
//        if (bitMapSize.width > differenceMaxX || bitMapSize.height > differenceMaxY) {
//            float ratio = Math.max(bitMapSize.width / differenceMaxX, bitMapSize.height / differenceMaxY);
//            adjustWidth = bitMapSize.width / ratio;
//            adjustHeight = bitMapSize.height / ratio;
//        }

        float ratioWidth = outerWidth / adjustWidth;
        float ratioHeight = outerWidth / adjustHeight;
        float[] cube = new float[]{
                GPUImageTextureCoordinates.squareVertices[0] / ratioWidth, GPUImageTextureCoordinates.squareVertices[1] / ratioHeight,
                GPUImageTextureCoordinates.squareVertices[2] / ratioWidth, GPUImageTextureCoordinates.squareVertices[3] / ratioHeight,
                GPUImageTextureCoordinates.squareVertices[4] / ratioWidth, GPUImageTextureCoordinates.squareVertices[5] / ratioHeight,
                GPUImageTextureCoordinates.squareVertices[6] / ratioWidth, GPUImageTextureCoordinates.squareVertices[7] / ratioHeight
        };
        if (mFilterPositionAttributeBuffer == null) {
            mFilterPositionAttributeBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mFilterPositionAttributeBuffer.clear();
        mFilterPositionAttributeBuffer.put(cube).position(0);
    }

    public void setFilterInited(boolean filterInited) {
        this.filterInited = filterInited;
    }
}
