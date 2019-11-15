package com.zp.libvideoedit.GPUImage.Core;

import android.opengl.GLES20;

/**
 * Created by gwd on 2018/2/7.
 */

public class GPUTextureOptions {
    /**
     * opengl 纹理 属性

     */
    private int minFilter;
    private int magFilter;
    private int wrapS;
    private int wrapT;
    private int internalFormat;
    private int format;
    private int type ;


    public  GPUTextureOptions(){
        this.minFilter = GLES20.GL_LINEAR;
        this.magFilter = GLES20.GL_LINEAR;
        this.wrapS = GLES20.GL_CLAMP_TO_EDGE;
        this.wrapT = GLES20.GL_CLAMP_TO_EDGE;
        this.internalFormat = GLES20.GL_RGBA;
        this.format = GLES20.GL_RGBA;
        this.type = GLES20.GL_UNSIGNED_BYTE;


    }

    public GPUTextureOptions(int minFilter, int magFilter, int wrapS, int wrapT, int internalFormat, int format, int type) {
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.wrapS = wrapS;
        this.wrapT = wrapT;
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
    }

    public int getMinFilter() {
        return minFilter;
    }

    public int getMagFilter() {
        return magFilter;
    }

    public int getWrapS() {
        return wrapS;
    }

    public int getWrapT() {
        return wrapT;
    }

    public int getInternalFormat() {
        return internalFormat;
    }

    public int getFormat() {
        return format;
    }

    public int getType() {
        return type;
    }

    public void setMinFilter(int minFilter) {
        this.minFilter = minFilter;
    }

    public void setMagFilter(int magFilter) {
        this.magFilter = magFilter;
    }

    public void setWrapS(int wrapS) {
        this.wrapS = wrapS;
    }

    public void setWrapT(int wrapT) {
        this.wrapT = wrapT;
    }

    public void setInternalFormat(int internalFormat) {
        this.internalFormat = internalFormat;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public void setType(int type) {
        this.type = type;
    }
}
