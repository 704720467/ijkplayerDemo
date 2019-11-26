package com.zp.libvideoedit.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.exceptions.EffectException;
import com.zp.libvideoedit.exceptions.EffectRuntimeException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.zp.libvideoedit.Constants.ASSERT_FILE_PREFIX;
import static com.zp.libvideoedit.Constants.DEFAULT_PBB;
import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_V;
import static com.zp.libvideoedit.Constants.TIMEOUT_USEC;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;
import static com.zp.libvideoedit.Constants.VERBOSE;
import static com.zp.libvideoedit.Constants.VERBOSE_LOOP_A;


/**
 * Created by guoxian on 2018/4/26.
 */

@TargetApi(21)
public class CodecUtils {

    private static int codecCapability = -1;
    private static int enCodeKeyIFrameInterval = 0;

    public static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    public static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    public static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    public static MediaCodecInfo selectCodec(String mimeType) {

        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

        for (MediaCodecInfo codecInfo : mediaCodecList.getCodecInfos()) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    Log.d(TAG, "use codecName:" + codecInfo.getName() + "\t" + Arrays.asList(codecInfo.getSupportedTypes()));
                    return codecInfo;
                }
            }
        }
        return null;

    }

    public static int selectVideoTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }

        return -1;
    }


    public static int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is " + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    public static int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is " + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    /**
     * 获取视频文件视频轨道的时长.如果没有视频轨道或者视频格式不包含时长，返回文件时长.耗时20-50ms左右
     *
     * @param videoPath
     * @return 返回视频轨道时长，单位毫秒 ms
     */
    public static long getVideoDurationMs(String videoPath) {
        long startTime = System.currentTimeMillis();
        MediaExtractor videoExtractor = null;
        long durationVideo = -1;
        try {
            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoPath);
            int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
            if (videoInputTrack != -1) {
                MediaFormat videoFormart = videoExtractor.getTrackFormat(videoInputTrack);
                if (videoFormart.containsKey(MediaFormat.KEY_DURATION)) {
                    durationVideo = videoFormart.getLong(MediaFormat.KEY_DURATION);
                    durationVideo = (long) Math.abs(durationVideo * 1.0f / 1000f);
                } else {
                    Log.w(TAG_V, "getVideoDurationMs-formart not contains duration :" + videoPath);
                }
            } else {
                Log.w(TAG_V, "getVideoDurationMs-no video track :" + videoPath);
            }
        } catch (Exception e) {
            Log.w(TAG, "getVideoDurationMs", e);
        } finally {
            if (videoExtractor != null)
                videoExtractor.release();
        }

        if (durationVideo == -1) {
            durationVideo = getDurationMS(videoPath);
        }
        if (VERBOSE)
            Log.d(TAG, "CodecUtils_getVideoDurationMs:" + (System.currentTimeMillis() - startTime) + ",path:" + videoPath);
        return durationVideo;


    }


    public static int getRotation(Context context, String path) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
            MediaUtils.getInstance(context).setDataSource(retr, path);
            String rotationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int rotation = Integer.valueOf(rotationStr);
            return rotation;
        } catch (Exception e) {
            Log.w(TAG, "CodecUtils_MediaMetadataRetriever error. file:" + path, e);
            return 0;
        } finally {
            try {
                retr.release();
            } catch (Exception e) {
                Log.w(TAG, "CodecUtils_MediaMetadataRetriever retr.release() error. file:" + path, e);
            }
        }
    }

    /**
     * 获取视频文件的时长
     *
     * @param context
     * @param path
     * @return
     */
    public static int getDurationMS(Context context, String path) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
            MediaUtils.getInstance(context).setDataSource(retr, path);
            String durationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int duration = Integer.valueOf(durationStr);
            return duration;
        } catch (Exception e) {
            Log.w(TAG, "CodecUtils_MediaMetadataRetriever error. file:" + path, e);
            return 0;
        } finally {
            try {
                retr.release();
            } catch (Exception e) {
                Log.w(TAG, "CodecUtils_MediaMetadataRetriever retr.release() error. file:" + path, e);
            }
        }
    }

    /**
     * 获取视频文件的时长
     *
     * @param path
     * @return
     */
    public static int getDurationMS(String path) {
        long startTime = System.currentTimeMillis();
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
            retr.setDataSource(path);
            String durationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int duration = Integer.valueOf(durationStr);
            return duration;
        } catch (Exception e) {
            Log.w(TAG, "CodecUtils_MediaMetadataRetriever error. filepath:" + path, e);
            return 0;
        } finally {
            try {
                retr.release();
            } catch (Exception e) {
                Log.w(TAG, "CodecUtils_MediaMetadataRetriever retr.release() error. filepath:" + path, e);
            }
            if (VERBOSE)
                Log.d(TAG, "CodecUtils_getDurationMS_duration:" + (System.currentTimeMillis() - startTime) + ", path:" + path);
        }
    }

    public static boolean isAssertExist(Context context, String assertPath) {
        try {
            String path = assertPath.substring(ASSERT_FILE_PREFIX.length());
            int lastindex = path.lastIndexOf('/');
            String folder = "";
            String fileName = path;
            if (lastindex > 0) {
                folder = path.substring(0, lastindex);
                fileName = fileName.substring(lastindex + 1);
            }

            String[] names = context.getAssets().list(folder);
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(fileName.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "assert not exits", e);
        }
        return false;
    }

    public static void checkMediaExist(Context context, String mediaPath) {
        if (mediaPath == null || mediaPath.length() == 0) throw new RuntimeException("输入文件为空");
        if (mediaPath.startsWith(Constants.ASSERT_FILE_PREFIX)) {
            if (!isAssertExist(context, mediaPath)) {
                throw new RuntimeException("输入assert文件不存在:" + mediaPath);
            }
        } else {
            File file = new File(mediaPath);
            if (!file.exists() || file.isDirectory()) {
                throw new RuntimeException("输入文件不存在:" + mediaPath);
            }
        }
    }

    public static void checkMediaExist(Context context, List<String> mediaPath) {
        if (mediaPath == null || mediaPath.size() == 0) throw new RuntimeException("输入文件为空");
        for (String path : mediaPath) {
            checkMediaExist(context, path);
        }
    }

    public static String generateNextMp4FileName(String basePath) {
//        String dbPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cts";
        File baseDir = new File(basePath);
        if (baseDir.exists()) {
            baseDir.delete();
            baseDir.mkdirs();
        }

        int lastIndex = 0;
        String regx = "^[0-9]{3}.mp4$";
        File[] projectDires = baseDir.listFiles();
        if (projectDires != null)
            for (File projectDir : projectDires) {
                if (!projectDir.isFile()) continue;
                try {
                    if (projectDir.getName().matches(regx)) {
                        int index = Integer.valueOf(projectDir.getName().substring(0, 3));
                        if (index > lastIndex) {
                            lastIndex = index;
                        }
                    }
                } catch (Exception e) {
                    Log.e("CTS", "generateProjectId error", e);
                }
            }
        lastIndex++;
        lastIndex = lastIndex % 1000;
        String id = String.format("%03d", lastIndex);
        if (basePath.charAt(basePath.length() - 1) == '/') basePath = basePath + id + ".mp4";
        else basePath = basePath + "/" + id + ".mp4";
        return basePath;
    }

    public static MediaExtractor createExtractor(Context context, String inPutFilePath) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        if (inPutFilePath.startsWith(Constants.ASSERT_FILE_PREFIX)) {
            String assertPath = inPutFilePath.substring(Constants.ASSERT_FILE_PREFIX.length());
            AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(assertPath);
            extractor.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        } else {
            extractor.setDataSource(inPutFilePath);
        }
        return extractor;
    }

    public static MediaFormat detectAudioFormat(Context context, String filePath) throws Exception {
        MediaExtractor audioExtractor = null;
        MediaCodec audioDecoder = null;
        try {
            BufferInfo audioDecoderOutputBufferInfo = new BufferInfo();
            MediaFormat decoderOutputAudioFormat = null;
            boolean audioExtractorDone = false;

            audioExtractor = createExtractor(context, filePath);
            int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);
            if (audioInputTrack == -1) {
                throw new RuntimeException("missing audio track in test video");
            }
            MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);
            if (VERBOSE) Log.d(TAG, "inputAudioFormat" + inputAudioFormat);
            audioDecoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputAudioFormat));
            audioDecoder.configure(inputAudioFormat, null, null, 0);
            audioDecoder.start();
            ;
            ByteBuffer[] audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
            ByteBuffer[] audioDecoderInputBuffers = audioDecoder.getInputBuffers();

            int maxInputSize = 0;

            while (decoderOutputAudioFormat == null || maxInputSize <= 0) {

                while (decoderOutputAudioFormat == null || maxInputSize <= 0) {
                    int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (VERBOSE_LOOP_A) Log.i(TAG, "no audio decoder input buffer");
                        break;
                    }
                    ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                    int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = audioExtractor.getSampleTime();
                    if (size >= 0) {
                        audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, size, presentationTime, audioExtractor.getSampleFlags());
                        if (VERBOSE_LOOP_A)
                            Log.i(TAG, "audio extractor :" + presentationTime + "\t" + size);
                    }
                    audioExtractorDone = !audioExtractor.advance();
                    if (audioExtractorDone) {
                        if (VERBOSE_LOOP_A) Log.i(TAG, "audio extractor: EOS");
                        audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    break;
                }


                while (decoderOutputAudioFormat == null || maxInputSize <= 0) {
                    int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(audioDecoderOutputBufferInfo, TIMEOUT_USEC);
                    if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (VERBOSE_LOOP_A) Log.i(TAG, "no audio decoder output buffer");
                        break;
                    }
                    if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        if (VERBOSE_LOOP_A) Log.i(TAG, "audio decoder: output buffers changed");
                        break;
                    }
                    if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        decoderOutputAudioFormat = audioDecoder.getOutputFormat();
                        if (VERBOSE_LOOP_A) {
                            Log.i(TAG, "audio decoder: output format changed: " + decoderOutputAudioFormat);
                        }
                        break;
                    }

                    ByteBuffer decoderOutputBuffer = audioDecoderOutputBuffers[decoderOutputBufferIndex];

                    if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (VERBOSE_LOOP_A)
                            Log.i(TAG, "audio decoder: codec onAudioFormatChanged buffer");
                        audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                        break;
                    }
                    if (decoderOutputAudioFormat != null) {
                        maxInputSize = decoderOutputBuffer.capacity();
                        Log.i(TAG, "inputAudioFormat:" + decoderOutputBuffer.capacity());
                    }

                    if (VERBOSE_LOOP_A) {
                        Log.i(TAG, "audio decoder: returned buffer of size " + audioDecoderOutputBufferInfo.size + "\t" + audioDecoderOutputBufferInfo.presentationTimeUs);
                    }
                    break;
                }
            }

//            decoderOutputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, Math.max(maxInputSize, inputAudioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)));
            decoderOutputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
            if (VERBOSE)
                Log.i(TAG, String.format("audio Formart of file %s:%s", filePath, decoderOutputAudioFormat));
            return decoderOutputAudioFormat;
        } finally {
            if (audioExtractor != null) {
                try {
                    audioDecoder.stop();
                    audioDecoder.release();
                } catch (Exception e) {
                    Log.w(TAG, "release audioDecoder error", e);
                }
            }
            if (audioExtractor != null) {
                try {
                    audioExtractor.release();
                } catch (Exception e) {
                    Log.w(TAG, "release audioExtractor error", e);
                }
            }
        }
    }


    /**
     * @param videoPath
     * @return true 关键帧视频，fase 非
     * @throws InvalidVideoSourceException
     */
    public static boolean detectIFrameInterval(String videoPath) throws EffectException {

        MediaExtractor extractor = null;
        boolean allKeyFrame = true;
        int intervalOfFormart = -2;
        float readedCount = 0;
        float keyFrameCount = 0;
        float keyFrameRate = 0;
        boolean canBeAllKeyFrame = false;
        long beginTime = System.currentTimeMillis();
        try {
            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, videoPath);
            int trackIndex = CodecUtils.selectVideoTrack(extractor);

            if (trackIndex < 0) {
                throw new EffectException("detectIFrameInterval No video track found in " + videoPath);
            }

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
                intervalOfFormart = format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL);
                if (VERBOSE)
                    Log.i(TAG, "detectIFrameInterval_KEY_I_FRAME_INTERVAL:" + intervalOfFormart);
            }
            extractor.selectTrack(trackIndex);


            while (readedCount < 200) {
                long pts = extractor.getSampleTime();
                int flag = extractor.getSampleFlags();
                if (pts < 0 || flag != MediaExtractor.SAMPLE_FLAG_SYNC) {
                    allKeyFrame = false;
                } else {
                    keyFrameCount += 1;
                }
                readedCount += 1;
                keyFrameRate = keyFrameCount / readedCount;
                if (keyFrameRate < 0.9 && readedCount > 30) {
                    break;
                }

                if (!extractor.advance()) {
                    if (VERBOSE) Log.d(TAG, "detectIFrameIntervalsaw input EOS");
                    break;
                }
            }
            canBeAllKeyFrame = (keyFrameRate >= 0.9);
            return canBeAllKeyFrame;
        } catch (Exception e) {
            Log.e(TAG, "detectIFrameInterval 准备视频发生错误 error:", e);
            throw new EffectException("准备视频发生错误");
        } finally {
            if (VERBOSE)
                Log.i(TAG, "detectIFrameInterval_result:" + allKeyFrame + "," + canBeAllKeyFrame + ", keyFrameRate:" + keyFrameRate + ", readCount:" + readedCount + ", elaspse:" + (System.currentTimeMillis() - beginTime) + ",intervalOfFormart:" + intervalOfFormart + ", videoPath:" + videoPath);
            if (extractor != null)
                try {
                    extractor.release();
                } catch (Exception e) {

                }
        }
    }

    public static float detectFps(String videoPath) {
        Pair<Float, Long> ret = detectFpsDuration(videoPath);
        if (ret == null)
            return -1;
        else return ret.first;
    }


    public static Pair<Float, Long> detectFpsDuration(String videoPath) {
        long beginTime = System.currentTimeMillis();
        MediaExtractor extractor = null;
        float readedCount = 0;
        float fps = 0;
        long durationUs = -1;
        try {
            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, videoPath);
            int trackIndex = CodecUtils.selectVideoTrack(extractor);

            if (trackIndex < 0) {
                throw new EffectException("detectIFrameInterval No video track found in " + videoPath);
            }

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            extractor.selectTrack(trackIndex);

            if (format.containsKey(MediaFormat.KEY_DURATION))
                durationUs = format.getLong(MediaFormat.KEY_DURATION);

            long firstPts = Long.MIN_VALUE;
            long lastPts = 0l;
            long frameCount = 0;
            while (readedCount <= 40) {
                long pts = extractor.getSampleTime();
                if (firstPts == Long.MIN_VALUE) {
                    firstPts = pts;
                }
                readedCount++;
                if (pts >= 0) {
                    lastPts = pts;
                    frameCount++;
                }

                if (!extractor.advance() && pts < 0) {
                    if (VERBOSE) Log.d(TAG, "detectIFrameIntervalsaw input EOS");
                    break;
                }
            }
            float sec = (lastPts - firstPts) * 1.0f / (1.0f * US_MUTIPLE);
            if (sec == 0) return null;
            fps = (float) ((frameCount - 1) * 1.0 / (sec));
            return new Pair<Float, Long>(fps, durationUs);

        } catch (Exception e) {
            Log.e(TAG, "detectFps 准备视频发生错误 error:", e);
//            throw new EffectException("detectFps 准备视频发生错误");
            return null;
        } finally {
            if (VERBOSE)
                Log.i(TAG, "detectFps_result fps:" + fps + "," + ", videoPath:" + videoPath);
            if (extractor != null)
                try {
                    extractor.release();
                } catch (Exception e) {

                }
            Log.d(TAG, "detectFps elapse_time:" + (System.currentTimeMillis() - beginTime));
        }
    }

    public static Size reduceSize(int width, int height, int capability) {
        float scale = ((float) capability / (float) Math.min(width, height));
        double newWidth = Math.round(width * scale);
        double newHeight = Math.round(height * scale);
        if (scale >= 1) {
            newWidth = width;
            newHeight = height;
        }
        if (newWidth % 4 != 0) {
            newWidth = 4 * Math.round(newWidth / 4.0f);
        }
        if (newHeight % 4 != 0) {
            newHeight = 4 * Math.round(newHeight / 4.0f);
        }

        return new Size((int) newWidth, (int) newHeight);
    }

    /**
     * 设置编码能力
     *
     * @param codecCapability 360,540,720 ,1280
     */
    public static void setCodecCapability(int codecCapability) {
        CodecUtils.codecCapability = codecCapability;
    }

    /**
     * 设置编码时关键帧间隔
     *
     * @param enCodeKeyIFrameInterval
     */
    public static void setEnCodeKeyIFrameInterval(int enCodeKeyIFrameInterval) {
        CodecUtils.enCodeKeyIFrameInterval = enCodeKeyIFrameInterval;
    }

    public static int getEnCodeKeyIFrameInterval() {
        return CodecUtils.enCodeKeyIFrameInterval;
    }


    public static int getCodecCapability() {
        if (codecCapability >= 160 && codecCapability % 4 == 0) {
            return codecCapability;
        } else {
            return calCodecCapability();
        }
    }


    //        28	9
//        27	8.1
//        26	8.0
//        25	7.1.1
//        24	7.0
//        23	6.0
//        22	5.1.1
//        21	5.0.1
//        20	4.4w.2
    public static int calCodecCapability() {
        int capability = 540;
        String[] modelListOf1080 = new String[]{};
        String[] modelListOf720 = new String[]{};
        String[] modelListOf540 = new String[]{"ATH-AL00", "ATH-AL10", "EVA-AL00", "EVA-AL10", "EVA-TL00", "EVA-DL00", "EVA-CL00"};
        String[] modelListOf360 = new String[]{};
        int apiVersion = Build.VERSION.SDK_INT;
        String model = Build.MODEL;
        String osVersion = Build.VERSION.RELEASE;

        if (Arrays.asList(modelListOf360).contains(model)) {
            capability = 360;
        } else if (Arrays.asList(modelListOf540).contains(model)) {
            capability = 540;
        } else if (Arrays.asList(modelListOf720).contains(model)) {
            capability = 720;
        } else if (Arrays.asList(modelListOf1080).contains(model)) {
            capability = 1080;
        } else {
            if (apiVersion >= 100) {
                capability = 1080;
            } else if (apiVersion >= 26) {//android 8.0=26
                capability = 720;
            } else if (apiVersion >= 23) { //android 7.0
                capability = 540;
            } else if (capability >= 21) { //android 5.0
                capability = 360;
            } else
                throw new EffectRuntimeException("手机系统版本太低，不支持视频编辑");
        }
        if (VERBOSE)
            Log.i(TAG, String.format("CodecUtils_codecCapability:%d, deviceInfo", capability, FormatUtils.deviceInfo()));
        return capability;
    }

    /**
     * @param width
     * @param height
     * @param fps
     * @param bpp    0.25-0.5
     * @return
     */
    public static final int calcBitRate(boolean forceAllKeyFrame, int width, int height, float fps) {
        if (forceAllKeyFrame)
            return (int) (2.0 * fps * width * height);
        else return (int) (0.25 * fps * width * height);
    }

    public static final int calcBitRate(int width, int height, float fps) {
        int bitrate = (int) (DEFAULT_PBB * fps * width * height);
        if (bitrate < 1024) bitrate = 1024;
        return bitrate;
    }

    public static String toString(BufferInfo info) {
        return "BufferInfo:[" + info == null ? "null" : "pts:" + String.format("%,d", info.presentationTimeUs) + ", size:" + info.size + ", flag:" + info.flags + ", offset:" + info.offset + "]";
    }
}
