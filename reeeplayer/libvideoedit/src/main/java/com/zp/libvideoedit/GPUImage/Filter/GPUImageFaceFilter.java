package com.zp.libvideoedit.GPUImage.Filter;

import android.content.Context;


import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

import java.io.IOException;

/**
 * Created by gwd on 2018/2/27.
 */

public class GPUImageFaceFilter extends GPUImageFilter {

    public GPUImageFaceFilter(Context context) throws IOException {
        super();
        this.mContext = context;

    }

    @Override
    public void init() {
        super.init();
        //版本检测
        try {
            if(checkLicense(mContext)==false)return ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //加载人脸识别模型
    }

    public  boolean checkLicense(Context context) throws IOException {
        return true;
    }
    private Context mContext ;
    private String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private String PREF_ACTIVATE_CODE = "activate_code";
    private final String LICENSE_NAME = "SenseME.lic";
    public static final String MODEL_NAME_ACTION= "action_5.0.0.model";
    public static final String MODEL_NAME_FACE_ATTRIBUTE =
            "M_SenseME_Attribute_1.0.1.model";
    public static final String MODEL_NAME_EYEBALL_CONTOUR =
            "M_SenseME_Iris_1.7.0.model";
    public static final String MODEL_NAME_FACE_EXTRA =
            "M_SenseME_Face_Extra_5.1.0.model";
    public static final String MODEL_NAME_BODY_FOURTEEN =
            "M_SenseME_Body_Fourteen_1.1.0.model";
    public static final String MODEL_NAME_HAND = "M_SenseME_Hand_5.0.0.model";
    
}
