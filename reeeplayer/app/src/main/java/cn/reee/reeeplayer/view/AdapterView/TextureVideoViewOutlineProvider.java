package cn.reee.reeeplayer.view.AdapterView;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Created by IT on 2018/9/12.
 */

public class TextureVideoViewOutlineProvider extends ViewOutlineProvider {
    private float mRadius;
    private int height;
    private int width;
    public TextureVideoViewOutlineProvider(float radius) {
        this.mRadius = radius;
    }

    public TextureVideoViewOutlineProvider(float radius, int height, int width) {
        this.mRadius = radius;
        this.height = height;
        this.width = width;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        int leftMargin = 0;
        int topMargin = 0;
        if(height == 0){
            height = rect.bottom - rect.top - topMargin;
        }
        if(width ==0){
            width = rect.right - rect.left - leftMargin;
        }
        Rect selfRect = new Rect(leftMargin, topMargin,
                width, height);
        outline.setRoundRect(selfRect, mRadius);
    }


}