package com.zp.libvideoedit.EditCore;

import android.util.Log;

import com.zp.libvideoedit.modle.TrackType;

import java.nio.ShortBuffer;

import static com.zp.libvideoedit.EditConstants.TAG_A_MIX;
import static com.zp.libvideoedit.EditConstants.VERBOSE_A_MIX;
import static com.zp.libvideoedit.utils.FormatUtils.caller;
import static java.lang.Math.max;


/**
 * 三个声音通道对应tracktype，原音main,bgm,rec。
 * 声音经过resample 处理后，channelcount 和音频采样率同一。AudioMixer只处理channelcount和sampleRate相同的pcm数据
 *
 * @author guoxian
 */
public class AudioMixer {
    public static final int SAMPLE_SIZE = 2048;
    /**
     * 四秒钟的缓存大小
     */
    private static final int BUFFER_SIZE = 44100 * 4 * 4;

    private static final int TRACKCOUNT = 3;

    private boolean eos = false;

    private ShortBuffer[] buffers;
    private boolean[] emptys;

    private float[] volumes;
    private Object addPcmSyn = new Object();


    public AudioMixer() {
        init();
    }

    private void init() {
        initBuffers();
        initVolumes();
        initEmptys();
    }

    /**
     * 初始化音量数组
     */
    private void initVolumes() {
        if (volumes != null) return;
        volumes = new float[TRACKCOUNT];
        float mainVolume = 0.5f;
        float bgmVolume = 0.5f;
        float recVolume = 0.f;
        volumes[0] = mainVolume;
        volumes[1] = bgmVolume;
        volumes[2] = recVolume;
    }

    /**
     * 初始化空状态数组
     */
    private void initEmptys() {
        if (emptys != null) return;
        emptys = new boolean[TRACKCOUNT];
        boolean mainEmpty = true;
        boolean bgmEmpty = true;
        boolean recEmpty = true;
        emptys[0] = mainEmpty;
        emptys[1] = bgmEmpty;
        emptys[2] = recEmpty;
    }

    /**
     * 初始化音频缓存数组
     */
    private void initBuffers() {
        if (buffers != null) return;
        buffers = new ShortBuffer[TRACKCOUNT];
        ShortBuffer mainBuffer = ShortBuffer.allocate(BUFFER_SIZE);
        ShortBuffer bgmBuffer = ShortBuffer.allocate(BUFFER_SIZE);
        ShortBuffer recBuffer = ShortBuffer.allocate(BUFFER_SIZE);
        buffers[0] = mainBuffer;
        buffers[1] = bgmBuffer;
        buffers[2] = recBuffer;
    }

    private float getVolume(TrackType trackType) {
        initVolumes();
        switch (trackType) {
            case TrackType_Main_Audio:
                return volumes[0];
            case TrackType_Audio_BackGround:
                return volumes[1];
            case TrackType_Audio_Recoder:
                return volumes[2];
        }
        throw new IllegalArgumentException("trackType 应该为音频类型");
    }

    private ShortBuffer getBuffer(TrackType trackType) {
        initBuffers();
        switch (trackType) {
            case TrackType_Main_Audio:
                return buffers[0];
            case TrackType_Audio_BackGround:
                return buffers[1];
            case TrackType_Audio_Recoder:
                return buffers[2];
        }
        throw new IllegalArgumentException("trackType 应该为音频类型");
    }

    private boolean getEmpty(TrackType trackType) {
        initEmptys();
        switch (trackType) {
            case TrackType_Main_Audio:
                return emptys[0];
            case TrackType_Audio_BackGround:
                return emptys[1];
            case TrackType_Audio_Recoder:
                return emptys[2];
        }
        throw new IllegalArgumentException("trackType 应该为音频类型");
    }

    private void setVolume(TrackType trackType, float volume) {
        if (volume == -1) return;//如果音量为负数，为不正常操作使用之前的值
        initVolumes();
        switch (trackType) {
            case TrackType_Main_Audio:
                volumes[0] = volume;
                return;
            case TrackType_Audio_BackGround:
                volumes[1] = volume;
                return;
            case TrackType_Audio_Recoder:
                volumes[2] = volume;
                return;
        }
        throw new IllegalArgumentException("trackType 应该为音频类型");
    }


    private void setEmpty(TrackType trackType, boolean empty) {
        initEmptys();
        switch (trackType) {
            case TrackType_Main_Audio:
                emptys[0] = empty;
                return;
            case TrackType_Audio_BackGround:
                emptys[1] = empty;
                return;
            case TrackType_Audio_Recoder:
                emptys[2] = empty;
                return;

        }
        throw new IllegalArgumentException("trackType 应该为音频类型");
    }


    /**
     * 设置三个通道的音量。三个音量和为1
     *
     * @param mainVolume 原视频音量0.0~1.0
     * @param bgmVolume  背景乐音量0.0~1.0
     * @param recVolume  录音0.0~1.0
     */
    public void setVolume(float mainVolume, float bgmVolume, float recVolume) {
        initVolumes();
        if (mainVolume < 0) mainVolume = 0;
        if (bgmVolume < 0) bgmVolume = 0;
        if (recVolume < 0) recVolume = 0;

        if (mainVolume > 1) mainVolume = 1;
        if (bgmVolume > 1) bgmVolume = 1;
        if (recVolume > 1) recVolume = 1;

        float volumeSum = mainVolume + bgmVolume + recVolume;
        if (volumeSum > 1) {
            Log.w(TAG_A_MIX, "三个通道音量和>1,将等比按比例分配");
            mainVolume = mainVolume / volumeSum;
            bgmVolume = bgmVolume / volumeSum;
            recVolume = recVolume / volumeSum;
        }
        volumes[0] = mainVolume;
        volumes[1] = bgmVolume;
        volumes[2] = recVolume;
    }

    /**
     * segment 为空
     *
     * @param trackType
     */
    public synchronized void onSegEmpty(TrackType trackType, boolean empty) {
        setEmpty(trackType, empty);
        logBuffer("onSegEmpty");
    }

    /**
     * 整个音频结束
     */
    public synchronized void onAudioChannelEos() {
        eos = true;
        logBuffer("clear");

    }

    public synchronized short[] onPCMArrived(TrackType trackType, short[] pcmData, float volume) {
        if (VERBOSE_A_MIX)
            Log.d(TAG_A_MIX, caller() + trackType + "|synch_audioTrack_onPCMArrived_size:" + pcmData.length + ",volume=" + volume);
        try {
            setVolume(trackType, volume);
            if (pcmData == null) {
                setEmpty(trackType, true);
                return mixBuffer();
            }

            ShortBuffer buffer = getBuffer(trackType);
            //如果以前为空，清除以前的数据
            if (getEmpty(trackType)) {
                buffer.clear();
                setEmpty(trackType, false);
            }
//            if (othersAreEmpty(buffer)) {
//                buffer.flip();
//                int remaining = buffer.remaining();
//                short[] result = new short[pcmData.length + remaining];
//                buffer.get(result, 0, remaining);
//                System.arraycopy(pcmData, 0, result, remaining, pcmData.length);
//                buffer.compact();
//                return result;
//            }

            if (buffer.remaining() < pcmData.length || buffer.position() > BUFFER_SIZE * 0.2) {
                if (VERBOSE_A_MIX)
                    Log.d(TAG_A_MIX, caller() + "_BGM_MAIN_AudioMixer_onPCMArrived_synch_audioTrack_wait_100_" + trackType.getName() + ", buffer:" + buffer + ",pcmData:" + pcmData.length);
                logBuffer(caller() + "synch_audioTrack_ SLEEP");
                try {
                    this.wait(40);
                    if (VERBOSE_A_MIX)
                        Log.d(TAG_A_MIX, caller() + "_BGM_MAIN_AudioMixer_onPCMArrived_synch_audioTrack_wait_100_continue!!!" + trackType.getName() + ", buffer:" + buffer + ",pcmData:" + pcmData.length);
                } catch (Exception e) {
                    Log.e(TAG_A_MIX, caller() + "synch_audioTrack_wait for decoder synchronization time out", e);
                }
            }

            if (buffer.remaining() >= pcmData.length) {
                buffer.put(pcmData);
                if (VERBOSE_A_MIX)
                    Log.d(TAG_A_MIX, caller() + "_synch_audioTrack_buffer.put(pcmData)  buffer+ :" + buffer + ",pcmData:" + pcmData.length);
            }

            short[] result = mixBuffer();
            if (VERBOSE_A_MIX)
                Log.d(TAG_A_MIX, caller() + "_synch_audioTrack_mixBuffer result+ :" + (result != null ? result.length : 0));
            this.notifyAll();
            return result;
        } catch (Exception e) {
            Log.e(TAG_A_MIX, Thread.currentThread().getName() + " mix error " + e.getMessage(), e);
            return null;
        }
    }

    private boolean othersAreEmpty(ShortBuffer buffer) {
        for (int i = 0; i < TRACKCOUNT; i++) {
            if (buffers[i] != buffer && !emptys[i]) return false;
        }

        return true;
    }


    /**
     * seek后，要重新播放,清楚缓存
     */
    public void clear() {

        for (int i = 0; i < emptys.length; i++) {
            emptys[i] = true;
            buffers[i].clear();
        }
        logBuffer("clear");
        eos = false;
    }

    public void release() {
        if (buffers != null)
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i] != null) {
                    buffers[i].clear();
                    buffers[i] = null;
                }
            }
        buffers = null;
    }

    /**
     * 取出所有可能mix的buffer,混音后输出。尽可能多取。
     *
     * @return
     */
    private short[] mixBuffer() {
        int maxSize = getMaxBufferSize();
        int sum = 0;
        short[] outPutBuffer = null;
        try {
            if (maxSize <= 0) return null;
            int synchSize = getSynchSize();
//            if(synchSize<SAMPLE_SIZE) return null;
//            synchSize=SAMPLE_SIZE;

            ShortBuffer mixBuffer = ShortBuffer.allocate(maxSize * 3);


            logBuffer("BEFOR");

            if (synchSize > 0) {
                flipBuffer();
                for (int pos = 0; pos < synchSize; pos++) {
                    float mixed = 0;
                    int channelCount = 0;
                    for (int i = 0; i < TRACKCOUNT; i++) {
                        if (buffers[i].remaining() > 0) channelCount++;
                    }
                    float rate = channelCount == 3 ? 0.7f : (channelCount == 2 ? 0.8f : 1);
                    for (int i = 0; i < TRACKCOUNT; i++) {
                        if (buffers[i].remaining() <= 0) continue;
                        mixed = mixed + buffers[i].get() * volumes[i];
                    }

                    if (mixed > Short.MAX_VALUE) mixed = Short.MAX_VALUE;
                    if (mixed < Short.MIN_VALUE) mixed = Short.MIN_VALUE;
                    mixBuffer.put((short) Math.round(mixed));
                    sum++;
                }
                compactBuffer();
            }
            mixBuffer.flip();
            outPutBuffer = new short[mixBuffer.remaining()];
            mixBuffer.get(outPutBuffer);

            logBuffer("AFTER");
            if (VERBOSE_A_MIX) {
                Log.d(TAG_A_MIX, "Mixed_TRACK_BUFFER:" + mixBuffer + ", " + outPutBuffer.length);
            }
            return outPutBuffer;
        } catch (Exception e) {
            if (VERBOSE_A_MIX)
                Log.e(TAG_A_MIX, "AudioMixer_mixBuffer:" + (outPutBuffer != null ? outPutBuffer.length : null) + "\tmaxSize:" + maxSize + "\tsum:" + sum, e);
        }
        return null;
    }

    private void logBuffer(String stage) {
        if (VERBOSE_A_MIX) {
            stage = stage + "_TRACK_BUFFER_BGM_MAIN_:";
            Log.d(TAG_A_MIX, TrackType.TrackType_Main_Audio.getName() + "\t" + stage + ":" + getBuffer(TrackType.TrackType_Main_Audio) + "\t" + emptys[0]);
            Log.d(TAG_A_MIX, TrackType.TrackType_Audio_BackGround.getName() + "\t" + stage + ":" + getBuffer(TrackType.TrackType_Audio_BackGround) + "\t" + emptys[1]);
            Log.d(TAG_A_MIX, TrackType.TrackType_Audio_Recoder.getName() + "\t" + stage + ":" + getBuffer(TrackType.TrackType_Audio_Recoder) + "\t" + emptys[2]);
        }
    }

    private boolean shouldWait(TrackType thisTrack) {
        ShortBuffer thisBuffer = getBuffer(thisTrack);
        int bufferDiff = 0;
        for (ShortBuffer buffer : buffers) {
            if (thisBuffer != buffer) {
                bufferDiff = max(bufferDiff, thisBuffer.position() - buffer.position());
            }
        }
        return (bufferDiff > BUFFER_SIZE * 0.2);
    }

    private void flipBuffer() {
        for (ShortBuffer buffer : buffers) {
            buffer.flip();
        }
    }

    private void compactBuffer() {
        for (ShortBuffer buffer : buffers) {
            buffer.compact();
        }
    }

    private int getMaxBufferSize() {
        int maxSize = 0;
        for (ShortBuffer buffer : buffers) {
            maxSize = max(maxSize, buffer.position());

        }
        return maxSize;
    }

    private int getSynchSize() {
        int size = Integer.MAX_VALUE;
        //不为空的最小值
        for (int i = 0; i < TRACKCOUNT; i++) {
            if (!emptys[i]) size = Math.min(size, buffers[i].position());
        }
        //如果都为空，去最大值
        if (size == Integer.MAX_VALUE) {
            size = 0;
            for (int i = 0; i < TRACKCOUNT; i++) {
                size = max(size, buffers[i].position());
            }
        }
        return size;
    }

    /**
     * 检测是否可以添加数据
     *
     * @param trackType 当前类型
     * @return
     */
    public boolean canAddData(TrackType trackType, short[] pcmData) {
        ShortBuffer buffer = getBuffer(trackType);
        return buffer.remaining() >= pcmData.length;
    }
}



