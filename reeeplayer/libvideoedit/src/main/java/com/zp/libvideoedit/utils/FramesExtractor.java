/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zp.libvideoedit.utils;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.renderscript.Matrix4f;
import android.util.Log;
import android.view.Surface;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.exceptions.EffectRuntimeException;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.TAG_FE;
import static com.zp.libvideoedit.EditConstants.US_MUTIPLE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_FE;
import static com.zp.libvideoedit.utils.FormatUtils.caller;


/**
 * 帧提取工具类。
 *
 * @author guoxian
 */
public class FramesExtractor {
    private static final int MAX_FRAMES = 1024; // stop extracting after this
    private static int threadIndex = 0;
    private String filePath;
    private int frameHeight;
    private int frameWidth;
    private List<Long> timesUsList;
    private long durationUS;
    private Callback callback;
    private int inputChunkCount = 0;
    private int decodeFrameIndex = 0;
    private int extractFrameIndex = 0;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private final int MSG_START_EXTRACTOR = 0;
    private final int MSG_CANCEL_EXTRACTOR = 1;
    private boolean stoping = false;
    private boolean stoped = false;
    private final Object stopLocker = new Object();
    Handler handler;
    private boolean exactExtract;
    private long step;
    private long exactThumbCount;

    public FramesExtractor(String filePath) {
        super();
        this.filePath = filePath;


    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    /**
     * 非关键帧视频抽图
     *
     * @param thumbCount  图片数量
     * @param thumbHeight 高度
     * @param callback    回调
     */
    public void extract(int thumbCount, int thumbHeight, final float excludeTailerSec, final Callback callback) {
        durationUS = (long) Math.round(CodecUtils.getVideoDurationMs(filePath) * 1000 - excludeTailerSec * US_MUTIPLE);
        if (thumbCount > 1)
            step = (long) (Math.round((durationUS * 1.0d - 0.1d * US_MUTIPLE) / (thumbCount - 1)));
        else if (thumbCount == 1) {
            step = 0;
        }
        if (VERBOSE_FE)
            Log.d(TAG_FE, "FramesExtractor_extract,step:" + step + ", thumbCount:" + thumbCount + ", thumbHeight" + thumbHeight + ",duration:" + durationUS + ",path:" + filePath);
        if (thumbCount < 1) return;
        exactThumbCount = thumbCount;
        extract(null, thumbHeight, true, callback);

    }

    /**
     * 提取视频文件的缩略图（只提取关键帧）
     *
     * @param timesUsList 时间点，单位us
     * @param thumbHeight 生成缩略图的高度。将按照视频比例进行等比压缩.如果<=0，使用原视频的宽和高
     * @param callback
     */
    public void extract(List<Long> timesUsList, int thumbHeight, final Callback callback) {
        extract(timesUsList, thumbHeight, false, callback);
    }

    /**
     * 提取视频文件的缩略图
     *
     * @param timesUsList  时间点，单位us
     * @param thumbHeight  生成缩略图的高度。将按照视频比例进行等比压缩.如果<=0，使用原视频的宽和高
     * @param exactExtract 是否精确提取
     * @param callback
     */
    public void extract(List<Long> timesUsList, int thumbHeight, boolean exactExtract, final Callback callback) {
        this.exactExtract = exactExtract;

        this.timesUsList = timesUsList;
        this.callback = callback;
        this.frameHeight = thumbHeight;
        final WeakReference<FramesExtractor> self = new WeakReference<FramesExtractor>(this);

        threadIndex++;
        String threadName = "FE_" + threadIndex;
        stoping = false;
        stoped = false;
        HandlerThread videoDecoderHandlerThread = new HandlerThread(threadName);
        videoDecoderHandlerThread.start();
        handler = new Handler(videoDecoderHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_START_EXTRACTOR:
                        try {
                            extractFrames();
                            if (!shouldStop())
                                callback.OnSuccessed(self.get(), extractFrameIndex);
                        } catch (Exception e) {
                            Log.e(TAG_FE, "", e);
                            if (!shouldStop())
                                callback.onError(self.get(), e);
                        }
                        break;
                    case MSG_CANCEL_EXTRACTOR: {
                        synchronized (stopLocker) {
                            if (!stoped) {
                                try {
                                    stopLocker.wait(50);
                                } catch (InterruptedException e) {
                                    Log.w(TAG, "extract_waite error", e);
                                }
                            }
                        }
                        Looper.myLooper().quitSafely();
                        if (VERBOSE_FE)
                            Log.d(TAG_FE, "FramesExtractor_cancel ok");

                    }
                }
            }
        };

        handler.sendMessage(handler.obtainMessage(MSG_START_EXTRACTOR));


    }

    /**
     * 取消提取缩略图
     */
    public void cancel() {
        if (VERBOSE_FE)
            Log.d(TAG_FE, "FramesExtractor_cancel...");
        stoping = true;
        handler.sendMessage(handler.obtainMessage(MSG_CANCEL_EXTRACTOR));
    }

    public boolean shouldStop() {
        if (VERBOSE_FE)
            Log.d(TAG_FE, caller() + "FramesExtractor_shouldStop:" + stoping);
        return stoping;
    }

    private void extractFrames() throws Exception {
        MediaCodec decoder = null;
        CodecOutputSurface outputSurface = null;
        MediaExtractor extractor = null;

        try {
            File inputFile = new File(filePath); // must be an absolute path

            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, filePath);
            int trackIndex = CodecUtils.selectVideoTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + inputFile);
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            if (VERBOSE_FE) {
                Log.d(TAG_FE, "Video size is " + format.getInteger(MediaFormat.KEY_WIDTH) + "x" + format.getInteger(MediaFormat.KEY_HEIGHT));
            }
            int rotation = 0;//CodecUtils.getVideoRotation(filePath);
            int videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            int videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            if (rotation == 90 || rotation == 270) {
                int temp = videoHeight;
                videoHeight = videoWidth;
                videoWidth = temp;
            }

            if (format.containsKey(MediaFormat.KEY_DURATION))
                durationUS = format.getLong(MediaFormat.KEY_DURATION);
            else {
                durationUS = CodecUtils.getDurationMS(filePath) * 1000;
            }
            if (frameHeight <= 0) {
                frameHeight = videoHeight;
                frameWidth = videoWidth;
            } else {
                frameWidth = videoWidth * frameHeight / videoHeight;
            }
            if (VERBOSE_FE) {
                Log.d(TAG_FE, "FramesExtrator_extractMpegFrames_CodecOutputSurface size:(" + frameWidth + "," + frameHeight + "), rotation:" + rotation);
            }
            outputSurface = new CodecOutputSurface(frameWidth, frameHeight, rotation);

            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface.getSurface(), null, 0);
            decoder.start();

            if (exactExtract && timesUsList != null) {
                doExactExtractForTimeList(extractor, trackIndex, decoder, outputSurface);
            } else if (exactExtract) {
                doExactExtract(extractor, trackIndex, decoder, outputSurface);
            } else {
                doExtract(extractor, trackIndex, decoder, outputSurface);
            }

        } finally {
            // release everything we grabbed
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "FramesExtractor_extractFrames_relase outputSurface ", e);
            }
            try {
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                }
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "FramesExtractor_extractFrames_relase decoder ", e);
            }
            try {
                if (extractor != null) {
                    extractor.release();
                }
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "FramesExtractor_extractFrames_relase extractor ", e);
            }
            if (VERBOSE_FE) {
                Log.d(TAG_FE, "FramesExtractor released codec stuff");
            }
            stoped = true;
        }
    }

    private void doExactExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder, CodecOutputSurface outputSurface) throws IOException {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        inputChunkCount = 0;
        decodeFrameIndex = 0;
        extractFrameIndex = 0;
        boolean outputDone = false;
        boolean inputDone = false;

        long nextpts = 0;

        while (!outputDone) {
            if (shouldStop()) return;
            if (!inputDone) {
                if (shouldStop()) return;
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {

                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
//                        extractor.seekTo(timesUsList.get(extractFrameIndex), MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            if (VERBOSE_FE)
                                Log.w(TAG_FE, "FramesExtractor WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /* flags */);
                        if (VERBOSE_FE) {
                            Log.d(TAG_FE, "FramesExtractor submitted frame " + inputChunkCount + " to dec, size=" + chunkSize);
                        }
                        inputChunkCount++;
                        extractFrameIndex++;


                        if (!extractor.advance()) {
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                            if (VERBOSE_FE)
                                Log.d(TAG_FE, "FramesExtractor advance false, sent input EOS");
                        }
                    }

                } else {
                    if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor input buffer not available");
                }
            }

            if (!outputDone) {
                if (shouldStop()) return;
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE_FE) Log.d(TAG_FE, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE_FE) Log.d(TAG_FE, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new EffectRuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE_FE) Log.d(TAG_FE, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0 && info.presentationTimeUs >= nextpts);
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (shouldStop()) return;
                    if (doRender) {

                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);

                        Bitmap thumb = outputSurface.generateFrame();
                        if (VERBOSE_FE)
                            Log.d(TAG_FE, "exactExtractgenerate thumbnail index:" + decodeFrameIndex + "\tpts:" + info.presentationTimeUs);
                        if (shouldStop()) return;
                        callback.onFrameGenerated(this, thumb, decodeFrameIndex, info.presentationTimeUs);
                        decodeFrameIndex++;
                        nextpts += step;
                        if (decodeFrameIndex >= exactThumbCount) outputDone = true;
                    }
                }
            }
        }

        if (VERBOSE_FE) {
            Log.d(TAG_FE, "exactExtract_finished.count of frame:" + decodeFrameIndex + "\t" + exactThumbCount + "\t" + nextpts);
        }

    }

    private void doExtract(MediaExtractor extractor, int trackIndex, MediaCodec
            decoder, CodecOutputSurface outputSurface) throws IOException {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        inputChunkCount = 0;
        decodeFrameIndex = 0;
        extractFrameIndex = 0;
        boolean outputDone = false;
        boolean inputDone = false;

        while (!outputDone) {
            if (shouldStop()) return;
            if (!inputDone) {
                if (shouldStop()) return;
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    if (extractFrameIndex >= timesUsList.size()) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE_FE)
                            Log.d(TAG_FE, "FramesExtractor next key frame exceed duration.sent input EOS");
                    } else {
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        extractor.seekTo(timesUsList.get(extractFrameIndex), MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        int chunkSize = extractor.readSampleData(inputBuf, 0);
                        if (chunkSize < 0) {
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                            if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor sent input EOS");
                        } else {
                            if (extractor.getSampleTrackIndex() != trackIndex) {
                                if (VERBOSE_FE)
                                    Log.w(TAG_FE, "FramesExtractor WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                            }
                            long presentationTimeUs = extractor.getSampleTime();
                            decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /* flags */);
                            if (VERBOSE_FE) {
                                Log.d(TAG_FE, "FramesExtractor submitted frame " + inputChunkCount + " to dec, size=" + chunkSize);
                            }
                            inputChunkCount++;
                            extractFrameIndex++;
                        }
                    }
                } else {
                    if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor input buffer not available");
                }
            }

            if (!outputDone) {
                if (shouldStop()) return;
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE_FE) Log.d(TAG_FE, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE_FE) Log.d(TAG_FE, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new EffectRuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE_FE) Log.d(TAG_FE, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);

                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (shouldStop()) return;
                    if (doRender) {

                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);

                        long startWhen = System.nanoTime();
                        Bitmap thumb = outputSurface.generateFrame();
                        if (VERBOSE_FE)
                            Log.d(TAG_FE, "generate thumbnail index:" + decodeFrameIndex + "\tpts:" + info.presentationTimeUs);
                        if (shouldStop()) return;
                        callback.onFrameGenerated(this, thumb, decodeFrameIndex, timesUsList.get(decodeFrameIndex));
                        decodeFrameIndex++;


                    }
                }
            }
        }

    }


    private void doExactExtractForTimeList(MediaExtractor extractor, int trackIndex, MediaCodec decoder, CodecOutputSurface outputSurface) throws IOException {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        inputChunkCount = 0;
        decodeFrameIndex = 0;
        extractFrameIndex = 0;
        boolean outputDone = false;
        boolean inputDone = false;

        long nextpts = timesUsList.get(decodeFrameIndex);

        while (!outputDone) {
            if (shouldStop()) return;
            if (!inputDone) {
                if (shouldStop()) return;
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {

                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
//                        extractor.seekTo(timesUsList.get(extractFrameIndex), MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            if (VERBOSE_FE)
                                Log.w(TAG_FE, "FramesExtractor WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, extractor.getSampleFlags()/* flags */);
                        if (VERBOSE_FE) {
                            Log.d(TAG_FE, "FramesExtractor submitted frame " + inputChunkCount + " to dec, size=" + chunkSize);
                        }
                        inputChunkCount++;
                        extractFrameIndex++;


                        if (!extractor.advance()) {
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                            if (VERBOSE_FE)
                                Log.d(TAG_FE, "FramesExtractor advance false, sent input EOS");
                        }
                    }

                } else {
                    if (VERBOSE_FE) Log.d(TAG_FE, "FramesExtractor input buffer not available");
                }
            }

            if (!outputDone) {
                if (shouldStop()) return;
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE_FE) Log.d(TAG_FE, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE_FE) Log.d(TAG_FE, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new EffectRuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if (VERBOSE_FE)
                        Log.d(TAG_FE, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE_FE) Log.d(TAG_FE, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0 && info.presentationTimeUs >= nextpts);
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (shouldStop()) return;
                    if (doRender) {

                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);

                        Bitmap thumb = outputSurface.generateFrame();
                        if (VERBOSE_FE)
                            Log.d(TAG_FE, "exactExtractgenerate thumbnail index:" + decodeFrameIndex + "\tpts:" + info.presentationTimeUs);
                        if (shouldStop()) return;
                        callback.onFrameGenerated(this, thumb, decodeFrameIndex, info.presentationTimeUs);
                        decodeFrameIndex++;
                        if (decodeFrameIndex < timesUsList.size()) {
                            nextpts = timesUsList.get(decodeFrameIndex);
                        }
                        if (decodeFrameIndex >= timesUsList.size()) outputDone = true;
                    }
                }
            }
        }

        if (VERBOSE_FE) {
            Log.d(TAG_FE, "exactExtract_finished.count of frame:" + decodeFrameIndex + "\t" + exactThumbCount + "\t" + nextpts);
        }

    }


    public interface Callback {
        /**
         * 生成缩略图回调
         *
         * @param framesExtractor
         * @param thumb           Bitmap 需要listener手动recycle
         * @param index
         * @param pts
         */
        public void onFrameGenerated(FramesExtractor framesExtractor, Bitmap thumb, int index, long pts);

        /**
         * 转码完成功成回调
         *
         * @param framesExtractor
         */
        public void OnSuccessed(FramesExtractor framesExtractor, int countOfThumb);

        /**
         * 转码失败回调
         *
         * @param framesExtractor
         * @param e
         */
        public void onError(FramesExtractor framesExtractor, Exception e);

    }

    /**
     * Code for rendering a texture onto a surface using OpenGL ES 2.0.
     */
    private static class STextureRender {
        /**
         * 申明单位矩阵
         */
        public static final float[] IDENTITY_MATRIX;
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;\n" + "attribute vec4 aPosition;\n" + "attribute vec4 aTextureCoord;\n" + "varying vec2 vTextureCoord;\n" + "void main() {\n" + "    gl_Position = uMVPMatrix * aPosition;\n" + "    vTextureCoord = (aTextureCoord).xy;\n" + "}\n";
        private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" + "precision mediump float;\n" + // highp here doesn't seem to matter
                "varying vec2 vTextureCoord;\n" + "uniform samplerExternalOES sTexture;\n" + "void main() {\n" + "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" + "}\n";

        static {
            IDENTITY_MATRIX = new float[16];
            Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        }

        private final float[] mTriangleVerticesData = {
                // X, Y, Z, U, V
                -1.0f, -1.0f, 0, 0.f, 0.f, 1.0f, -1.0f, 0, 1.f, 0.f, -1.0f, 1.0f, 0, 0.f, 1.f, 1.0f, 1.0f, 0, 1.f, 1.f,};
        int rotation;
        private FloatBuffer mTriangleVertices;
        private int mProgram;
        private int mTextureID = -12345;
        private int muMVPMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        public STextureRender(int rotation) {
            mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);
            this.rotation = rotation;

        }

        public static void checkLocation(int location, String label) {
            if (location < 0) {
                throw new RuntimeException("Unable to locate '" + label + "' in program");
            }
        }

        public int getTextureId() {
            return mTextureID;
        }

        public float[] getRatationMatrix() {
            Matrix4f matrix = new Matrix4f(IDENTITY_MATRIX);
            switch (rotation) {
                case 0:
                    break;
                case 90:
                    matrix.rotate(90, 0, 0, 1);
                    break;
                case 180:
                    matrix.rotate(180, 0, 0, 1);
                    break;
                case 270:
                    matrix.rotate(270, 0, 0, 1);
                    break;
                default:
                    break;
            }
            float[] viewModel = matrix.getArray();
            return viewModel;
        }

        /**
         * Draws the external texture in SurfaceTexture onto the current EGL
         * surface.
         */
        public void drawFrame(SurfaceTexture st, boolean invert) {
            checkGlError("onDrawFrame start");
            // (optional) clear to green so we can see if we're failing to set
            // pixels
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, this.getRatationMatrix(), 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        /**
         * Initializes GL state. Call this after the EGL surface has been
         * created and made current.
         */
        public void surfaceCreated() {
            mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgram == 0) {
                throw new RuntimeException("failed creating program");
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkLocation(maPositionHandle, "aPosition");
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkLocation(maTextureHandle, "aTextureCoord");

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkLocation(muMVPMatrixHandle, "uMVPMatrix");

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameter");
        }

        /**
         * Replaces the fragment shader. Pass in null to reset to default.
         */
        public void changeFragmentShader(String fragmentShader) {
            if (fragmentShader == null) {
                fragmentShader = FRAGMENT_SHADER;
            }
            GLES20.glDeleteProgram(mProgram);
            mProgram = createProgram(VERTEX_SHADER, fragmentShader);
            if (mProgram == 0) {
                throw new RuntimeException("failed creating program");
            }
        }

        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            checkGlError("glCreateShader type=" + shaderType);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG_FE, "Could not compile shader " + shaderType + ":");
                Log.e(TAG_FE, " " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program == 0) {
                Log.e(TAG_FE, "Could not create program");
            }
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG_FE, "Could not link program: ");
                Log.e(TAG_FE, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
            return program;
        }

        public void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                if (VERBOSE_FE) Log.e(TAG_FE, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }
    }

    private class CodecOutputSurface implements SurfaceTexture.OnFrameAvailableListener {
        int mWidth;
        int mHeight;
        private FramesExtractor.STextureRender mTextureRender;
        private SurfaceTexture mSurfaceTexture;
        private Surface mSurface;
        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        private Object mFrameSyncObject = new Object(); // guards
        // mFrameAvailable
        private boolean mFrameAvailable;

        private ByteBuffer mPixelBuf; // used by saveFrame()

        /**
         * Creates a CodecOutputSurface backed by a pbuffer with the specified
         * dimensions. The new EGL context and surface will be made current.
         * Creates a Surface that can be passed to MediaCodec.configure().
         */
        public CodecOutputSurface(int width, int height, int rotation) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException();
            }
            mWidth = width;
            mHeight = height;

            eglSetup();
            makeCurrent();
            setup(rotation);
        }

        /**
         * Creates interconnected instances of TextureRender, SurfaceTexture,
         * and Surface.
         */
        private void setup(int rotation) {
            mTextureRender = new FramesExtractor.STextureRender(rotation);
            mTextureRender.surfaceCreated();

            if (VERBOSE_FE) Log.d(TAG_FE, "textureID=" + mTextureRender.getTextureId());
            mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());

            mSurfaceTexture.setOnFrameAvailableListener(this);

            mSurface = new Surface(mSurfaceTexture);

            mPixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
            mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
            //			checkEglError("gl setup");
        }

        /**
         * Prepares EGL. We want a GLES 2.0 context and a surface that supports
         * pbuffer.
         */
        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = null;
                throw new RuntimeException("unable to initialize EGL14");
            }

            // Configure EGL for pbuffer and OpenGL ES 2.0, 24-bit RGB.
            int[] attribList = {EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE};
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL onAudioFormatChanged");
            }

            // Configure context for OpenGL ES 2.0.
            int[] attrib_list = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
            checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }

            // Create a pbuffer surface.
            int[] surfaceAttribs = {EGL14.EGL_WIDTH, mWidth, EGL14.EGL_HEIGHT, mHeight, EGL14.EGL_NONE};
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
            checkEglError("eglCreatePbufferSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }
        }

        /**
         * Discard all resources held by this class, notably the EGL context.
         */
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface.release();

            mTextureRender = null;
            mSurface = null;
            mSurfaceTexture = null;
            if (VERBOSE_FE) {
                Log.d(TAG_FE, "CodecOutputSurface release gl stuff");
            }
        }

        /**
         * Makes our EGL context and surface current.
         */
        public void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }

        /**
         * Returns the Surface.
         */
        public Surface getSurface() {
            return mSurface;
        }

        public void awaitNewImage() {
            long awaitBeging = 0;
            if (VERBOSE_FE) {
                awaitBeging = System.currentTimeMillis();
                Log.d(TAG_FE, Thread.currentThread().getName() + "_awaitNewImage...");
            }
//            final int TIMEOUT_MS = 500;

            synchronized (mFrameSyncObject) {
                if (!mFrameAvailable) {
                    try {
                        mFrameSyncObject.wait(100);
//                        if (!mFrameAvailable) {
//                            throw new RuntimeException(Thread.currentThread().getName() + "_frame wait timed out");
//                        }
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
                mFrameAvailable = false;
            }
            if (VERBOSE_FE) {
                Log.d(TAG_FE, Thread.currentThread().getName() + "_awaitNewImage_mFrameAvailable :" + (System.currentTimeMillis() - awaitBeging));
            }
            // Latch the data.
            mTextureRender.checkGlError("before updateTexImage");
            mSurfaceTexture.updateTexImage();
            if (VERBOSE_FE) {
                Log.d(TAG_FE, Thread.currentThread().getName() + "_awaitNewImage_updateTexImage_ok.elapse:" + (System.currentTimeMillis() - awaitBeging));
            }
        }

        public void drawImage(boolean invert) {
            mTextureRender.drawFrame(mSurfaceTexture, invert);
        }

        @Override
        public void onFrameAvailable(SurfaceTexture st) {
            if (VERBOSE_FE)
                Log.d(TAG_FE, Thread.currentThread().getName() + "_onFrameAvailable new frame available");
            synchronized (mFrameSyncObject) {
//                if (mFrameAvailable) {
//                    throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
//                }
                mFrameAvailable = true;
                mFrameSyncObject.notifyAll();
            }
        }

        /**
         * Saves the current frame to disk as a PNG image.
         */
        public Bitmap generateFrame() throws IOException {
            mPixelBuf.rewind();
            GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);

            try {
                Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
                mPixelBuf.rewind();
                bmp.copyPixelsFromBuffer(mPixelBuf);

                if (VERBOSE_FE) {
                    Log.d(TAG_FE, "FramesExtractor generated Frame ");
                }
                return bmp;

            } finally {

            }

        }

        /**
         * Checks for EGL errors.
         */
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }

}