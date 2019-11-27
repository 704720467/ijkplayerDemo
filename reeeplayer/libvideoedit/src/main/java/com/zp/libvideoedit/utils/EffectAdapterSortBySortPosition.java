package com.zp.libvideoedit.utils;


import com.zp.libvideoedit.modle.effectModel.EffectAdapter;

import java.util.Comparator;

/**
 * EffectAdapter根据SortPosition 来进行排序
 * Created by zp on 2018/7/25.
 */

public class EffectAdapterSortBySortPosition implements Comparator<EffectAdapter> {

    @Override
    public int compare(EffectAdapter o, EffectAdapter t1) {
        if (o == null || t1 == null) return 0;
        if (o.getSortPosition() > t1.getSortPosition()) {
            return 1;
        } else if (o.getSortPosition() < t1.getSortPosition()) {
            return -1;
        } else {
            return 0;
        }
    }
}
