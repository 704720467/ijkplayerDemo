package cn.reee.reeeplayer.view.CutScrollLayout;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.util.ScreenUtil;

/**
 * 裁剪 滚动布局
 * Create by zp on 2019-11-30
 */
public class KLScrollLayout extends FrameLayout {
    private View mRootLayot;
    private View mLeftMarginView;
    private View mRightMarginView;
    private LinearLayout mSelectMaskView;
    private LinearLayout mMiddleVideoCoverContent;
    private RelativeLayout mVideoCoverContent;
    private HorizontalScrollView mContentScrollView;
    private float screentwidth;
    private float screentwidthScale;
    private float scaleValue;


    public KLScrollLayout(@NonNull Context context) {
        this(context, null);
    }

    public KLScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KLScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
        initView();
        initVideoCover();
    }


    private void initData() {
        screentwidth = ScreenUtil.getScreenWidth(getContext());
        scaleValue = ScreenUtil.getInstance(getContext()).getHorizontalScale();
        screentwidthScale = screentwidth / scaleValue;
    }

    /**
     * 初始化布局
     */
    private void initView() {
        mRootLayot = LayoutInflater.from(getContext()).inflate(R.layout.kl_scroll_layout, null);
        mLeftMarginView = mRootLayot.findViewById(R.id.left_margin_view);
        mRightMarginView = mRootLayot.findViewById(R.id.right_margin_view);
        mMiddleVideoCoverContent = mRootLayot.findViewById(R.id.middle_video_cover_content);
        mVideoCoverContent = mRootLayot.findViewById(R.id.video_cover_content);
        mSelectMaskView = mRootLayot.findViewById(R.id.select_view_mask_layout);
        mContentScrollView = mRootLayot.findViewById(R.id.content_scroll_view);
        mLeftMarginView.getLayoutParams().width = Math.round(screentwidthScale / 2);
        mRightMarginView.getLayoutParams().width = Math.round(screentwidthScale / 2);

        addView(mRootLayot);
    }

    private void initVideoCover() {
        float childWidth = 200 / ScreenUtil.getInstance(getContext()).getHorizontalScale();
        View view = new View(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(childWidth), ViewGroup.LayoutParams.MATCH_PARENT);
        view.setBackgroundColor(Color.parseColor("#A33B0E"));
        mMiddleVideoCoverContent.addView(view, lp);
        view.setTag(Math.round(screentwidth / 2));
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initMaskLayout(v);
            }
        });
//
//        View view2 = new View(getContext());
//        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(Math.round(childWidth * 3), ViewGroup.LayoutParams.MATCH_PARENT);
//        view2.setBackgroundColor(Color.parseColor("#8A0202"));
//        mMiddleVideoCoverContent.addView(view2, lp2);
//        view2.setTag(Math.round(screentwidth / 2) + 200);
//        view2.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                initMaskLayout(v);
//            }
//        });

        ChunkViewLayout chunkViewLayout = new ChunkViewLayout(getContext());
        chunkViewLayout.setTag(Math.round(screentwidth / 2) + 200);
        chunkViewLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initMaskLayout(view);
            }
        });
        mMiddleVideoCoverContent.addView(chunkViewLayout);


        //
        View view2 = new View(getContext());
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(Math.round(childWidth * 3), ViewGroup.LayoutParams.MATCH_PARENT);
        view2.setBackgroundColor(Color.parseColor("#8A0202"));
        mMiddleVideoCoverContent.addView(view2, lp2);
//        view2.setTag(Math.round(screentwidth / 2) + 200);
//        view2.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                initMaskLayout(v);
//            }
//        });
//        mMiddleVideoCoverContent.getLayoutParams().width = Math.round(childWidth * 4);
//        mSelectMaskView.getLayoutParams().width = Math.round(childWidth * 4);
    }

    private void initMaskLayout(View selectView) {
        int selectWidth = selectView.getWidth();
        int leftMargin = (Integer) selectView.getTag();
        mSelectMaskView.removeAllViews();

        View leftView = new View(getContext());
        LinearLayout.LayoutParams lpleft = new LinearLayout.LayoutParams(Math.round(leftMargin - 42 * scaleValue), ViewGroup.LayoutParams.MATCH_PARENT);
        leftView.setBackgroundColor(Color.parseColor("#00000000"));
        mSelectMaskView.addView(leftView, lpleft);

        CutView selectMask = new CutView(getContext());
        selectMask.initCutView((ChunkViewLayout) selectView, mLeftMarginView, leftView);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(Math.round(selectWidth + 42 * 2 * scaleValue)
                , ViewGroup.LayoutParams.MATCH_PARENT);
//        selectMask.setOnTouchListener(this);

        mSelectMaskView.addView(selectMask, lp2);
    }
}
