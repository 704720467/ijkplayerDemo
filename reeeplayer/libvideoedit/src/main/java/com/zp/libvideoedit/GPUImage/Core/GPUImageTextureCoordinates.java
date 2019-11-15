package com.zp.libvideoedit.GPUImage.Core;

import android.opengl.GLES20;

/**
 * Created by gwd on 2018/2/8.
 */

public class GPUImageTextureCoordinates {
    public static float noRotationTextureCoordinates[] = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,};

    public static float rotateLeftTextureCoordinates[] = {1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 0.0f, 1.0f,};

    public static float rotateRightTextureCoordinates[] = {0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f,};

    public static float verticalFlipTextureCoordinates[] = {0.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};

    public static float horizontalFlipTextureCoordinates[] = {1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};

    public static float rotateRightVerticalFlipTextureCoordinates[] = {0.0f,
            0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,};

    public static float rotateRightHorizontalFlipTextureCoordinates[] = {1.0f,
            1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,};

    public static float rotate180TextureCoordinates[] = {1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 0.0f, 0.0f,};

    public static float imageVertices[] = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, 1.0f,};

    public static float squareVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,};
    public static float textureCoordinates[] = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f,};


    public static final int GL_BGRA = GLES20.GL_RGBA;
}
