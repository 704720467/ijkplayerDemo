package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.content.Context;

import com.zp.libvideoedit.GPUImage.Core.GPUFilterParam;
import com.zp.libvideoedit.GPUImage.Core.SavePictureTask;

import java.io.File;

/**
 * Created by why8222 on 2016/2/25.
 */
public class MagicEngine {

    private static Context context;

    private final GPUSurfaceBaseView GPUSurfaceBaseView;

    private MagicEngine(Builder builder) {
        GPUSurfaceBaseView = builder.GPUSurfaceBaseView;
        context = GPUSurfaceBaseView.getContext();
    }

    public static Context getContext() {
        return context;
    }

    public void onResume() {
        GPUSurfaceBaseView.onResume();
    }


    public void onDestroy() {
        GPUSurfaceBaseView.onDestroy();
    }

    public void onPause() {
        GPUSurfaceBaseView.onPause();
    }
    public void cancle(){
        GPUSurfaceBaseView.cancle();
    }

    public void setFilter(String filterName) {
        GPUSurfaceBaseView.setFilter(filterName);
    }

    /**
     * 拉腿的比例  设置scalue大小 value[0-----1]
     *
     * @param scaleValue
     */
    public void setScaleValue(float scaleValue) {
        GPUSurfaceBaseView.setLegScaleValue(scaleValue);
    }

    /**
     * 拉腿总是下半部分 设置从哪一点以下拉腿 value  [0~~1]
     *
     * @param centerValue
     */
    public void setCenterValue(float centerValue) {
        GPUSurfaceBaseView.setLegCenterValue(centerValue);
    }

    /**
     * 设置拉腿时视频的方向【-1，1】
     *
     * @param rotationValue
     */
    public void setLegRotationValue(float rotationValue) {
        GPUSurfaceBaseView.setLegRotationValue(rotationValue);
    }

    public void startRecoding() {
        GPUSurfaceBaseView.startRecoding();
    }

    public void stopRecoding() {
        GPUSurfaceBaseView.stopRecording();
    }

    public void getAllfilterModels(final GPUSurfaceCameraView.FilterCallable callback) {
        GPUSurfaceBaseView.getAllfilterModels(callback);
    }

    public void changeCamera() {
        GPUSurfaceBaseView.changeCamera();
    }

    public void savePicture(File file, SavePictureTask.OnPictureSaveListener listener) {
//        SavePictureTask savePictureTask = new SavePictureTask(file, listener);
        GPUSurfaceBaseView.savePicture(100, 100);
    }

    public void changeRecordingState(boolean isRecording) {
        ((GPUSurfaceCameraView) GPUSurfaceBaseView).changeRecordingState(isRecording);
    }

    public static class Builder {
        private GPUSurfaceBaseView GPUSurfaceBaseView;

        public Builder(GPUSurfaceBaseView GPUSurfaceBaseView) {
            this.GPUSurfaceBaseView = GPUSurfaceBaseView;
        }

        public MagicEngine build() {
            return new MagicEngine(this);
        }

        public Builder setVideoSize(int width, int height) {
            GPUFilterParam.videoWidth = width;
            GPUFilterParam.videoHeight = height;
            return this;
        }

        public Builder setVideoPath(String path) {
            GPUFilterParam.videoPath = path;
            return this;
        }
    }
}
