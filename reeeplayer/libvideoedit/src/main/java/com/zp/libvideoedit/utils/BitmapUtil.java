package com.zp.libvideoedit.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class BitmapUtil {

    public static Bitmap decodeBitmapFromResource(Resources res, int resId,
                                                  int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = ImageUtils.calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap aspectGLSampledBitmapFromFile(String filePath, int maxWidth) {
        if (maxWidth % 16 != 0) {
            Log.e("BitmapUtils", "aspectGLSampledBitmapFromFile need maxWidth%16 ==0 ,otherwise Opengl Cannot Render");
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        // 调用上面定义的方法计算inSampleSize值
        // 源图片的高度和宽度
        final int width = bitmap.getWidth();
        int tmpWidth = width;
        if (width < maxWidth) {
            while (tmpWidth % 16 != 0) {
                tmpWidth++;
            }
        } else {
            tmpWidth = maxWidth;
        }
        if (tmpWidth == width) {
            return bitmap;
        } else {
            Bitmap newBitmap = zoom(bitmap, tmpWidth / (width * 1.f));
            if (!bitmap.isRecycled()) bitmap.recycle();
            return newBitmap;
        }
    }

    public static Bitmap decodeSampledBitmapFromFile(String filePath, int maxWidth) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        // 调用上面定义的方法计算inSampleSize值
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width > maxWidth) {
            int widthRatio = width / maxWidth;
            if (width % maxWidth != 0) {
                widthRatio++;
            }
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = widthRatio;
        }
        options.inSampleSize = inSampleSize;
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }


    public static Bitmap decodeSampledBitmapFromFile(String filePath,
                                                     int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    /**
     * 缩放图片
     *
     * @param bitmap
     * @param zf
     * @return
     */
    public static Bitmap zoom(Bitmap bitmap, float zf) {
        Matrix matrix = new Matrix();
        matrix.postScale(zf, zf);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    /**
     * 缩放图片
     *
     * @param bitmap
     * @param
     * @return
     */
    public static Bitmap zoom(Bitmap bitmap, float wf, float hf) {
        Matrix matrix = new Matrix();
        matrix.postScale(wf, hf);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    /**
     * 图片圆角处理
     *
     * @param bitmap
     * @param roundPX
     * @return
     */
    public static Bitmap getRCB(Bitmap bitmap, float roundPX) {
        // RCB means
        // Rounded
        // Corner Bitmap
        Bitmap dstbmp = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(dstbmp);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPX, roundPX, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return dstbmp;
    }

    private static Bitmap loadBitmapFromView(View v) {
        int w = v.getWidth();
        int h = v.getHeight();

        Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        c.drawColor(Color.WHITE);
        /** 如果不设置canvas画布为白色，则生成透明 */

        v.layout(0, 0, w, h);
        v.draw(c);

        return bmp;
    }

    public static Bitmap getBitmapWithP(Activity activity, View v) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();

        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int pX = GlobalTools.dip2px(activity, 10);
        bitmap = Bitmap.createBitmap(bitmap, x + pX, y + pX, v.getWidth() - 2 * pX, v.getHeight() - 2 * pX);

        return bitmap;
    }

    public static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "vni");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
//        try {
//            MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                    file.getAbsolutePath(), fileName, null);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }

    public static Bitmap layout2Bitmap(View parentView) {
        Bitmap bitmap = Bitmap.createBitmap(parentView.getMeasuredWidth(), parentView.getMeasuredHeight(), Config.ARGB_4444);
        Canvas c = new Canvas(bitmap);
        parentView.draw(c);
        return bitmap;
    }

    public static Bitmap getBitmapFromView(View view, int width, int height, float rotate) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        canvas.rotate(rotate, view.getLeft() + view.getMeasuredWidth() / 2, view.getTop() + view.getMeasuredHeight() / 2);
        view.draw(canvas);
        return bitmap;
    }

    public static Bitmap takeScreenShot(View view, int width, int height, float rotate) {
        Bitmap bitmap = null;
        View dView = view;
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        bitmap = Bitmap.createBitmap(dView.getDrawingCache());

        return bitmap;
    }

    public static Bitmap createWaterMaskBitmap(Bitmap src, Bitmap watermark,
                                               int paddingLeft, int paddingTop) {
        if (src == null) {
            return null;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        //创建一个bitmap
        Bitmap newb = Bitmap.createBitmap(width, height, Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        //将该图片作为画布
        Canvas canvas = new Canvas(newb);
        //在画布 0，0坐标上开始绘制原始图片
        canvas.drawBitmap(src, 0, 0, null);
        //在画布上绘制水印图片
        canvas.drawBitmap(watermark, paddingLeft, paddingTop, null);
        // 保存
        canvas.save();
        // 存储
        canvas.restore();
        return newb;
    }

//    /**
//     * 生成只有水印的透明bitmap
//     *
//     * @param mContext
//     * @param isVertical
//     * @return
//     */
//    public static Bitmap createWaterMaskBitmapOnlyWaterMask(Context mContext, boolean isVertical) {
////        float scale = 2f * (!isVertical ? 1f : (9f / 16));
//        int width = !isVertical ? 1280 : 720;
//        int height = !isVertical ? 720 : 1280;
////
//        Bitmap watermarkTemp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_min_watermark);
////        int width = (int) ((105f / 4f) * 3f);
////        int height = (int) ((40f / 4f) * 3f);
//        float scale = (float) ((105f / 4f) * 3f) / watermarkTemp.getWidth();
//        Bitmap watermark = BitmapUtil.zoom(watermarkTemp, scale);
//        if (watermarkTemp != null && !watermarkTemp.isRecycled())
//            watermarkTemp.recycle();
//        watermarkTemp = null;
//
//        //创建一个bitmap
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        int margin = watermark.getWidth() / 5;
//        int paddingLeft = bitmap.getWidth() - watermark.getWidth() - margin;
//        int paddingTop = bitmap.getHeight() - watermark.getHeight() - margin;
//
//        // 建立Paint 物件
//        Paint vPaint = new Paint();
//        vPaint.setStyle(Paint.Style.STROKE);   //空心
//        vPaint.setAlpha(153);   //
//        //将该图片作为画布
//        Canvas canvas = new Canvas(bitmap);
//        //在画布上绘制水印图片
//        canvas.drawBitmap(watermark, paddingLeft, paddingTop, vPaint);
//        // 保存
//        canvas.save();
//        // 存储
//        canvas.restore();
//        if (watermark != null && !watermark.isRecycled())
//            watermark.recycle();
//        watermark = null;
//        return bitmap;
//    }

    /**
     * 将文件转换为bitmap
     *
     * @param filePath
     * @return
     */
    public static Bitmap loadFileToBitmap(String filePath) {
        Bitmap bitmap = null;
        try {
            InputStream in = new FileInputStream(filePath);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "a7", e);
        }
        return bitmap;
    }

    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;

    }

    /**
     * 保存图片到本地
     *
     * @param mBitmap
     * @param fileName
     * @return
     */
    public static void saveBitmap(Context context, Bitmap mBitmap, String fileName, SaveBitmapListener listener) {
        File filePic;
        try {
            filePic = new File(FileUtils.getExportFilePath(context) + File.separator + fileName);
//            if (!filePic.exists()) {
//                filePic.getParentFile().mkdirs();
//                filePic.createNewFile();
//            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            if (listener != null) {
                listener.saveSuccess(filePic.getAbsolutePath());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            if (listener != null) {
                listener.saveFail();
            }
        }

    }

    public static void saveBitmap(Bitmap mBitmap, String path) {
        try {
            File filePic = new File(path);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveBitmap2(Bitmap mBitmap, String path) {
        try {
            File filePic = new File(path);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Bitmap compressImage(Bitmap image, int size, int options) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        image.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
        while (baos.toByteArray().length / 1024 > size) {
            options -= 10;// 每次都减少10
            baos.reset();// 重置baos即清空baos
            // 这里压缩options%，把压缩后的数据存放到baos中
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }
        // 把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        // 把ByteArrayInputStream数据生成图片
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    public interface SaveBitmapListener {
        void saveSuccess(String path);

        void saveFail();
    }

    /**
     * 修改BitMap透明度zp
     *
     * @param sourceImg
     * @param number    透明度值
     * @return
     */
    public static Bitmap getAlplaBitmap(Bitmap sourceImg, int number) {

        Bitmap newBitMap = null;
        int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];

        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg.getWidth(), sourceImg.getHeight());

        number = number * 255 / 100;

        for (int i = 0; i < argb.length; i++) {

            argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF);

        }
        newBitMap = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Config.ARGB_8888);
        if (sourceImg != null && !sourceImg.isRecycled())
            sourceImg.recycle();
        sourceImg = null;

        return newBitMap;
    }

}
