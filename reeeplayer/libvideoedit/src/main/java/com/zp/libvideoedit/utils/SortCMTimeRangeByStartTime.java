package com.zp.libvideoedit.utils;

import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;

import java.util.Comparator;

/**
 * CMTimeRange 比较器 根据开始时间
 * Created by zp on 2018/8/23.
 */

public class SortCMTimeRangeByStartTime implements Comparator {
    @Override
    public int compare(Object o, Object t1) {
        if (o instanceof CMTimeRange && t1 instanceof CMTimeRange) {
            CMTime oCMTime = ((CMTimeRange) o).getStartTime();
            CMTime t1CMTime = ((CMTimeRange) t1).getStartTime();
            return (CMTime.compare(oCMTime, t1CMTime));
        } else {
            return 0;
        }
    }

}
