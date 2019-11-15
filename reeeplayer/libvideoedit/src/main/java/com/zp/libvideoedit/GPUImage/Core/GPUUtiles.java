package com.zp.libvideoedit.GPUImage.Core;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gwd on 2018/2/8.
 */

public class GPUUtiles {
    public static int NO_TEXTURE = -1 ;
    public static FloatBuffer directFloatBufferFromFloatArray(float []data) {
        FloatBuffer buffer = null;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);

        byteBuffer.order(ByteOrder.nativeOrder());

        buffer = byteBuffer.asFloatBuffer();
        buffer.put(data);
        buffer.position(0);

        return buffer;
    }

    public static void saveBitmap(String picPath, Bitmap bitmap) {
        File f = new File(picPath);
        if (f.exists()) {
            f.delete();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getExternalOESTextureID(){
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public static FloatBuffer textureCoordinatesForRotation(GPUImageRotationMode rotationMode) {
        switch(rotationMode)
        {
            case kGPUImageNoRotation: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates);
            case kGPUImageRotateLeft: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateLeftTextureCoordinates);
            case kGPUImageRotateRight: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightTextureCoordinates);
            case kGPUImageFlipVertical: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.verticalFlipTextureCoordinates);
            case kGPUImageFlipHorizonal: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.horizontalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipVertical: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightVerticalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipHorizontal: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightHorizontalFlipTextureCoordinates);
            case kGPUImageRotate180: return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotate180TextureCoordinates);
        }
        return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates);
    }

//    CG_INLINE CGSize
//    __CGSizeApplyAffineTransform(CGSize size, CGAffineTransform t)
//    {
//        CGSize s;
//        s.width = (CGFloat)((double)t.a * size.width + (double)t.c * size.height);
//        s.height = (CGFloat)((double)t.b * size.width + (double)t.d * size.height);
//        return s;
//    }

}
