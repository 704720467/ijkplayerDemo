package com.zp.libvideoedit.GPUImage.FilterCore;

import android.content.Context;

import com.zp.libvideoedit.GPUImage.Carma.Core.FilterSelectModel;
import com.zp.libvideoedit.GPUImage.Carma.Core.VNIFilterType;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/2/27.
 */

public class VNIFilterEnum {

    public static synchronized VNIFilterEnum getFilterEnumManager(Context context) {
        if (FilterEnumManager == null) {
            FilterEnumManager = new VNIFilterEnum();
            mContext = context;
        }
        return FilterEnumManager;
    }

    public GPUImageFilter getFilter(VNIFilterType type) {
        if (type == VNIFilterType.NONE) {
            GPUImageFilter filter =  new GPUImageFilter();
            filter.init();
            return filter;
        } else {
            imagePicture = new GPUImagePicture(VNIFilterType.lookupMapping(type), mContext);
            lookupFilter = new GPUImageLookupFilter();
            imagePicture.init();
            lookupFilter.init();
            imagePicture.addTarget(lookupFilter, 1);
            imagePicture.processImage();
            return lookupFilter;
        }
    }

    public static ArrayList<FilterSelectModel> allfilters(Context context) {
        mContext = context;
        if(filtermodes == null){
            filtermodes = new ArrayList<FilterSelectModel>();
            for (int i = 0; i < VNIFilterType.allType().size(); i++) {
                VNIFilterType type = VNIFilterType.allType().get(i);
                String name = type.toString();
                String lookupPath = VNIFilterType.lookupMapping(type);
                FilterSelectModel mo = new FilterSelectModel(name, lookupPath, type);
                filtermodes.add(mo);
            }
        }
        return filtermodes;

    }

    public ArrayList<FilterSelectModel> getFiltermodes() {
        return filtermodes;
    }

    public GPUImageFilter getFilter(int index) {
        return null;
    }

    private GPUImagePicture imagePicture;
    private GPUImageLookupFilter lookupFilter;
    private static VNIFilterEnum FilterEnumManager = null;
    private static Context mContext;
    private static ArrayList<FilterSelectModel> filtermodes ;
}
