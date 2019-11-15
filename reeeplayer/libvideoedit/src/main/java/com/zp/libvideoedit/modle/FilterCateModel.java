package com.zp.libvideoedit.modle;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by gwd on 2018/4/26.
 */

public class FilterCateModel implements Serializable {

    private String name;

    @SerializedName("packages")
    private ArrayList<FilterModel> filters;

    public FilterCateModel(String name, ArrayList<FilterModel> filters) {
        this.name = name;
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<FilterModel> getFilters() {
        return filters;
    }

    public void setFilters(ArrayList<FilterModel> filters) {
        this.filters = filters;
    }
}
