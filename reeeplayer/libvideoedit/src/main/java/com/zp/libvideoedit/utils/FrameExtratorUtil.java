package com.zp.libvideoedit.utils;

import android.graphics.Bitmap;
import android.util.Log;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.zp.libvideoedit.Constants.TAG;


/**
 * Created by qin on 2018/5/7.
 */

public class FrameExtratorUtil {

    public interface FrameGenerated {
        void onFrameGenerated(Bitmap thumb, String videoPath, String chunkId, long pts);

        void onSuccessed();
    }

    private static FrameExtratorUtil singleton;

    public static FrameExtratorUtil getInstance() {
        if (singleton == null) {
            synchronized (FrameExtratorUtil.class) {
                if (singleton == null) {
                    singleton = new FrameExtratorUtil();
                }
            }
        }
        return singleton;
    }

    HashMap<String, List<Long>> frameTimeMap = new HashMap<>();

    private synchronized List<Long> updateFrameTimes(String videoPath, List<Long> times, long removePts) {
        if (removePts >= 0) {
            List<Long> mapTimes = frameTimeMap.get(videoPath);
            mapTimes.remove(removePts);
            return null;
        }
        List<Long> frameTimes = new ArrayList<>();
        if (!frameTimeMap.containsKey(videoPath)) {
            frameTimeMap.put(videoPath, times);
            frameTimes.addAll(times);
        } else {
            List<Long> mapTimes = frameTimeMap.get(videoPath);
            for (Long time : times) {
                if (!mapTimes.contains(time)) {
                    frameTimes.add(time);
                }
            }
            mapTimes.addAll(frameTimes);
        }
        return frameTimes;
    }

    public void frameExtrator(final String videoPath, final String chunkId, List<Long> times, int height,
                              final FrameGenerated frameGenerated) {

        final String path = StringUtil.isEmptyOrNull(chunkId) ? videoPath : chunkId;
        List<Long> frameTimes = updateFrameTimes(path, times, -1);

        FramesExtractor framesExtractor = new FramesExtractor(videoPath);
        final long beginTime = System.currentTimeMillis();

        framesExtractor.extract(frameTimes, height, new FramesExtractor.Callback() {

            long saveFileElapseTime = 0;
            long firstElapseTime = 0;

            @Override
            public void onFrameGenerated(FramesExtractor framesExtractor, Bitmap thumb, int index, long pts) {
                LogUtil.i(TAG, String.format("onFrameGenerated %dx%d\t index:%d\tpts%d",
                        thumb.getWidth(), thumb.getHeight(), index, pts));
                if (frameGenerated != null) {
                    frameGenerated.onFrameGenerated(thumb, videoPath, chunkId, pts);
                }
                LogUtil.e("onFrameGenerated", "videopath:" + videoPath + " ------ pts:" + pts);

                updateFrameTimes(path, null, pts);

            }

            @Override
            public void OnSuccessed(FramesExtractor framesExtractor, int countOfThumb) {

                long elapseTime = System.currentTimeMillis() - beginTime;
                String result = String.format("OnSuccessed:图片分辨率: %dx%d,生成的数量:%d,总共耗时:%d," +
                                "保存文件耗时:%d;第一张耗时:%d",
                        framesExtractor.getFrameWidth(),
                        framesExtractor.getFrameHeight(), countOfThumb,
                        elapseTime, saveFileElapseTime, firstElapseTime);
                Log.i(TAG, result);
                if (frameGenerated != null) {
                    frameGenerated.onSuccessed();
                }

            }

            @Override
            public void onError(FramesExtractor framesExtractor, Exception e) {
                Log.e(TAG, "onError", e);
            }
        });

    }

    /**
     * 取帧 非编辑器页面调用
     *
     * @param videoPath
     * @param times
     * @param height
     * @param frameGenerated
     */
    public FramesExtractor frameExtrator(final String videoPath, List<Long> times, int height,
                                         final FrameGenerated frameGenerated) {

        FramesExtractor framesExtractor = new FramesExtractor(videoPath);
        final long beginTime = System.currentTimeMillis();

        framesExtractor.extract(times, height, new FramesExtractor.Callback() {

            long saveFileElapseTime = 0;
            long firstElapseTime = 0;

            @Override
            public void onFrameGenerated(FramesExtractor framesExtractor, Bitmap thumb, int index, long pts) {
                LogUtil.i(TAG, String.format("onFrameGenerated %dx%d\t index:%d\tpts%d",
                        thumb.getWidth(), thumb.getHeight(), index, pts));
                if (frameGenerated != null) {
                    frameGenerated.onFrameGenerated(thumb, videoPath, null, pts);
                }

            }

            @Override
            public void OnSuccessed(FramesExtractor framesExtractor, int countOfThumb) {

                long elapseTime = System.currentTimeMillis() - beginTime;
                String result = String.format("OnSuccessed:图片分辨率: %dx%d,生成的数量:%d,总共耗时:%d," +
                                "保存文件耗时:%d;第一张耗时:%d",
                        framesExtractor.getFrameWidth(),
                        framesExtractor.getFrameHeight(), countOfThumb,
                        elapseTime, saveFileElapseTime, firstElapseTime);
                Log.i(TAG, result);
                if (frameGenerated != null) {
                    frameGenerated.onSuccessed();
                }

            }

            @Override
            public void onError(FramesExtractor framesExtractor, Exception e) {
                Log.e(TAG, "onError", e);
            }
        });

        return framesExtractor;

    }
}
