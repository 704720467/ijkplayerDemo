package com.zp.libvideoedit.utils;


import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.effectModel.EffectAdapter;

import java.util.Comparator;


/**
 * EffectAdapter根据开始时间进行排序
 * Created by zp on 2019/6/27.
 */

public class EffectAdapterSortByCMTime implements Comparator {

    @Override
    public int compare(Object o, Object t1) {
        if (o instanceof EffectAdapter && t1 instanceof EffectAdapter) {
            CMTime oCmTime = ((EffectAdapter) o).getTimeRange().getStartTime();
            CMTime t1CMTime = ((EffectAdapter) t1).getTimeRange().getStartTime();
            return (CMTime.compare(oCmTime, t1CMTime));
        } else {
            return 0;
        }
    }
}

