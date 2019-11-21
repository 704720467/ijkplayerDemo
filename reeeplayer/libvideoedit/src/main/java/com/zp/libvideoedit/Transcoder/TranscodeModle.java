package com.zp.libvideoedit.Transcoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 转码模型
 * Create by zp on 2019-11-19
 */
public class TranscodeModle {
    private ArrayList<String> mTsFile;//ts 文件集合
    private ArrayList<TranscoderNew> mTsTranscoder;
    private int mVideoWidth;
    private int mVideoHeight;
    private int bitRate;
    private int fps;
    private long durationUs;
    private Context context;
    private int currentDecodePosition = -1;
    private LinkedList<Integer> pendingAudioDecoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos;
    private TranscodeListener callback;

    public TranscodeModle(Context context, ArrayList<String> mTsFile, LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos,
                          LinkedList<Integer> pendingAudioDecoderOutputBufferIndices, TranscodeListener callback) {
        this.mTsFile = mTsFile;
        this.context = context;
        this.pendingAudioDecoderOutputBufferIndices = pendingAudioDecoderOutputBufferIndices;
        this.pendingAudioDecoderOutputBufferInfos = pendingAudioDecoderOutputBufferInfos;
        this.callback = callback;
        currentDecodePosition = -1;
        initTsTranscoder();
    }

    private void initTsTranscoder() {
        if (mTsTranscoder == null) mTsTranscoder = new ArrayList<>();
        mTsTranscoder.clear();
        if (mTsFile == null || mTsFile.isEmpty()) return;
        for (String tsFile : mTsFile) {
            TranscoderNew transcoderNew = new TranscoderNew(context, tsFile, pendingAudioDecoderOutputBufferIndices,
                    pendingAudioDecoderOutputBufferInfos);
            transcoderNew.setCallback(callback);
            transcoderNew.validateParams();
            durationUs += transcoderNew.getDurationUs();
            mTsTranscoder.add(transcoderNew);
        }

        mVideoWidth = mTsTranscoder.get(0).getWidth();
        mVideoHeight = mTsTranscoder.get(0).getHeight();
        bitRate = mTsTranscoder.get(0).getBitRate();
        fps = mTsTranscoder.get(0).getFps();
    }

    public int getmVideoWidth() {
        return mVideoWidth;
    }

    public int getmVideoHeight() {
        return mVideoHeight;
    }

    public int getBitRate() {
        return bitRate;
    }

    public MediaFormat getDecoderOutputAudioFormat() {

        return mTsTranscoder.get(currentDecodePosition).getDecoderOutputAudioFormat();
    }

    public MediaCodec getAudioDecoder() {
        return mTsTranscoder.get(currentDecodePosition).getAudioDecoder();
    }

    public int getFps() {
        return fps;
    }

    public long getDurationUs() {
        return durationUs;
    }

    public void startTransCode() {
        doNextTs();
    }


    public void doNextTs() {
        if (!haseNext()) return;
        currentDecodePosition++;
        TranscoderNew transcoderNew = mTsTranscoder.get(currentDecodePosition);
        transcoderNew.transCode();
    }

    public boolean haseNext() {
        if (mTsTranscoder == null || mTsTranscoder.isEmpty()) return false;
        if ((currentDecodePosition + 1) >= mTsTranscoder.size()) return false;
        return true;
    }
}
