package com.zp.libvideoedit.utils;

/**
 * 贝塞尔插值器
 * Created by zp on 2018/8/16.
 */

public class VNIInterpolator {

    private float ax;
    private float bx;
    private float cx;

    private float ay;
    private float by;
    private float cy;
    private float duration;

    VNIPoint p1;
    VNIPoint p2;

    public VNIInterpolator() {

        p1 = new VNIPoint(1, 0);
        p2 = new VNIPoint(0, 1);
        duration = 0.8f;
        cx = 3.0f * p1.x;
        bx = 3.0f * (p2.x - p1.x) - cx;
        ax = 1.0f - cx - bx;
        cy = 3.0f * p1.y;
        by = 3.0f * (p2.y - p1.y) - cy;
        ay = 1.0f - cy - by;
    }

    private float epsilon() {
        // Higher precision in the timing function for longer duration to avoid ugly discontinuities
        return 1.0f / (200.0f * duration);
    }


    public float valueForX(float progress) {
        float epsilon = epsilon();
        float xSolved = solveCurveX(progress, epsilon);
        float y = sampleCurveY(xSolved);
        return y;
    }


    private float sampleCurveX(float t) {
        // 'ax t^3 + bx t^2 + cx t' expanded using Horner's rule.
        return ((ax * t + bx) * t + cx) * t;
    }

    private float sampleCurveY(float t) {
        return ((ay * t + by) * t + cy) * t;
    }

    private float sampleCurveDerivativeX(float t) {
        return (3.0f * ax * t + 2.0f * bx) * t + cx;
    }


    private float solveCurveX(float x, float epsilon) {
        float t0;
        float t1;
        float t2;
        float x2;
        float d2;
        int i;

        // First try a few iterations of Newton's method -- normally very fast.
        for (t2 = x, i = 0; i < 8; i++) {
            x2 = sampleCurveX(t2) - x;
            if (Math.abs(x2) < epsilon) {
                return t2;
            }
            d2 = sampleCurveDerivativeX(t2);
            if (Math.abs(d2) < 1e-6) {
                break;
            }
            t2 = t2 - x2 / d2;
        }

        // Fall back to the bisection method for reliability.
        t0 = 0.0f;
        t1 = 1.0f;
        t2 = x;

        if (t2 < t0) {
            return t0;
        }
        if (t2 > t1) {
            return t1;
        }

        while (t0 < t1) {
            x2 = sampleCurveX(t2);
            if (Math.abs(x2 - x) < epsilon) {
                return t2;
            }
            if (x > x2) {
                t0 = t2;
            } else {
                t1 = t2;
            }
            t2 = (t1 - t0) * 0.5f + t0;
        }

        // Failure.
        return t2;

    }

    class VNIPoint {
        float x;
        float y;

        public VNIPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
