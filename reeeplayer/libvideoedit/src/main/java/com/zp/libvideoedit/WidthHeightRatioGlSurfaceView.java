package com.zp.libvideoedit;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by qin on 2018/6/1.
 */

public class WidthHeightRatioGlSurfaceView extends GLSurfaceView {

    private double wh_ratio = 0.0;

    public WidthHeightRatioGlSurfaceView(Context context) {
        super(context);
    }

    public WidthHeightRatioGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setWh_ratio(double wh_ratio) {
        this.wh_ratio = wh_ratio;
//        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // 父容器传过来的宽度方向上的模式
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        // 父容器传过来的高度方向上的模式
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // 父容器传过来的宽度的值
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()
                - getPaddingRight();

        // 父容器传过来的高度的值
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom()
                - getPaddingTop();

        if (wh_ratio > 0 && wh_ratio < 1) {
            width = (int) (height * wh_ratio);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                    MeasureSpec.EXACTLY);
        }

//        if(VERBOSE_GL) LogUtil.e("surface_02", width + " ... " + height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
