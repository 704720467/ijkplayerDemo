package com.zp.libvideoedit.utils;

import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.Segment;

import java.util.Comparator;

/**
 * 根据Segment的TargetTimeRange的开始时间 升序排列
 * Created by zp on 2019/5/27.
 */

public class SortSegmentByTargetTimeRange implements Comparator<Segment> {
    @Override
    public int compare(Segment o1, Segment o2) {
        return (CMTime.compare(o1.getTimeMapping().getTargetTimeRange().getStartTime(),
                o2.getTimeMapping().getTargetTimeRange().getStartTime()));
    }
}
