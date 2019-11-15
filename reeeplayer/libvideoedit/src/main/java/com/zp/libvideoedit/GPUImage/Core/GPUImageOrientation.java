package com.zp.libvideoedit.GPUImage.Core;

/**
 * Created by gwd on 2018/2/8.
 */

public  enum GPUImageOrientation{
    GPUImageOrientationUp,            // default orientation
    GPUImageOrientationDown,          // 180 deg rotation
    GPUImageOrientationLeft,          // 90 deg CCW
    GPUImageOrientationRight,         // 90 deg CW
    GPUImageOrientationUpMirrored,    // as above but image mirrored along other axis. horizontal flip
    GPUImageOrientationDownMirrored,  // horizontal flip
    GPUImageOrientationLeftMirrored,  // vertical flip
    GPUImageOrientationRightMirrored, // vertical flip
}