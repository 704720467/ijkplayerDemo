package com.zp.libvideoedit.GPUImage.Core;


import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/4/8.
 */

public class GPUImageFilterPipeline {
    private String stringValue;
    public ArrayList<GPUImageFilter> filters;
    public GPUImageFilter inputfilter;
    public GPUImageFilter output;

    public GPUImageFilterPipeline() {
        this.filters = new ArrayList<GPUImageFilter>();
    }

    public void removeFilter(GPUImageFilter filter) {
        this.filters.remove(filter);
        this.refreshFilters();
    }

    public void replaceFilter(GPUImageFilter filter, int index) {
        this.filters.set(index, filter);
        this.refreshFilters();
    }

    public void addFilter(GPUImageFilter filter, int index) {
        this.filters.add(index, filter);
        this.refreshFilters();
    }

    public void addFilter(GPUImageFilter filter) {
        this.filters.add(filter);
        this.refreshFilters();
    }

    protected void refreshFilters() {
        GPUImageOutput prevFilter = this.inputfilter;
        GPUImageFilter theFilter = null;
        for (int i = 0; i < this.filters.size(); i++) {
            theFilter = this.filters.get(i);
            prevFilter.removeAllTargets();
            prevFilter.addTarget(theFilter,0);
            prevFilter = theFilter;
        }
        prevFilter.removeAllTargets();
        if (this.output != null) {
            prevFilter.addTarget(this.output);
        }
    }

    public GPUImageFilter getOutput() {
        return output;
    }

    public GPUImageFilter getInputfilter() {
        return inputfilter;
    }
}
