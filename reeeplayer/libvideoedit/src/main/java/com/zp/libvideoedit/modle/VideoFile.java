package com.zp.libvideoedit.modle;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.opengl.Matrix;
import android.renderscript.Matrix4f;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;


import androidx.annotation.NonNull;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.MediaUtils;

import java.nio.ByteBuffer;

import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_TR;
import static com.zp.libvideoedit.EditConstants.VERBOSE_V;


public class VideoFile {
    /**
     * 申明单位矩阵
     */
    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 时长。单位秒
     */
    private float duration;
    /**
     * duration
     */
    private CMTime cDuration;
    /**
     * 帧率
     */
    private float fps = -1;
    /**
     * 码率 bits/sec.
     */
    private float bitrate;
    /**
     * 宽度
     */
    private int width;
    /**
     * 高度
     */
    private int height;
    /**
     * 总帧数
     */
    private int frameCounts;

//    private AudioFile audioFile;
    /**
     * second
     */
    private float iframesIntevalSec;
    /**
     * 文件格式
     */
    private MediaFormat videoFormat;
    private boolean defective = false;
    /**
     * 原视频旋转的角度
     */
    private int rotation = 0;


    public static LruCache<String, VideoFile> videoFileLruCache = new LruCache<String, VideoFile>(30);

    /**
     * @param path
     * @return
     * @throws InvalidVideoSourceException
     */
    public static VideoFile getVideoFileInfo(String path, Context context) throws InvalidVideoSourceException {
        VideoFile fileInfo = null;
        if ((fileInfo = videoFileLruCache.get(path)) != null) {
            if (VERBOSE) {
                Log.i(TAG, "MediaFile_Cache_hit_videoFile:" + fileInfo);
            }
            return fileInfo;
        } else {
            if (VERBOSE) {
                Log.i(TAG, "MediaFile_Cache_miss_videoFile");
            }
        }


        MediaExtractor extractor = new MediaExtractor();
        try {
            MediaUtils.getInstance(context).setDataSource(extractor, path);
            MediaFormat videoFormat = null;
            int videoTrackIndex = CodecUtils.selectVideoTrack(extractor);
            if (videoTrackIndex == -1) {
                throw new InvalidVideoSourceException("format exception");
            }
            videoFormat = extractor.getTrackFormat(videoTrackIndex);


            VideoFile videoFile = new VideoFile();
            videoFile.setVideoFormat(videoFormat);
            videoFile.setFilePath(path);
            try {
                videoFile.setFps(videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
            } catch (Exception e) {
                Log.d(TAG, "can not get fps of file:" + path);
            }

            try {
                videoFile.setBitrate(videoFormat.getInteger(MediaFormat.KEY_BIT_RATE));
            } catch (Exception e) {
                Log.d(TAG, "can not get bitrate of file:" + path);
            }

            float duration = 0;
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            try {
//                retr.setDataSource(path);
                MediaUtils.getInstance(context).setDataSource(retr, path);
                try {
                    if (videoFormat.containsKey(MediaFormat.KEY_DURATION))
                        duration = ((float) videoFormat.getLong(MediaFormat.KEY_DURATION)) / EditConstants.US_MUTIPLE;
                    else {
                        String durationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        duration = Integer.valueOf(durationStr) * 1.0f / 1000.0f;
                    }
                    videoFile.setDuration(duration);
                } catch (Exception e) {
                    Log.w(TAG, "VIDEO_FILE_error by get duration", e);
                }

                String rotationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                try {
                    int rotation = Integer.valueOf(rotationStr);
                    if (rotation % 90 != 0) {
                        Log.w(TAG, "can not get rotation of file:" + path + ", invalid value:" + rotation);
                    } else {
                        videoFile.setRotation(rotation);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "can not get rotation of file:" + path, e);
                }

                String bps = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                try {
                    videoFile.setBitrate(Float.valueOf(bps));
                } catch (Exception e) {
                    Log.w(TAG, "can not get bps of file:" + path, e);
                }
            } catch (Exception e) {
                Log.w(TAG, "MediaMetadataRetriever error. file:" + path, e);
            } finally {
                retr.release();
            }
            if (videoFile.getRotation() == 90 || videoFile.getRotation() == 270) {
                videoFile.setWidth(videoFormat.getInteger(MediaFormat.KEY_HEIGHT));
                videoFile.setHeight(videoFormat.getInteger(MediaFormat.KEY_WIDTH));
            } else {
                videoFile.setHeight(videoFormat.getInteger(MediaFormat.KEY_HEIGHT));
                videoFile.setWidth(videoFormat.getInteger(MediaFormat.KEY_WIDTH));
            }

            Pair<Float, Float> metaData = extractorMetaData(path);
            if (metaData == null) {
                Log.w(TAG, "invalid video. can not retrive fps and keyframeinteval:" + path);
            } else {
                float interval = metaData.second;
                videoFile.setIframesIntevalSec(interval);

                if (videoFile.getFps() == -1) {

                    videoFile.setFps(metaData.first);
                }
            }
            videoFile.setcDuration(new CMTime(duration));
            videoFile.setFrameCounts((int) Math.floor(videoFile.getDuration() * videoFile.getFps()));

//            videoFile.setAudioFile(AudioFile.getAudioFileInfo(path));
            videoFileLruCache.put(path, videoFile);
            return videoFile;
        } catch (Exception e) {
            Log.w(TAG, "error", e);
            InvalidVideoSourceException ee = null;
            if (e instanceof InvalidVideoSourceException) {
                ee = (InvalidVideoSourceException) e;
            } else {
                ee = new InvalidVideoSourceException("get videoFile error.path:" + path, e);
            }
            throw ee;
        } finally {
            if (extractor != null) {
                extractor.release();
            }
        }
    }

    @NonNull
    private static Pair<Float, Float> extractorMetaData(String videoPath) throws InvalidVideoSourceException {
        float keyFrameInterval = -1;
        float fps = -1;

        MediaExtractor extractor = null;
        try {
            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, videoPath);
            int trackIndex = CodecUtils.selectVideoTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + videoPath);
            }
            extractor.selectTrack(trackIndex);
            long lastPts = -1;
            long firstPts = -1;
            int readedCount = 0;
            long firstKeyFramePts = -1;
            long secKeyFramePts = -1;
            while (readedCount < EditConstants.ACCECPT_VIDEO_MAX_GOP) {
                ByteBuffer inputBuf = ByteBuffer.allocate(1024 * 1024);
                inputBuf.clear();
                int size = extractor.readSampleData(inputBuf, 0);
                long pts = extractor.getSampleTime();
                int flag = extractor.getSampleFlags();
                if (VERBOSE_V)
                    Log.d(TAG, "VideoFile_extractorMetaData. pts:" + pts + "flag:" + flag + ", size:" + size);
                if (size < 0) {
                    Log.d(TAG, "VideoFile_saw input EOS while size=0");
                    break;
                } else {
                    if (extractor.getSampleTrackIndex() != trackIndex) {
                        Log.w(TAG, "VideoFile_WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                    }
                    if (firstPts == -1) {
                        firstPts = pts;
                    }
                    lastPts = pts;
                    if (flag == MediaExtractor.SAMPLE_FLAG_SYNC) {
                        if (firstKeyFramePts == -1) {
                            firstKeyFramePts = pts;
                        } else if (secKeyFramePts == -1) {
                            secKeyFramePts = pts;
                            keyFrameInterval = ((float) (secKeyFramePts - firstKeyFramePts)) / EditConstants.US_MUTIPLE;

                            // computeFps
                            fps = ((float) readedCount) / (lastPts - firstPts) * EditConstants.US_MUTIPLE;

                            Log.w(TAG, "VideoFile_extractorMetaData finished. fps: " + fps + ", keyFrameInterval " + keyFrameInterval);
                            return new Pair<Float, Float>(fps, keyFrameInterval);
                        }
                    }
                    readedCount++;
                }
                if (!extractor.advance()) {
                    if (VERBOSE_V) Log.d(TAG, "saw input EOS");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "VideoFile_extractorMetaData error:", e);
        } finally {
            if (extractor != null) try {
                extractor.release();
                extractor = null;
            } catch (Exception e) {
                Log.e(TAG, "VideoFile_extractorMetaData error:", e);
            }
        }
//        throw new InvalidVideoSourceException("invalid video. can not retrive fps and keyframeinteval");
        return null;
    }

    public static int getVideoRotation(String path) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
//            retr.setDataSource(path);
            MediaUtils.getInstance().setDataSource(retr, path);
            String rotationStr = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            try {
                int rotation = Integer.valueOf(rotationStr);
                if (rotation % 90 != 0) {
                    Log.w(TAG, "VideoFile_can not get rotation of file:" + path + ", invalid value:" + rotation);
                } else {
                    return rotation;
                }
            } catch (Exception e) {
                Log.w(TAG, "VideoFile_can not get rotation of file:" + path, e);
            }

        } catch (Exception e) {
            Log.w(TAG, "VideoFile_MediaMetadataRetriever error. file:" + path, e);
        } finally {
            retr.release();
        }
        return 0;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public MediaFormat getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(MediaFormat videoFormat) {
        this.videoFormat = videoFormat;
    }

    /**
     * 时长。单位秒
     *
     * @return
     */
    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    /**
     * @return 帧率
     */
    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        if (VERBOSE_TR) {
            Log.d(TAG, "VideoFile_setFps:" + fps + ",path:" + this.getFileName());
        }

        this.fps = fps;
    }

    /**
     * @return bitrate
     */
    public float getBitrate() {
        return bitrate;
    }

    public void setBitrate(float bitrate) {
        this.bitrate = bitrate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }


    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrameCounts() {
        return frameCounts;
    }

    public void setFrameCounts(int frameCounts) {
        this.frameCounts = frameCounts;
    }

    /**
     * second
     */
    public float getIframesIntevalSec() {
        return iframesIntevalSec;
    }

    public void setIframesIntevalSec(float iframesInteval) {
        this.iframesIntevalSec = iframesInteval;
    }

    public String getFileName() {
        try {
            String name = this.filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());
            return name;
        } catch (Exception e) {
            return "";
        }
    }

    public String prettyString() {
        return this.filePath + ", " + videoFormat.getString(MediaFormat.KEY_MIME) + ", " + duration + "s, " + fps + "fps," + bitrate + "bps , (" + width + "," + height + "), " + frameCounts + "frames ," + iframesIntevalSec + "iframe/s , rotation:" + rotation;

    }

//    public AudioFile getAudioFile() {
//        return audioFile;
//    }
//
//    public void setAudioFile(AudioFile audioFile) {
//        this.audioFile = audioFile;
//    }

    @Override
    public String toString() {
        return prettyString();
    }

    public boolean isDefective() {
        return defective;
    }

    public void setDefective(boolean defective) {
        this.defective = defective;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public float[] getRatationMatrix() {
        Matrix4f matrix = new Matrix4f(IDENTITY_MATRIX);
        switch (this.rotation) {
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

    public CMTime getcDuration() {
        return cDuration;
    }

    public void setcDuration(CMTime cDuration) {
        this.cDuration = cDuration;
    }
}
