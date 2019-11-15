package com.zp.libvideoedit.utils;

import android.content.Context;
import android.text.TextUtils;

import com.zp.libvideoedit.modle.FilterCateModel;
import com.zp.libvideoedit.modle.FilterModel;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/4/27.
 */

public class LookupInstance {
    private static LookupInstance instance = null;
    private static ArrayList<FilterCateModel> filterCateModels;
    private ArrayList<FilterModel> filterModels;
    private ArrayList<String> filterTitles;

    private LookupInstance() {
        filterModels = new ArrayList<>();
        filterTitles = new ArrayList<>();
        filterCateModels = new ArrayList<>();

        for (FilterCateModel filterCateModel : filterCateModels) {
            for (FilterModel filterModel : filterCateModel.getFilters()) {
                filterTitles.add(filterModel.getName());
            }
            filterModels.addAll(filterCateModel.getFilters());
        }

    }

    public ArrayList<FilterCateModel> getFilterArrayList() {
        return filterCateModels;
    }

    public ArrayList<FilterModel> getAllFilter() {
        return filterModels;
    }

    public ArrayList<String> getFilterTitles() {
        return filterTitles;
    }

    public int indexOfName(String name) {
        return filterTitles.indexOf(name);
    }

    public int filterCount() {
        if (filterModels == null || filterModels.size() == 0) return 0;
        return filterModels.size();
    }

    public String nameOfIndex(int index) {
        if (filterTitles == null || filterTitles.size() == 0) return "";
        return filterTitles.get(index);
    }


    public static LookupInstance getInstance(Context context) {
        if (instance == null) {
            instance = new LookupInstance();
        }
        return instance;
    }

    public static void update() {
        instance = new LookupInstance();
    }

    public boolean isChargeFilter(String name) {
        for (FilterModel filterModel : filterModels) {
            if (TextUtils.equals(filterModel.getName(), name)) {
                return filterModel.getIs_vip() == 1;
            }
        }
        return false;
    }

    public String getId(String name) {
        for (FilterModel filterModel : filterModels) {
            if (TextUtils.equals(filterModel.getName(), name)) {
                return filterModel.getId();
            }
        }
        return "";
    }

    public String getName(String id) {
        for (FilterModel filterModel : filterModels) {
            if (TextUtils.equals(filterModel.getId(), id)) {
                return filterModel.getName();
            }
        }
        return "";
    }
}
