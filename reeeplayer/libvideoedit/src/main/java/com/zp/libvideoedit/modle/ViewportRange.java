package com.zp.libvideoedit.modle;


/**
 * 素材可视范围
 * Created by zp on 2019/4/18.
 */

public class ViewportRange {
    private float left;//可是范围左边界
    private float top;//可是范围上边界
    private float right = 1f;//可是范围右边界
    private float bottom = 1f;//可是范围下边界

    public ViewportRange() {
        this.left = 0f;
        this.top = 0f;
        this.right = 1f;
        this.bottom = 1f;
    }

    /**
     * @param left   可是范围左边界
     * @param top    可是范围上边界
     * @param right  可是范围右边界
     * @param bottom 可是范围下边界
     *               <p>
     *               取值范围在0f-1f
     */
    public ViewportRange(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * 更新范围
     *
     * @param newViewportRange
     */
    public void upDataViewportRange(ViewportRange newViewportRange) {
        this.left = newViewportRange.left;
        this.top = newViewportRange.top;
        this.right = newViewportRange.right;
        this.bottom = newViewportRange.bottom;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

//    public ViewportRangeVo getViewportRangeVo() {
//        ViewportRangeVo viewportRangeVo = new ViewportRangeVo();
//        viewportRangeVo.setLeft(left);
//        viewportRangeVo.setTop(top);
//        viewportRangeVo.setRight(right);
//        viewportRangeVo.setBottom(bottom);
//        return viewportRangeVo;
//    }

    /**
     * 垂直方向是否居中展示
     *
     * @return
     */
    public boolean isVerticalShowCenter() {
        return bottom - top == 1f;
    }

    /**
     * 水平方向是否居中展示
     *
     * @return
     */
    public boolean isHorizontalShowCenter() {
        return right - left == 1f;
    }
}
