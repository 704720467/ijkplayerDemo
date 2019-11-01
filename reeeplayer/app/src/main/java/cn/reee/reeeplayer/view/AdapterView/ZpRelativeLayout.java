package cn.reee.reeeplayer.view.AdapterView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import cn.reee.reeeplayer.util.ScreenUtil;

/**
 * 屏幕适配 相对布局
 * Create by zp on 2019-07-30
 */
public class ZpRelativeLayout extends RelativeLayout {
    private boolean flag;

    public ZpRelativeLayout(Context context) {
        super(context);
    }

    public ZpRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZpRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
