package com.zp.libvideoedit.EditCore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.Effect.VNiImageFilter;
import com.zp.libvideoedit.GPUImage.Carma.Core.GPUSurfaceCameraView;
import com.zp.libvideoedit.GPUImage.Carma.utils.EGLBase;
import com.zp.libvideoedit.GPUImage.Core.AndroidDispatchQueue;
import com.zp.libvideoedit.GPUImage.Core.EglCore;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.OffscreenSurface;
import com.zp.libvideoedit.GPUImage.Filter.GPUImageTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNIColorFadeTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNISmoothRotateTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNISmoothZoomTransitionFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNiFilterManager;
import com.zp.libvideoedit.GPUImage.Filter.VNiVideoBlendFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageAddBlendFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraInputFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageCameraOutput;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.modle.AfterEffectListener;
import com.zp.libvideoedit.modle.AudioMixInputParameter;
import com.zp.libvideoedit.modle.AudioMixParam;
import com.zp.libvideoedit.modle.Chunk;
import com.zp.libvideoedit.modle.FilterModel;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.TrackType;
import com.zp.libvideoedit.modle.Transition.Origentation;
import com.zp.libvideoedit.modle.Transition.TransitionStyle;
import com.zp.libvideoedit.modle.Transition.VNITransitionRotateType;
import com.zp.libvideoedit.modle.Transition.VNITransitionZoomType;
import com.zp.libvideoedit.modle.VideoCompositionCallBack;
import com.zp.libvideoedit.modle.VideoSegment;
import com.zp.libvideoedit.modle.VideoTimer;
import com.zp.libvideoedit.modle.effectModel.EffectAdapter;
import com.zp.libvideoedit.modle.effectModel.EffectType;
import com.zp.libvideoedit.utils.Common;
import com.zp.libvideoedit.utils.EffectAdapterSortBySortPosition;
import com.zp.libvideoedit.utils.FormatUtils;
import com.zp.libvideoedit.utils.FrameExtratorUtil;
import com.zp.libvideoedit.utils.LookupInstance;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.TAG_V;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_LOOP_V;
import static com.zp.libvideoedit.EditConstants.VERBOSE_SEEK;
import static com.zp.libvideoedit.EditConstants.VERBOSE_V;
import static com.zp.libvideoedit.EditConstants.VIDEO_PRE_START_TIME;
import static com.zp.libvideoedit.modle.MediaType.MEDIA_TYPE_Video;
import static com.zp.libvideoedit.modle.TrackType.TrackType_Video_Main;
import static com.zp.libvideoedit.modle.TrackType.TrackType_Video_Mask;
import static com.zp.libvideoedit.modle.TrackType.TrackType_Video_Mask_Ext;
import static com.zp.libvideoedit.modle.TrackType.TrackType_Video_Second;
import static com.zp.libvideoedit.modle.TrackType.TrackType_Video_Transition;
import static com.zp.libvideoedit.utils.FormatUtils.caller;
import static com.zp.libvideoedit.utils.FormatUtils.generateCallStack;


/**
 * Created by gwd on 2018/3/13.
 * 用于管理所有的videotrack的类
 */

public class VideoPlayerCoreManager implements GLSurfaceView.Renderer, VideoDecoderThread.DecoderCallBack, SurfaceTexture.OnFrameAvailableListener {
    private static final int surfaceMaxValue = 5;
    private static final int MESSEGETEXTRE_CREATED = 100;
    public static int count = 0;
    protected Context context;
    protected AfterEffectListener.VideoTrackListener trackListener;
    protected boolean eofFlag;
    protected GPUImageCameraInputFilter firstVideoinputFilter;
    protected GPUImageCameraInputFilter secondVideoinputFilter;
    protected GPUImageCameraInputFilter maskVideoinputFilter;
    protected GPUImageCameraInputFilter maskExtVideoinputFilter;
    protected GPUImageCameraInputFilter transitionVideoinputFilter;
    protected MediaTrack firstMediatrack;
    protected MediaTrack secondMediaTrack;
    protected MediaTrack maskMediaTrack;
    protected MediaTrack maskExtMediaTrack;
    protected MediaTrack transitionMediaTrack;
    protected GPUImageFilter dataOutput;
    protected GPUImageFilter blendDataOutput;
    protected GPUImageCameraOutput blendoutput;
    protected GPUImageFilter blendExtDataOutput;
    protected GPUImageFilter transitionDataOutput;
    protected GPUImageCameraOutput transitionOutput;
    protected GPUImageCameraOutput output;
    protected GPUImageCameraOutput blendExtoutput;
    boolean seekFlag = false;
    private ArrayList<MediaTrack<VideoSegment>> mediaTracks = null;
    private VideoTimer timer = null;
    private SurfaceTexture[] surfaceTextures;
    private Surface[] surfaces;
    private GLSurfaceView glSurfaceView;
    private int[] texutres = null;
    private Object glSurfaceViewGLReadySynchObject = new Object();
    private boolean playerRenderAlready = false;
    private int width;
    private int height;
    private boolean inited = false;
    private boolean decoderThreadInited = false;

    private GPUImageAddBlendFilter addBlendFilter;
    private boolean hasFistAvailableData;
    private boolean hasSecondAvailableData;
    private boolean hasBlendAvailableData;
    private boolean hasBlendExtAvailableData;
    private boolean hasTransitionAvailableData;
    private long currentPts;
    private CMTime currentTime = null;
    private MediaComposition mediaComposition;
    //    private VNiVideoBlendFilter videoBlendFilter;
    private GPUImageFilter testFilter;
    private long firstVideoLastPts = 0;
    private long secondVideoLastPts = 0;
    private VideoEffect videoEffect;
    private VNiFilterManager foregroundFilterManager;
    private VNiFilterManager backgroundFilterManager;
    private int renderFilterWidth = 0;
    private boolean hasVideoBlend = false;
    private boolean hasPicBlend = false;
    private boolean firstPicture = false;
    private VideoManagerCallBack videoManagerCallBack;
    private double playingProgress;
    private AndroidDispatchQueue mContextQueue = AndroidDispatchQueue.dispatchQueueCreate("VPM");
    private boolean isCaptureFirstVideo = false;
    private VNiVideoBlendFilter videoBlendFilter;
    private AudioMixParam audioMixParam;

    protected VideoDecoderThread firstVideoDecoderThread;
    protected VideoDecoderThread secondVideoDecoderThread;
    protected VideoDecoderThread maskVideoDecoderThread;
    protected VideoDecoderThread maskExtVideoDecoderThread;
    protected VideoDecoderThread transitionVideoDecoderThread;

    private boolean playing = false;
    private int unReadyCount;//为准备好数量
    private long currentSeekTime;//当前seek时间

    //texture创建完成之后回调创建surface
    public Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSEGETEXTRE_CREATED) {
                initifNeeded();
                if (VERBOSE)
                    Log.i(TAG, "VideoPlayerCoreManager_MESSEGETEXTRE_CREATED PLAY_LIFECYCLE ");
//                seekTo(CMTime.zeroTime());
            }
            return false;
        }
    });

    public VideoPlayerCoreManager(Context context, GLSurfaceView glSurfaceView, int width, int height, VideoTimer timer, VideoManagerCallBack videoManagerCallBack) {
        this.context = context;
        this.timer = timer;
        this.playerRenderAlready = false;
        this.width = width;
        this.height = height;
        this.glSurfaceView = glSurfaceView;
        currentTime = CMTime.zeroTime();
        this.videoManagerCallBack = videoManagerCallBack;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
//        glSurfaceView.setZOrderOnTop(true);
        this.glSurfaceView.setRenderer(this);
        this.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        surfaceTextures = new SurfaceTexture[surfaceMaxValue];
        surfaces = new Surface[surfaceMaxValue];
        createInputFilter();
        foregroundFilterManager = new VNiFilterManager(context);
        backgroundFilterManager = new VNiFilterManager(context);
        videoBlendFilter = new VNiVideoBlendFilter();
    }

    public void setViewSize(GPUSize size) {
        this.width = size.width;
        this.height = size.height;
    }

    public void setMediaComposition(MediaComposition mediaComposition, VideoTimer timer, VideoEffect videoEffect) {

        if (VERBOSE) {
            Log.i(TAG, "VideoPlayerCoreManager_setMediaComposition PLAY_LIFECYCLE ");
            mediaComposition.prettyPrintLog();
        }
        this.mediaComposition = mediaComposition;
        this.timer = timer;
        this.videoEffect = videoEffect;
        this.mediaTracks = (ArrayList) this.mediaComposition.trackOfType(MEDIA_TYPE_Video);
//        for (VideoDecoderThread decoderThread : videoTrackDecoderThreads) {
//            decoderThread.pause();
//        }
        pauseVideoDecoderThreads();
//        releaseDecoder();
        initifNeeded();
        seekTo(CMTime.zeroTime(), false);


    }

    private void createInputFilter() {
        firstVideoinputFilter = new GPUImageCameraInputFilter();
        secondVideoinputFilter = new GPUImageCameraInputFilter();
        maskVideoinputFilter = new GPUImageCameraInputFilter();
        maskExtVideoinputFilter = new GPUImageCameraInputFilter();
        transitionVideoinputFilter = new GPUImageCameraInputFilter();
        output = new GPUImageCameraOutput();
        addBlendFilter = new GPUImageAddBlendFilter();
//        videoBlendFilter = new VNiVideoBlendFilter();

        blendoutput = new GPUImageCameraOutput();
        blendExtoutput = new GPUImageCameraOutput();
        transitionOutput = new GPUImageCameraOutput();

    }

    private int getTexture(TrackType trackType) {
        return texutres[trackType.getValue()];
    }

    /**
     * 初始化解码线程
     *
     * @param mediaTrack
     * @return
     */
    private VideoDecoderThread initDecordThreadIfNeed(MediaTrack mediaTrack, Surface surface, int textureId) {
        TrackType trackType = mediaTrack.getTrackType();
        if (TrackType_Video_Main == trackType) {
            if (firstVideoDecoderThread == null) {
                unReadyCount++;
                firstVideoDecoderThread = new VideoDecoderThread(mediaTrack, surface, width, height, timer, textureId, this);
                firstVideoDecoderThread.start();
            } else {
                firstVideoDecoderThread.resetDecode(mediaTrack);
            }
            return firstVideoDecoderThread;
        }

        if (TrackType_Video_Second == trackType) {
            if (secondVideoDecoderThread == null) {
                unReadyCount++;
                secondVideoDecoderThread = new VideoDecoderThread(mediaTrack, surface, width, height, timer, textureId, this);
                secondVideoDecoderThread.start();
            } else {
                secondVideoDecoderThread.resetDecode(mediaTrack);
            }
            return secondVideoDecoderThread;
        }

        if (TrackType_Video_Mask == trackType) {

            if (maskVideoDecoderThread == null) {
                unReadyCount++;
                maskVideoDecoderThread = new VideoDecoderThread(mediaTrack, surface, width, height, timer, textureId, this);
                maskVideoDecoderThread.start();
            } else {
                maskVideoDecoderThread.resetDecode(mediaTrack);
            }
            return maskVideoDecoderThread;
        }
        if (TrackType_Video_Mask_Ext == trackType) {

            if (maskExtVideoDecoderThread == null) {
                unReadyCount++;
                maskExtVideoDecoderThread = new VideoDecoderThread(mediaTrack, surface, width, height, timer, textureId, this);
                maskExtVideoDecoderThread.start();
            } else {
                maskExtVideoDecoderThread.resetDecode(mediaTrack);
            }
            return maskExtVideoDecoderThread;
        }
        if (TrackType_Video_Transition == trackType) {
            if (transitionVideoDecoderThread == null) {
                unReadyCount++;
                transitionVideoDecoderThread = new VideoDecoderThread(mediaTrack, surface, width, height, timer, textureId, this);
                transitionVideoDecoderThread.start();
            } else {
                transitionVideoDecoderThread.resetDecode(mediaTrack);
            }
            return transitionVideoDecoderThread;
        }
        return null;
    }


    private SurfaceTexture getSurfaceTexture(TrackType trackType) {
        return surfaceTextures[trackType.getValue()];
    }

    //重新创建surface 并且启动解码线程
    private void initifNeeded() {
        Log.w(TAG, "initifNeeded" + "," + Arrays.toString(texutres) + "\t" + mediaComposition);
        if (texutres == null) return;

        if (mediaTracks == null) return;
        inited = false;
        for (MediaTrack mediaTrack : mediaTracks) {
            int index = mediaTrack.getTrackType().getValue();
            int textureId = texutres[index];
            mediaTrack.setSurfaceTextrureid(textureId);

            SurfaceTexture surfaceTexture = surfaceTextures[index];
            if (surfaceTexture == null) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTextures[index] = surfaceTexture;
                if (mediaTrack.getTrackType() == TrackType_Video_Main || mediaTrack.getTrackType() == TrackType_Video_Second || mediaTrack.getTrackType() == TrackType_Video_Transition) {
                    surfaceTexture.setOnFrameAvailableListener(videoSurfaceListener);
                } else if (mediaTrack.getTrackType() == TrackType_Video_Mask || mediaTrack.getTrackType() == TrackType_Video_Mask_Ext) {
                    surfaceTexture.setOnFrameAvailableListener(maskSurfaceListener);
                }
                surfaceTexture.setOnFrameAvailableListener(this);
            }
            mediaTrack.setSurfaceTexture(surfaceTexture);

            Surface surface = surfaces[index];
            if (surface == null) {
                surface = new Surface(surfaceTexture);
                surfaces[index] = surface;
            }

            initDecordThreadIfNeed(mediaTrack, surface, textureId);

            if (mediaTrack.getTrackType() == TrackType_Video_Main) {
                firstMediatrack = mediaTrack;
            } else if (mediaTrack.getTrackType() == TrackType_Video_Second) {
                secondMediaTrack = mediaTrack;
            } else if (mediaTrack.getTrackType() == TrackType_Video_Mask) {
                maskMediaTrack = mediaTrack;
            } else if (mediaTrack.getTrackType() == TrackType_Video_Mask_Ext) {
                maskExtMediaTrack = mediaTrack;
            } else if (mediaTrack.getTrackType() == TrackType_Video_Transition) {
                transitionMediaTrack = mediaTrack;
            }
//            if (videoManagerCallBack != null) videoManagerCallBack.onPlayerReady();
        }
        inited = true;

        if (videoManagerCallBack != null && inited && unReadyCount == 0)
            AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoManagerCallBack.onCompositionComplete();
                }
            });

    }

    private SurfaceTexture.OnFrameAvailableListener videoSurfaceListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, Thread.currentThread().getName() + "onFrameAvailable: hasVaildData :" + surfaceTexture);
            if (firstMediatrack != null && surfaceTexture == firstMediatrack.getSurfaceTexture()) {
                if (VERBOSE_LOOP_V) {
                    Log.d(TAG_V, Thread.currentThread().getName() + "_onFrameAvailable: 视频第一路:" + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
                }
                hasFistAvailableData = true;
            }
            if (secondMediaTrack != null && surfaceTexture == secondMediaTrack.getSurfaceTexture()) {
                if (VERBOSE_LOOP_V) {
                    Log.d(TAG_V, Thread.currentThread().getName() + "_onFrameAvailable: 视频第二路:" + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
                }
                hasSecondAvailableData = true;
            }
            if (transitionMediaTrack != null && surfaceTexture == transitionMediaTrack.getSurfaceTexture()) {
                hasTransitionAvailableData = true;
                if (VERBOSE_LOOP_V) {
                    Log.d(TAG_V, Thread.currentThread().getName() + "_onFrameAvailable: 视频 Transition " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
                }
            }
            requestRender();
        }
    };
    private SurfaceTexture.OnFrameAvailableListener maskSurfaceListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (maskExtMediaTrack != null && surfaceTexture == maskExtMediaTrack.getSurfaceTexture()) {
                if (VERBOSE_LOOP_V) {
                    Log.d(TAG_V, Thread.currentThread().getName() + "_onFrameAvailable: 视频maskExt:" + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
                }
                hasBlendExtAvailableData = true;
            }
            if (maskMediaTrack != null && surfaceTexture == maskMediaTrack.getSurfaceTexture()) {
                hasBlendAvailableData = true;
                if (VERBOSE_LOOP_V) {
                    Log.d(TAG_V, Thread.currentThread().getName() + "_onFrameAvailable: 视频mask:" + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
                }
            }
            requestRender();
        }
    };

    /**
     * 释放提取器
     */
    public void releaseExtors() {

    }

    public void resume() {
        if (VERBOSE_V)
            Log.d(TAG, "VideoPlayerCoreManager_resume.. PLAY_LIFECYCLE");
        List<VideoDecoderThread> decoderThreads = getVideoDecoderThread();
        if (decoderThreads != null && decoderThreads.size() != 0) {
            Iterator<VideoDecoderThread> iterator = decoderThreads.iterator();
            while (iterator.hasNext()) {
                iterator.next().resumeDecode();
            }
        } else {
            stop();
            initifNeeded();
            start();
        }

    }


    public void start() {
        if (firstVideoDecoderThread != null)
            firstVideoDecoderThread.resumeDecode();
        if (secondVideoDecoderThread != null)
            secondVideoDecoderThread.resumeDecode();
        if (maskVideoDecoderThread != null)
            maskVideoDecoderThread.resumeDecode();
        if (maskExtVideoDecoderThread != null)
            maskExtVideoDecoderThread.resumeDecode();
        if (transitionVideoDecoderThread != null)
            transitionVideoDecoderThread.resumeDecode();
        playing = true;
    }

    public void pause() {
        if (VERBOSE_V)
            Log.d(TAG, caller() + "VideoPlayerCoreManager_pause.. PLAY_LIFECYCLE " + generateCallStack());
        playing = false;
        pauseVideoDecoderThreads();

    }

    public void stop() {
        if (VERBOSE_V) {
            Log.d(TAG_V, caller() + "... PLAY_LIFECYCLE ");
        }
        playing = false;
        try {
            if (firstVideoDecoderThread != null)
                firstVideoDecoderThread.stopDecode();
            if (secondVideoDecoderThread != null)
                secondVideoDecoderThread.stopDecode();
            if (maskVideoDecoderThread != null)
                maskVideoDecoderThread.stopDecode();
            if (maskExtVideoDecoderThread != null)
                maskExtVideoDecoderThread.stopDecode();
            if (transitionVideoDecoderThread != null)
                transitionVideoDecoderThread.stopDecode();
        } catch (Exception e) {
            Log.w(TAG, "VideoPlayerCoreManager_stop error", e);
        }

        if (VERBOSE_V) {
            Log.d(TAG_V, caller() + "oook");
        }
    }

    public void setEofFlag(boolean eofFlag) {
        this.eofFlag = eofFlag;
    }

    public AfterEffectListener.VideoTrackListener getTrackListener() {
        return trackListener;
    }

    private void createTexture() {
        this.texutres = new int[surfaceMaxValue];
        GLES20.glGenTextures(surfaceMaxValue, texutres, 0);
        Message message = new Message();
        message.what = MESSEGETEXTRE_CREATED;
        handler.sendMessage(message);

    }

    public void requestRender() {
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }

    private void initFilter() {
        foregroundFilterManager.init();
        backgroundFilterManager.init();
        output.init();
        firstVideoinputFilter.init();
        secondVideoinputFilter.init();
        maskExtVideoinputFilter.init();
        maskVideoinputFilter.init();
        transitionVideoinputFilter.init();
        addBlendFilter.init();

        blendoutput.init();
        blendExtoutput.init();

        transitionOutput.init();
        videoBlendFilter.init();

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("GLSurfaceView", "Thread already");
        initFilter();
        createTexture();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        LogUtil.e("surface", width + " ... " + height);
        if (VERBOSE)
            Log.d(TAG, "glSurfaceView onSurfaceChanged " + "width:  " + width + " height " + height);
        GLES20.glViewport(0, 0, width, height);
//        output.setDisplaySize(new GPUSize(width, height));
//        blendoutput.setDisplaySize(new GPUSize(width, height));
//        blendExtoutput.setDisplaySize(new GPUSize(width, height));
//        transitionOutput.setDisplaySize(new GPUSize(width, height));
        firstVideoinputFilter.onInputSizeChanged(width, height);
        secondVideoinputFilter.onInputSizeChanged(width, height);
        maskVideoinputFilter.onInputSizeChanged(width, height);
        maskExtVideoinputFilter.onInputSizeChanged(width, height);
        transitionVideoinputFilter.onInputSizeChanged(width, height);
        this.width = width;
        this.height = height;
    }

    private void clearnTarget() {
        firstVideoinputFilter.removeAllTargets();
        secondVideoinputFilter.removeAllTargets();
        maskVideoinputFilter.removeAllTargets();
        maskExtVideoinputFilter.removeAllTargets();
        foregroundFilterManager.removeAllTargets();
        backgroundFilterManager.removeAllTargets();
    }


    private GPUImageFilter updateAdapter(GPUImageFilter lastFilter, float second) {
        lastFilter.removeAllTargets();
        Collections.sort(videoEffect.getEffects(), new EffectAdapterSortBySortPosition());
        ArrayList<EffectAdapter> effectAdapters = videoEffect.getEffects();
        lastFilter.removeAllTargets();
        for (int i = 0; i < effectAdapters.size(); i++) {
            EffectAdapter adapter = effectAdapters.get(i);

            boolean canAddFilter = CMTime.getSecond(adapter.getTimeRange().getStartTime()) <= second
                    && CMTime.getSecond(adapter.getTimeRange().getEnd()) > second;
            if (!canAddFilter) {
                if (VERBOSE)
                    Log.i("onDrawFrame", "updateAdapter()_Don't Conform to the conditions! Not in the valid interval!");
                continue;
            }
            if (adapter.getEffectType() == EffectType.EffectType_Video) {//添加视频排版
                //多检测一下文件完整性，缺少文件不能添加
                canAddFilter = canAddFilter && (adapter.getMaskExtVideoChunk() != null && adapter.getMaskVideoChunk() != null);
                if (!canAddFilter) {
                    if (VERBOSE)
                        Log.i("onDrawFrame", "updateAdapter()_Don't Conform to the conditions!");
                    continue;
                }
                hasVideoBlend = true;
                videoBlendFilter.removeAllTargets();
                lastFilter.addTarget(videoBlendFilter, 0);
                maskVideoinputFilter.addTarget(videoBlendFilter, 1);
                maskExtVideoinputFilter.addTarget(videoBlendFilter, 2);
                lastFilter = videoBlendFilter;
            }
        }

        for (int i = 0; i < effectAdapters.size(); i++) {
            EffectAdapter adapter = effectAdapters.get(i);

            boolean canAddFilter = CMTime.getSecond(adapter.getTimeRange().getStartTime()) <= second
                    && CMTime.getSecond(adapter.getTimeRange().getEnd()) > second;
            if (!canAddFilter) {
                if (VERBOSE)
                    Log.i("onDrawFrame", "updateAdapter()_Don't Conform to the conditions! Not in the valid interval!");
                continue;
            }
//            if (adapter.getEffectType() == EffectType.EffectType_Pic) {//添加文字
//                hasPicBlend = true;
//                if (adapter.canSetBitMap() && SelectActivity.getTypesetBitmaps() != null) {
//                    if (adapter.getPosition() >= SelectActivity.getTypesetBitmaps().size()) {
//                        Log.w(TAG_V, "VideoPlayerCoreManager_UpdateAdapter_Adapter IndexOutOfBounds:" +
//                                "adapter.getPosition()=" + adapter.getPosition() +
//                                "；SelectActivity.getTypesetBitmaps().size=" + SelectActivity.getTypesetBitmaps().size());
//                        continue;
//                    }
//                    String imgPath = SelectActivity.getTypesetBitmaps().get(adapter.getPosition()).getImgPath();
//                    Bitmap bitmap = ImageDownloader.getInstance().getBitmapFromMemCache(imgPath);
//                    if (bitmap == null) {
//                        bitmap = BitmapUtil.loadFileToBitmap(imgPath);
//                        if (bitmap == null) continue;
//                        ImageDownloader.getInstance().addBitmapToMemory(imgPath, bitmap);
//                    }
//                    adapter.setBitmap(bitmap);
//                }
//                adapter.getFilter().removeAllTargets();
//                adapter.getPicture().removeAllTargets();
//                adapter.getPicture().addTarget(adapter.getFilter(), 1);
//                adapter.getPicture().init();
//                adapter.getFilter().init();
//                lastFilter.addTarget(adapter.getFilter(), 0);
//                adapter.getPicture().processImage();
//                lastFilter = adapter.getFilter();
//            } else if (adapter.getEffectType() == EffectType.EffectType_Sticker) {//添加贴纸
//                hasPicBlend = true;
//                if (adapter.getStickerConfig() == null) continue;
//                StickerModel stickerModel = adapter.getStickerConfig().getStickerModelByTime(second - (float) CMTime.getSecond(adapter.getTimeRange().getStartTime()));
//                if (stickerModel == null) continue;
//                String imgPath = stickerModel.getPicPath();
//                Bitmap bitmap = ImageDownloader.getInstance().getBitmapFromMemCache(imgPath);
//                if (bitmap == null) {
//                    bitmap = BitmapUtil.loadFileToBitmap(imgPath);
//                    if (bitmap == null) continue;
//                    ImageDownloader.getInstance().addBitmapToMemory(imgPath, bitmap);
//                }
//                adapter.setBitmap(bitmap);
//                adapter.getFilter().removeAllTargets();
//                adapter.getPicture().removeAllTargets();
//                adapter.getPicture().addTarget(adapter.getFilter(), 1);
//                adapter.getPicture().init();
//                adapter.getFilter().init();
//                ((VNIStickerFilter) adapter.getFilter()).setStickerConfig(adapter.getStickerConfig(), new GPUSize(bitmap.getWidth(), bitmap.getHeight()));
//                lastFilter.addTarget(adapter.getFilter(), 0);
//                adapter.getPicture().processImage();
//                lastFilter = adapter.getFilter();
//            }
        }
        return lastFilter;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long timeStampOfTimer = timer.getCurrentTimeMs() * 1000 * 1000;
        clearnTarget();
        long currentTimeStamp = 0;
        //获取当前渲染的时间戳
        if (secondMediaTrack == null && firstMediatrack != null) {
            if (VERBOSE_LOOP_V)
                Log.i("onDrawFrame", "==========>firstMediatrack=" + firstMediatrack + ";;firstMediatrack.getSurfaceTexture()=" + firstMediatrack.getSurfaceTexture());
            firstMediatrack.getSurfaceTexture().updateTexImage();
            currentTimeStamp = firstMediatrack.getSurfaceTexture().getTimestamp();
            if (currentTimeStamp == 0) {
                currentTimeStamp = timeStampOfTimer;
            }
            if (VERBOSE_V) {
                Log.d(TAG_V, "SurfaceTexture_getTimestamp_timer currentTime: " + String.format("%2d", timeStampOfTimer) + "   firstMediatTrackTimeStamp: " + String.format("%2d", currentTimeStamp) + "\t" + firstMediatrack.getSurfaceTextrureid() + ":" + firstMediatrack.getSurfaceTexture());
            }

        } else if (secondMediaTrack == null && firstMediatrack == null) {
            currentTimeStamp = 0;
        } else {
            firstMediatrack.getSurfaceTexture().updateTexImage();
            secondMediaTrack.getSurfaceTexture().updateTexImage();
            long t1 = firstMediatrack.getSurfaceTexture().getTimestamp();
            long t2 = secondMediaTrack.getSurfaceTexture().getTimestamp();
            if (VERBOSE_V) {
                Log.d(TAG_V, "SurfaceTexture_getTimestamp_timer currentTime: " + String.format("%2d", timeStampOfTimer) + "   firstMediatTrackTimeStamp: " + String.format("%2d", t1) + "   secondMediaTrackTimeStamp:  " + String.format("%2d", t2) + "\t" + firstMediatrack.getSurfaceTextrureid() + ":" + firstMediatrack.getSurfaceTexture() + "\t" + secondMediaTrack.getSurfaceTextrureid() + ":" + secondMediaTrack.getSurfaceTexture());
            }
            if (Math.abs(t1 - timeStampOfTimer) < Math.abs(t2 - timeStampOfTimer)) {

                currentTimeStamp = t1;
            } else {
                currentTimeStamp = t2;
            }
            if (t1 == 0 && t2 == 0) {
                currentTimeStamp = timeStampOfTimer;
            }

        }
        currentTime = new CMTime(currentTimeStamp, EditConstants.NS_MUTIPLE);
        //判断chunk
        float second = currentTimeStamp / (1.0f * EditConstants.NS_MUTIPLE);
        if (VERBOSE_LOOP_V) Log.d("renderRender", "on draw frame " + second);
        if (videoEffect == null) {
            return;
        }

        ArrayList<Chunk> currentChunks = videoEffect.getChunksWithSecond(second);
        if (currentChunks.size() == 1 && currentChunks.get(0).chunkIndex % 2 == 0 && firstMediatrack != null) {
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            Chunk chunk = currentChunks.get(0);
            if (chunk == null || chunk.getVideoFile() == null || TextUtils.isEmpty(chunk.getVideoFile().getFilePath()))
                return;
            chunk.updateAspectFillTransFormCanvaSize(new GPUSize(this.width, this.height));
            //移除入口的target
            firstVideoinputFilter.removeAllTargets();
            //添加需要的filtr
            foregroundFilterManager.setEffectAdapters(videoEffect.getAllEffect());
            foregroundFilterManager.updateFilter(chunk, currentTime);
//            //first add 调色
            firstVideoinputFilter.addTarget(foregroundFilterManager.getInputfilter());
//            //调色add 输出
            GPUImageFilter lastFilter = foregroundFilterManager.getOutput();
            //视频排版
            GPUImageFilter lastfilter = updateAdapter(lastFilter, second);
            if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && !hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = firstMediatrack.getSurfaceTextrureid();
                firstMediatrack.getSurfaceTexture().updateTexImage();
                firstVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
                //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);

            } else if (!hasVideoBlend && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = firstMediatrack.getSurfaceTextrureid();
                firstMediatrack.getSurfaceTexture().updateTexImage();
                firstVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
            } else if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = firstMediatrack.getSurfaceTextrureid();
                firstMediatrack.getSurfaceTexture().updateTexImage();
                firstVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
//                        //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
//                        //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);
            } else {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                firstMediatrack.getSurfaceTexture().updateTexImage();
                int texture = firstMediatrack.getSurfaceTextrureid();
                firstVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                firstVideoinputFilter.onDrawToTexture(texture, currentTimeStamp);
                if (VERBOSE_V)
                    Log.w(TAG, "onDrawFrame————" + "第一路视频渲染" + currentTimeStamp / (1.0 * EditConstants.NS_MUTIPLE));

            }
            if (!firstPicture) {
//                Bitmap bitmap = firstVideoinputFilter.newBitMapFromCurrentlyProcessedOutput();
//                videoEffect.setCoverImg(bitmap);
//                firstPicture = true;
            }
            hasFistAvailableData = false;

        } else if (currentChunks.size() == 1 && currentChunks.get(0).chunkIndex % 2 == 1 && secondMediaTrack != null) {
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            Chunk chunk = currentChunks.get(0);
            if (chunk == null || chunk.getVideoFile() == null || TextUtils.isEmpty(chunk.getVideoFile().getFilePath()))
                return;
            chunk.updateAspectFillTransFormCanvaSize(new GPUSize(this.width, this.height));
            //移除入口的target
            secondVideoinputFilter.removeAllTargets();
            //添加需要的filtr
            backgroundFilterManager.setEffectAdapters(videoEffect.getAllEffect());
            backgroundFilterManager.updateFilter(chunk, currentTime);
            //first add 调色
            secondVideoinputFilter.addTarget(backgroundFilterManager.getInputfilter());
            //调色add 输出
//            foregroundFilterManager.getOutput().addTarget(output);
            GPUImageFilter lastFilter = backgroundFilterManager.getOutput();

            GPUImageFilter lastfilter = updateAdapter(lastFilter, second);
            if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && !hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                secondMediaTrack.getSurfaceTexture().updateTexImage();
                secondVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                secondVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
//                        //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
//                        //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);

            } else if (!hasVideoBlend && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                secondMediaTrack.getSurfaceTexture().updateTexImage();
                secondVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                secondVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
            } else if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                secondMediaTrack.getSurfaceTexture().updateTexImage();
                secondVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                secondVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
//                        //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
//                        //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);
            } else {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                secondMediaTrack.getSurfaceTexture().updateTexImage();
                int texture = secondMediaTrack.getSurfaceTextrureid();
                secondVideoinputFilter.setViewportRange(chunk.getmViewportRange());
                secondVideoinputFilter.onDrawToTexture(texture, currentTimeStamp);
                if (VERBOSE_V)
                    Log.w(TAG, "onDrawFrame————" + "第二路视频渲染" + currentTimeStamp / (1.0 * EditConstants.NS_MUTIPLE));

            }
            hasSecondAvailableData = false;
            //双路视频
        } else if (currentChunks.size() == 2 && firstMediatrack != null && secondMediaTrack != null) {
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            Chunk firstChunk = currentChunks.get(0);
            if (firstChunk == null || firstChunk.getVideoFile() == null || TextUtils.isEmpty(firstChunk.getVideoFile().getFilePath()))
                return;
            int firstChunkIndex = firstChunk.chunkIndex;
            firstChunk.updateAspectFillTransFormCanvaSize(new GPUSize(this.width, this.height));

            Chunk secondChunk = currentChunks.get(1);
            if (secondChunk == null || secondChunk.getVideoFile() == null || TextUtils.isEmpty(secondChunk.getVideoFile().getFilePath()))
                return;
            int secondChunkIndex = secondChunk.chunkIndex;
            this.foregroundFilterManager.setEffectAdapters(videoEffect.getAllEffect());
            this.foregroundFilterManager.updateFilter(firstChunk, currentTime);
            this.backgroundFilterManager.setEffectAdapters(videoEffect.getAllEffect());
            this.backgroundFilterManager.updateFilter(secondChunk, currentTime);
            secondChunk.updateAspectFillTransFormCanvaSize(new GPUSize(this.width, this.height));

            //调色
            this.firstVideoinputFilter.addTarget(foregroundFilterManager.getInputfilter());
            GPUImageFilter lastFilterOfFirstChunk = foregroundFilterManager.getOutput();
            lastFilterOfFirstChunk.removeAllTargets();
            this.secondVideoinputFilter.addTarget(backgroundFilterManager.getInputfilter());
            GPUImageFilter lastFilterOfSecondChunk = backgroundFilterManager.getOutput();
            lastFilterOfSecondChunk.removeAllTargets();
            //滤镜转场
            GPUImageFilter lastFilter = lastFilterOfFirstChunk;
            GPUImageTransitionFilter transitionFilter = secondChunk.getTransitionFilter();
            if (transitionFilter != null) {
                transitionFilter.removeAllTargets();
                lastFilterOfFirstChunk.addTarget(transitionFilter, 0);
                lastFilterOfSecondChunk.addTarget(transitionFilter, 1);
                lastFilter = transitionFilter;
            }

            if (transitionFilter != null && !transitionFilter.isFilterInited()) {
                transitionFilter.init();
                if (secondChunk.getTransitionStyle() == TransitionStyle.VNITransitionTypeWhiteFade && transitionFilter instanceof VNIColorFadeTransitionFilter) {
                    VNIColorFadeTransitionFilter filter = (VNIColorFadeTransitionFilter) transitionFilter;
                    float[] blackColor = {1.0f, 1.0f, 1.0f, 1.0f};
                    filter.setFadeColor(blackColor);
                } else if (secondChunk.getTransitionStyle() == TransitionStyle.VNITransitionTypeSmoothZoomOut && transitionFilter instanceof VNISmoothZoomTransitionFilter) {
                    VNISmoothZoomTransitionFilter filter = (VNISmoothZoomTransitionFilter) transitionFilter;
                    filter.setType(VNITransitionZoomType.VNITransitionZoomTypeOut);
                    if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Vertical) {
                        filter.setRatio(9.f / 16.f);
                    } else if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Horizontal) {
                        filter.setRatio(16.f / 9.f);
                    }
                } else if (secondChunk.getTransitionStyle() == TransitionStyle.VNITransitionTypeSmoothRotateRight && transitionFilter instanceof VNISmoothRotateTransitionFilter) {
                    VNISmoothRotateTransitionFilter filter = (VNISmoothRotateTransitionFilter) transitionFilter;
                    filter.setType(VNITransitionRotateType.VNITransitionRotateTypeRight);
                    if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Vertical) {
                        filter.setRatio(9.f / 16.f);
                    } else if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Horizontal) {
                        filter.setRatio(16.f / 9.f);
                    }
                } else if (secondChunk.getTransitionStyle() == TransitionStyle.VNITransitionTypeSmoothRotateLeft && transitionFilter instanceof VNISmoothRotateTransitionFilter) {
                    VNISmoothRotateTransitionFilter filter = (VNISmoothRotateTransitionFilter) transitionFilter;
                    filter.setType(VNITransitionRotateType.VNITransitionRotateTypeLeft);
                    if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Vertical) {
                        filter.setRatio(9.f / 16.f);
                    } else if (secondChunk.getVideoAspectOrigentation() == Origentation.kVideo_Horizontal) {
                        filter.setRatio(16.f / 9.f);
                    }
                }
            }
            if (transitionFilter != null) {
                float progress = 0.f;
                CMTime firstChunkTransitionEndTime = firstChunk.getChunkTransitionEndTime();
                //需要算时间
                if (currentTime.getSecond() <= firstChunkTransitionEndTime.getSecond()) {
                    progress = (float) CMTime.subTime(currentTime, CMTime.subTime(firstChunk.getEndTime(), firstChunk.chunkTransitionTailTime)).getSecond() / (float) (firstChunk.chunkTransitionTailTime.getSecond());
                }
                CMTime secondChunkTransitionStartTime = secondChunk.getChunkTransitionStartTime();
                if (currentTime.getSecond() >= secondChunkTransitionStartTime.getSecond()) {
                    progress = (float) CMTime.subTime(currentTime, secondChunkTransitionStartTime).getSecond() / (float) secondChunk.getChunkTransitionHeadTime().getSecond() + 0.5f;
                }
                transitionFilter.setProgress(progress);
            }

            GPUImageFilter lastfilter = updateAdapter(lastFilter, second);
            if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && !hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                if (firstChunkIndex % 2 == 0) {
                    int videoTexture = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                } else {
                    int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                }

//                        //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
//                        //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);
//                if (transitionFilter != null && transitionMediaTrack != null) {
//                    int transitionTexture = transitionMediaTrack.getSurfaceTextrureid();
//                    transitionMediaTrack.getSurfaceTexture().updateTexImage();
//                    transitionVideoinputFilter.onDrawToTexture(transitionTexture, currentTimeStamp);
//                }


            } else if (!hasVideoBlend && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id

                if (firstChunkIndex % 2 == 0) {
                    int videoTexture = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                } else {
                    int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                }


//                if (transitionFilter != null && transitionMediaTrack != null) {
//                    int transitionTexture = transitionMediaTrack.getSurfaceTextrureid();
//                    transitionMediaTrack.getSurfaceTexture().updateTexImage();
//                    transitionVideoinputFilter.onDrawToTexture(transitionTexture, currentTimeStamp);
//                }

            } else if ((hasVideoBlend && maskMediaTrack != null && maskExtMediaTrack != null) && hasPicBlend) {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                //视频id
                //视频id

                if (firstChunkIndex % 2 == 0) {
                    int videoTexture = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                } else {
                    int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                }


//                if (transitionFilter != null && transitionMediaTrack != null) {
//                    int transitionTexture = transitionMediaTrack.getSurfaceTextrureid();
//                    transitionMediaTrack.getSurfaceTexture().updateTexImage();
//                    transitionVideoinputFilter.onDrawToTexture(transitionTexture, currentTimeStamp);
//                }
//                        //获取mask id
                int maskTexture = maskMediaTrack.getSurfaceTextrureid();
                maskMediaTrack.getSurfaceTexture().updateTexImage();
                maskVideoinputFilter.onDrawToTexture(maskTexture, currentTimeStamp);
//                        //获取maskext id
                int maskextTexture = maskExtMediaTrack.getSurfaceTextrureid();
                maskExtMediaTrack.getSurfaceTexture().updateTexImage();
                maskExtVideoinputFilter.onDrawToTexture(maskextTexture, currentTimeStamp);

            } else {
                lastfilter.removeAllTargets();
                lastfilter.addTarget(output);
                if (firstChunkIndex % 2 == 0) {
                    int videoTexture = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                } else {
                    int videoTexture = secondMediaTrack.getSurfaceTextrureid();
                    secondMediaTrack.getSurfaceTexture().updateTexImage();
                    firstVideoinputFilter.setViewportRange(firstChunk.getmViewportRange());
                    firstVideoinputFilter.onDrawToTexture(videoTexture, currentTimeStamp);
                    //secondid
                    int videoTexture2 = firstMediatrack.getSurfaceTextrureid();
                    firstMediatrack.getSurfaceTexture().updateTexImage();
                    secondVideoinputFilter.setViewportRange(secondChunk.getmViewportRange());
                    secondVideoinputFilter.onDrawToTexture(videoTexture2, currentTimeStamp);
                }

//                if (transitionFilter != null && transitionMediaTrack != null) {
//                    int transitionTexture = transitionMediaTrack.getSurfaceTextrureid();
//                    transitionMediaTrack.getSurfaceTexture().updateTexImage();
//                    transitionVideoinputFilter.onDrawToTexture(transitionTexture, currentTimeStamp);
//                    if (EditConstants.VERBOSE_V)
//                        Log.w(EditConstants.TAG, "onDrawFrame————" + "转场视频渲染" + currentTimeStamp / (1.0 * EditConstants.NS_MUTIPLE));
//
//                } else {
//                    if (EditConstants.VERBOSE_V)
//                        Log.w(EditConstants.TAG, "onDrawFrame————" + "转场filter没有draw" + currentTimeStamp / (1.0 * EditConstants.NS_MUTIPLE));
//
//                }

            }

        } else {
            //TODO 错误处理
            if (VERBOSE)
                Log.e(TAG_V, caller() + "should_never_happed_render_error:second:" + second + ", currentChunks:" + (currentChunks != null ? currentChunks.size() : "-1") + "chunkIndex:" + (currentChunks != null && currentChunks.size() > 0 ? currentChunks.get(0).chunkIndex : -1));
        }

    }

    public void release() {
        //停止解码
        stop();
        releaseDecoder();
        //释放提取器
        releaseExtors();
        if (mContextQueue != null) {
            AndroidDispatchQueue.dispatchQueueDestroy(mContextQueue);
        }
        try {
            if (foregroundFilterManager != null)
                foregroundFilterManager.clearSpecialEffect();
            if (backgroundFilterManager != null)
                backgroundFilterManager.clearSpecialEffect();
        } catch (Exception e) {
            Log.e(TAG_V, "ClearSpecialEffect ERROR:" + e.getMessage());
        }
    }

    @Override
    public void onFrameArrive(VideoDecoderThread decoderThread, MediaTrack mediaTrack, VideoSegment segment, long frameIndexInchannel, long decodePts) {
        if (VERBOSE_LOOP_V)
            Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + decodePts + "\t" + timer.getCurrentTimeMs());
        if (playing)
            wakeupDecoderThreadsIfNeed(decoderThread, mediaTrack, decodePts);

        double duration = mediaComposition.getDuration().getUs();
        if (duration > 0) {
            final double percent = decodePts / duration;
            if (percent > playingProgress) {
                playingProgress = percent;
                final long currentPalyTimeInOrange = segment.getSrcUsNew(decodePts);
                if (VERBOSE_LOOP_V)
                    Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",playingProgress:" + playingProgress);
                final WeakReference<VideoPlayerCoreManager> self = new WeakReference<VideoPlayerCoreManager>(this);
                AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                    @Override
                    public void run() {

                        videoManagerCallBack.onVideoPlaying(self.get(), playingProgress, currentPalyTimeInOrange);
                    }
                });

            }
        }

    }

    @Override
    public void onDecoderThreadReady(VideoDecoderThread decoderThread) {
        decoderThreadInited = true;
        unReadyCount--;
        final WeakReference<VideoPlayerCoreManager> self = new WeakReference<VideoPlayerCoreManager>(this);
        if (videoManagerCallBack != null)
            AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                @Override
                public void run() {
                    videoManagerCallBack.onVideoManagerReady(self.get());
                }
            });

        if (videoManagerCallBack != null && inited && unReadyCount == 0)
            AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoManagerCallBack.onCompositionComplete();
                }
            });
    }

    @Override
    public void onTrackEos(VideoDecoderThread decoderThread, MediaTrack mediaTrack, long decodePts) {
        if (VERBOSE_V)
            Log.d(TAG_V, "VideoPlayerCoreManager_onTrackEos_Thread:" + decoderThread.getName() + "__" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId()
                    + "_decodePts:" + String.format("%,d", decodePts)
                    + ",trackDuration:" + String.format("%,d", mediaTrack.getDuration().getUs())
                    + ", compositionDuration:" + String.format("%,d", mediaComposition.getVideoDuration().getUs()));


        if (videoManagerCallBack != null && mediaComposition.isLongest(mediaTrack)) {
            playingProgress = 1;
            final WeakReference<VideoPlayerCoreManager> self = new WeakReference<VideoPlayerCoreManager>(this);
            AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoManagerCallBack.onVideoPlaying(self.get(), playingProgress, 0);
                    videoManagerCallBack.onVideoPlayFinished(self.get());
                }
            });
        }
    }

    @Override
    public void onDecoderPaused(VideoDecoderThread decoderThread) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (VERBOSE_LOOP_V)
            Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: hasVaildData :" + surfaceTexture);
        if (firstMediatrack != null && surfaceTexture == firstMediatrack.getSurfaceTexture()) {
            if (VERBOSE_LOOP_V) {
                Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: 视频第一路: " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
            }
            hasFistAvailableData = true;
        }
        if (secondMediaTrack != null && surfaceTexture == secondMediaTrack.getSurfaceTexture()) {
            if (VERBOSE_LOOP_V) {
                Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: 视频第二路: " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
            }
            hasSecondAvailableData = true;
        }
        if (maskExtMediaTrack != null && surfaceTexture == maskExtMediaTrack.getSurfaceTexture()) {
            if (VERBOSE_LOOP_V) {
                Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: 视频maskExt: " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
            }
            hasBlendExtAvailableData = true;
        }
        if (maskMediaTrack != null && surfaceTexture == maskMediaTrack.getSurfaceTexture()) {
            hasBlendAvailableData = true;
            if (VERBOSE_LOOP_V) {
                Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: 视频mask: " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
            }
        }
        if (transitionMediaTrack != null && surfaceTexture == transitionMediaTrack.getSurfaceTexture()) {
            hasTransitionAvailableData = true;
            if (VERBOSE_LOOP_V) {
                Log.d(TAG_V, caller() + Thread.currentThread().getName() + "_onFrameAvailable: 视频 Transition: " + (surfaceTexture.getTimestamp() / 1.0 * EditConstants.NS_MUTIPLE));
            }
        }
        requestRender();
    }


    public void screenshotForFilter(final int width, final VideoCompositionCallBack callBack) {
        this.renderFilterWidth = width;
        float currentTime = (float) CMTime.getSecond(timer.getcCurrentTime());
        Chunk chunk = null;
        if (videoEffect != null) {
            ArrayList<Chunk> chunks = videoEffect.getChunksWithSecond(currentTime);
            if (chunks.size() > 0) {
                chunk = chunks.get(0);
            }
            if (chunk == null) return;
            if (chunk.chunkIndex % 2 == 0) {
                isCaptureFirstVideo = true;
            } else {
                isCaptureFirstVideo = false;
            }
        }
        final String videoPath = (chunk.isReverseVideo() ? chunk.getReverseVideoPath() : chunk.getFilePath());
        this.glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isCaptureFirstVideo) {
                    if (foregroundFilterManager != null) {
                        foregroundFilterManager.createCurrentBitmap(new GPUSurfaceCameraView.TakePhtoListener() {
                            @Override
                            public void takePhtoComplete(Bitmap bitmap) {
                                renderByFilter(bitmap, videoPath, callBack);
                            }
                        });
                    }

                } else {
                    if (backgroundFilterManager != null) {
                        backgroundFilterManager.createCurrentBitmap(new GPUSurfaceCameraView.TakePhtoListener() {
                            @Override
                            public void takePhtoComplete(Bitmap bitmap) {
                                renderByFilter(bitmap, videoPath, callBack);
                            }
                        });
                    }
                }
            }
        });
        this.requestRender();
    }


    private void renderByFilter(Bitmap bitmap, String videoPath, final VideoCompositionCallBack callBack) {
        if (bitmap == null) {
            FrameExtratorUtil.getInstance().frameExtrator(videoPath, Arrays.asList(0L), 180, new FrameExtratorUtil.FrameGenerated() {
                @Override
                public void onFrameGenerated(Bitmap thumb, String videoPath, String chunkId, long pts) {
                    thumb = Common.convert(thumb, true, false, 180);
                    renderForFilter(thumb, EGL14.eglGetCurrentContext(), callBack);
                }

                @Override
                public void onSuccessed() {

                }
            });
        } else {
            bitmap = Common.convert(bitmap, true, false, 180);
            renderForFilter(bitmap, EGL14.eglGetCurrentContext(), callBack);
        }
    }

    public void setAudioMixParam(AudioMixParam audioMixParam) {
        if (mediaComposition == null) return;
        this.audioMixParam = audioMixParam;
        for (AudioMixInputParameter inputParameter : audioMixParam.getInputParameters()) {
            MediaTrack mediaTrack = mediaComposition.trackOfTrackType(inputParameter.getTrackType());
            mediaTrack.setInputParameter(inputParameter);
        }
    }

    private class RenderFilter extends Thread {
        private Bitmap newbitMap = null;
        private EGLContext shared_context = null;
        private EGLBase mEgl = null;
        private EGLBase.EglSurface mInputSurface = null;
        private VideoCompositionCallBack callable;

        RenderFilter(Bitmap bitmap, final EGLContext shared_context, VideoCompositionCallBack filterCallable) {
            this.newbitMap = bitmap;
            this.shared_context = shared_context;
            this.callable = filterCallable;
        }

        @Override
        public void run() {
            mEgl = new EGLBase(shared_context, false, true);
            EglCore mEglCore = new EglCore(shared_context, EglCore.FLAG_RECORDABLE);
            OffscreenSurface windowSurface = new OffscreenSurface(mEglCore, newbitMap.getWidth(), newbitMap.getHeight());
            windowSurface.makeCurrent();
//            mInputSurface = mEgl.createOffscreen(newbitMap.getWidth(), newbitMap.getHeight());
//            mInputSurface.makeCurrent();
            GPUImagePicture picture = new GPUImagePicture(newbitMap);
            for (int i = 0; i < LookupInstance.getInstance(context).getFilterTitles().size(); i++) {
                picture.removeAllTargets();
                FilterModel filterModel = LookupInstance.getInstance(context).getAllFilter().get(i);
                String name = LookupInstance.getInstance(context).getFilterTitles().get(i);
                VNiImageFilter filter = new VNiImageFilter(context, name);
                filter.useNextFrameForImageCapture();
                filter.init();
                picture.init();
                picture.addTarget(filter, 0);
                picture.processImage();
                GLES20.glFlush();
//                Bitmap lookMap = filter.newBitMapFromCurrentlyProcessedOutput();
                filter.unload();
//                filterModel.setBitmap(lookMap);
            }
            if (this.callable != null)
                this.callable.finishedRenderFilter(LookupInstance.getInstance(context).getFilterArrayList());
        }
    }

    private void renderForFilter(final Bitmap bitmap, EGLContext glcontext, VideoCompositionCallBack callBack) {
        final Bitmap newBitmap = Common.zoomImg(bitmap, renderFilterWidth);
        GPUImagePicture picture = new GPUImagePicture(newBitmap);
        for (int i = 0; i < LookupInstance.getInstance(context).getFilterTitles().size(); i++) {
            picture.removeAllTargets();
            FilterModel filterModel = LookupInstance.getInstance(context).getAllFilter().get(i);
            String name = LookupInstance.getInstance(context).getFilterTitles().get(i);
            VNiImageFilter filter = new VNiImageFilter(context, name);
            filter.useNextFrameForImageCapture();
            filter.init();
            picture.init();
            picture.addTarget(filter, 0);
            picture.processImage();
            GLES20.glFlush();
//            Bitmap lookMap = filter.newBitMapFromCurrentlyProcessedOutput();
            filter.unload();
//            filterModel.setBitmap(lookMap);
        }
        if (callBack != null)
            callBack.finishedRenderFilter(LookupInstance.getInstance(context).getFilterArrayList());
    }

    public void seekTo(final CMTime time) {
        seekTo(time, true);
    }

    public void seekTo(CMTime time, boolean updatePreview) {
//        if (currentSeekTime == time.getMs()) {
//            if (VERBOSE_SEEK)
//                Log.i(TAG, "VideoPlayerCoreManager_seekTo. PLAY_LIFECYCLE 重复Seek本次跳过 inited:"
//                        + inited + "\tdecoderThreadInited:" + decoderThreadInited + "\t" + "time:" + time
//                        + ",updatePreview:" + updatePreview + "\t" + FormatUtils.generateCallStack() + "\t" + "currentSeekTime:" + currentSeekTime);
//            return;
//        }
        currentSeekTime = time.getMs();

        double seekSec = -1;
        if (videoEffect != null) {//防止seek到最后取不到数据
            float durationSec = videoEffect.getProjectDuration();
            if (Math.abs(durationSec - CMTime.getSecond(time)) <= 0.04f)
                seekSec = durationSec - 0.04f;
            else
                seekSec = CMTime.getSecond(time);
        }

        final CMTime seekTime = new CMTime(seekSec);
        if (VERBOSE_SEEK)
            Log.i(TAG, "VideoPlayerCoreManager_seekTo. PLAY_LIFECYCLE inited:" + inited + "\tdecoderThreadInited:" + decoderThreadInited + "\t" + "time:" + time + ",updatePreview:" + updatePreview + "\t" + FormatUtils.generateCallStack());
        if (!inited || !decoderThreadInited) return;
        pause();

        timer.seekTime(seekTime.getMs());
        List<VideoDecoderThread> decoderThreads = getVideoDecoderThread();
        if (updatePreview) {
            for (final VideoDecoderThread decoderThread : decoderThreads) {
                AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, new Runnable() {
                    @Override
                    public void run() {
                        decoderThread.seek(seekTime.getUs());
                    }
                });
            }
        } else {
            for (VideoDecoderThread decoderThread : decoderThreads)
                decoderThread.seek(seekTime.getUs(), false);
        }
        playingProgress = 0;
    }

    public interface VideoManagerCallBack {
        void onVideoManagerReady(VideoPlayerCoreManager videoPlayerCoreManager);

        void onVideoPlaying(VideoPlayerCoreManager videoPlayerCoreManager, double percent, long currentPalyTimeInOrange);

        void onVideoPlayFinished(VideoPlayerCoreManager videoPlayerCoreManager);

        void onVideoPlayerPaused(VideoPlayerCoreManager videoPlayerCoreManager);

        //准备成功
        void onCompositionComplete();
    }


    private void pauseVideoDecoderThreads() {
        if (firstVideoDecoderThread != null)
            firstVideoDecoderThread.pause();
        if (secondVideoDecoderThread != null)
            secondVideoDecoderThread.pause();
        if (maskVideoDecoderThread != null)
            maskVideoDecoderThread.pause();
        if (maskExtVideoDecoderThread != null)
            maskExtVideoDecoderThread.pause();
        if (transitionVideoDecoderThread != null)
            transitionVideoDecoderThread.pause();
    }

    private void wakeupDecoderThreadsIfNeed(VideoDecoderThread decoderThread, MediaTrack mediaTrack, long decodePts) {
//优化wakeup 满足0.5秒的视频片段
        if (firstVideoDecoderThread != null && firstVideoDecoderThread != decoderThread
                && (firstVideoDecoderThread.getMediaTrack().getMediaType() == MEDIA_TYPE_Video)) {
            MediaTrack threadMediaTrack = firstVideoDecoderThread.getMediaTrack();
            long wakeupTime = firstVideoDecoderThread.wakeupIfNeed(decodePts, VIDEO_PRE_START_TIME);
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",resume:" + threadMediaTrack.getTrackType().getName() + "_" + threadMediaTrack.getTrackId() + ",  wakeupTime：" + String.format("%,d", wakeupTime));
        }
        if (secondVideoDecoderThread != null && secondVideoDecoderThread != decoderThread
                && (secondVideoDecoderThread.getMediaTrack().getMediaType() == MEDIA_TYPE_Video)) {
            MediaTrack threadMediaTrack = secondVideoDecoderThread.getMediaTrack();
            long wakeupTime = secondVideoDecoderThread.wakeupIfNeed(decodePts, VIDEO_PRE_START_TIME);
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",resume:" + threadMediaTrack.getTrackType().getName() + "_" + threadMediaTrack.getTrackId() + ",  wakeupTime：" + String.format("%,d", wakeupTime));

        }
        if (maskVideoDecoderThread != null && maskVideoDecoderThread != decoderThread
                && (maskVideoDecoderThread.getMediaTrack().getMediaType() == MEDIA_TYPE_Video)) {
            MediaTrack threadMediaTrack = maskVideoDecoderThread.getMediaTrack();
            long wakeupTime = maskVideoDecoderThread.wakeupIfNeed(decodePts, VIDEO_PRE_START_TIME);
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",resume:" + threadMediaTrack.getTrackType().getName() + "_" + threadMediaTrack.getTrackId() + ",  wakeupTime：" + String.format("%,d", wakeupTime));

        }
        if (maskExtVideoDecoderThread != null && maskExtVideoDecoderThread != decoderThread
                && (maskExtVideoDecoderThread.getMediaTrack().getMediaType() == MEDIA_TYPE_Video)) {
            MediaTrack threadMediaTrack = maskExtVideoDecoderThread.getMediaTrack();
            long wakeupTime = maskExtVideoDecoderThread.wakeupIfNeed(decodePts, VIDEO_PRE_START_TIME);
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",resume:" + threadMediaTrack.getTrackType().getName() + "_" + threadMediaTrack.getTrackId() + ",  wakeupTime：" + String.format("%,d", wakeupTime));

        }
        if (transitionVideoDecoderThread != null && transitionVideoDecoderThread != decoderThread
                && (transitionVideoDecoderThread.getMediaTrack().getMediaType() == MEDIA_TYPE_Video)) {
            MediaTrack threadMediaTrack = transitionVideoDecoderThread.getMediaTrack();
            long wakeupTime = transitionVideoDecoderThread.wakeupIfNeed(decodePts, VIDEO_PRE_START_TIME);
            if (VERBOSE_LOOP_V)
                Log.d(TAG_V, "VideoPlayerCoreManager_synch|" + Thread.currentThread().getName() + "|" + mediaTrack.getTrackType().getName() + "_" + mediaTrack.getTrackId() + "_decodePts:" + String.format("%,d", decodePts) + ",resume:" + threadMediaTrack.getTrackType().getName() + "_" + threadMediaTrack.getTrackId() + ",  wakeupTime：" + String.format("%,d", wakeupTime));

        }
    }

    /**
     * 获取所有存活的解码线程
     *
     * @return
     */
    private List<VideoDecoderThread> getVideoDecoderThread() {
        List<VideoDecoderThread> decoderThreads = new ArrayList<>();
        if (firstVideoDecoderThread != null)
            decoderThreads.add(firstVideoDecoderThread);
        if (secondVideoDecoderThread != null)
            decoderThreads.add(secondVideoDecoderThread);
        if (maskVideoDecoderThread != null)
            decoderThreads.add(maskVideoDecoderThread);
        if (maskExtVideoDecoderThread != null)
            decoderThreads.add(maskExtVideoDecoderThread);
        if (transitionVideoDecoderThread != null)
            decoderThreads.add(transitionVideoDecoderThread);
        return decoderThreads;
    }

    private void releaseDecoder() {
        if (firstVideoDecoderThread != null) {
            firstVideoDecoderThread.stopDecode();
            firstVideoDecoderThread.release();
            firstVideoDecoderThread = null;
        }
        if (secondVideoDecoderThread != null) {
            secondVideoDecoderThread.stopDecode();
            secondVideoDecoderThread.release();
            secondVideoDecoderThread = null;
        }
        if (maskVideoDecoderThread != null) {
            maskVideoDecoderThread.stopDecode();
            maskVideoDecoderThread.release();
            maskVideoDecoderThread = null;
        }
        if (maskExtVideoDecoderThread != null) {
            maskExtVideoDecoderThread.stopDecode();
            maskExtVideoDecoderThread.release();
            maskExtVideoDecoderThread = null;
        }
        if (transitionVideoDecoderThread != null) {
            transitionVideoDecoderThread.stopDecode();
            transitionVideoDecoderThread.release();
            transitionVideoDecoderThread = null;
        }
    }
}
