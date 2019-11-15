package com.zp.libvideoedit.GPUImage.Core;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

/**
 * Created by gwd on 2018/7/2.
 */

public class LoadPicture extends GPUImageFilter {
    private Bitmap bitmap;
    private int bitMapTexture;

    public LoadPicture(Bitmap bitmap) {
        super();
        this.bitmap = bitmap;
    }

    @Override
    public void init() {
        super.init();
        int[] texutreIds = new int[1];
        GLES20.glGenTextures(1, texutreIds, 0);
        bitMapTexture = texutreIds[0];
    }

    public void onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 1);
        mFilterProgram.use();
        GLES20.glViewport(0, 0, bitmap.getWidth(), bitmap.getHeight());
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (bitmap != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitMapTexture);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }


}
