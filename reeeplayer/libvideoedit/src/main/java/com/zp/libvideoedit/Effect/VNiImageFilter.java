package com.zp.libvideoedit.Effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.AndroidResourceManager;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
import com.zp.libvideoedit.modle.FilterModel;
import com.zp.libvideoedit.utils.BitmapUtil;
import com.zp.libvideoedit.utils.LookupInstance;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by gwd on 2018/4/26.
 */

public class VNiImageFilter extends VNiLutFilter {
    private float levelValue = 0;
    private GPUImagePicture lookupImageSource;
    private GPUImagePicture overlayImageSource;
    private GPUImagePicture softLightImageSource;
    private Context context;
    private int texwidth;
    private int gridsize;
    private ArrayList<FilterModel> filterModels;
    private ArrayList<String> filterTitles;
    private FilterModel filterModel;
    private String filterName;
    private int lutpicWidth;
    private int lutpicHeight;
    private static final String lutDir = "lut/";

    public VNiImageFilter(Context context, String filterName) {
        super();
        this.context = context;
        loadFilters();
        int index = LookupInstance.getInstance(context).indexOfName(filterName);
        index = index < 0 ? 0 : index;
        if (index >= 0) {
            filterModel = (FilterModel) LookupInstance.getInstance(context).getAllFilter().get(index);
            addLut(filterModel.getUrl());
        }
    }

    public VNiImageFilter(Context context, int index) {
        super();
        this.context = context;
        loadFilters();
        filterName = LookupInstance.getInstance(context).nameOfIndex(index);
        filterModel = (FilterModel) LookupInstance.getInstance(context).getAllFilter().get(index);
        addLut(filterModel.getUrl());
    }

    private void loadFilters() {
        filterModels = LookupInstance.getInstance(context).getAllFilter();
        filterTitles = LookupInstance.getInstance(context).getFilterTitles();
    }

    public float getLevelValue() {
        return levelValue;
    }


    @Override
    public void init() {
        super.init();
        if (lookupImageSource != null) {
            lookupImageSource.init();
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
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (lutpicHeight != 0 && lutpicWidth != 0) {
            GLES20.glUniform1f(gridsizeUniform, lutpicHeight);
            GLES20.glUniform1f(texwidthUniform, lutpicWidth);
        }
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


    public void processWithType(String path) {
        this.addLut(path);
    }

    public void addLut(String url) {
        Bitmap bitmap = null;
        String path = EditConstants.TEMP_FILTER_PATH + "/" + url;
        if (new File(path).exists()) {
            bitmap = BitmapUtil.loadFileToBitmap(path);
        }
        if (bitmap == null)
            bitmap = AndroidResourceManager.getAndroidResourceManager(context).readBitmapFromAssets(lutDir + "0.png");

        lutpicWidth = bitmap.getWidth();
        lutpicHeight = bitmap.getHeight();
        lookupImageSource = new GPUImagePicture(bitmap);
        lookupImageSource.init();
        lookupImageSource.addTarget(this, 1);
        lookupImageSource.processImage();
    }

    public void setLevelValue(float levelValue) {
        this.levelValue = levelValue;
        setIntensity(this.levelValue);
    }

    public void unload() {
        if (lookupImageSource != null) {
            lookupImageSource.release();
            lookupImageSource.removeTarget(this);
            lookupImageSource = null;
        }
    }

}
