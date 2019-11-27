package com.zp.libvideoedit.utils;


import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.RecodeModel;

import java.util.Comparator;

/**
 * Created by zp on 2018/8/24.
 */

public class SortRecoderByAtTime implements Comparator {
    @Override
    public int compare(Object o, Object t1) {
        if (o instanceof RecodeModel && t1 instanceof RecodeModel) {
            CMTime oCMTime = ((RecodeModel) o).getAtTime();
            CMTime t1CMTime = ((RecodeModel) t1).getAtTime();
            return (CMTime.compare(oCMTime, t1CMTime));
        } else {
            return 0;
        }
    }

}