package cn.reee.reeeplayer.view.AdapterView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import cn.reee.reeeplayer.util.ScreenUtil;


/**
 * 屏幕适配 线性布局
 * Create by zp on 2019-07-30
 */
public class ZpLinearLayout extends LinearLayout {
    private boolean flag;

    public ZpLinearLayout(Context context) {
        super(context);
    }

    public ZpLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZpLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!flag) {
            float  scale = ScreenUtil.getInstance(getContext()).getHorizontalScale();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
                if (params.width != LayoutParams.MATCH_PARENT && params.width != LayoutParams.WRAP_CONTENT)
                    params.width = (int) (params.width * scale);
                if (params.height != LayoutParams.MATCH_PARENT && params.height != LayoutParams.WRAP_CONTENT)
                    params.height = (int) (params.height * scale);
                params.leftMargin = (int) (params.leftMargin * scale);
                params.rightMargin = (int) (params.rightMargin * scale);
                params.topMargin = (int) (params.topMargin * scale);
                params.bottomMargin = (int) (params.bottomMargin * scale);
                child.setPadding((int) (child.getPaddingLeft() * scale), (int) (child.getPaddingTop() * scale),
                        (int) (child.getPaddingRight() * scale), (int) (child.getPaddingBottom() * scale));
            }
            flag = true;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
