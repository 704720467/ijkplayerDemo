package com.zp.libvideoedit.GPUImage.FilterCore;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.zp.libvideoedit.GPUImage.Carma.Core.GPUSurfaceCameraView;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by gwd on 2018/4/9.
 */

public class GPUImageTransformFilter extends GPUImageFilter {
    public static final String TRANSFORM_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            " attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            " uniform mat4 transformMatrix;\n" +
            " uniform mat4 orthographicMatrix;\n" +
            " \n" +
            " varying vec2 textureCoordinate;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     gl_Position = transformMatrix * vec4(position.xyz, 1.0) * orthographicMatrix;\n" +
            "     textureCoordinate = inputTextureCoordinate.xy;\n" +
            " }";

    private int transformMatrixUniform;
    private int orthographicMatrixUniform;
    private float[] orthographicMatrix;

    private float[] transform3D;
    private boolean ignoreAspectRatio;
    private boolean anchorTopLeft;
    private boolean isTakeePhto;
    private GPUSurfaceCameraView.TakePhtoListener takePhtoListener;

    public GPUImageTransformFilter() {
        super(TRANSFORM_VERTEX_SHADER, kGPUImagePassthroughFragmentShaderString);

        orthographicMatrix = new float[16];
        Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

        transform3D = new float[16];
        Matrix.setIdentityM(transform3D, 0);
    }

    @Override
    public void init() {
        super.init();
        transformMatrixUniform = mFilterProgram.uniformIndex("transformMatrix");
        orthographicMatrixUniform = mFilterProgram.uniformIndex("orthographicMatrix");
        setUniformMatrix4f(transformMatrixUniform, transform3D);
        setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix);
    }

    @Override
    public void setInputSize(GPUSize newSize, int index) {
        super.setInputSize(newSize, index);
        mInputTextureSize = newSize;
        if (!ignoreAspectRatio) {
            Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f * (float) newSize.height / (float) newSize.width, 1.0f * (float) newSize.height / (float) newSize.width, -1.0f, 1.0f);
            setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix);
        }
    }

    public void setTransform3D(float[] transform3D) {
        this.transform3D = transform3D;
        setUniformMatrix4f(transformMatrixUniform, transform3D);
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
        setUniformMatrix4f(transformMatrixUniform, transform3D);
        setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix);
        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        if (isTakeePhto) {
            GLES20.glFlush();
            mOutputFramebuffer.activeFramebuffer();
            IntBuffer ib = IntBuffer.allocate(sizeOfFBO().width * sizeOfFBO().height * 4);
            GLES20.glReadPixels(0, 0, sizeOfFBO().width, sizeOfFBO().height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
            Bitmap result = Bitmap.createBitmap(sizeOfFBO().width, sizeOfFBO().height, Bitmap.Config.ARGB_8888);
            result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
            if (result != null) {
                if (takePhtoListener != null) takePhtoListener.takePhtoComplete(result);
            }
            isTakeePhto = false;
        }
        mFirstInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void createCurrentBitmap(GPUSurfaceCameraView.TakePhtoListener takePhtoListener) {
        isTakeePhto = true;
        this.takePhtoListener = takePhtoListener;
    }


    public float[] getTransform3D() {
        return transform3D;
    }

    public void setIgnoreAspectRatio(boolean ignoreAspectRatio) {
        this.ignoreAspectRatio = ignoreAspectRatio;

        if (ignoreAspectRatio) {
            Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
            setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix);
        } else {
            if (!ignoreAspectRatio) {
                Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f * (float) mInputTextureSize.height / (float) mInputTextureSize.width, 1.0f * (float) mInputTextureSize.height / mInputTextureSize.width, -1.0f, 1.0f);
                setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix);
            }
        }
    }


    public boolean ignoreAspectRatio() {
        return ignoreAspectRatio;
    }

    public void setAnchorTopLeft(boolean anchorTopLeft) {
        this.anchorTopLeft = anchorTopLeft;
        setIgnoreAspectRatio(ignoreAspectRatio);
    }

    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        mFirstInputFramebuffer = newInputFramebuffer;
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.lock();
        }
    }

    @Override
    public void setupFilterForSize(GPUSize filterFrameSize) {
        super.setupFilterForSize(filterFrameSize);
    }

    public boolean anchorTopLeft() {
        return anchorTopLeft;
    }

}
