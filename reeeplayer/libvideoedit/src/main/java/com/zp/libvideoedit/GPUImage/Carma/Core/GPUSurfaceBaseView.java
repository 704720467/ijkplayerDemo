package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.zp.libvideoedit.Effect.VNiImageFilter;
import com.zp.libvideoedit.GPUImage.Core.GPUFilterParam;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Filter.VNiLegFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraInputFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraOutput;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.VNIFilterEnum;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class GPUSurfaceBaseView extends GLSurfaceView implements GLSurfaceView.Renderer {

    protected GPUImageCameraInputFilter cameraInputFilter;
    protected GPUImageCameraOutput cameraOutput;
    protected GPUImageFilter faceOutputFilter;

    protected VNiLegFilter legFilter;
    protected GPUImageFilter filter;
    private Context context;

    /**
     * SurfaceTexure纹理id
     */
//    protected int textureId = OpenGlUtils.NO_TEXTURE;


    /**
     * GLSurfaceView的宽高
     */
    protected int surfaceWidth, surfaceHeight;
    private VNIFilterEnum filtermanager;
    protected String filterName;


    /**
     * 图像宽高
     */
    protected int imageWidth, imageHeight;

    public GPUSurfaceBaseView(Context context) {
        this(context, null);
    }

    public GPUSurfaceBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        filtermanager = VNIFilterEnum.getFilterEnumManager(context);
        this.context = context;

        //默认的顶点和纹理左边 ，横屏录制的时候需要修改

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        GPUFilterParam.getGPUperformace(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        surfaceWidth = width;
        surfaceHeight = height;
        onFilterChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    protected void onFilterChanged() {
        if (filter != null) {
            filter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
            filter.setInputSize(new GPUSize(imageWidth, imageHeight), 0);
        }
    }

    public void setLegScaleValue(float scaleValue) {
        if (legFilter != null) {
            legFilter.setScaleValue(scaleValue);
            this.requestRender();
        }
    }

    public void setLegCenterValue(float centerValue) {
        if (legFilter != null) {
            legFilter.setCenterValue(centerValue);
            this.requestRender();
        }

    }

    public void setLegRotationValue(float rotationValue) {
        if (legFilter != null) {
            legFilter.setRotateVaule(rotationValue);
            this.requestRender();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {

    }

    /**
     * 相机预览设置filter
     *
     * @param filterName
     */
    public void setFilter(final String filterName) {
        this.filterName = filterName;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceOutputFilter.removeAllTargets();
                if (filter != null) {
                    filter.removeAllTargets();
                    filter.destroy();
                    if (filter instanceof VNiImageFilter) {
                        VNiImageFilter imageFilter = (VNiImageFilter) filter;
                        imageFilter.unload();
                    }
                    filter = null;
                }
                filter = new VNiImageFilter(context, filterName);
                filter.init();
                faceOutputFilter.addTarget(filter, 0);
                filter.addTarget(cameraOutput);
            }
        });
    }

//    protected void deleteTextures() {
//        if (textureId != OpenGlUtils.NO_TEXTURE) {
//            queueEvent(new Runnable() {
//                @Override
//                public void run() {
//                    GLES20.glDeleteTextures(1, new int[]{
//                            textureId
//                    }, 0);
//                    textureId = OpenGlUtils.NO_TEXTURE;
//                }
//            });
//        }
//    }

    public void stopRecording() {

    }

    public void cancle() {

    }


    public void startRecoding() {
    }

    public void getAllfilterModels(final GPUSurfaceCameraView.FilterCallable callback) {

    }

    public void changeCamera() {
    }

    public abstract void savePicture(final int width, final int height);

    public interface CreatePicCallBack {
        public void complete(Bitmap picBit);
    }
}
