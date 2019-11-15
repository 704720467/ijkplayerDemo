package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.zp.libvideoedit.GPUImage.Carma.CameraProxy;
import com.zp.libvideoedit.utils.SharedPreferencesTools;

import java.util.ArrayList;


public class CameraEngine {
    private static int camera_FACING_BACK_ID = CameraInfo.CAMERA_FACING_BACK;
    private static int camera_FACING_FRONT_ID = CameraInfo.CAMERA_FACING_FRONT;
    public int cameraID = -1;
    private CameraProxy cameraProxy = null;
    private Context context;

    public CameraEngine(Context context) {
        this.context = context;
        cameraProxy = new CameraProxy(context);
//        cameraID = camera_FACING_BACK_ID;
    }

    public boolean isFlipHorizontal() {
        return cameraProxy.isFlipHorizontal();
    }


    public Camera getCamera() {
        return this.cameraProxy.getCamera();
    }

    public int getOrientation() {
        return cameraProxy.getOrientation();
    }


    public void changeCamera(Context context) {

        if (cameraID == camera_FACING_FRONT_ID) {
            cameraID = camera_FACING_BACK_ID;
        } else {
            cameraID = camera_FACING_FRONT_ID;
        }
        SharedPreferencesTools.setParam(context, "cameraID", cameraID);
    }

    public int getCameraID() {
        return cameraID;
    }

    public boolean openCamera(Context context, int cameraID) {

        if (this.cameraID == -1) {
            cameraID = (int) SharedPreferencesTools.getParam(context, "cameraID", 0);
        }
        SharedPreferencesTools.setParam(context, "cameraID", cameraID);
        this.cameraID = cameraID;

        boolean opened = cameraProxy.openCamera(cameraID);
        if (opened) {
            setDefaultParameters(context);
        }

//        setRotation(90);
        return opened;
    }

    public void releaseCamera() {
        cameraProxy.releaseCamera();
    }

    public void setParameters(Parameters parameters) {
        cameraProxy.getCamera().setParameters(parameters);
    }

    public Parameters getParameters() {
        return cameraProxy.getParameters();
    }

    public void setDefaultParameters(Context context) {
        int result = getCameraOrientation(context);
        cameraProxy.getCamera().setDisplayOrientation(result);

    }

    public int getCameraOrientation(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        GLCameraInfo cameraInfo = getCameraInfo();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (cameraID == camera_FACING_FRONT_ID) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Size getPreviewSize() {
        return cameraProxy.getPreviewSize();
    }

    private Size getPictureSize() {
        return cameraProxy.getCamera().getParameters().getPictureSize();
    }


    public void startPreview(SurfaceTexture surfaceTexture, Camera.PreviewCallback previewcallback) {
        cameraProxy.startPreview(surfaceTexture, previewcallback);
    }

    public void startPreview() {
        cameraProxy.startPreview();

    }

    public void stopPreview() {
        cameraProxy.stopPreview();
    }

    public void setRotation(int rotation) {
        cameraProxy.setRotation(rotation);
    }

    public void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                            Camera.PictureCallback jpegCallback) {
        cameraProxy.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public GLCameraInfo getCameraInfo() {
        GLCameraInfo info = new GLCameraInfo();
        Size size = getPreviewSize();
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);
        info.previewWidth = size.width;
        info.previewHeight = size.height;
        info.orientation = cameraInfo.orientation;
        info.isFront = cameraID == 1 ? true : false;
        size = getPictureSize();
        info.pictureWidth = size.width;
        info.pictureHeight = size.height;
        return info;
    }

    public ArrayList<String> getSupportedPreviewSize(String[] previewSizes) {
        return cameraProxy.getSupportedPreviewSize(previewSizes);
    }

    public void setCameraID(int cameraID) {
        this.cameraID = cameraID;
    }

    public void setPreviewSize(int width, int height) {
        cameraProxy.setPreviewSize(width, height);
    }

    public int getDisplayOrientation(int dir) {
        return cameraProxy.getDisplayOrientation(dir);
    }


    public int getNumberOfCameras() {
        return cameraProxy.getNumberOfCameras();
    }

    public boolean cameraOpenFailed() {
        return cameraProxy.cameraOpenFailed();
    }

    public boolean isCameraOpen() {
        return cameraProxy.isCameraOpen();
    }

}