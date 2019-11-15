package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.renderscript.Matrix4f;
import android.util.AttributeSet;
import android.util.Log;

import com.zp.libvideoedit.Effect.VNiImageFilter;
import com.zp.libvideoedit.GPUImage.Carma.Accelerometer;
import com.zp.libvideoedit.GPUImage.Carma.GlUtil;
import com.zp.libvideoedit.GPUImage.Carma.LogUtils;
import com.zp.libvideoedit.GPUImage.Carma.MediaAudioEncoder;
import com.zp.libvideoedit.GPUImage.Carma.MediaEncoder;
import com.zp.libvideoedit.GPUImage.Carma.MediaMuxerWrapper;
import com.zp.libvideoedit.GPUImage.Carma.MediaVideoEncoder;
import com.zp.libvideoedit.GPUImage.Carma.STRotateType;
import com.zp.libvideoedit.GPUImage.Carma.utiles.CheckAudioPermission;
import com.zp.libvideoedit.GPUImage.Core.EglCore;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.OffscreenSurface;
import com.zp.libvideoedit.GPUImage.Filter.VNiLegFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraInputFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraOutput;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
import com.zp.libvideoedit.modle.FilterCateModel;
import com.zp.libvideoedit.modle.FilterModel;
import com.zp.libvideoedit.utils.Common;
import com.zp.libvideoedit.utils.LogUtil;
import com.zp.libvideoedit.utils.LookupInstance;
import com.zp.libvideoedit.utils.SharedPreferencesTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gwd on 2018/7/5.
 */

public class GPUSurfaceCameraView extends GPUSurfaceBaseView {

    private String TAG = "CameraDisplayDoubleInput";

    private boolean mNeedBeautyOutputBuffer = false;
    private boolean mNeedStickerOutputBuffer = false;
    private boolean mNeedFilterOutputBuffer = false;

    /**
     * SurfaceTexure texture id
     */
    protected int mTextureId = OpenGlUtils.NO_TEXTURE;

    private int mImageWidth;
    private int mImageHeight;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private Context mContext;

    protected CameraEngine cameraEngine;
    private SurfaceTexture mSurfaceTexture;
    private String mCurrentSticker;
    private String mCurrentFilterStyle;
    private float mCurrentFilterStrength = 1.0f;//阈值为[0,1]
    private float mFilterStrength = 1.0f;
    private String mFilterStyle;

    private int mCameraID = -1;

    private ByteBuffer mRGBABuffer;
    private int[] mBeautifyTextureId;
    private int[] mTextureOutId;
    private int[] mFilterTextureOutId;
    private boolean mCameraChanging = false;
    private int mCurrentPreview = 1;
    private ArrayList<String> mSupportedPreviewSizes;
    private boolean mSetPreViewSizeSucceed = false;
    private boolean mIsChangingPreviewSize = false;

    private long mStartTime;
    private boolean mShowOriginal = false;
    private boolean mNeedBeautify = false;
    private boolean mNeedFaceAttribute = false;
    private boolean mNeedSticker = false;
    private boolean mNeedFilter = false;
    private boolean mNeedSave = false;
    private boolean mNeedObject = false;
    private boolean recordingEnabled;

    private FloatBuffer mTextureBuffer;
    private float[] mBeautifyParams = {0f, 0f, 0f, 0f, 0f, 0f};

    private Handler mHandler;
    private String mFaceAttribute;
    private boolean mIsPaused = false;
    private long mDetectConfig = 0;
    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private Object mHumanActionHandleLock = new Object();
    private Object mImageDataLock = new Object();

    private boolean mNeedShowRect = true;
    private int mScreenIndexRectWidth = 0;

    private Rect mTargetRect = new Rect();
    private Rect mIndexRect = new Rect();
    private boolean mNeedSetObjectTarget = false;
    private boolean mIsObjectTracking = false;


    private static final int MESSAGE_PROCESS_IMAGE = 100;
    private long mRotateCost = 0;
    private long mObjectCost = 0;
    private long mFaceAttributeCost = 0;
    private int mFrameCount = 0;

    //for test fps
    private int mCount = 0;
    private long mCurrentTime = 0;
    private boolean mIsFirstCount = true;
    private int mFrameCost = 0;

    private MediaVideoEncoder mVideoEncoder;
    private MediaAudioEncoder mAudioEncoder;

    private final float[] mStMatrix = new float[16];
    private int[] mVideoEncoderTexture;
    private boolean mNeedResetEglContext = false;

    private static final int MESSAGE_ADD_SUB_MODEL = 1001;
    private static final int MESSAGE_REMOVE_SUB_MODEL = 1002;
    private HandlerThread mSubModelsManagerThread;
    private Handler mSubModelsManagerHandler;
    private Accelerometer accelerometer;
    private boolean mIsHasAudioPermission;

    private boolean[] mFaceExpressionResult;
    private String outputFilePath;
    private int videoRotation;
    private boolean cancleRecoding;
    private boolean audioEncodeFinished = false;
    private boolean videoEncodeFinished = false;
    private Object releaseObject = new Object();

    public interface ChangePreviewSizeListener {
        void onChangePreviewSize(int previewW, int previewH);
    }

    public GPUSurfaceCameraView(Context context) {
        this(context, null);
    }

    public GPUSurfaceCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createFilter();
        if (mCameraID == -1) {
            mCameraID = (int) SharedPreferencesTools.getParam(getContext(), "cameraID", 0);
        }
        cameraEngine = new CameraEngine(context);
        mContext = context;
        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mIsHasAudioPermission = CheckAudioPermission.isHasPermission(mContext);
        mTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        accelerometer = new Accelerometer(context);
        accelerometer.start();
    }


    public void getAllfilterModels(final int width, final GPUSurfaceCameraView.FilterCallable callback) {
        if (legFilter == null) return;
        legFilter.getCurrentPic(true, new CreatePicCallBack() {
            @Override
            public void complete(final Bitmap picBit) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap newbitMap = Common.zoomImg(picBit, width);
                        GPUImagePicture picture = new GPUImagePicture(newbitMap);
                        for (int i = 0; i < LookupInstance.getInstance(mContext).getFilterTitles().size(); i++) {
                            picture.removeAllTargets();
                            FilterModel filterModel = LookupInstance.getInstance(mContext).getAllFilter().get(i);
                            String name = LookupInstance.getInstance(mContext).getFilterTitles().get(i);
                            VNiImageFilter imageFilter = new VNiImageFilter(mContext, name);
                            imageFilter.init();
                            picture.init();
                            picture.addTarget(imageFilter, 0);
                            imageFilter.useNextFrameForImageCapture();
                            picture.processImage();
                            GLES20.glFlush();
//                            Bitmap lookMap = imageFilter.newBitMapFromCurrentlyProcessedOutput();
                            imageFilter.unload();
//                            filterModel.setBitmap(lookMap);
                        }
                        if (callback != null)
                            callback.complete(LookupInstance.getInstance(mContext).getFilterArrayList());
                    }
                });


            }
        });

    }

    private class RenderFilterThread extends Thread {
        private Bitmap currentBitmap;
        private EGLContext eglContext;
        private GPUSurfaceCameraView.FilterCallable callback;
        private EglCore eglCore;
        private OffscreenSurface offscreenSurface;

        public RenderFilterThread(Bitmap bitmap, EGLContext eglContext, GPUSurfaceCameraView.FilterCallable callback) {
            this.currentBitmap = bitmap;
            this.eglContext = eglContext;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();
            eglCore = new EglCore(eglContext, 0);
            offscreenSurface = new OffscreenSurface(eglCore, currentBitmap.getWidth(), currentBitmap.getHeight());
            offscreenSurface.makeCurrent();

            GPUImagePicture picture = new GPUImagePicture(currentBitmap);
            for (int i = 0; i < LookupInstance.getInstance(mContext).getFilterTitles().size(); i++) {
                picture.removeAllTargets();
                FilterModel filterModel = LookupInstance.getInstance(mContext).getAllFilter().get(i);
                String name = LookupInstance.getInstance(mContext).getFilterTitles().get(i);
                VNiImageFilter imageFilter = new VNiImageFilter(mContext, name);
                imageFilter.init();
                picture.init();
                picture.addTarget(imageFilter, 0);
                imageFilter.useNextFrameForImageCapture();
                picture.processImage();
                GLES20.glFlush();
                offscreenSurface.swapBuffers();
//                Bitmap lookMap = offscreenSurface.getFrame();
                imageFilter.unload();
//                filterModel.setBitmap(lookMap);
            }
            if (callback != null)
                callback.complete(LookupInstance.getInstance(mContext).getFilterArrayList());

        }
    }

    public void enableFilter(boolean needFilter) {
        mNeedFilter = needFilter;
        mNeedResetEglContext = true;
    }

    public boolean getFaceAttribute() {
        return mNeedFaceAttribute;
    }

    public String getFaceAttributeString() {
        return mFaceAttribute;
    }

    public boolean getSupportPreviewsize(int size) {
        if (size == 0 && mSupportedPreviewSizes.contains("640x480")) {
            return true;
        } else if (size == 1 && mSupportedPreviewSizes.contains("1280x720")) {
            return true;
        } else {
            return false;
        }
    }

    private MediaMuxerWrapper mMuxer;

    public void startRecoding() {
        startRecoding(null);
    }

    public void startRecoding(Integer defaultVideoOrientation) {
        try {
            int width, height;
            width = height = 0;
            videoRotation = getCurrentOrientation();
            if (defaultVideoOrientation != null) {
                videoRotation = defaultVideoOrientation;
            }
            if (videoRotation == 1 || videoRotation == 3) {
                width = getPreviewHeight();
                height = getPreviewWidth();
            } else {
                width = getPreviewWidth();
                height = getPreviewHeight();
            }

            mMuxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
            // for video capturing
            new MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height);
            if (mIsHasAudioPermission) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }

            mMuxer.prepare();
            mMuxer.startRecording();
            outputFilePath = mMuxer.getOutputPath();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    public void stopRecording() {
        if (mMuxer != null) {
            outputFilePath = mMuxer.getFilePath();
            mMuxer.stopRecording();

            //mMuxer = null;
        }

        System.gc();
    }

    MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder) {
                setVideoEncoder((MediaVideoEncoder) encoder);
            }
            if (encoder instanceof MediaAudioEncoder) {
                setmAudioEncoder((MediaAudioEncoder) encoder);
            }
        }

        @Override
        public void onStopped(MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder) {
                setVideoEncoder(null);
                videoEncodeFinished = true;
            }
            if (encoder instanceof MediaAudioEncoder) {
                setmAudioEncoder(null);
                audioEncodeFinished = true;
            }

            if (videoEncodeFinished && audioEncodeFinished) {
                videoEncodeFinished = false;
                audioEncodeFinished = false;

                if (cancleRecoding) {
                    cancleRecoding = false;
                    //可能删除文件会异常
//                if (Common.fileExist(outputFilePath)) {
//                    Common.delete(outputFilePath);
//                }
                    return;
                }
                if (recordListener != null) {
                    recordListener.stop(outputFilePath);
                }
            }

        }
    };


    public void setSaveImage() {
        mNeedSave = true;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view创建的时候调用
     *
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.i(TAG, "onSurfaceCreated");
        Log.e("onResume", "onSurfaceCreatedonSurfaceCreatedonSurfaceCreatedonSurfaceCreated");

        if (mIsPaused == true) {
            return;
        }
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);

        while (!cameraEngine.isCameraOpen()) {
            if (cameraEngine.cameraOpenFailed()) {
                return;
            }
            try {
                Thread.sleep(10, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (cameraEngine.getCamera() != null) {
            setUpCamera();
        }

        //初始化GL相关的句柄，包括美颜，贴纸，滤镜
        initFilter();
        if (filterName != null && filterName.length() != 0) {
            setFilter(filterName);
            setFilterStrength(mFilterStrength);
        }
    }


    public void changeCamera() {
        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return;
        }
        final int cameraID = 1 - mCameraID;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                cameraInputFilter.destroy();
                legFilter.release();
            }
        });
        mCameraChanging = true;
        cameraEngine.openCamera(mContext, cameraID);

        if (cameraEngine.cameraOpenFailed()) {
            return;
        }

        mSetPreViewSizeSucceed = false;

        queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteTextures();
                if (cameraEngine.getCamera() != null) {
                    setUpCamera();
                }
                mCameraChanging = false;
                mCameraID = cameraID;
            }
        });
    }


    private Matrix4f videoRotationMatrix(int dir) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        switch (dir) {
            case 3: {
                matrix4f.rotate(270, 0, 0, 1);
                return matrix4f;
            }
            case 2: {
                matrix4f.rotate(180, 0, 0, 1);
                return matrix4f;
            }
            case 1: {
                matrix4f.rotate(90, 0, 0, 1);
                return matrix4f;
            }
            default:
                return matrix4f;
        }
    }

    private void destoryFilter() {
        if (cameraInputFilter != null) {
            cameraInputFilter.removeAllTargets();
        }
        if (faceOutputFilter != null) {
            faceOutputFilter.removeAllTargets();
        }
        if (legFilter != null) {
            legFilter.removeAllTargets();
        }
        cameraInputFilter = null;
        faceOutputFilter = null;
        legFilter = null;
    }


    private void createFilter() {
        cameraInputFilter = new GPUImageCameraInputFilter();
        cameraOutput = new GPUImageCameraOutput();
        faceOutputFilter = new GPUImageFilter();
        legFilter = new VNiLegFilter();
    }

    private void initFilter() {
//        int result = mSTMobileStreamFilterNative.createInstance();
//        LogUtils.i(TAG, "filter create instance result %d", result);
//
//        mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
//
//        mCurrentFilterStrength = mFilterStrength;
//        mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);

        cameraInputFilter.init();
        cameraOutput.init();
        faceOutputFilter.init();
        legFilter.init();
        faceOutputFilter.addTarget(cameraOutput);
    }


    public float[] getBeautyParams() {
        float[] values = new float[6];
        for (int i = 0; i < mBeautifyParams.length; i++) {
            values[i] = mBeautifyParams[i];
        }

        return values;
    }

    public void setShowOriginal(boolean isShow) {
        mShowOriginal = isShow;
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view尺寸改变的时候调用
     *
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.i(TAG, "onSurfaceChanged");
        if (mIsPaused == true) {
            return;
        }

        adjustViewPort(width, height);
        cameraInputFilter.onInputSizeChanged(width, height);
        faceOutputFilter.setInputSize(new GPUSize(width, height), 0);
        legFilter.setInputSize(new GPUSize(width, height), 0);
        mStartTime = System.currentTimeMillis();
    }

    /**
     * 根据显示区域大小调整一些参数信息
     *
     * @param width
     * @param height
     */
    private void adjustViewPort(int width, int height) {
        mSurfaceHeight = height;
        mSurfaceWidth = width;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        cameraInputFilter.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            if (mCameraChanging || cameraEngine.getCamera() == null) {
                return;
            }
            requestRender();
        }
    };

    /**
     * 工作在opengl线程, 具体渲染的工作函数
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // during switch camera
        if (mCameraChanging) {
            return;
        }

        if (cameraEngine.getCamera() == null) {
            return;
        }


        LogUtils.i(TAG, "onDrawFrame");
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        }

        if (mBeautifyTextureId == null) {
            mBeautifyTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
        }

        if (mTextureOutId == null) {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
        }

        if (mVideoEncoderTexture == null) {
            mVideoEncoderTexture = new int[1];
        }

        if (mSurfaceTexture != null && !mIsPaused) {
            mSurfaceTexture.updateTexImage();
        } else {
            return;
        }

        mStartTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRGBABuffer.rewind();
        int textureId = cameraInputFilter.onDrawToTexture(mTextureId);

        textureId = legFilter.renderToTextureWithVertices(textureId);

        faceOutputFilter.newFrameReadyAtTime(0, 0, textureId);


        if (mVideoEncoder != null) {
            GLES20.glFinish();
        }
        mVideoEncoderTexture[0] = textureId;
        mSurfaceTexture.getTransformMatrix(mStMatrix);
        processStMatrix(mStMatrix, mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT,
                videoRotation == 0 || videoRotation == 2);
        synchronized (this) {
            if (mVideoEncoder != null) {
                if (mNeedResetEglContext) {
                    mVideoEncoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    mNeedResetEglContext = false;
                }
                mVideoEncoder.frameAvailableSoon(mStMatrix, videoRotationMatrix(videoRotation).getArray());

            }
        }
    }

    private void savePicture(int textureId) {
        ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
//        mGLRender.saveTextureToFrameBuffer(textureId, mTmpBuffer);

        mTmpBuffer.position(0);
        Message msg = Message.obtain(mHandler);
//        msg.what = STCameraActivity.MSG_SAVING_IMG;
        msg.obj = mTmpBuffer;
        Bundle bundle = new Bundle();
        bundle.putInt("imageWidth", mImageWidth);
        bundle.putInt("imageHeight", mImageHeight);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    private void saveImageBuffer2Picture(byte[] imageBuffer) {
        ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        mTmpBuffer.put(imageBuffer);

        Message msg = Message.obtain(mHandler);
//        msg.what = STCameraActivity.MSG_SAVING_IMG;
        msg.obj = mTmpBuffer;
        Bundle bundle = new Bundle();
        bundle.putInt("imageWidth", mImageWidth);
        bundle.putInt("imageHeight", mImageHeight);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    private int getCurrentOrientation() {
        int dir = Accelerometer.getDirection();
        int orientation = dir - 1;
        if (orientation < 0) {
            orientation = dir ^ 3;
        }

        return orientation;
    }

    /**
     * camera设备startPreview
     */
    private void setUpCamera() {
        // 初始化Camera设备预览需要的显示区域(mSurfaceTexture)
        if (mTextureId == OpenGlUtils.NO_TEXTURE) {
            mTextureId = OpenGlUtils.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureId);

//            mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
        }

        if (mSupportedPreviewSizes == null) {
            mSupportedPreviewSizes = cameraEngine.getSupportedPreviewSize(new String[]{"640x480", "1280x720"});
        }
        String size = mSupportedPreviewSizes.get(mCurrentPreview);
        int index = size.indexOf('x');
        mImageHeight = Integer.parseInt(size.substring(0, index));
        mImageWidth = Integer.parseInt(size.substring(index + 1));

        if (mIsPaused) {
            return;

        }

        while (!mSetPreViewSizeSucceed) {
            try {
                cameraEngine.setPreviewSize(mImageHeight, mImageWidth);
                mSetPreViewSizeSucceed = true;
            } catch (Exception e) {
                mSetPreViewSizeSucceed = false;
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {

            }
        }

        boolean flipHorizontal = cameraEngine.isFlipHorizontal();
        cameraInputFilter.adjustTextureBuffer(cameraEngine.getOrientation(), flipHorizontal);

        if (mIsPaused) {
            return;
        }
        cameraEngine.startPreview(mSurfaceTexture, mPreviewCallback);
    }


    public void setFilterStyle(String modelPath) {
        mFilterStyle = modelPath;
    }

    public void setFilterStrength(float strength) {
        mFilterStrength = strength;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (filter != null && filter instanceof VNiImageFilter) {
                    VNiImageFilter filterNew = (VNiImageFilter) filter;
                    filterNew.setLevelValue(mFilterStrength);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraEngine.getCamera() == null) {
            if (cameraEngine.getNumberOfCameras() == 1) {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            cameraEngine.openCamera(mContext, mCameraID);
            mSupportedPreviewSizes = cameraEngine.getSupportedPreviewSize(new String[]{"640x480", "1280x720"});
        }
        mIsPaused = false;
        mSetPreViewSizeSucceed = false;
//        queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
//            }
//        });
//        mGLRender = new STGLRender();
        createFilter();
    }

    @Override
    public void onPause() {
        LogUtils.i(TAG, "onPause");
        mSetPreViewSizeSucceed = false;
        //mCurrentSticker = null;
        mIsPaused = true;
        cameraEngine.releaseCamera();
        LogUtils.d(TAG, "Release camera");

        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRGBABuffer = null;
                deleteTextures();
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                }
                destoryFilter();
                GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
                synchronized (releaseObject) {
                    releaseObject.notifyAll();
                }
            }
        });
        synchronized (releaseObject) {
            try {
                releaseObject.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.onPause();
    }

    public void onDestroy() {
        //必须释放非opengGL句柄资源,负责内存泄漏
        if (accelerometer != null) {
            accelerometer.stop();
        }
    }

    /**
     * 释放纹理资源
     */
    protected void deleteTextures() {
        LogUtils.i(TAG, "delete textures");
        deleteCameraPreviewTexture();
        deleteInternalTextures();
    }

    @Override
    public void savePicture(int width, int height) {

    }

    // must in opengl thread
    private void deleteCameraPreviewTexture() {
        if (mTextureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, new int[]{
                    mTextureId
            }, 0);
        }
        mTextureId = OpenGlUtils.NO_TEXTURE;
    }

    private void deleteInternalTextures() {
        if (mBeautifyTextureId != null) {
            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
            mBeautifyTextureId = null;
        }

        if (mTextureOutId != null) {
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }

        if (mFilterTextureOutId != null) {
            GLES20.glDeleteTextures(1, mFilterTextureOutId, 0);
            mFilterTextureOutId = null;
        }

        if (mVideoEncoderTexture != null) {
            GLES20.glDeleteTextures(1, mVideoEncoderTexture, 0);
            mVideoEncoderTexture = null;
        }
    }

    public void switchCamera() {
        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return;
        }


        final int cameraID = 1 - mCameraID;
        mCameraChanging = true;
        cameraEngine.openCamera(mContext, cameraID);

        if (cameraEngine.cameraOpenFailed()) {
            return;
        }

        mSetPreViewSizeSucceed = false;

        if (mNeedObject) {
            resetIndexRect();
        } else {
//            Message msg = mHandler.obtainMessage(STCameraActivity.MSG_CLEAR_OBJECT);
//            mHandler.sendMessage(msg);
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteTextures();
                if (cameraEngine.getCamera() != null) {
                    setUpCamera();
                }
                mCameraChanging = false;
                mCameraID = cameraID;
            }
        });
        //fix 双输入camera changing时，贴纸和画点mirrow显示
        //mGlSurfaceView.requestRender();
    }

    public int getCameraID() {
        return mCameraID;
    }

    public void changePreviewSize(int currentPreview) {
        if (cameraEngine.getCamera() == null || mCameraChanging
                || mIsPaused) {
            return;
        }

        mCurrentPreview = currentPreview;
        mSetPreViewSizeSucceed = false;
        mIsChangingPreviewSize = true;

        mCameraChanging = true;
        cameraEngine.stopPreview();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRGBABuffer != null) {
                    mRGBABuffer.clear();
                }
                mRGBABuffer = null;

                deleteTextures();
                if (cameraEngine.getCamera() != null) {
                    setUpCamera();
                }

//                mGLRender.init(mImageWidth, mImageHeight);
                if (mNeedObject) {
                    resetIndexRect();
                }

//                mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);

                mCameraChanging = false;
                mIsChangingPreviewSize = false;
                //mGlSurfaceView.requestRender();
                LogUtils.d(TAG, "exit  change Preview size queue event");
            }
        });


    }

    public void enableObject(boolean enabled) {
        mNeedObject = enabled;

        if (mNeedObject) {
            resetIndexRect();
        }
    }

    public void setIndexRect(int x, int y, boolean needRect) {
        mIndexRect = new Rect(x, y, x + mScreenIndexRectWidth, y + mScreenIndexRectWidth);
        mNeedShowRect = needRect;
    }

    public Rect getIndexRect() {
        return mIndexRect;
    }


    public void disableObjectTracking() {
        mIsObjectTracking = false;
    }

    public void resetIndexRect() {
        if (mImageWidth == 0) {
            return;
        }

        mScreenIndexRectWidth = mSurfaceWidth / 4;

        mIndexRect.left = (mSurfaceWidth - mScreenIndexRectWidth) / 2;
        mIndexRect.top = (mSurfaceHeight - mScreenIndexRectWidth) / 2;
        mIndexRect.right = mIndexRect.left + mScreenIndexRectWidth;
        mIndexRect.bottom = mIndexRect.top + mScreenIndexRectWidth;

        mNeedShowRect = true;
        mNeedSetObjectTarget = false;
        mIsObjectTracking = false;
    }

    /**
     * 用于humanActionDetect接口。根据传感器方向计算出在不同设备朝向时，人脸在buffer中的朝向
     *
     * @return 人脸在buffer中的朝向
     */
    private int getHumanActionOrientation() {
        boolean frontCamera = (mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT);

        //获取重力传感器返回的方向
        int orientation = Accelerometer.getDirection();

        //在使用后置摄像头，且传感器方向为0或2时，后置摄像头与前置orentation相反
        if (!frontCamera && orientation == STRotateType.ST_CLOCKWISE_ROTATE_0) {
            orientation = STRotateType.ST_CLOCKWISE_ROTATE_180;
        } else if (!frontCamera && orientation == STRotateType.ST_CLOCKWISE_ROTATE_180) {
            orientation = STRotateType.ST_CLOCKWISE_ROTATE_0;
        }

        // 请注意前置摄像头与后置摄像头旋转定义不同 && 不同手机摄像头旋转定义不同
        if (((cameraEngine.getOrientation() == 270 && (orientation & STRotateType.ST_CLOCKWISE_ROTATE_90) == STRotateType.ST_CLOCKWISE_ROTATE_90) ||
                (cameraEngine.getOrientation() == 90 && (orientation & STRotateType.ST_CLOCKWISE_ROTATE_90) == STRotateType.ST_CLOCKWISE_ROTATE_0)))
            orientation = (orientation ^ STRotateType.ST_CLOCKWISE_ROTATE_180);
        return orientation;
    }

    public int getPreviewWidth() {
        return mImageWidth;
    }

    public int getPreviewHeight() {
        return mImageHeight;
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {

        queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (encoder != null && mVideoEncoderTexture != null) {
                        encoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    }
                    mVideoEncoder = encoder;
                }
            }
        });
    }

    public void setmAudioEncoder(MediaAudioEncoder mAudioEncoder) {
        this.mAudioEncoder = mAudioEncoder;
    }

    private void processStMatrix(float[] matrix, boolean needMirror, boolean needFlip) {
        if (needMirror && matrix != null && matrix.length == 16) {
            for (int i = 0; i < 3; i++) {
                matrix[4 * i] = -matrix[4 * i];
            }

            if (matrix[4 * 3] == 0) {
                matrix[4 * 3] = 1.0f;
            } else if (matrix[4 * 3] == 1.0f) {
                matrix[4 * 3] = 0f;
            }
        }

        if (needFlip && matrix != null && matrix.length == 16) {
            matrix[0] = 1.0f;
            matrix[5] = -1.0f;
            matrix[12] = 0f;
            matrix[13] = 1.0f;
        }

        return;
    }

    public int getFrameCost() {
        return mFrameCost;
    }


    public boolean isChangingPreviewSize() {
        return mIsChangingPreviewSize;
    }

    public void addSubModelByName(String modelName) {
        Message msg = mSubModelsManagerHandler.obtainMessage(MESSAGE_ADD_SUB_MODEL);
        msg.obj = modelName;

        mSubModelsManagerHandler.sendMessage(msg);
    }

    public void removeSubModelByConfig(int Config) {
        Message msg = mSubModelsManagerHandler.obtainMessage(MESSAGE_REMOVE_SUB_MODEL);
        msg.obj = Config;
        mSubModelsManagerHandler.sendMessage(msg);
    }


    public boolean[] getFaceExpressionInfo() {
        return mFaceExpressionResult;
    }


    public interface RecordListener {
        void stop(String url);
    }

    public void changeRecordingState(boolean isRecording) {
        recordingEnabled = isRecording;
    }

    private RecordListener recordListener;

    public void setRecordListener(RecordListener recordListener) {
        this.recordListener = recordListener;
    }

    public interface TakePhtoListener {
        public void takePhtoComplete(Bitmap bitmap);
    }

    public interface FilterCallable {
        public void complete(ArrayList<FilterCateModel> filters);
    }

    public String getOutPutFilePath() {
        return outputFilePath;
    }

    @Override
    public void cancle() {
        cancleRecoding = true;
        if (mMuxer != null) {
            outputFilePath = mMuxer.getFilePath();
            mMuxer.stopRecording();
        }
        System.gc();
    }
}