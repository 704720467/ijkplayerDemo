package cn.reee.reeeplayer.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;


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
}
