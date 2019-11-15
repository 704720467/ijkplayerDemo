package com.zp.libvideoedit.GPUImage.Carma.Core;//package com.zp.libvideoedit.GPUImage.Carma.Core;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.graphics.Bitmap;
//import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
//import android.opengl.EGL14;
//import android.opengl.EGLContext;
//import android.opengl.GLES20;
//import android.os.Environment;
//import android.renderscript.Matrix4f;
//import android.util.AttributeSet;
//import android.util.Log;
//import android.util.Size;
//
//import com.sensetime.stmobile.STBeautifyNative;
//import com.sensetime.stmobile.STBeautyParamsType;
//import com.sensetime.stmobile.STCommon;
//import com.sensetime.stmobile.STHumanActionParamsType;
//import com.sensetime.stmobile.STMobileAuthentificationNative;
//import com.sensetime.stmobile.STMobileHumanActionNative;
//import com.sensetime.stmobile.STMobileStickerNative;
//import com.sensetime.stmobile.STRotateType;
//import com.sensetime.stmobile.model.STHumanAction;
//import com.sensetime.stmobile.model.STMobile106;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.EGLBase;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.EGLBase.EglSurface;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.MediaAudioEncoder;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.MediaEncoder;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.MediaMuxerWrapper;
//import com.zp.libvideoedit.GPUImage.Carma.encoder.MediaVideoEncoder;
//import com.zp.libvideoedit.GPUImage.Carma.utiles.CheckAudioPermission;
//import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
//import com.zp.libvideoedit.GPUImage.Core.GPUSize;
//import com.zp.libvideoedit.GPUImage.Filter.VNiLegFilter;
//import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraInputFilter;
//import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraOutput;
//import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
//import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
//import com.vnision.VNICore.Effect.VNiImageFilter;
//import com.vnision.VNICore.Model.FilterCateModel;
//import com.vnision.VNICore.Model.FilterModel;
//import com.vnision.VNICore.utils.CodecUtils;
//import com.vnision.VNICore.utils.Common;
//import com.vnision.VNICore.utils.LookupInstance;
//import com.vnision.videostudio.util.LogUtil;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
//import sensetime.senseme.com.effects.glutils.OpenGLUtils;
//import sensetime.senseme.com.effects.utils.Accelerometer;
//import sensetime.senseme.com.effects.utils.FileUtils;
//import sensetime.senseme.com.effects.utils.LogUtils;
//
///**
// * Created by why8222 on 2016/2/25.
// */
//public class GPUSurfaceCameraView0 extends GPUSurfaceBaseView {
//
//    private SurfaceTexture surfaceTexture;
//
//    public GPUSurfaceCameraView0(Context context) {
//        this(context, null);
//    }
//
//    private static final int MSG_NEED_START_RECORDING = 10;
//    private static final int MSG_STOP_RECORDING = 11;
//    private Context context;
//    private boolean recordingEnabled;
//    private int recordingStatus;
//    private static final int RECORDING_OFF = 0;
//    private static final int RECORDING_ON = 1;
//    private static final int RECORDING_RESUMED = 2;
//    //@ guanweidong
//    private MediaVideoEncoder mVideoEncoder;
//    private static MediaMuxerWrapper mediaMuxerWrapper = null;
//    private File outputFile;
//    private Object mHumanActionHandleLock = new Object();
//    private Object mImageDataLock = new Object();
//    private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
//    private STBeautifyNative mStBeautifyNative = new STBeautifyNative();
//    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
//    private String TAG = this.getClass().getSimpleName();
//    private boolean mIsCreateHumanActionHandleSucceeded = false;
//    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;
//    private Context mContext;
//    private boolean mNeedBeautify = false;
//    private boolean mNeedResetEglContext = false;
//    private long mDetectConfig = 0;
//    private boolean mNeedFaceAttribute = false;
//    private boolean mNeedSticker = false;
//    private String mCurrentSticker;
//    private float[] mBeautifyParams = {0, 0, 0, 0, 0, 0};
//    private String mFaceAttribute;
//    public static int[] beautyTypes = {
//            STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH,
//            STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,
//            STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH,
//            STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO,
//            STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO,
//            STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO
//    };
//    private byte[] mImageData;
//    private boolean mShowOriginal = false;
//    //    private byte[] mNv21ImageData;
//    private int[] mBeautifyTextureId;
//    private int[] mTextureOutId;
//    private Accelerometer mAccelerometer = null;
//    protected CameraEngine cameraEngine = null;
//    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
//    private boolean mIsPaused;
//    private ArrayList<String> mSupportedPreviewSizes;
//    private boolean mSetPreViewSizeSucceed;
//    private int mSurfaceHeight, mSurfaceWidth;
//    private boolean mCameraChanging;
//    private int videoRotation;
//
//    public interface RecordListener {
//        void stop(String url);
//    }
//
//    private RecordListener recordListener;
//
//    public void setRecordListener(RecordListener recordListener) {
//        this.recordListener = recordListener;
//    }
//
//    public GPUSurfaceCameraView0(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        this.context = context;
//        cameraEngine = new CameraEngine(context);
//        createFilter();
//        recordingStatus = -1;
//        recordingEnabled = false;
//        mContext = context;
//        boolean checkVersion = false;
//        mIsHasAudioPermission = CheckAudioPermission.isHasPermission(mContext);
//        try {
//            checkVersion = checkLicense(context);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (checkVersion == false) {
//            LogUtil.e(TAG, "check version error!");
//        }
//        mAccelerometer = new Accelerometer(context);
//        //商汤sdk 与GL无关的初始化
//        //人脸检测相关的初始化
//        initHumanAction();
//        //人脸属性句柄初始化
//        enableBeautify(true);
//
//    }
//
//    private void createFilter() {
//        outputFile = new File(Environment.getExternalStorageDirectory().getPath(), "test.mp4");
//        cameraInputFilter = new GPUImageCameraInputFilter();
//        cameraOutput = new GPUImageCameraOutput();
//        faceOutputFilter = new GPUImageFilter();
//        legFilter = new VNiLegFilter();
//    }
//
//    //人脸检测相关的初始化因为加载模型比较慢采用异步加载
//    private void initHumanAction() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (mHumanActionHandleLock) {
//                    int result = mSTHumanActionNative.createInstanceFromAssetFile(FileUtils.getActionModelName(), mHumanActionCreateConfig, mContext.getAssets());
//                    LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);
//
//                    if (result == 0) {
//                        mIsCreateHumanActionHandleSucceeded = true;
//                        mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BACKGROUND_BLUR_STRENGTH, 0.35f);
//                    }
//                }
//            }
//        }).start();
//    }
//
//    public void enableBeautify(boolean needBeautify) {
//        mNeedBeautify = needBeautify;
//        setHumanActionDetectConfig(mNeedBeautify | mNeedFaceAttribute, mStStickerNative.getTriggerAction());
//        mNeedResetEglContext = true;
//
//    }
//
//    private void setHumanActionDetectConfig(boolean needFaceDetect, long config) {
//        if (!mNeedSticker || mCurrentSticker == null) {
//            config = 0;
//        }
//        if (needFaceDetect) {
//            mDetectConfig = config | STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
//        } else {
//            mDetectConfig = config;
//        }
//    }
//
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        super.onSurfaceCreated(gl, config);
//
//        LogUtils.i(TAG, "onSurfaceCreated");
//        if (mIsPaused == true) {
//            return;
//        }
//        GLES20.glEnable(GL10.GL_DITHER);
//        GLES20.glClearColor(0, 0, 0, 0);
//        GLES20.glEnable(GL10.GL_DEPTH_TEST);
//
//        while (!cameraEngine.isCameraOpen()) {
//            if (cameraEngine.cameraOpenFailed()) {
//                return;
//            }
//            try {
//                Thread.sleep(10, 0);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (cameraEngine.getCamera() != null) {
//            setUpCamera();
//        }
//        mNeedResetEglContext = true;
//        //初始化GL相关的句柄，包括美颜，贴纸，滤镜
//        initBeauty();
//        initSticker();
//        initFilter();
//    }
//
//    private void destoryFilter() {
//        cameraInputFilter.removeAllTargets();
//        faceOutputFilter.removeAllTargets();
//        legFilter.removeAllTargets();
//        cameraInputFilter = null;
//        faceOutputFilter = null;
//        legFilter = null;
//    }
//
//    private void initFilter() {
//        cameraInputFilter.init();
//        cameraOutput.init();
//        faceOutputFilter.init();
//        legFilter.init();
//        faceOutputFilter.addTarget(legFilter);
//        legFilter.addTarget(cameraOutput);
//    }
//
//    public void setUpCamera() {
//        // 初始化Camera设备预览需要的显示区域(mSurfaceTexture)
////        if (textureId == OpenGLUtils.NO_TEXTURE) {
////            textureId = OpenGLUtils.getExternalOESTextureID();
////            surfaceTexture = new SurfaceTexture(textureId);
////            surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
////            Log.e("setUpCamera", "setUpCamera" + textureId + "  " + surfaceTexture);
////
////        }
//
//        String size = mSupportedPreviewSizes.get(0);
//        int index = size.indexOf('x');
//        imageHeight = Integer.parseInt(size.substring(0, index));
//        imageWidth = Integer.parseInt(size.substring(index + 1));
//
//        if (mIsPaused)
//            return;
//
//        while (!mSetPreViewSizeSucceed) {
//            try {
//                cameraEngine.setPreviewSize(imageHeight, imageWidth);
//                mSetPreViewSizeSucceed = true;
//            } catch (Exception e) {
//                mSetPreViewSizeSucceed = false;
//            }
//
//            try {
//                Thread.sleep(10);
//            } catch (Exception e) {
//
//            }
//        }
//
//        boolean flipHorizontal = cameraEngine.isFlipHorizontal();
//        cameraInputFilter.adjustTextureBuffer(cameraEngine.getOrientation(), flipHorizontal);
//        if (mIsPaused)
//            return;
//        cameraEngine.startPreview(surfaceTexture, new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//                if (cameraEngine.getCamera() == null || mCameraChanging) {
//                    return;
//                }
//                if (mImageData == null || mImageData.length != imageHeight * imageWidth * 3 / 2) {
//                    mImageData = new byte[imageWidth * imageHeight * 3 / 2];
//                }
//                synchronized (mImageDataLock) {
//                    System.arraycopy(data, 0, mImageData, 0, data.length);
//                }
//                requestRender();
//            }
//        });
//    }
//
//    public void initBeauty() {
//        int result = mStBeautifyNative.createInstance();
//        LogUtils.i(TAG, "the result is for initBeautify " + result);
//        if (result == 0) {
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, mBeautifyParams[0]);
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, mBeautifyParams[1]);
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, mBeautifyParams[2]);
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, mBeautifyParams[3]);
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, mBeautifyParams[4]);
//            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, mBeautifyParams[5]);
//        }
//
//    }
//
//    public void initSticker() {
//        int result = mStStickerNative.createInstance(mContext, null);
//
//        if (mNeedSticker) {
//            mStStickerNative.changeSticker(mCurrentSticker);
//        }
//
//        setHumanActionDetectConfig(mNeedBeautify | mNeedFaceAttribute, mStStickerNative.getTriggerAction());
//        LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);
//
//    }
//
//    public void setBeautyParam(int index, float value) {
////        if (mBeautifyParams[index] != value) {
//        mStBeautifyNative.setParam(index, value);
////            mBeautifyParams[index] = value;
////        }
//    }
//
//    public float[] getBeautyParams() {
//        float[] values = new float[6];
//        for (int i = 0; i < mBeautifyParams.length; i++) {
//            values[i] = mBeautifyParams[i];
//        }
//
//        return values;
//    }
//
//
//    public void changeCamera() {
//        if (Camera.getNumberOfCameras() == 1
//                || mCameraChanging) {
//            return;
//        }
//        final int cameraID = 1 - mCameraID;
//        mCameraChanging = true;
//        cameraEngine.openCamera(context, cameraID);
//
//        if (cameraEngine.cameraOpenFailed()) {
//            return;
//        }
//
//        mSetPreViewSizeSucceed = false;
//
//        queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                deleteTextures();
//                if (cameraEngine.getCamera() != null) {
//                    setUpCamera();
//                }
//                mCameraChanging = false;
//                mCameraID = cameraID;
//            }
//        });
//    }
//
//
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        super.onSurfaceChanged(gl, width, height);
//        LogUtils.i(TAG, "onSurfaceChanged");
//        if (mIsPaused == true) {
//            return;
//        }
//        adjustViewPort(width, height);
//        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
////        cameraOutput.setDisplaySize(new GPUSize(width, height));
//    }
//
//    public void adjustViewPort(int width, int height) {
//        mSurfaceHeight = height;
//        mSurfaceWidth = width;
//        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
//        cameraInputFilter.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, imageWidth, imageHeight);
//
//    }
//
//    @Override
//    public void onDrawFrame(GL10 gl) {
//        super.onDrawFrame(gl);
//        if (surfaceTexture != null && !mIsPaused) {
//            surfaceTexture.updateTexImage();
////            Log.e("onDrawFrame", "onDrawFrame:  " +""+" error "+GLES20.glGetError());
//        } else {
//            return;
//        }
//        faceOutputFilter.setInputSize(new GPUSize(imageWidth, imageHeight), 0);
////        faceOutputFilter.newFrameReadyAtTime(0, 0, textureId);
////
////        int id = textureId;
////        int textureid = cameraInputFilter.onDrawToTexture(textureId);
//        int result = -1;
//        //创建美颜纹理id
//        if (mBeautifyTextureId == null) {
//            mBeautifyTextureId = new int[1];
//            OpenGlUtils.getTexutre2DId(imageWidth, imageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
//        }
//        if (cameraEngine != null) {
//            int origon = cameraEngine.getDisplayOrientation(mCameraID);
////           Log.e("onDrawFrame","cameraEngine dir: "+origon);
//        }
//        if (!mShowOriginal) {
//            if ((mNeedBeautify || mNeedSticker || mNeedFaceAttribute) && mIsCreateHumanActionHandleSucceeded) {
//                if (mImageData == null || mImageData.length != imageHeight * imageWidth * 3 / 2) {
//                    return;
//                }
//                if (imageHeight * imageWidth * 3 / 2 > mImageData.length) {
//                    return;
//                }
//                STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mImageData, STCommon.ST_PIX_FMT_NV21,
//                        mDetectConfig, getHumanActionOrientation(), imageHeight, imageWidth);
//                /**
//                 * HumanAction rotate && mirror:双输入场景中，buffer为相机原始数据，而texture已根据预览旋转和镜像处理，所以buffer和texture方向不一致，
//                 * 根据buffer计算出的HumanAction不能直接使用，需要根据摄像头ID和摄像头方向处理后使用
//                 */
//                if (cameraEngine.getCamera() != null) {
//                    humanAction = STHumanAction.humanActionRotateAndMirror(humanAction, imageWidth, imageHeight, cameraEngine.getCameraID(), cameraEngine.getOrientation());
//                }
//                STMobile106[] arrayFaces = null, arrayOutFaces = null;
//                //美颜
//                if (mNeedBeautify) {// do beautify
//                    if (humanAction != null) {
//                        arrayFaces = humanAction.getMobileFaces();
//                        if (arrayFaces != null && arrayFaces.length > 0) {
//                            arrayOutFaces = new STMobile106[arrayFaces.length];
//                        }
//                    }
//                    //如果需要输出buffer推流或其他，设置该开关为true
//                    result = mStBeautifyNative.processTexture(textureid, imageWidth, imageHeight, arrayFaces, mBeautifyTextureId[0], arrayOutFaces);
//                    if (result == 0) {
//                        textureid = mBeautifyTextureId[0];
//                    } else {
//                        LogUtil.e("ResultCode ", "STUtils.ResultCode result = " + result);
//                    }
//                    if (arrayOutFaces != null && arrayOutFaces.length != 0 && humanAction != null && result == 0) {
//                        boolean replace = humanAction.replaceMobile106(arrayOutFaces);
//                    }
//                }
//                //美颜后的纹理id  =  textureid  ;
//                faceOutputFilter.setInputSize(new GPUSize(imageWidth, imageHeight), 0);
//                faceOutputFilter.newFrameReadyAtTime(0, 0, textureid);
//            }
//        } else {
//
//        }
//
//        float[] mStMatrix = new float[16];
//        surfaceTexture.getTransformMatrix(mStMatrix);
//        processStMatrix(mStMatrix, mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT,
//                videoRotation == 0 || videoRotation == 2);
//        synchronized (this) {
//            if (mVideoEncoder != null) {
//                if (mNeedResetEglContext) {
//                    mVideoEncoder.setEglContext(EGL14.eglGetCurrentContext(), textureid);
//                    mNeedResetEglContext = false;
//                }
//                mVideoEncoder.frameAvailableSoon(mStMatrix, videoRotationMatrix(videoRotation).getArray());
//
//
////                Matrix4f identify = new Matrix4f();
////                identify.loadIdentity();
////                Matrix4f matrix4f = new Matrix4f(identify.getArray());
////                matrix4f.rotate(180, 0, 0, 1);
////                mVideoEncoder.frameAvailableSoon(identify.getArray(), matrix4f.getArray());
//            }
//        }
//    }
//
//    private void processStMatrix(float[] matrix, boolean needMirror, boolean needFlip) {
//        if (needMirror && matrix != null && matrix.length == 16) {
//            for (int i = 0; i < 3; i++) {
//                matrix[4 * i] = -matrix[4 * i];
//            }
//
//            if (matrix[4 * 3] == 0) {
//                matrix[4 * 3] = 1.0f;
//            } else if (matrix[4 * 3] == 1.0f) {
//                matrix[4 * 3] = 0f;
//            }
//        }
//
//        if (needFlip && matrix != null && matrix.length == 16) {
//            matrix[0] = 1.0f;
//            matrix[5] = -1.0f;
//            matrix[12] = 0f;
//            matrix[13] = 1.0f;
//        }
//
//    }
//
//
//    public boolean checkLicense(Context context) throws IOException {
//        StringBuilder sb = new StringBuilder();
//        InputStreamReader isr = null;
//        BufferedReader br = null;
//        isr = new InputStreamReader(context.getResources().getAssets().open(LICENSE_NAME));
//        br = new BufferedReader(isr);
//        String line = null;
//        while ((line = br.readLine()) != null) {
//            sb.append(line).append("\n");
//        }
//        if (sb.toString().length() == 0) {
//            LogUtils.e(TAG, "read license data error");
//            return false;
//        }
//        String licenseBuffer = sb.toString();
//        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_ACTIVATE_CODE_FILE, Context.MODE_PRIVATE);
//        String activateCode = sp.getString(PREF_ACTIVATE_CODE, null);
//        Integer error = new Integer(-1);
//        if (activateCode == null || (STMobileAuthentificationNative.checkActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length(), activateCode, activateCode.length()) != 0)) {
//            activateCode = STMobileAuthentificationNative.generateActiveCodeFromBuffer(context, licenseBuffer, licenseBuffer.length());
//            if (activateCode != null && activateCode.length() > 0) {
//                SharedPreferences.Editor editor = sp.edit();
//                editor.putString(PREF_ACTIVATE_CODE, activateCode);
//                editor.commit();
//                return true;
//            }
//            return false;
//        }
//        return true;
//    }
//
//    private int getCurrentOrientation() {
//        int dir = Accelerometer.getDirection();
//        int orientation = dir - 1;
//        if (orientation < 0) {
//            orientation = dir ^ 3;
//        }
//        return orientation;
//    }
//
//    /**
//     * 用于humanActionDetect接口。根据传感器方向计算出在不同设备朝向时，人脸在buffer中的朝向
//     *
//     * @return 人脸在buffer中的朝向
//     */
//    private int getHumanActionOrientation() {
//        boolean frontCamera = (cameraEngine.getCameraID() == Camera.CameraInfo.CAMERA_FACING_FRONT);
//
//        //获取重力传感器返回的方向
//        int orientation = Accelerometer.getDirection();
//
//        //在使用后置摄像头，且传感器方向为0或2时，后置摄像头与前置orentation相反
//        if (!frontCamera && orientation == STRotateType.ST_CLOCKWISE_ROTATE_0) {
//            orientation = STRotateType.ST_CLOCKWISE_ROTATE_180;
//        } else if (!frontCamera && orientation == STRotateType.ST_CLOCKWISE_ROTATE_180) {
//            orientation = STRotateType.ST_CLOCKWISE_ROTATE_0;
//        }
//
//        // 请注意前置摄像头与后置摄像头旋转定义不同 && 不同手机摄像头旋转定义不同
//        if (((cameraEngine.getOrientation() == 270 && (orientation & STRotateType.ST_CLOCKWISE_ROTATE_90) == STRotateType.ST_CLOCKWISE_ROTATE_90) ||
//                (cameraEngine.getOrientation() == 90 && (orientation & STRotateType.ST_CLOCKWISE_ROTATE_90) == STRotateType.ST_CLOCKWISE_ROTATE_0)))
//            orientation = (orientation ^ STRotateType.ST_CLOCKWISE_ROTATE_180);
//        return orientation;
//    }
//
//    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
//
//        @Override
//        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
////            surfaceTexture.updateTexImage();
////            Log.e("requestRender ", "onFrameAvailable");
//        }
//    };
//
//    @Override
//    public void setFilter(String filterName) {
//        super.setFilter(filterName);
//        mNeedResetEglContext = true;
//    }
//
//
//    public void changeRecordingState(boolean isRecording) {
//        recordingEnabled = isRecording;
//    }
//
//    protected void onFilterChanged() {
//        super.onFilterChanged();
//        cameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
//    }
//
//    @Override
//    public void onResume() {
//        GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
//        GPUImageContext.sharedImageProcessingContexts().destroyProgrames();
//        if (cameraEngine.getCamera() == null) {
//            if (cameraEngine.getNumberOfCameras() == 1) {
//                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
//            }
//            cameraEngine.openCamera(context, this.mCameraID);
//            this.mCameraID = cameraEngine.cameraID;
//
//            mSupportedPreviewSizes = cameraEngine.getSupportedPreviewSize(new String[]{"1280x720"});
//        }
//        mIsPaused = false;
//        mSetPreViewSizeSucceed = false;
//        createFilter();
//        super.onResume();
//        this.forceLayout();
//        mAccelerometer.start();
//    }
//
//    @Override
//    public void onPause() {
//        LogUtils.i(TAG, "onPause");
//        mSetPreViewSizeSucceed = false;
//        //mCurrentSticker = null;
//        mIsPaused = true;
//        mImageData = null;
//        cameraEngine.releaseCamera();
//        LogUtils.d(TAG, "Release camera");
//
//        queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                mSTHumanActionNative.reset();
//                mStBeautifyNative.destroyBeautify();
//                mStStickerNative.destroyInstance();
//                mImageData = null;
//                deleteTextures();
//                if (surfaceTexture != null) {
//                    surfaceTexture.release();
//                    surfaceTexture = null;
//                }
//                destoryFilter();
//            }
//        });
//
//        super.onPause();
//    }
//
//
//    public void savePicture(final int width, final int height) {
//        if (cameraInputFilter != null) {
//            cameraInputFilter.setTakeePhto(true, new GPUSurfaceCameraView.TakePhtoListener() {
//                @Override
//                public void takePhtoComplete(final Bitmap bitmap) {
//                    queueEvent(new Runnable() {
//                        @Override
//                        public void run() {
//                            Bitmap newbitMap = Common.zoomImg(bitmap, width);
////                            RenderFilter renderFilter = new RenderFilter(newbitMap, EGL14.eglGetCurrentContext());
////                            renderFilter.start();
////                            GPUImagePicture picture = new GPUImagePicture(newbitMap);
////                            for (int i = 0; i < LookupInstance.getInstance(mContext).getFilterTitles().size(); i++) {
////                                FilterModel filterModel = LookupInstance.getInstance(mContext).getAllFilter().get(i);
////                                String name = LookupInstance.getInstance(mContext).getFilterTitles().get(i);
////                                VNiImageFilter filter = new VNiImageFilter(mContext, name);
////                                filter.init();
////                                picture.init();
////                                picture.addTarget(filter, 0);
////                                picture.processImage();
////                                Bitmap lookMap = filter.newBitMapFromCurrentlyProcessedOutput();
////                                filterModel.setNickName(lookMap);
////                            }
//
//                        }
//                    });
//                }
//            });
//        }
//    }
//
//    private class RenderFilter extends Thread {
//        private Bitmap newbitMap = null;
//        private EGLContext shared_context = null;
//        private EGLBase mEgl = null;
//        private EglSurface mInputSurface = null;
//        private FilterCallable callable;
//
//        RenderFilter(Bitmap bitmap, final EGLContext shared_context, FilterCallable filterCallable) {
//            this.newbitMap = bitmap;
//            this.shared_context = shared_context;
//            this.callable = filterCallable;
//        }
//
//        @Override
//        public void run() {
//            mEgl = new EGLBase(shared_context, false, true);
//            mInputSurface = mEgl.createOffscreen(newbitMap.getWidth(), newbitMap.getHeight());
//            mInputSurface.makeCurrent();
//            GPUImagePicture picture = new GPUImagePicture(newbitMap);
//            for (int i = 0; i < LookupInstance.getInstance(mContext).getFilterTitles().size(); i++) {
//                picture.removeAllTargets();
//                FilterModel filterModel = LookupInstance.getInstance(mContext).getAllFilter().get(i);
//                String name = LookupInstance.getInstance(mContext).getFilterTitles().get(i);
//                VNiImageFilter filter = new VNiImageFilter(mContext, name);
//                filter.init();
//                picture.init();
//                picture.addTarget(filter, 0);
//                filter.useNextFrameForImageCapture();
//                picture.processImage();
//                GLES20.glFlush();
//                Bitmap lookMap = filter.newBitMapFromCurrentlyProcessedOutput();
//                filter.unload();
//                filterModel.setBitmap(lookMap);
//            }
//            if (this.callable != null)
//                this.callable.complete(LookupInstance.getInstance(mContext).getFilterArrayList());
//        }
//    }
//
//    public void getAllfilterModels(final int width, final FilterCallable callback) {
//        if (cameraInputFilter != null) {
//            cameraInputFilter.setTakeePhto(true, new GPUSurfaceCameraView.TakePhtoListener() {
//                @Override
//                public void takePhtoComplete(final Bitmap bitmap) {
//                    queueEvent(new Runnable() {
//                        @Override
//                        public void run() {
//                            Bitmap newbitMap = Common.zoomImg(bitmap, width);
////                            RenderFilter renderFilter = new RenderFilter(newbitMap,EGL14.eglGetCurrentContext(),callback);
////                            renderFilter.start();
//                            GPUImagePicture picture = new GPUImagePicture(newbitMap);
//                            for (int i = 0; i < LookupInstance.getInstance(mContext).getFilterTitles().size(); i++) {
//                                picture.removeAllTargets();
//                                FilterModel filterModel = LookupInstance.getInstance(mContext).getAllFilter().get(i);
//                                String name = LookupInstance.getInstance(mContext).getFilterTitles().get(i);
//                                VNiImageFilter filter = new VNiImageFilter(mContext, name);
//                                filter.init();
//                                picture.init();
//                                picture.addTarget(filter, 0);
//                                filter.useNextFrameForImageCapture();
//                                picture.processImage();
//                                GLES20.glFlush();
//                                Bitmap lookMap = filter.newBitMapFromCurrentlyProcessedOutput();
//                                filter.unload();
//                                filterModel.setBitmap(lookMap);
//                            }
//                            if (callback != null)
//                                callback.complete(LookupInstance.getInstance(mContext).getFilterArrayList());
//                        }
//                    });
//                }
//            });
//        }
//
//    }
//
//    public interface FilterCallable {
//        public void complete(ArrayList<FilterCateModel> filters);
//    }
//
////    @Override
////    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
////
////        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
////        int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
////
////        if (cameraEngine.getCamera() != null) {
////
////            List<Camera.Size> supportedPreviewSizes =
////                    cameraEngine.getCamera().getParameters().getSupportedPreviewSizes();
////            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
////                if (supportedPreviewSize.height == DensityUtils.getScreenWidth(context)) {
////                    width = supportedPreviewSize.height;
////                    height = supportedPreviewSize.width;
////                    break;
////                }
////            }
////        }
////        setMeasuredDimension(width, height);
////
////    }
//
//    @Override
//    public void startRecoding() {
//        this.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    int width, height;
//                    width = height = 0;
//                    videoRotation = getCurrentOrientation();
//                    if (videoRotation == 1 || videoRotation == 3) {
//                        width = imageHeight;
//                        height = imageWidth;
//                    } else {
//                        width = imageWidth;
//                        height = imageHeight;
//                    }
//
//                    int capability = CodecUtils.getCodecCapability();
//                    Size size = CodecUtils.reduceSize(width, height, capability);
//                    width = size.getWidth();
//                    height = size.getHeight();
//
//                    mediaMuxerWrapper = new MediaMuxerWrapper(".mp4");
//                    new MediaVideoEncoder(mediaMuxerWrapper, (MediaEncoder.MediaEncoderListener) mMediaEncoderListener, width, height);
//                    if (mIsHasAudioPermission) {
//                        new MediaAudioEncoder(mediaMuxerWrapper, mMediaEncoderListener);
//                    }
//                    mediaMuxerWrapper.prepare();
//                    mediaMuxerWrapper.startRecording();
//                    mNeedResetEglContext = true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });
//    }
//
//    private Matrix4f videoRotationMatrix(int dir) {
//        Matrix4f matrix4f = new Matrix4f();
//        matrix4f.loadIdentity();
//        switch (dir) {
//            case 3: {
//                matrix4f.rotate(270, 0, 0, 1);
//                return matrix4f;
//            }
//            case 2: {
//                matrix4f.rotate(180, 0, 0, 1);
//                return matrix4f;
//            }
//            case 1: {
//                matrix4f.rotate(90, 0, 0, 1);
//                return matrix4f;
//            }
//            default:
//                return matrix4f;
//        }
//    }
//
//    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
//        @Override
//        public void onPrepared(final MediaEncoder encoder) {
//            if (encoder instanceof MediaVideoEncoder) {
//                mVideoEncoder = (MediaVideoEncoder) encoder;
//            }
//        }
//
//        @Override
//        public void onStopped(final MediaEncoder encoder) {
//            if (encoder instanceof MediaVideoEncoder) {
//                mVideoEncoder = null;
//                if (recordListener != null) {
//                    recordListener.stop(outputFilter);
//                }
//            }
//        }
//    };
//
//    String outputFilter;
//
//    @Override
//    public void stopRecording() {
//        this.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                if (mediaMuxerWrapper != null) {
//                    mediaMuxerWrapper.stopRecording();
//                    outputFilter = mediaMuxerWrapper.getFilePath();
//
//                }
//            }
//        });
//
//    }
//
//    @Override
//    public void cancle() {
//        //显示预览 ，停止编码 ， 不给回调
//        super.cancle();
//    }
//
//    public String getOutPutFilePath() {
//        if (mediaMuxerWrapper != null) {
//            return mediaMuxerWrapper.getFilePath();
//        }
//        return null;
//    }
//
//    public interface TakePhtoListener {
//        public void takePhtoComplete(Bitmap bitmap);
//    }
//
//    public void onDestroy() {
//        //必须释放非opengGL句柄资源,负责内存泄漏
//        synchronized (mHumanActionHandleLock) {
//            mSTHumanActionNative.destroyInstance();
//        }
//        mStStickerNative.destroyInstance();
//        mStBeautifyNative.destroyBeautify();
//    }
//
//    /**
//     * 释放纹理资源
//     */
//    protected void deleteTextures() {
//        LogUtils.i(TAG, "delete textures");
//        deleteCameraPreviewTexture();
//        deleteInternalTextures();
//    }
//
//    // must in opengl thread
//    private void deleteCameraPreviewTexture() {
//        if (textureId != OpenGLUtils.NO_TEXTURE) {
//            GLES20.glDeleteTextures(1, new int[]{
//                    textureId
//            }, 0);
//        }
//        textureId = OpenGLUtils.NO_TEXTURE;
//    }
//
//    private void deleteInternalTextures() {
//        if (mBeautifyTextureId != null) {
//            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
//            mBeautifyTextureId = null;
//        }
//
//        if (mTextureOutId != null) {
//            GLES20.glDeleteTextures(1, mTextureOutId, 0);
//            mTextureOutId = null;
//        }
//    }
//
//    private void deleteFramebuffer() {
//        cameraInputFilter.destroyFramebuffers();
//    }
//
//
//    private boolean isRecoding = false;
//    private String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
//    private String PREF_ACTIVATE_CODE = "activate_code";
//    private final String LICENSE_NAME = "senseme.lic";
//    public static final String MODEL_NAME_ACTION = "M_SenseME_Action_5.0.3.model";
//    public static final String MODEL_NAME_FACE_ATTRIBUTE =
//            "M_SenseME_Attribute_1.0.1.model";
//    public static final String MODEL_NAME_EYEBALL_CONTOUR =
//            "M_SenseME_Iris_1.7.0.model";
//    public static final String MODEL_NAME_FACE_EXTRA =
//            "M_SenseME_Face_Extra_5.1.0.model";
//    private boolean mIsHasAudioPermission = false;
//
//
//}
