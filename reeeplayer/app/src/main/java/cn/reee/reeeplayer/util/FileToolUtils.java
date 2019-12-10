package cn.reee.reeeplayer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import cn.reee.reeeplayer.modle.ZpFileInfo;


public class FileToolUtils {
    private static String TAG = "FileToolUtils";

    // 删除文件夹
    // param folderPath 文件夹完整绝对路径
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除指定文件夹下所有文件
    // param path 文件夹完整绝对路径
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);// 先删除文件夹里面的文件
                // delFolder(path + "/" + tempList[i]);// 再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 判断文件夹是否存在，不存在就创建出来
     *
     * @param strFolder
     */
    public static void isFolderExists(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 判断文件是否存在，不存在就创建出来
     *
     * @param strFolder
     */
    public static boolean isFileExists(String strFolder) {
        File file = new File(strFolder);
        return file.exists();
    }

    /** */
    /**
     * 文件重命名
     * <p>
     * 文件目录
     *
     * @param oldPath 原来的文件名
     * @param newPath 新文件名
     */
    public static void renameFile(String oldPath, String newPath) {
        if (!oldPath.equals(newPath)) {// 新的文件名和以前文件名不同时,才有必要进行重命名
            File oldfile = new File(oldPath);
            File newfile = new File(newPath);
            if (!oldfile.exists()) {
                return;// 重命名文件不存在
            }
            if (newfile.exists()) {// 若在该目录下已经有一个文件和新文件名相同，则不允许重命名
            } else {
                oldfile.renameTo(newfile);
            }
        } else {
        }
    }

    /**
     * 删除文件
     *
     * @param path
     */
    public static void deleteFile(String path) {
        File oldfile = new File(path);
        if (oldfile.exists()) {
            oldfile.delete();
        }
    }

    /**
     * 查询指定文件夹中的文件
     */
    public static ArrayList<String> getFileName(String fileAbsolutePath) {
        ArrayList<String> allFile = new ArrayList<String>();
        File file = new File(fileAbsolutePath);
        File[] subFile = file.listFiles();
        if (subFile == null)
            return allFile;
        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                // 判断是否为MP4结尾
                if (filename.trim().toLowerCase().endsWith(".ttf")) {
                    allFile.add(filename.substring(filename.lastIndexOf("/") + 1, filename.lastIndexOf(".")));
                }
            }
        }
        return allFile;
    }

    /**
     * 删除第三方分享中缓存图片的问题，不能每次分享图片都是最新的图片
     */
    public static void deleteMobCacheImage() {
        try {
            String mobCasheImagePath = "/sdcard/Mob/cn.rootsports.reee/cache/images";
            File file = new File(mobCasheImagePath);
            if (!file.exists()) {
                Log.i(TAG, "文件不存在！");
                return;
            }
            boolean state = delAllFile(mobCasheImagePath);
            Log.i(TAG, "Mob删除：" + (state ? "成功" : "失败"));
        } catch (Exception e) {
            Log.e(TAG, "Mob 缓存图片删除报错！");
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
//    public static boolean copyFile(String oldPath, String newPath) {
//        boolean sucess = false;
//        InputStream inStream = null;//读入原文件
//        FileOutputStream fs = null;
//        try {
//            int bytesum = 0;
//            int byteread = 0;
//            File oldfile = new File(oldPath);
////            return !oldfile.renameTo(new File(newPath));
//            File file = new File(newPath.substring(0, newPath.lastIndexOf("/")));
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            if (oldfile.exists()) { //文件存在时
//                inStream = new FileInputStream(oldPath); //读入原文件
//                fs = new FileOutputStream(newPath);
//                byte[] buffer = new byte[1024];
//                int length;
//                while ((byteread = inStream.read(buffer)) != -1) {
//                    bytesum += byteread; //字节数 文件大小
//                    System.out.println(bytesum);
//                    fs.write(buffer, 0, byteread);
//                }
//                sucess = true;
//            } else {
//                Log.e("--Method--", "copyFile:  oldFile not exist.");
//            }
//        } catch (Exception e) {
//            sucess = false;
//            Log.e("--Method--", "copyFile:  复制单个文件操作出错！" + e.getMessage());
//        } finally {
//            try {
//                if (inStream != null)
//                    inStream.close();
//                if (inStream != null) {
//                    fs.flush();
//                    fs.close();
//                }
//            } catch (Exception e) {
//                Log.e("--Method--", "关闭流失败！" + e.getMessage());
//            }
//        }
//        return sucess;
//    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static boolean copyFile(String oldPath, String newPath) {
        try {
            Log.i("--Method--", oldPath + " " + newPath);
            File oldFile = new File(oldPath);
            if (!oldFile.exists()) {
                Log.e("--Method--", "copyFile:  oldFile not exist.");
                return false;
            } else if (!oldFile.isFile()) {
                Log.e("--Method--", "copyFile:  oldFile not file.");
                return false;
            } else if (!oldFile.canRead()) {
                Log.e("--Method--", "copyFile:  oldFile cannot read.");
                return false;
            }

            /* 如果不需要打log，可以使用下面的语句
            if (!oldFile.exists() || !oldFile.isFile() || !oldFile.canRead()) {
                return false;
            }
            */
            FileInputStream fileInputStream = new FileInputStream(oldPath);
            FileOutputStream fileOutputStream = new FileOutputStream(newPath);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static ArrayList<ZpFileInfo> getAllVideo(Context context) {
        ArrayList<ZpFileInfo> videos = new ArrayList<ZpFileInfo>();
        String[] mediaColumns = new String[]{
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DURATION
        };
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaColumns, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");

        if (cursor == null) return videos;

        if (cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                File file = new File(path);
                boolean canRead = file.canRead();
                long length = file.length();
                if (!canRead || length <= 0) {
                    continue;
                }
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                if (duration < 1000) {
                    continue;
                }
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                ZpFileInfo fileItem = new ZpFileInfo();
                fileItem.setFilePath(path);
                fileItem.setFileName(name);
                fileItem.setDuration(duration);
                fileItem.setFileType(ZpFileInfo.FILE_TYPE_VIDEO);
                fileItem.setThumbPath(cursor.getString(cursor
                        .getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
                if (fileItem.getFileName() != null && fileItem.getFileName().endsWith(".mp4")) {
                    videos.add(fileItem);
                }
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return videos;
    }

    public static ArrayList<ZpFileInfo> getAllPictrue(Context context) {
        ArrayList<ZpFileInfo> pictureList = new ArrayList<ZpFileInfo>();
        String[] mediaColumns = new String[]{
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DESCRIPTION
        };
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();

        Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaColumns, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");//按照创建时间降序排序
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            File file = new File(path);
            boolean canRead = file.canRead();
            long length = file.length();
            if (!canRead || length <= 0) {
                continue;
            }
            ZpFileInfo fileItem = new ZpFileInfo();
            fileItem.setFilePath(path);
            fileItem.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
            fileItem.setFileType(ZpFileInfo.FILE_TYPE_PICTURE);
            fileItem.setThumbPath(cursor.getString(cursor
                    .getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
            pictureList.add(fileItem);
        }
        if (cursor != null) {
            cursor.close();
        }
        return pictureList;
    }


    public static Pair<Long, String> getLatestPhoto(Context context) {
        //拍摄照片的地址
        String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera";
        //截屏照片的地址
        String SCREENSHOTS_IMAGE_BUCKET_NAME = getScreenshotsPath();
        //拍摄照片的地址ID
        String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);
        //截屏照片的地址ID
        String SCREENSHOTS_IMAGE_BUCKET_ID = getBucketId(SCREENSHOTS_IMAGE_BUCKET_NAME);
        //查询路径和修改时间
        String[] projection = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_MODIFIED};
        //
        String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        //
        String[] selectionArgs = {CAMERA_IMAGE_BUCKET_ID};
        String[] selectionArgsForScreenshots = {SCREENSHOTS_IMAGE_BUCKET_ID};

        //检查camera文件夹，查询并排序
        Pair<Long, String> cameraPair = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        if (cursor.moveToFirst()) {
            cameraPair = new Pair(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
        }
        //检查Screenshots文件夹
        Pair<Long, String> screenshotsPair = null;
        //查询并排序
        cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgsForScreenshots,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");

        if (cursor.moveToFirst()) {
            screenshotsPair = new Pair(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        //对比
        if (cameraPair != null && screenshotsPair != null) {
            if (cameraPair.first > screenshotsPair.first) {
                screenshotsPair = null;
                return cameraPair;
            } else {
                cameraPair = null;
                return screenshotsPair;
            }

        } else if (cameraPair != null && screenshotsPair == null) {
            return cameraPair;

        } else if (cameraPair == null && screenshotsPair != null) {
            return screenshotsPair;
        }
        return null;
    }

    /**
     * 获取截图路径
     *
     * @return
     */
    public static String getScreenshotsPath() {
        String path = Environment.getExternalStorageDirectory().toString() + "/DCIM/Screenshots";
        File file = new File(path);
        if (!file.exists()) {
            path = Environment.getExternalStorageDirectory().toString() + "/Pictures/Screenshots";
        }
        file = null;
        return path;
    }

    private static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }
}
