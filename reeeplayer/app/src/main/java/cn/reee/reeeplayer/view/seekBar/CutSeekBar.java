package cn.reee.reeeplayer.view.seekBar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.view.View;

import androidx.fragment.app.Fragment;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.view.AdapterView.ZpLinearLayout;
import cn.reee.reeeplayer.view.AdapterView.ZpRelativeLayout;

/**
 * 带裁剪 seekbar
 * Create by zp on 2019-11-21
 */
public class CutSeekBar extends LinearLayout implements View.OnTouchListener {

    private View mRootLayout;
    private ZpLinearLayout mImageThumbContent;
    private View mLeftMarker;//左边遮罩
    private View mRightMarker;//左边遮罩
    private View mLeftTouchBar;//左边把手
    private View mRightTouchBar;//右边把手
    private View mTopLineView;//顶部线
    private View mBottomLineView;//底部线
    private View mMiddleSelectView;//中间选择区域
    private int mTouchType;//1:拖动左边 2:拖动右边 3:拖动中间

    public CutSeekBar(Context context) {
        this(context, null);
    }

    public CutSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CutSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        addListener();
    }

    private void initView() {
        mRootLayout = LayoutInflater.from(getContext()).inflate(R.layout.cut_seek_bar_layout2, null);
        mImageThumbContent = mRootLayout.findViewById(R.id.image_thumb_content);
        mLeftMarker = mRootLayout.findViewById(R.id.left_marker);
        mRightMarker = mRootLayout.findViewById(R.id.right_marker);
        mLeftTouchBar = mRootLayout.findViewById(R.id.left_touch_bar);
        mRightTouchBar = mRootLayout.findViewById(R.id.right_touch_bar);
        mTopLineView = mRootLayout.findViewById(R.id.top_line_view);
        mBottomLineView = mRootLayout.findViewById(R.id.bottom_line_view);
        mMiddleSelectView = mRootLayout.findViewById(R.id.middle_select_view);
        addView(mRootLayout);
    }

    private void addListener() {
        mLeftTouchBar.setOnTouchListener(this);
        mRightTouchBar.setOnTouchListener(this);
        mMiddleSelectView.setOnTouchListener(this);
    }

    private float downX;
    private int oldWidth = 0;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == mLeftTouchBar) mTouchType = 1;
        if (view == mRightTouchBar) mTouchType = 2;
        if (view == mMiddleSelectView) mTouchType = 3;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = motionEvent.getRawX();
                if (mTouchType == 3)
                    oldWidth = mLeftMarker.getWidth();
                break;
            case MotionEvent.ACTION_MOVE:

                if (mTouchType == 1) {
                    mLeftMarker.getLayoutParams().width = (int) motionEvent.getRawX();
                    mMiddleSelectView.getLayoutParams().width = getWidth() - mRightMarker.getLayoutParams().width - mLeftMarker.getLayoutParams().width;
                }
                if (mTouchType == 2) {
                    mMiddleSelectView.getLayoutParams().width = (int) (motionEvent.getRawX() - mMiddleSelectView.getX());
                    mRightMarker.getLayoutParams().width = getWidth() - mMiddleSelectView.getLayoutParams().width - mLeftMarker.getLayoutParams().width;
                }
                if (mTouchType == 3) {
                    mLeftMarker.getLayoutParams().width = (int) (motionEvent.getRawX() - downX) + oldWidth;
                    mRightMarker.getLayoutParams().width = getWidth() - mMiddleSelectView.getLayoutParams().width - mLeftMarker.getLayoutParams().width;
                }
//                mLeftMarker.getLayoutParams().width = getWidth() - mMiddleSelectView.getLayoutParams().width;
                break;
        }
        requestLayout();
        return true;
    }

    private void changeViewWidth() {

    }
}
