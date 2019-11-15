package com.zp.libvideoedit.GPUImage.Core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class AndroidDispatchQueue extends Thread {
    private static int index = 0;

    public static AndroidDispatchQueue dispatchQueueCreate() {
        AndroidDispatchQueue dispatchQueue = new AndroidDispatchQueue();
        dispatchQueue.startDispatch();
        return dispatchQueue;
    }

    public static AndroidDispatchQueue dispatchQueueCreate(String threadName) {
        AndroidDispatchQueue dispatchQueue = new AndroidDispatchQueue(threadName);
        dispatchQueue.startDispatch();
        return dispatchQueue;
    }

    public static void dispatchQueueDestroy(AndroidDispatchQueue dispatchQueue) {
        if (dispatchQueue.mEventHandler.getLooper().getThread().isAlive())
            dispatchQueue.mEventHandler.sendEmptyMessage(DISPATCH_QUEUE_EXIT);
    }

    public void remove(Runnable runnable) {
        mEventHandler.removeCallbacks(runnable);
    }

    private AndroidDispatchQueue(String threadName) {
        super(threadName + index);
        index++;
        mSyncLock = new Object();
    }

    private AndroidDispatchQueue() {
        this("DISQUE");
    }

    public void dispatchAsync(Runnable runnable) {
        mEventHandler.post(runnable);
    }

    public void dispatchSync(Runnable runnable) {
        synchronized (mSyncLock) {
            mEventHandler.post(new SyncRunnable(this, runnable));

            //Wait for for thread is ok
            try {
                mSyncLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startDispatch() {

        synchronized (mSyncLock) {
            //Start the thread
            start();

            //Wait the Handler is ok
            try {
                mSyncLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void run() {
//		super.run();

        synchronized (mSyncLock) {

            Looper.prepare();

            //Create an event queue on current context
            mEventHandler = new Handler(Looper.myLooper()) {
                @SuppressLint("NewApi")
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case DISPATCH_QUEUE_EXIT:
                            Looper.myLooper().quitSafely();
                            break;
                        default:
                            break;
                    }
                }
            };

            //Tell the caller the handler is ok
            mSyncLock.notifyAll();
        }
        //Enter event loop
        Looper.loop();
    }

    private static class SyncRunnable implements Runnable {

        public SyncRunnable(AndroidDispatchQueue androidDispatchQueue, Runnable runnable) {
            mAndroidDispatchQueue = androidDispatchQueue;
            mRunnable = runnable;
        }

        @Override
        public void run() {

            synchronized (mAndroidDispatchQueue.mSyncLock) {
                mRunnable.run();

                //Tell the caller the runnable is ok
                mAndroidDispatchQueue.mSyncLock.notifyAll();
            }
        }

        private Runnable mRunnable;
        private AndroidDispatchQueue mAndroidDispatchQueue;
    }

    private Handler mEventHandler = null;
    private Object mSyncLock = null;

    private static final int DISPATCH_QUEUE_EXIT = 0;

    //The main thread interface
    private AndroidDispatchQueue(Looper looper) {
        super("DIS_QL");
        mEventHandler = new Handler(looper);
        mSyncLock = new Object();
    }

    public static synchronized AndroidDispatchQueue getMainDispatchQueue() {
        if (mMainDispatchQueue == null) {
            mMainDispatchQueue = new AndroidDispatchQueue(Looper.getMainLooper());
        }
        return mMainDispatchQueue;
    }

    public static boolean isMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return true;
        }
        return false;
    }

    public static boolean isSameDispatchQueue(AndroidDispatchQueue dispatchQueue) {
        if (Looper.myLooper() == dispatchQueue.mEventHandler.getLooper()) {
            return true;
        }
        return false;
    }

    private static AndroidDispatchQueue mMainDispatchQueue = null;

    public static void runAsynchronouslyOnVideoProcessingQueue(AndroidDispatchQueue otherContextQueue, Runnable runnable) {
        if (AndroidDispatchQueue.isSameDispatchQueue(otherContextQueue)) {
            runnable.run();
        } else {
            otherContextQueue.dispatchAsync(runnable);
        }
    }

}
