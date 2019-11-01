package cn.reee.reeeplayer.view.AdapterView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import cn.reee.reeeplayer.util.ScreenUtil;


/**
 * Create by zp on 2019-08-12
 */
public class ZpFrameLayout extends FrameLayout {
    private boolean flag;

    public ZpFrameLayout(Context context) {
        super(context);
    }

    public ZpFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZpFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!flag) {
            float scale = ScreenUtil.getInstance(getContext()).getHorizontalScale();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                if (params.width != LinearLayout.LayoutParams.MATCH_PARENT && params.width != LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.width = (int) (params.width * scale);
                if (params.height != LinearLayout.LayoutParams.MATCH_PARENT && params.height != LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.height = (int) (params.height * scale);
                params.leftMargin = (int) (params.leftMargin * scale);
                params.rightMargin = (int) (params.rightMargin * scale);
                params.topMargin = (int) (params.topMargin * scale);
                params.bottomMargin = (int) (params.bottomMargin * scale);
            }
            flag = true;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
