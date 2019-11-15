package com.zp.libvideoedit.GPUImage.Core;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GPUImageFramebufferCache {

    private Map<String, GPUImageFrameBuffer> framebufferCache = null;
    private Map<String, Integer> framebufferTypeCounts = null;
    private List<GPUImageFrameBuffer> activeImageCaptureList = null;

    public GPUImageFramebufferCache() {
        framebufferCache = new HashMap<String, GPUImageFrameBuffer>();
        framebufferTypeCounts = new HashMap<String, Integer>();
        activeImageCaptureList = new ArrayList<GPUImageFrameBuffer>();
    }

    private synchronized GPUImageFrameBuffer fetchFramebufferForSize(final GPUSize framebufferSize, final GPUTextureOptions textureOptions, final boolean onlyTexture) {
        final List<GPUImageFrameBuffer> outputList = new ArrayList<GPUImageFrameBuffer>();
        GPUImageFrameBuffer framebufferFromCache = null;
        String lookupHash = hashKeyString(framebufferSize, textureOptions, onlyTexture);
        Integer numberOfMatchingTexturesInCache = framebufferTypeCounts.get(lookupHash);
        int numberOfMatchingTextures = 0;
        if (numberOfMatchingTexturesInCache == null) {
            numberOfMatchingTextures = 0;
        } else {
            numberOfMatchingTextures = numberOfMatchingTexturesInCache.intValue();
        }
//        if (numberOfMatchingTexturesInCache != null && numberOfMatchingTexturesInCache.intValue() == 0) {
//            Log.e("error", "error");
//            Log.e("FrameBufferCache", "Framebuffer 可能存在异常！！！");
//        }
        if (numberOfMatchingTextures < 1) {
            framebufferFromCache = new GPUImageFrameBuffer(framebufferSize, textureOptions, onlyTexture);
//            Log.e("FrameBufferCache", caller() + " stack " + generateCallStack() + "Framebuffer 重建 正常使用" + " width: " + framebufferSize.width + " height: " + framebufferSize.height + "  count: " + framebufferCache.size());
        } else {
            int currentTextureID = numberOfMatchingTextures - 1;
            while ((framebufferFromCache == null) && (currentTextureID >= 0)) {
                String textureHash = String.format("%s-%d", lookupHash, currentTextureID);
                framebufferFromCache = framebufferCache.get(textureHash);
                if (framebufferFromCache != null) {
                    framebufferCache.remove(textureHash);
                }
                currentTextureID--;
            }
            currentTextureID++;
            framebufferTypeCounts.put(lookupHash, Integer.valueOf(currentTextureID));
            if (framebufferFromCache == null) {
                framebufferFromCache = new GPUImageFrameBuffer(framebufferSize, textureOptions, onlyTexture);
//                Log.e("FrameBufferCache", "Framebuffer重建，有可能引起内存问题。。。。");
            }
        }

        framebufferFromCache.lock();
//        Log.e("FrameBufferCache", caller() + generateCallStack() + "  获取buffer成功" + framebufferFromCache);

        return framebufferFromCache;
    }

    public GPUImageFrameBuffer fetchFramebufferForSize(GPUSize framebufferSize, boolean onlyTexture) {
        GPUTextureOptions defaultTextureOptions = new GPUTextureOptions();
        return fetchFramebufferForSize(framebufferSize, defaultTextureOptions, onlyTexture);
    }

    public synchronized void returnFramebufferToCache(final GPUImageFrameBuffer framebuffer) {
        framebuffer.clearAllLocks();
        GPUSize framebufferSize = framebuffer.getTextureSize();
        GPUTextureOptions framebufferTextureOptions = framebuffer.getDefaultOpetion();

        String lookupHash = hashKeyString(framebufferSize, framebufferTextureOptions, framebuffer.isMissingFramebuffer());
        int numberOfMatchingTextures = 0;
        if (framebufferTypeCounts.get(lookupHash) == null) {
            numberOfMatchingTextures = 0;
        } else {
            numberOfMatchingTextures = framebufferTypeCounts.get(lookupHash).intValue();

        }
        String textureHash = lookupHash;
        textureHash = String.format("%s-%d", textureHash, numberOfMatchingTextures);

        framebufferCache.put(textureHash, framebuffer);
        if (numberOfMatchingTextures + 1 == 0) {
            Log.e("ttt", "ttt");
        }
        framebufferTypeCounts.put(lookupHash, Integer.valueOf(numberOfMatchingTextures + 1));
//        Log.e("FrameBufferCache", "FrameBufferCacheSize:  " + framebufferCache.size());

//        Log.e("FrameBufferCache", caller() + generateCallStack() + "  渲染完成 放回到渲染队列中" + framebuffer);

    }


    public synchronized void purgeAllUnassignedFramebuffers() {
//        for (String frameBufferKey : framebufferCache.keySet()) {
//            GPUImageFrameBuffer frameBuffer = framebufferCache.get(frameBufferKey);
//            frameBuffer.destoryFramebuffer();
//        }
        Iterator<String> iterator = framebufferCache.keySet().iterator();
        while (iterator.hasNext()) {
            GPUImageFrameBuffer frameBuffer = framebufferCache.get(iterator.next());
            frameBuffer.destoryFramebuffer();
        }
        framebufferCache.clear();
        framebufferTypeCounts.clear();
    }

    public void addFramebufferToActiveImageCaptureList(final GPUImageFrameBuffer framebuffer) {
        activeImageCaptureList.add(framebuffer);
    }

    public void removeFramebufferFromActiveImageCaptureList(final GPUImageFrameBuffer framebuffer) {
        activeImageCaptureList.remove(framebuffer);
    }

    private String hashKeyString(GPUSize size, GPUTextureOptions textureOptions, boolean onlyTexture) {
        String hashString = "";

        hashString += Integer.toString(size.width);
        hashString += Integer.toString(size.height);
        hashString += "-";
        hashString += Integer.toString(textureOptions.getMinFilter());
        hashString += Integer.toString(textureOptions.getMagFilter());
        hashString += Integer.toString(textureOptions.getWrapS());
        hashString += Integer.toString(textureOptions.getWrapT());
        hashString += Integer.toString(textureOptions.getInternalFormat());
        hashString += Integer.toString(textureOptions.getFormat());
        hashString += Integer.toString(textureOptions.getType());
        if (onlyTexture) {
            hashString += "-NOFB";
        }
        return hashString;
    }

}
