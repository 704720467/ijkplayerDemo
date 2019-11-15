package com.zp.libvideoedit.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.util.UUID;

public class MediaUtils {
    private MediaUtils() {

    }

    private static MediaUtils instance;
    private Context context;

    public Context getContext() {
        return context;
    }

    // 全局初始化，建议在appstart时调用该方法
    public static MediaUtils getInstance(Context context) {
        if (instance == null) {
            instance = new MediaUtils();
            instance.context = context;
        }
        return instance;
    }

    public static MediaUtils getInstance() {
        if (instance == null) {
            throw new RuntimeException("media shold be init with Context");
        }
        return instance;
    }

    /**
     * @param extrator
     * @param path     assert:// 为前缀，会从assert中读取，否则认为是绝对路径
     * @throws IOException
     */
    public void setDataSource(MediaExtractor extrator, String path) throws IOException {
        if (path.startsWith(Constants.ASSERT_FILE_PREFIX)) {
            String assertPath = path.substring(Constants.ASSERT_FILE_PREFIX.length());
            AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(assertPath);
            extrator.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        } else {
            extrator.setDataSource(path);
        }
    }

    public void setDataSource(MediaMetadataRetriever retriever, String path) throws IOException {
        if (path.startsWith(Constants.ASSERT_FILE_PREFIX)) {
            String assertPath = path.substring(Constants.ASSERT_FILE_PREFIX.length());
            AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(assertPath);
            retriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
        } else {
            retriever.setDataSource(path);
        }
    }


    public void copyAssertToFile(String asset, String destPath) throws IOException {

        InputStream is = context.getAssets().open(asset);
        File destinationFile = new File(destPath);
        destinationFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[4096];
        int nread;

        while ((nread = is.read(buffer)) != -1) {
            if (nread == 0) {
                nread = is.read();
                if (nread < 0)
                    break;
                os.write(nread);
                continue;
            }
            os.write(buffer, 0, nread);
        }
        os.close();

    }

    public static void setTextureDefaultConfig() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }


    public void copyAssertToFile(AssetFileDescriptor afd, String destPath) throws IOException {

        InputStream is = afd.createInputStream();
        File destinationFile = new File(destPath);
        destinationFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[4096];
        int nread;

        while ((nread = is.read(buffer)) != -1) {
            if (nread == 0) {
                nread = is.read();
                if (nread < 0)
                    break;
                os.write(nread);
                continue;
            }
            os.write(buffer, 0, nread);
        }
        os.close();

    }

    /**
     * 根据视频路径获得相应的音频路径
     *
     * @param videoPath
     * @return
     */
    public static String audioPath(String videoPath) {
        String aacPath = videoPath.substring(0, videoPath.lastIndexOf('.')) + ".aac";
        return aacPath;
    }

    /**
     * 文件移动或者重命名
     *
     * @param src
     * @param dest
     */
    public void move(String src, String dest) {
        File from = new File(src);
        File to = new File(dest);
        from.renameTo(to);

    }

    /**
     * String assertName = "glsl/" + style.toString() + ".frag"; <br/>
     * String script = getGlslFromAssert(assertName);
     *
     * @param assertName
     * @return
     */
    public String getGlslFromAssert(String assertName) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream is = assetManager.open(assertName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuffer stringBuffer = new StringBuffer();
            String str = null;
            while ((str = br.readLine()) != null) {
                stringBuffer.append(str);
                stringBuffer.append("\n");
            }
            return stringBuffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GPUSize checkPowOf2(GPUSize size) {
        float ration = (float) Math.log(size.width) / (float) Math.log(2);
        int powRation = (int) Math.ceil(ration + 1);
        int tempWidth = (int) Math.pow(2, powRation);
        ration = (float) tempWidth / (float) size.width;
        int height = (int) (ration * size.height);
        return new GPUSize(tempWidth, height);
    }

    public static GPUSize changeSize(GPUSize size) {
        int width = size.width;
        int height = size.height;
        if (size.width % 16 == 0) return size;
        int tempWidth = size.width;
        while (tempWidth % 16 != 0) {
            tempWidth++;
        }
        float ration = tempWidth / width;
        height = (int) (ration * height);
        width = tempWidth;
        return new GPUSize(width, height);
    }

    public static Bitmap debugCurrentBitmap(GPUImageFrameBuffer frameBuffer, GPUSize size, String dir, boolean save) {
        frameBuffer.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(size.width * size.height * 4);
        GLES20.glReadPixels(0, 0, size.width, size.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        if (save) {
            String path = "/sdcard/" + dir ;
            boolean success =  FileUtils.createDir(path);
            if(!success){
                Log.e("debugCurrentBitmap" ,"debugCurrentBitmap Create Dir Faild!!!");
                return null;
            }
            File file = new File(path + "/"+ UUID.randomUUID().toString() + ".jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
