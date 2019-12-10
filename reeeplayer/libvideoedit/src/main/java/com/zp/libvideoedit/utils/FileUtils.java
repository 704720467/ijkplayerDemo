package com.zp.libvideoedit.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.R;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileUtils {

    public static boolean copyFileIfNeed(Context context, String fileName) {
        String path = getFilePath(context, fileName);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                //如果模型文件不存在
                try {
                    if (file.exists())
                        file.delete();

                    file.createNewFile();
                    InputStream in = context.getApplicationContext().getAssets().open(fileName);
                    if (in == null) {
                        LogUtil.e("copyMode", "the src is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    public static String getFilePath(Context context, String fileName) {
        String path = null;
        File dataDir = context.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                LogUtil.e("FileUtil", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }


    public static String getExternalFilesDir(Context context, String filename) {
        File file = context.getExternalFilesDir(filename);
        String path = "";
        if (file != null) {
            path = file.getPath();
        }
        return path;
    }

    public static String getLoginBackgroundVideo(Context context) {
        return getExternalFilesDir(context, "video") + "/login_bg.mp4";

    }

    public static String getAvatarFilePath(Context context) {
        String path = getExternalFilesDir(context, "avatar") + "/avatar" + "" + ".jpg";
//        File file = new File(path);
//        if (!file.exists()) {
//            file.mkdir();
//        }

        File dir = new File(path);

        try {
            //在指定的文件夹中创建文件
            if (!dir.exists()) {
                dir.createNewFile();
            }
        } catch (Exception e) {
        }
        return path;
    }

    public static String getScriptPath(Context context, String scriptId) {
        String path = getExternalFilesDir(context, "scripts") + "/" + scriptId;
        return path;
    }

    public static String getMusicModulePath(Context context) {
//        String[] urls = url.split("/");
        String path = getExternalFilesDir(context, "musicmodule");
        return path;
    }

    public static String getNormalModulePath(Context context) {
        String path = getExternalFilesDir(context, "noramlmodule");
        return path;
    }

    public static String getFontPath(Context context) {
        String path = getExternalFilesDir(context, "font");
        return path;
    }

    public static String geResourcesPath(Context context) {
        String path = getExternalFilesDir(context, "resources");
        return path;
    }

    /**
     * 获取音乐文件存储路径
     *
     * @return
     */
    public static String getMusicFilePath(Context context) {
        String path1 = getExternalFilesDir(context, "resources")
                + File.separator + ".music";
//            Constant.TEMP_MUSIC_PATH;
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取mv文件存储路径
     *
     * @return
     */
    public static String getMvFilePath(Context context) {
//            String path1 = Constant.TEMP_MV_PATH;
        String path1 = getExternalFilesDir(context, "resources")
                + File.separator + "mv";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 视频倒播
     *
     * @return
     */
    public static String getReverseVideoFilePath(Context context) {
//            String path1 = Constant.TEMP_REVERSE_PATH;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "reverse";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取临时文件路径
     *
     * @return
     */
    public static String getTempFilePath(Context context) {
//            String path1 = Constant.TEMP_REVERSE_PATH;
        String path1 = getExternalFilesDir(context, "temp");
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取文字排版储存路径
     *
     * @return
     */
    public static String getEffectFilePath(Context context) {
//            String path1 = Constant.TEMP_EFFECT_PATH;
        String path1 = getExternalFilesDir(context, "resources")
                + File.separator + "effect";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取字体存储路径
     *
     * @return
     */
    public static String getFontFilePath(Context context) {
//            String path1 = Constant.TEMP_FONT_PATH;
        String path1 = getExternalFilesDir(context, "resources")
                + File.separator + "font";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取录音压缩路径
     *
     * @return
     */
    public static String getRecordFilePath(Context context) {
//            String path1 = Constant.TEMP_RECORDER_ZIP;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "temp_record";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取导出mp4文件的路径
     *
     * @return
     */
    public static String getExportFilePath(Context context) {
//            String path1 = Constant.TEMP_RELEASE_VIDEO;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "temp_release_video";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 贴图路径
     *
     * @return
     */
    public static String getStickerFilePath(Context context) {
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "sticker";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath() + File.separator;
    }

    /**
     * 断点续传路径
     *
     * @return
     */
    public static String getRecorderFilePath(Context context) {
//            String path1 = Constant.TEMP_RELEASE_VIDEO;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "recorder";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 录音路径
     *
     * @return
     */
    public static String getAudioFilePath(Context context) {
//            String path1 = Constant.TEMP_RELEASE_VIDEO;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "audio";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取脚本json文件路径
     *
     * @return
     */
    public static String getScriptFilePath(Context context) {
//            String path1 = Constant.TEMP_RELEASE_VIDEO;
        String path1 = getExternalFilesDir(context, "temp")
                + File.separator + "script";
        File file = new File(path1);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 获取用户move视频路径
     *
     * @return
     */
    public static String getPhotoVideoPath() {
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + "bigshot";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }


    public static void copyAssetstoSD(Context context, String assetsName, String outputPath) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(outputPath);
        myInput = context.getAssets().open(assetsName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }

        myOutput.flush();
        myInput.close();
        myOutput.close();
    }

    public static boolean isFileExist(String filePath) {

        File file = new File(filePath);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static String upLoadImagePath(Context context) {
        return getExternalFilesDir(context, "image");
    }

    public static String adPath(Context context) {
        return getExternalFilesDir(context, "adimage") + "/ad.jpg";
    }

    public static String getVideoCache(Context context) {
        try {
            File file = new File(context.getExternalCacheDir() + "/video-cache");
            long size = getFolderSize(file);
            return formetFileSize(size);
        } catch (Exception e) {
            e.printStackTrace();
            return "0.00MB";
        }
    }

    public static String getAeResourceCache(Context context) {
        long size = 0l;
        try {
            File scripts = new File(getExternalFilesDir(context, "scripts"));
            size = size + getFolderSize(scripts);

            File musicmodule = new File(getMusicModulePath(context));
            size = size + getFolderSize(musicmodule);

            File noramlmodule = new File(getNormalModulePath(context));
            size = size + getFolderSize(noramlmodule);

            File font = new File(getFontPath(context));
            size = size + getFolderSize(font);

            File resources = new File(geResourcesPath(context));
            size = size + getFolderSize(resources);

            return formetFileSize(size);
        } catch (Exception e) {
            e.printStackTrace();
            return "0.00MB";
        }
    }

    public static void clearAeResourceCache(Context context) {
        try {
            File scripts = new File(getExternalFilesDir(context, "scripts"));
            deleteAllFile(scripts);
        } catch (Exception e) {
            e.printStackTrace();

        }

        try {
            File musicmodule = new File(getMusicModulePath(context));
            deleteAllFile(musicmodule);
        } catch (Exception e) {
            e.printStackTrace();

        }

        try {
            File noramlmodule = new File(getNormalModulePath(context));
            deleteAllFile(noramlmodule);
        } catch (Exception e) {
            e.printStackTrace();

        }

        try {
            File font = new File(getFontPath(context));
            deleteAllFile(font);
        } catch (Exception e) {
            e.printStackTrace();

        }

        try {
            File resources = new File(geResourcesPath(context));
            deleteAllFile(resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            File resources = new File(getFontFilePath(context));
            deleteAllFile(resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            File resources = new File(getEffectFilePath(context));
            deleteAllFile(resources);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File resources = new File(getStickerFilePath(context));
            deleteAllFile(resources);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File resources = new File(EditConstants.TEMP_SPECIAL_PATH);
            deleteAllFile(resources);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //视频提取的音乐
        try {
            File resources = new File(EditConstants.EXTRACT_AUDIO_PATH);
            deleteAllFile(resources);

            SharedPreferencesTools.setParam(context, "video2audio", "");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     */
    public static void deleteAllFile(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                deleteAllFile(f);
            }
            file.delete();
        }
    }

    public static long getFolderSize(File file) throws Exception {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // 如果下面还有文件
                if (fileList[i].isDirectory()) {
                    size = size + getFolderSize(fileList[i]);
                } else {
                    size = size + fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */
    public static String formetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#0.00");
        String fileSizeString = "";
        String wrongSize = "0.00MB";
        if (fileS == 0) {
            return wrongSize;
        }
//        if (fileS < 1024) {
//            fileSizeString = df.format((double) fileS) + "B";
//        } else if (fileS < 1048576) {
//            fileSizeString = df.format((double) fileS / 1024) + "KB";
//        }
//        else
        if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 下载新版本
     *
     * @param context
     * @param url
     */
    public static void downLoadAPK(Context context, String url) {

        if (TextUtils.isEmpty(url)) {
            return;
        }

        try {

            String serviceString = Context.DOWNLOAD_SERVICE;
            final DownloadManager downloadManager = (DownloadManager) context.getSystemService(serviceString);

//            DownloadManager.Query query = new DownloadManager.Query();
//            query.setFilterById(SharedPreferencesManager.readDownloadId(context));
//            Cursor cursor = downloadManager.query(query);
//            if (cursor.moveToFirst()) {// 有记录
//                int totalSizeBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
//                int bytesDownloadSoFarIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
//                int totalSizeBytes = cursor.getInt(totalSizeBytesIndex);
//                int bytesDownloadSoFar = cursor.getInt(bytesDownloadSoFarIndex);
//                if (bytesDownloadSoFar < totalSizeBytes) {
//                    return;
//                }
//
//            }

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.allowScanningByMediaScanner();
            request.setVisibleInDownloadsUi(true);
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
            request.setMimeType("application/vnd.android.package-archive");

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/", "vni.apk");
            if (file.exists()) {
                file.delete();
            }
//            request.setDestinationInExternalFilesDir(context, "apk", "/vni.apk");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "vni.apk");
            long refernece = downloadManager.enqueue(request);
//            SharedPreferencesManager.saveDownloadId(context, refernece);
        } catch (Exception exception) {
            Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 复制assets的视频文件复制到SD卡
     */
    public static void coypLoginVideo(final Context context) {
        if (!FileUtils.isFileExist(FileUtils.getExternalFilesDir(context, "video") + "/login_bg.mp4")) {
            //文件不存在进行复制
            new Thread() {
                public void run() {
                    try {
                        FileUtils.copyAssetstoSD(context, "login_bg.mp4", FileUtils.getExternalFilesDir(context, "video") + "/login_bg.mp4");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
    }


    public static void zip(String targetPath, String destinationFilePath, String password) {
        try {
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

            if (password.length() > 0) {
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD); // 加密方式
                parameters.setPassword(password.toCharArray());
//                parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
//                parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
//                parameters.setPassword(password);
            }

            ZipFile zipFile = new ZipFile(destinationFilePath);

            File targetFile = new File(targetPath);
            if (targetFile.isFile()) {
                zipFile.addFile(targetFile, parameters);
            } else if (targetFile.isDirectory()) {
                zipFile.addFolder(targetFile, parameters);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zip(List<String> targetPath, String destinationFilePath, String password) {
        try {
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

            if (password.length() > 0) {
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD); // 加密方式
                parameters.setPassword(password.toCharArray());
//                parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
//                parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
//                parameters.setPassword(password);
            }

            ZipFile zipFile = new ZipFile(destinationFilePath);
            for (String s : targetPath) {
                File targetFile = new File(s);
                if (targetFile.isFile()) {
                    zipFile.addFile(targetFile, parameters);
                } else if (targetFile.isDirectory()) {
                    zipFile.addFolder(targetFile, parameters);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<File> unzip(Context context, String targetZipFilePath, String destinationFolderPath, String password) {
        List<File> extractedFileList = new ArrayList<File>();
        try {
            ZipFile zipFile = new ZipFile(targetZipFilePath);
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password);
            }
            zipFile.extractAll(destinationFolderPath);


            List<FileHeader> headerList = zipFile.getFileHeaders();

            for (FileHeader fileHeader : headerList) {
                if (!fileHeader.isDirectory()) {
                    extractedFileList.add(new File(destinationFolderPath, fileHeader.getFileName()));
                }
            }
            File[] extractedFiles = new File[extractedFileList.size()];
            extractedFileList.toArray(extractedFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return extractedFileList;
    }

    public static File getDBPath(Context context) {
        return new File(getExternalFilesDir(context, "db"));
    }


    /**
     * 将文本写入文件
     *
     * @param context
     * @param str
     * @param filePath
     * @param fileName
     */
    public static String writeData2File(Context context, String str, String filePath, String fileName) {
        File file = new File(filePath + File.separator + fileName);
        if (file.exists()) {
            file.delete();
        }
//        file.mkdirs();
//        File file1 = new File(file.getAbsolutePath() + File.separator + fileName);
//        try {
//            file1.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        try {
//            FileOutputStream fos = context.openFileOutput(filePath, MODE_PRIVATE);//获得FileOutputStream
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());//获得FileOutputStream
            //将要写入的字符串转换为byte数组
            byte[] bytes = str.getBytes();
            fos.write(bytes);//将byte数组写入文件
            fos.close();//关闭文件输出流
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath + File.separator + fileName;

    }


    public static String copyToDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + "bigshot";

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static void copyFile(Context context, String oldPath, String newPath, CopyListener listener) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File file = new File(copyToDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                if (listener != null) {
                    listener.onSuccess(newPath);
                }
            } else {
                if (listener != null) {
                    listener.onFail(context.getResources().getString(R.string.video_not_find));
                }
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
            if (listener != null) {
                listener.onFail(e.toString());
            }
        }

    }

    public interface CopyListener {
        void onSuccess(String newPath);

        void onFail(String e);
    }


    public static String getVideoEnd(Context context) {
        return getFilePath(context, "videoend") + "/endVideo.zip";
    }

    public static String getUserVideoEndUnzip(Context context) {
        return getFilePath(context, "videoend") + "/user";
    }

    public static String getTailVideoEndUnzip(Context context) {
        return getFilePath(context, "videoend") + "/tail";
    }

    public static String getReftailVideoEndUnzip(Context context) {
        return getFilePath(context, "videoend") + "/refTail";//脚本的片尾
    }

    //
//    /**
//     * 从assets目录中复制整个文件夹内容
//     *
//     * @param context Context 使用CopyFiles类的Activity
//     * @param oldPath String  原文件路径  如：/aa
//     * @param newPath String  复制后路径  如：xx:/bb/cc
//     */
//    public static void copyAssets2SD(Context context, String oldPath, String newPath) {
//        try {
//            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
//            if (fileNames.length > 0) {//如果是目录
//                File file = new File(newPath);
//                file.mkdirs();//如果文件夹不存在，则递归
//                for (String fileName : fileNames) {
//                    copyFilesFromAssets(context,oldPath + "/" + fileName, newPath + "/" + fileName);
//                }
//            } else {//如果是文件
//                InputStream is = context.getAssets().open(oldPath);
//                FileOutputStream fos = new FileOutputStream(new File(newPath));
//                byte[] buffer = new byte[1024];
//                int byteCount = 0;
//                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
//                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
//                }
//                fos.flush();//刷新缓冲区
//                is.close();
//                fos.close();
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            //如果捕捉到错误则通知UI线程
////            MainActivity.handler.sendEmptyMessage(COPY_FALSE);
//        }
//    }
    public static void copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        try {
            String fileNames[] = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {// 如果是文件
                InputStream is = context.getAssets().open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 判断文字是否存在
     *
     * @param filePath
     * @return
     */
    public static boolean fileIsExists(String filePath) {
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean createDir(String dirPath) {
        try {
            File f = new File(dirPath);
            if (f.exists() && f.isDirectory()) {
                return true;
            } else {
                boolean fp = f.mkdirs();
                if (fp) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * oldPath 和 newPath必须是新旧文件的绝对路径
     */
    public static void renameFile(String oldPath, String newPath) {
        if (TextUtils.isEmpty(oldPath)) {
            return;
        }

        if (TextUtils.isEmpty(newPath)) {
            return;
        }

        File file = new File(oldPath);
        file.renameTo(new File(newPath));
    }

    public static void copyVideoEnd(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(FileUtils.getTailVideoEndUnzip(context));
                File file1 = new File(FileUtils.getReftailVideoEndUnzip(context));

                if (file.exists()) {
                    file.delete();
                }
                if (file1.exists()) {
                    file1.delete();
                }
                FileUtils.copyFilesFromAssets(context, "tail", FileUtils.getTailVideoEndUnzip(context));
                FileUtils.copyFilesFromAssets(context, "refTail", FileUtils.getReftailVideoEndUnzip(context));
            }
        }).start();
    }

    public static String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }


    /**
     * 对文件重命名
     *
     * @param filePath 文件的路径
     */
    public static String reName(String filePath, String reName) {
        File file = new File(filePath);
        String path = filePath.substring(0, filePath.lastIndexOf("/") + 1) + reName;
        file.renameTo(new File(path));
        return path;
    }
}
