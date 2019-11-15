package com.zp.libvideoedit.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import com.zp.libvideoedit.GPUImage.Core.AndroidDispatchQueue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.UUID;

/**
 * Created by gwd on 2018/5/14.
 */

public class Common {
    private static final String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private static final String PREF_ACTIVATE_CODE = "activate_code";
    private static final String LICENSE_NAME = "senseme.lic";

    public static Bitmap zoomImg(Bitmap bm, int newWidth) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
//        float scaleHeight = ((float) newWidth) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        bm.recycle();
        return newbm;
    }

    public static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.postScale(-1, 1);   //镜像水平翻转
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;
        } catch (OutOfMemoryError ex) {
        }
        return null;
    }

    public static boolean fileExist(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("删除文件失败:" + fileName + "不存在！");
            return false;
        } else {
            if (file.isFile())
                return deleteFile(fileName);
        }
        return true;
    }


    /**
     * 删除单个文件
     *
     * @param fileName 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("删除单个文件" + fileName + "成功！");
                return true;
            } else {
                System.out.println("删除单个文件" + fileName + "失败！");
                return false;
            }
        } else {
            System.out.println("删除单个文件失败：" + fileName + "不存在！");
            return false;
        }
    }


    public static Bitmap convert(Bitmap a, boolean hFlip, boolean vFlip, int rotation) {
        int w = a.getWidth();
        int h = a.getHeight();
        Matrix m = new Matrix();
        if (hFlip) {
            m.postScale(-1, 1);   //镜像水平翻转
        }
        if (vFlip) {
            m.postScale(1, -1);   //镜像垂直翻转
        }
        if (rotation != 0) {
            m.postRotate(180);  //旋转-90度
        }
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
        return new2;
    }

    public static byte[] bitMapToByte(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.setHasAlpha(true);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] datas = baos.toByteArray();
        return datas;
    }


    public static Bitmap bitmapFromByte(byte[] b) {
//        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
//        return bitmap;

        Bitmap bitmap = null;
        InputStream input = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            input = new ByteArrayInputStream(b);
            SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(input, null, options));
            bitmap = (Bitmap) softRef.get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (b != null) {
                b = null;
            }
        }
        return bitmap;

    }

    public static void runOnMainQueueWithoutDeadlocking(Runnable runnable) {
        if (AndroidDispatchQueue.isMainThread()) {
            runnable.run();
        } else {
            AndroidDispatchQueue.getMainDispatchQueue().dispatchSync(runnable);
        }
    }


    public static String getExternalFilesDir(Context context, String filename) {
        File file = context.getExternalFilesDir(filename);
        String path = "";
        if (file != null) {
            path = file.getPath();
        }
        return path;
    }


    /**
     * 获取图片文件的信息，是否旋转了90度，如果是则反转
     *
     * @param bitmap 需要旋转的图片
     * @param path   图片的路径
     */
    public static Bitmap reviewPicRotate(Bitmap bitmap, String path) {
        int degree = getPicRotate(path);
        if (degree != 0) {
            Matrix m = new Matrix();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            m.setRotate(degree); // 旋转angle度
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);// 从新生成图片
        }
        return bitmap;
    }


    /**
     * 读取图片文件旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片旋转的角度
     */
    public static int getPicRotate(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


    /**
     * 随机生产文件名
     *
     * @return
     */
    private static String generateFileName() {
        return UUID.randomUUID().toString();
    }

    /**
     * 保存bitmap到本地
     * * @param mBitmap
     *
     * @return
     */
    public static String saveBitmap(String outputFilePath, Bitmap mBitmap) {
        String savePath;
        File filePic;
        savePath = outputFilePath;
        try {
            filePic = new File(savePath + generateFileName() + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return filePic.getAbsolutePath();
    }
}
