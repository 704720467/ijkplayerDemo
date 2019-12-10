package com.zp.libvideoedit.utils;

import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.EditCore.AudioDataPacket;

import static com.zp.libvideoedit.EditConstants.TAG_A_MIX;
import static com.zp.libvideoedit.EditConstants.VERBOSE_A_MIX;


/**
 * Created by gwd on 2018/3/20.
 */

public class Resample {
    static {
        System.loadLibrary("native");
    }

    public native short[] resample(int channel_count, short[] inputBuffer, int src_sample_rate, int inputbuffer_count, int dstSample_rate);

    //初始化音频
    public native long nativeAudioPrepare(String audioPath, long startTime);

    //开始解码音频解码
    private native void native_Audio_Decode_Start(long mVNIFFmpegId);

    //停止音频解码
    private native void native_Audio_Decode_Stop();

    //暂停音频解码
    private native void native_Audio_Decode_Pause(long mVNIFFmpegId);

    //释放资源，结束解码
    private native void native_Audio_Decode_Release(long mVNIFFmpegId);

    //获取音频数据
    private native AudioDataPacket native_Get_Audio_Data(long mVNIFFmpegId);

    //设置开始解码pts
    private native void native_Audio_Seek(long pts, long mVNIFFmpegId);

    final private int channelCount = EditConstants.DEFAULT_AUDIO_CHANNEL_COUNT;//2
    /**
     * resample后的音频采样率
     */
    private int sampleRate;

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * @param sampleRate resample后的音频采样率
     */
    public Resample(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * @param inputBuffer     pcm数据
     * @param srcChannelCount 原音频通道个数
     * @param srcSampleRate   原音频采样率
     * @return
     */
    public short[] resampleAudio(short[] inputBuffer, int srcChannelCount, int srcSampleRate) {
        short[] steroBuffer = toStero(srcChannelCount, inputBuffer);
        if (srcSampleRate == sampleRate) return steroBuffer;
        if (VERBOSE_A_MIX)
            Log.i(TAG_A_MIX, String.format("resampleAudio resample:%s, inputbufferSize:%d, srcChannelCount:%d,srcSampleRate:%d, destSampleRate:%d", this.toString(), inputBuffer.length, srcChannelCount, srcSampleRate, sampleRate));
        short[] outPutBuffer = resample(channelCount, steroBuffer, srcSampleRate, steroBuffer.length, this.sampleRate);
        return outPutBuffer;
    }

    public static short[] moneToStero(short[] inputBuffer) {
        if (inputBuffer == null || inputBuffer.length == 0) return inputBuffer;
        short[] outBuffer = new short[inputBuffer.length * 2];
        for (int i = 0; i < inputBuffer.length; i++) {
            outBuffer[2 * i] = inputBuffer[i];
            outBuffer[2 * i + 1] = inputBuffer[i];
        }
        return outBuffer;
    }

    public static short[] toStero(int srcChannelCount, short[] inputBuffer) {
        if (srcChannelCount == 1) return moneToStero(inputBuffer);
        else if (srcChannelCount == 2) return inputBuffer;
        else if (srcChannelCount > 2) {
            short[] outBuffer = new short[inputBuffer.length / srcChannelCount * 2];
            if (srcChannelCount > 3) {
                for (int i = 0; i < outBuffer.length / 2; i++) {
                    outBuffer[i * 2] = inputBuffer[i * srcChannelCount + 1];
                    outBuffer[i * 2 + 1] = inputBuffer[i * srcChannelCount + 2];
                }
            }
            return outBuffer;
        } else return null;
    }

    public static short[] steroToMone(short[] inputBuffer) {
        short[] monoBuffer = null;
        monoBuffer = new short[inputBuffer.length / 2 + inputBuffer.length % 2];
        short preData = 0;
        for (int i = 0; i < inputBuffer.length; i++) {
            int j = i / 2;
            if (inputBuffer.length % 2 != 0 && i == inputBuffer.length - 1) {
                monoBuffer[j] = inputBuffer[i];
            }
            if ((i + 1) % 2 == 0) {
                monoBuffer[j] = (short) ((inputBuffer[i] + preData) / 2);
            }
            preData = inputBuffer[i];
        }
        return monoBuffer;
    }

    /**
     * 准备音频解码
     *
     * @param audioPath
     */
    public long prepareDecodeForAudio(String audioPath, long startTime) {
        return nativeAudioPrepare(audioPath, startTime);
    }

    /**
     * 开始解码视频
     */
    public void startDecodeForAudio(long mVNIFFmpegId) {
        native_Audio_Decode_Start(mVNIFFmpegId);
    }


    /**
     * 停止解码视频
     */
    public void stopDecodeForAudio() {
        native_Audio_Decode_Stop();
    }

    /**
     * 停止解码视频
     */
    public void pauseDecodeForAudio(long mVNIFFmpegId) {
        native_Audio_Decode_Pause(mVNIFFmpegId);
    }

    /**
     * 结束解码释放资源
     *
     * @param mVNIFFmpegId
     */
    public void releaseAudioDecode(long mVNIFFmpegId) {
        native_Audio_Decode_Release(mVNIFFmpegId);
    }

    public AudioDataPacket getAudioData(long mVNIFFmpegId) {
        return native_Get_Audio_Data(mVNIFFmpegId);
    }

    /**
     * seek到指定位置
     *
     * @param pts          pts 毫秒单位
     * @param mVNIFFmpegId 对应的解码器
     * @return
     */
    public void getAudioSeek(long pts, long mVNIFFmpegId) {
        native_Audio_Seek(pts, mVNIFFmpegId);
    }


    //==========================回调相关Start=========================================
    private FFmpegDecodeListener onFFmpegDecodeListener;
    private OnDecodeAudioListener onDecodeAudioListener;

    public void setOnFFmpegDecodeListener(FFmpegDecodeListener onFFmpegDecodeListener) {
        this.onFFmpegDecodeListener = onFFmpegDecodeListener;
    }

    public void setOnDecodeAudioListener(OnDecodeAudioListener onDecodeAudioListener) {
        this.onDecodeAudioListener = onDecodeAudioListener;
    }

    //    不断调用
    public void onProgress(int progress) {
        if (null != onFFmpegDecodeListener) {
            onFFmpegDecodeListener.onProgress(progress);
        }

    }

    public void onPrepare() {
        if (null != onFFmpegDecodeListener) {
            onFFmpegDecodeListener.onPrepared();
        }

    }

    public void onError(int errorCode) {
        if (null != onFFmpegDecodeListener) {
            onFFmpegDecodeListener.onError(errorCode);
        }
    }

    public void onFinish() {
        if (null != onFFmpegDecodeListener) {
            onFFmpegDecodeListener.onFinish();
        }
    }

    /**
     * 返回解码后的音频数据
     *
     * @param data
     * @param pts
     */
    public void audioFrameCallBack(byte[] data, long pts) {
        if (onDecodeAudioListener != null)
            onDecodeAudioListener.onDataCallBack(data, pts, -1);
    }

    public void audioFrameCallBackForShort(short[] data, long pts) {
        if (onDecodeAudioListener != null)
            onDecodeAudioListener.onDataCallBackForShort(data, pts, -1);
    }


    public interface FFmpegDecodeListener {
        void onPrepared();

        void onProgress(int progress);

        void onFinish();

        void onError(int error);
    }

    public interface OnDecodeAudioListener {
        void onDataCallBack(byte[] data, long pts, long bufferSize);

        void onDataCallBackForShort(short[] data, long pts, long bufferSize);
    }
    //==========================回调相关end=========================================

}
