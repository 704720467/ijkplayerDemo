package com.zp.libvideoedit.Transcoder;


import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Stack;

import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_TR;
import static com.zp.libvideoedit.Constants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.FormatUtils.caller;


/**
 * Created by gx on 2018/6/7.
 */


public abstract class AbstractFrameStackCache {
    protected String path;

    private AbstractFrameStackCache(String path) {
        this.path = path;
    }

    public abstract void clean();


    public abstract BufferFrame pop();


    public abstract void push(ByteBuffer outputFrame, long pts);

    public abstract void release();

    public static class StackBufferCacheFile extends AbstractFrameStackCache {
        private Stack<Long> ptsStack;
        private Stack<Integer> lengthStack;
        private int cursor;
        FileInputStream inputStream;
        FileOutputStream outputStream;
        MappedByteBuffer mappedByteBuffer;
        File cacheFile;

        public StackBufferCacheFile(String path) {
            super(path);
            init();
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller());


        }

        private void init() {
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller());
            ptsStack = new Stack<Long>();
            lengthStack = new Stack<Integer>();
            cacheFile = new File(path);
            cursor = 0;
            if (cacheFile.exists()) cacheFile.delete();
            try {
                outputStream = new FileOutputStream(cacheFile);
            } catch (Exception e) {
                Log.w(TAG, "error by create cache file" + e.getMessage(), e);
            }

        }

        @Override
        public BufferFrame pop() {
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller());
            if (lengthStack.isEmpty())
                return null;
            if(ptsStack.isEmpty())
                return null;
            try {
                if (mappedByteBuffer == null)
                    initReader();
                int length = lengthStack.pop();
                cursor = cursor - length;
                mappedByteBuffer.mark();
                mappedByteBuffer.position(cursor);
                byte[] buffer = new byte[length];
                mappedByteBuffer.get(buffer, 0, length);
                mappedByteBuffer.reset();
                long pts=ptsStack.pop();
                if(VERBOSE_TR)
                    Log.d(TAG_TR,caller()+"size:"+buffer.length+",\tpts:"+pts);
                return new BufferFrame(pts, buffer);

            } catch (IOException e) {
                Log.w(TAG, caller(), e);
                return null;
            }

        }

        private void initReader() throws IOException {
            inputStream = new FileInputStream(cacheFile);
            mappedByteBuffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, cacheFile.length());

        }

        @Override
        public void push(ByteBuffer outputFrame, long pts) {
            ptsStack.push(pts);
            int length = outputFrame.remaining();
            lengthStack.push(length);
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller()+"size:"+length+",\tpts:"+pts);
            byte[] buffer = new byte[length];
            outputFrame.get(buffer);
            try {
                outputStream.write(buffer, 0, length);
            } catch (IOException e) {
                Log.w(TAG, caller(), e);
            }
            cursor += length;
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller()+"size:"+length+",\tpts:"+pts+"，cursor:"+cursor);

        }

        @Override
        public void clean() {
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller());
            release();
            init();


        }

        @Override
        public void release() {
            if(VERBOSE_TR)
                Log.d(TAG_TR,caller());

            mappedByteBuffer = null;

            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "error by close inputStream " + e.getMessage(), e);
            }

            try {
                if (outputStream != null)
                outputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "error by close outputStream " + e.getMessage(), e);
            }

            File cacheFile = new File(path);
            if (cacheFile != null && cacheFile.exists()) {
                cacheFile.delete();
            }
        }
    }


    public static class StackBufferCacheMem extends AbstractFrameStackCache {
        private Stack<BufferFrame> stack;

        public StackBufferCacheMem(String path) {
            super(path);
            stack = new Stack<BufferFrame>();
        }


        public void clean() {
            stack.clear();
        }

        @Override
        public BufferFrame pop() {
            if (VERBOSE_TR) Log.d(TAG_TR, caller());
            BufferFrame bufferFrame = null;
            if (!stack.isEmpty())
                bufferFrame = stack.pop();
            if (VERBOSE_TR) {
                Log.d(TAG_TR, caller() + "pop frame :" + (bufferFrame == null ? "null" : "pts:" + bufferFrame.pts + ", length:" + bufferFrame.buffer.length));
            }
            return bufferFrame;
        }


        public void push(ByteBuffer outputFrame, long pts) {
            if (VERBOSE_TR) Log.d(TAG_TR, caller() + "pts:" + pts + ", outputFrame:" + outputFrame);
            byte[] buffer = new byte[outputFrame.remaining()];
            outputFrame.get(buffer);
            stack.push(new BufferFrame(pts, buffer));
        }

        public void release() {
            stack.clear();
            stack = null;
        }
    }

    public class BufferFrame {
        public long pts;
        public byte[] buffer;

        public BufferFrame(long pts, byte[] buffer) {
            this.pts = pts;
            this.buffer = buffer;
        }
    }

}


