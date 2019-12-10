package com.zp.libvideoedit.modle;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.util.LruCache;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.MediaUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.VERBOSE;


public class AudioFile {
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 时长。单位秒
     */
    private float duration;
    private CMTime cDuration;
    /**
     * momo或者stereo
     */
    private boolean mono;

    private int channelCount;

    /**
     * 采样率.单位HZ
     */
    private int sampleRate;

    /**
     * 码率 bps
     */
    private int bitrate;
    /**
     * 格式
     */
    private MediaFormat formart;


    private long timebase = -1;

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        try {
            String name = this.filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());
            return name;
        } catch (Exception e) {
            return "";
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public boolean isMono() {
        return mono;
    }

    public void setMono(boolean mono) {
        this.mono = mono;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public MediaFormat getFormart() {
        return formart;
    }

    public void setFormart(MediaFormat formart) {
        this.formart = formart;
    }

    public static LruCache<String, AudioFile> audioFileLruCache = new LruCache<String, AudioFile>(30);

    /**
     * @param path assert:// 为前缀，会从assert中读取，否则认为是绝对路径 @see EditConstants.ASSERT_FILE_PREFIX)
     * @return null，没有音频轨道
     * @throws InvalidVideoSourceException
     */
    public static AudioFile getAudioFileInfo(String path, Context context) throws InvalidVideoSourceException {
        AudioFile audioFile = null;
        if ((audioFile = audioFileLruCache.get(path)) != null) {
            if(VERBOSE){
                Log.i(TAG,"MediaFile_Cache_hit_audioFile:"+audioFile);
            }
            return audioFile;
        }else{
            if(VERBOSE){
                Log.i(TAG,"MediaFile_Cache_miss_audioFile:"+audioFile);
            }
        }

        MediaExtractor extractor = new MediaExtractor();
        long startTime = System.currentTimeMillis();
        try {
            MediaUtils.getInstance(context).setDataSource(extractor, path);
            MediaFormat audioformart = null;
            for (int index = 0; index < extractor.getTrackCount(); ++index) {
                MediaFormat format = extractor.getTrackFormat(index);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioformart = format;
                    break;
                }
            }
            if (audioformart == null) {
                return null;

//				throw new InvalidVideoSourceException("format exception");
            }
            audioFile = new AudioFile();
            audioFile.setFormart(audioformart);
            audioFile.setFilePath(path);
            audioFile.setTimebase(detectTimeBase(path));
            try {
                audioFile.setBitrate(audioformart.getInteger(MediaFormat.KEY_BIT_RATE));
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "can not get bitrate of file:" + path);
            }
            try {
                audioFile.setSampleRate(audioformart.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "can not get sampleRate of file:" + path);
            }
            float duration = 0;
            if (audioformart.containsKey(MediaFormat.KEY_DURATION))
                duration = ((float) audioformart.getLong(MediaFormat.KEY_DURATION)) / EditConstants.US_MUTIPLE;
            else {
                duration = CodecUtils.getDurationMS(context, path) * 1.0f / 1000.0f;
            }
            audioFile.setDuration(duration);
            audioFile.setcDuration(new CMTime(duration, 1000));
            try {
                int cc = audioformart.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//				int cc=detectAudioChannelCount(path);
                audioFile.setChannelCount(cc);
            } catch (Exception e) {
                Log.w(EditConstants.TAG, "can not get KEY_CHANNEL_COUNT of file:" + path);
            }

            if (EditConstants.VERBOSE_LOOP_A) {
                Log.d(EditConstants.TAG_AUDIO_INFO, "videoFileInfo:" + audioFile);
            }
            audioFileLruCache.put(path, audioFile);
            return audioFile;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidVideoSourceException("file dose not exist.path:" + path + "." + e.getMessage());
        } finally {
            extractor.release();
            if (VERBOSE)
                Log.i(EditConstants.TAG_AUDIO_INFO, "getAudioFileInfo elpased time:" + (System.currentTimeMillis() - startTime));
        }

    }

    private static long detectTimeBase(String path) {

        MediaExtractor extractor = new MediaExtractor();
        try {
            MediaUtils.getInstance().setDataSource(extractor, path);

            for (int index = 0; index < extractor.getTrackCount(); index++) {
                MediaFormat format = extractor.getTrackFormat(index);
                String mimeType = format.getString(MediaFormat.KEY_MIME);
                Log.d(EditConstants.TAG, "format for track " + index + " is " + mimeType);
                if (mimeType.startsWith("audio/")) {
                    extractor.selectTrack(index);
                    break;
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            long prePts = -1;
            int count = 0;
            long sum = 0;
            ArrayList<Long> ts = new ArrayList<Long>();
            for (int i = 0; i < 10; i++) {
                int size = extractor.readSampleData(buffer, 0);
                long pts = extractor.getSampleTime();
                if (size > 0 && prePts >= 0) {
                    sum += (pts - prePts);
                    ts.add(pts - prePts);
                    count++;
                }
                if (size <= 0)
                    break;
                buffer.clear();
                boolean hasNext = extractor.advance();

                prePts = pts;
                if (EditConstants.VERBOSE_LOOP_A) {
                    Log.d(EditConstants.TAG_AUDIO_INFO, "timebase.pts:" + pts);
                }
                if (!hasNext)
                    break;
            }
            if (count == 0)
                return 0;
            long timebase = Math.round(sum / count);
            if (EditConstants.VERBOSE_LOOP_A) {
                Log.d(EditConstants.TAG_AUDIO_INFO, "timebase:" + timebase);
            }

            return timebase;
        } catch (Exception e) {
            return 0;
        } finally {
            extractor.release();

        }

    }

    public static int detectAudioChannelCount(String path) {
        MediaFormat audioFormart = null;
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        try {

            MediaUtils.getInstance().setDataSource(extractor, path);
            int index = -1;
            for (int i = 0; i < extractor.getTrackCount(); ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mimeType = format.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    index = i;
                    extractor.selectTrack(i);
                }
            }
            extractor.selectTrack(index);

            audioFormart = extractor.getTrackFormat(index);

            decoder = MediaCodec.createDecoderByType(audioFormart.getString(MediaFormat.KEY_MIME));
            decoder.configure(audioFormart, null, null, 0);

            decoder.start();
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int loopCount = 0;
            while (!sawOutputEOS && loopCount <= 200) {

                if (!sawInputEOS) {
                    int inputBufIndex = decoder.dequeueInputBuffer(EditConstants.TIMEOUT_USEC);

                    if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        loopCount++;
                    } else {
                        ByteBuffer dstBuf = decoderInputBuffers[inputBufIndex];

                        int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);
                        long presentationTimeUs = 0;
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();

                        }
                        decoder.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize, presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        if (!sawInputEOS) {
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, EditConstants.TIMEOUT_USEC);

                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    loopCount++;
                    continue;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    decoder.releaseOutputBuffer(outputIndex, false);

                    continue;
                }
                if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(EditConstants.TAG_A, "INFO_OUTPUT_BUFFERS_CHANGED");

                    decoderOutputBuffers = decoder.getOutputBuffers();
                    continue;
                }

                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioFormart = decoder.getOutputFormat();
                    Log.d(EditConstants.TAG_A, "INFO_OUTPUT_FORMAT_CHANGED");
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
                if (bufferInfo.size < 0) {
                    sawOutputEOS = true;
                } else {
                    ByteBuffer outputBuffer = decoderOutputBuffers[outputIndex];
                    decoder.releaseOutputBuffer(outputIndex, false);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (decoder != null) {
                try {
                    decoder.stop();
                } catch (Exception e) {

                }
                try {
                    decoder.release();
                } catch (Exception e) {

                }
                try {
                    extractor.release();
                } catch (Exception e) {

                }
            }
        }
        int channelCount = audioFormart.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        return channelCount;

    }

    private static long detectAudioInfo(String path) {

        MediaExtractor extractor = new MediaExtractor();
        try {
            MediaUtils.getInstance().setDataSource(extractor, path);

            for (int index = 0; index < extractor.getTrackCount(); index++) {
                MediaFormat format = extractor.getTrackFormat(index);
                String mimeType = format.getString(MediaFormat.KEY_MIME);
                Log.d(EditConstants.TAG, "format for track " + index + " is " + mimeType);
                if (mimeType.startsWith("audio/")) {
                    extractor.selectTrack(index);
                    break;
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            long prePts = -1;
            int count = 0;
            long sum = 0;
            ArrayList<Long> ts = new ArrayList<Long>();
            for (int i = 0; i < 10; i++) {
                int size = extractor.readSampleData(buffer, 0);
                long pts = extractor.getSampleTime();
                if (size > 0 && prePts >= 0) {
                    sum += (pts - prePts);
                    ts.add(pts - prePts);
                    count++;
                }
                if (size <= 0)
                    break;
                buffer.clear();
                boolean hasNext = extractor.advance();

                prePts = pts;
                if (EditConstants.VERBOSE_LOOP_A) {
                    Log.d(EditConstants.TAG_AUDIO_INFO, "timebase.pts:" + pts);
                }
                if (!hasNext)
                    break;
            }
            if (count == 0)
                return 0;
            long timebase = Math.round(sum / count);
            if (EditConstants.VERBOSE_LOOP_A) {
                Log.d(EditConstants.TAG_AUDIO_INFO, "timebase:" + timebase);
            }

            return timebase;
        } catch (Exception e) {
            return 0;
        } finally {
            extractor.release();

        }

    }

    public CMTime getcDuration() {
        return cDuration;
    }

    public void setcDuration(CMTime cDuration) {
        this.cDuration = cDuration;
    }

    public long getTimebase() {
        return timebase;
    }

    public void setTimebase(long timebase) {
        this.timebase = timebase;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    @Override
    public String toString() {
        return "AudioFile [filePath=" + filePath + ", duration=" + duration + ", mono=" + mono + ", channelCount=" + channelCount + ", sampleRate="
                + sampleRate + ", timebase=" + timebase + ", bitrate=" + bitrate + ", formart=" + formart + "]";
    }

}
