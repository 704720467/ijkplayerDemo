package com.zp.libvideoedit.Transcoder;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Pair;

import com.zp.libvideoedit.utils.CodecUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zp.libvideoedit.Constants.TAG_TR;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;
import static com.zp.libvideoedit.Constants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.CodecUtils.getAndSelectAudioTrackIndex;
import static com.zp.libvideoedit.utils.CodecUtils.getAndSelectVideoTrackIndex;
import static com.zp.libvideoedit.utils.FormatUtils.caller;


public class TSMediaExtractor {
    public enum MediaTrackType {
        video, audio
    }

    private MediaTrackType trackType;
    private List<TSSegemnt> tsSegemnts;
    private MediaExtractor mediaExtractor;
    Context context;
    long trimBeginUs=-1;
    long trimEndUs=-1;
    long durationUs;
    float fps;


    int trackIndex;
    int tsIndex = -1;

    long currentTSStartTime=0;


    public TSMediaExtractor(Context context, List<TSSegemnt> tsSegemnts, MediaTrackType trackType,long trimBeginUs,long trimEndUs) {
        this.context = context;
        this.trackType = trackType;
        this.tsSegemnts = tsSegemnts;
        this.trimBeginUs=trimBeginUs;
        this.trimEndUs=trimEndUs;
        if(shouldTrim() && tsSegemnts.size()>1){
            throw new TranscodeRunTimeException("剪裁智能应用于单片断");
        }
        if(shouldTrim()){
            durationUs=trimEndUs-trimBeginUs;
        }else{
            TSSegemnt lastTs=tsSegemnts.get(tsSegemnts.size()-1);
            durationUs= lastTs.durationUs+lastTs.startTimeUs;
        }
        fps=tsSegemnts.get(0).fps;
        tsIndex = 0;


        createMediaExtractor(tsSegemnts.get(0));
        if(shouldTrim()){
            mediaExtractor.seekTo(trimBeginUs,MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

    }

    public MediaFormat getTrackFormat() {
        return mediaExtractor.getTrackFormat(trackIndex);
    }

    public int readSampleData(ByteBuffer byteBuf, int offset) {
        if(shouldTrim() &&mediaExtractor.getSampleTime()>trimEndUs)
            return -1;
        int size = mediaExtractor.readSampleData(byteBuf, 0);
        return size;
    }

    public long getSampleTime() {
        long pts = mediaExtractor.getSampleTime();
        if(shouldTrim() &pts>trimEndUs)
            return -1;

        if (pts >= 0) {
           pts=currentTSStartTime+pts;
        }
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "| getSampleTime_" + trackType + "\t" + pts+"\t"+currentTSStartTime);
        return pts;
    }

    public int getSampleFlags() {
        return mediaExtractor.getSampleFlags();
    }
    private boolean shouldTrim(){
        return trimBeginUs>=0&&  trimEndUs-trimBeginUs>1.8*US_MUTIPLE;
    }

    public boolean advance() {
        if(shouldTrim() &&mediaExtractor.getSampleTime()>trimEndUs)
            return false;

        boolean ret = mediaExtractor.advance();

        if (ret == false) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "| index+" + tsIndex + "/" + tsSegemnts.size() + "__video --extractor--: EOS" + tsSegemnts.get(tsIndex));

            if (tsIndex < tsSegemnts.size() - 1) {
                tsIndex++;
                createMediaExtractor(tsSegemnts.get(tsIndex));
                ret = true;
            }
        }
        if (ret == false && VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "| index+" + tsIndex + "/" + tsSegemnts.size() + " all _ts --extractor--: EOS");
        }
        return ret;
    }


    public void release() {
        try {
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }
        } catch (Exception e) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoExtractor", e);
        }
        mediaExtractor = null;
    }

    private void createMediaExtractor(TSSegemnt segemnt) {
        if (mediaExtractor != null)
            release();
        try {
            mediaExtractor = CodecUtils.createExtractor(context, segemnt.tsFile);
            if (trackType == MediaTrackType.audio) {
                trackIndex = getAndSelectAudioTrackIndex(mediaExtractor);
            } else {
                trackIndex = getAndSelectVideoTrackIndex(mediaExtractor);
            }
            if (trackIndex == -1) {
                throw new TranscodeRunTimeException("missing " + trackType.name() + " track in  file:" + segemnt.tsFile);
            }
            if (VERBOSE_TR)
                Log.i(TAG_TR, caller() + "createMediaExractor...." + segemnt.tsFile + ", index:" + trackIndex + " trackType:" + trackType.name());
            currentTSStartTime=segemnt.startTimeUs;
        } catch (IOException e) {
            throw new TranscodeRunTimeException(e.getMessage(), e);
        }
    }

    /**
     * CA000145_1911031100070000200_0_000004.ts
     *
     * @param tsFileName
     * @return
     */
    public static boolean matchOldTsc(String tsFileName) {
        String regex = "^CA[0-9]{6}_[0-9]{19}_[0-9]{1,3}_[0-9]{6}.ts$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(tsFileName);
        return matcher.matches();
    }

    /**
     * 191119150016476_2.00_1920X1080_30_00000006.ts
     *
     * @param tsFileName
     * @return
     */
    public static boolean matchiPCTsc(String tsFileName) {
        String regex = "^[0-9]{15}_[0-9]{1,2}.[0-9]{2}_[0-9]{1,4}X[0-9]{1,4}_[0-9]{1,3}_[0-9]{8}.ts$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(tsFileName);

        return matcher.matches();
    }

    private static void validateTsFile(Context context, List<String> tsFiles) throws TranscodeRunTimeException {
        //只有一个文件，只检测文件是否存在
        if(tsFiles.size()==1){
            CodecUtils.checkMediaExist(context, tsFiles.get(0));
            return;
        }

        int tsType = 0;//1 old tsc,2 ipc
        for (String ts : tsFiles) {
            CodecUtils.checkMediaExist(context, ts);
            File f = new File((ts));
            if (tsType == 1) {
                if (!matchOldTsc(f.getName()))
                    throw new TranscodeRunTimeException("文件格式与之前的不匹配（old tsc 格式:CA000145_1911031100070000200_0_000004.ts ）");
            } else if (tsType == 2) {
                if (!matchiPCTsc(f.getName()))
                    throw new TranscodeRunTimeException("文件格式与之前的不匹配(ipc ts 格式:191119150016476_2.00_1920X1080_30_00000006.ts)");
            } else {
                if (matchOldTsc(f.getName()))
                    tsType = 1;
                else if (matchiPCTsc(f.getName()))
                    tsType = 2;
                else
                    throw new TranscodeRunTimeException("ts文件格式非法,不是老的tsc,也不是新摄像机");
            }
        }
    }

    public float getFps() {
        return fps;
    }

    public long getDurationUs() {
        return durationUs;
    }

    public static List<TSSegemnt> generateTsSegemnt(Context context, List<String> tsFiles) throws TranscodeRunTimeException {
        if (tsFiles == null || tsFiles.size() == 0)
            throw new TranscodeRunTimeException("ts file is empty");
        validateTsFile(context, tsFiles);
        List<TSSegemnt> segemntList = new ArrayList<TSSegemnt>();

        long startTimeUs=0;
        for (int i = 0; i < tsFiles.size(); i++) {
            Pair<Float,Long>fpsAndDuration=CodecUtils.detectFpsDuration(tsFiles.get(i));
            if(fpsAndDuration==null){
                throw new TranscodeRunTimeException("can not detect fps and duration");
            }
            long duration=fpsAndDuration.second;
            float fps=fpsAndDuration.first;
            TSSegemnt tsSegemnt=   new TSSegemnt(i,tsFiles.get(i),fps,startTimeUs,duration);
            segemntList.add(tsSegemnt);
            startTimeUs=startTimeUs+duration+ Math.round((1.0f*US_MUTIPLE/fps));
        }
        if(segemntList.size()<=0)
            throw new TranscodeRunTimeException("generateTsSegemnt result count 0");
        return segemntList;
    }

    public static class TSSegemnt {
        int index;
        String tsFile;
        float fps;
        long startTimeUs;
        long durationUs;

        public TSSegemnt(int index, String tsFile, float fps, long startTimeUs, long durationUs) {
            this.index = index;
            this.tsFile = tsFile;
            this.fps = fps;
            this.startTimeUs = startTimeUs;
            this.durationUs = durationUs;
        }
    }
}

