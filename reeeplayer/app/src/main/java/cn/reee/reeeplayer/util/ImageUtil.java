package cn.reee.reeeplayer.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import cn.reee.reeeplayer.R;

/**
 * Create by zp on 2019-11-18
 */
public class ImageUtil {

    //    private static final int loadDrawable = R.drawable.load_ing;
    private static final int loadDrawable = R.mipmap.ic_launcher;

    private static final int errorDrawable = R.mipmap.ic_launcher;

    private static final int getErrorDrawable = R.mipmap.ic_launcher;

    /**
     * 视频默认图片
     */
    public static void loadVideoImage(String url, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(getErrorDrawable)
                .error(getErrorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }

    /**
     * 从网络中加载图片
     *
     * @param url
     * @param imageView
     * @param isCenter
     * @param isSkipMemoryCache
     */
    public static void loadImage(String url, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }

    public static void loadmAdapterContextImage(String url, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(getErrorDrawable)
                .error(getErrorDrawable)
//                .priority(Priority.HIGH)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }


    public static void loadAdapterContextImage(String url, Context context, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(getErrorDrawable)
                .error(getErrorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(context)
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }


    public static void loadVAdapterContextImage(String url, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(getErrorDrawable)
                .error(getErrorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }


    /**
     * 从网络中加载图片 加载广告专用
     *
     * @param url
     * @param imageView
     */
    public static void loadImageAdv(String url, ImageView imageView, int res) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()
                .placeholder(res)
                .error(res)
                .centerCrop()
                .skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.05f)
                .into(imageView);
    }


    /**
     * 从网络中加载图片
     *
     * @param url
     * @param imageView
     * @param isCenter
     */
    public static void loadImage(String url, ImageView imageView, boolean isCenter, int res) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(res)
                .error(R.color.black);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();


        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.1f)
                .into(imageView);
    }


    /**
     * 从网络中加载图片
     *
     * @param url
     * @param imageView
     * @param isCenter
     */
    public static void loadImage(String url, ImageView imageView, boolean isCenter, int res1, int res2) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(res1)
                .error(res2);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();


        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.01f)
                .into(imageView);
    }


    public static void loadBlurImg(ImageView imageView, String url, float radius, @DrawableRes int placeholder) {

        RequestOptions option = new RequestOptions()
                .placeholder(placeholder)
                .centerCrop()
//                .transform(new BlurTransformation(14, 3))
                .dontAnimate();

        Glide.with(imageView.getContext())
                .load(url)
                .apply(option)
                .into(imageView);
    }

    /**
     * 从网络中加载图片 低质量
     *
     * @param url
     * @param imageView
     */
    public static void loadImage_ForVideo(String url, ImageView imageView) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(R.color.black)
                .error(R.color.black).format(DecodeFormat.PREFER_RGB_565);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        options.skipMemoryCache(false);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.1f)
                .into(imageView);
    }


    /**
     * 从网络中加载图片 低质量
     *
     * @param url
     * @param imageView
     */
    public static void loadImage_ForVideo(Context context, String url, ImageView imageView) {
        if (imageView == null || imageView.getContext() == null) return;
        RequestOptions options = new RequestOptions()

                .placeholder(R.color.black)
                .error(R.color.black).format(DecodeFormat.PREFER_RGB_565);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        options.skipMemoryCache(false);

        Glide.with(context.getApplicationContext())
                .load(url)
                .apply(options)
                .thumbnail(0.1f)
                .into(imageView);
    }


    /**
     * 从网络中加载图片 不是从服务器加载
     *
     * @param url
     * @param imageView
     * @param isCenter
     * @param isSkipMemoryCache
     */
    public static void loadImage2(String url, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {

        if (TextUtils.isEmpty(url) || null == imageView.getContext())
            return;
        RequestOptions options = new RequestOptions()

                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .into(imageView);
    }


    /**
     * 从文件中加载图片
     *
     * @param file
     * @param imageView
     * @param isCenter
     */
    public static void loadImage(File file, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {
        if (TextUtils.isEmpty(file.getPath()) || null == imageView.getContext())
            return;
        RequestOptions options = new RequestOptions()

                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(file).thumbnail(0.1f)
                .apply(options)
                .into(imageView);
    }

    /**
     * 从资源中加载图片
     *
     * @param resId
     * @param imageView
     * @param isCenter
     */
    public static void loadImage(int resId, ImageView imageView, boolean isCenter, boolean isSkipMemoryCache) {

        RequestOptions options = new RequestOptions()

                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);
        if (isCenter)
            options.centerCrop();

        if (isSkipMemoryCache) //真则跳过内存缓存
            options.skipMemoryCache(true);

        Glide.with(imageView.getContext())
                .load(resId).thumbnail(0.1f)
                .apply(options)
                .into(imageView);
    }


    /**
     * 加载  圆   图
     *
     * @param url
     * @param imageView
     */
    public static void loadImageCircle(String url, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .placeholder(loadDrawable)
                .dontAnimate()
                .circleCrop()
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .thumbnail(0.1f)
                .into(imageView);

    }

    /**
     * 加载  圆   图
     *
     * @param url
     * @param imageView
     */
    public static void loadImageCircle(String url, ImageView imageView, final booleanCallback booleanCallback) {
        if (TextUtils.isEmpty(url) || null == imageView.getContext()) {
//            ToastUtil.showToast(get,"地址为空或者上下文为空");
            booleanCallback.returnCallback(false);
            return;
        }
        RequestOptions options = new RequestOptions()
//                .placeholder(place == 0 ? loadDrawable : place)
                .dontAnimate()
                .circleCrop();

//                .error(R.drawable.tx4);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        booleanCallback.returnCallback(false);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        booleanCallback.returnCallback(true);

                        return false;
                    }
                })
                .into(imageView);

    }

    /**
     * 加载  圆   图
     *
     * @param url
     * @param imageView
     */
    public static void loadImageCircle(int url, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .placeholder(loadDrawable)
                .dontAnimate()
                .error(errorDrawable)
                .circleCrop();

//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .into(imageView);

    }

    /**
     * 加载小图（指定像素大小）
     *
     * @param url
     * @param imageView
     * @param width
     * @param high
     */
    public static void loadSmallImageWSize(String url, ImageView imageView, int width, int high) {
        RequestOptions options = new RequestOptions()
                .override(width, high)//todo 设置最终显示的图片像素为width*high,注意:这个是像素,而不是控件的宽高
                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);


        Glide.with(imageView.getContext())
                .load(url).thumbnail(0.1f)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载大图，适合浏览大图（无加载在内存缓存中）
     *
     * @param url
     * @param imageView //
     */
    public static void loadBigImageNoCache(String url, ImageView imageView) {

        RequestOptions options = new RequestOptions()
                .placeholder(loadDrawable)
                .skipMemoryCache(true)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);


        Glide.with(imageView.getContext())
                .load(url)
                .thumbnail(0.1f)
                .apply(options)
                .transition(new DrawableTransitionOptions().crossFade(100))
                .into(imageView);

    }

    /**
     * 加载大图(加载在内存缓存中)
     *
     * @param url
     * @param imageView
     */
    public static void loadBigImage(String url, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .placeholder(loadDrawable)
                .error(errorDrawable);
//                .priority(Priority.HIGH)
//                .diskCacheStrategy(DiskCacheStrategy.NONE);


        Glide.with(imageView.getContext())
                .load(url)
                .thumbnail(0.1f)
                .apply(options)
                .transition(new DrawableTransitionOptions().crossFade(100))
                .into(imageView);
    }

    /**
     * 清除内存中的缓存  当加载过大图片之后,记得清理一下内存缓存  虽然不知道这方法有没有卵用
     *
     * @param context
     */
    public static void clearMemory(Context context) {

    }

    /**
     * 清除磁盘中的缓存 必须在后台线程中调用，建议同时clearMemory()  一般不使用
     *
     * @param context
     */
    public static void clearCache(Context context) {
//        if (context == null)
//            return;
//        Observable.create(e -> {
//            get(context).clearDiskCache();
//            e.onNext(true);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()).subscribe(o -> {
//            get(context).clearMemory();//必须在UI线程中调用
//        });
    }

    public interface booleanCallback {
        void returnCallback(boolean b);
    }
}
