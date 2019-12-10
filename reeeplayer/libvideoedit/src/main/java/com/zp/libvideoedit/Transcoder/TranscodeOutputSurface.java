package com.zp.libvideoedit.Transcoder;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.zp.libvideoedit.utils.FormatUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static com.zp.libvideoedit.EditConstants.GL_DEBUG;
import static com.zp.libvideoedit.EditConstants.TAG_EN;
import static com.zp.libvideoedit.EditConstants.TAG_TR;
import static com.zp.libvideoedit.EditConstants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.FormatUtils.caller;


/**
 *
 */
public class TranscodeOutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" + "precision mediump float;\n" + "varying vec2 vTextureCoord;\n" + "uniform samplerExternalOES sTexture;\n" + "void main() {\n" + "  gl_FragColor = texture2D(sTexture, vTextureCoord).rgba;\n" + "}\n";

    private static final int EGL_OPENGL_ES2_BIT = 4;
    private ByteBuffer mPixelBuf;
    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;
    private TranscodeTextureRender mTextureRender;
    private int width;
    private int height;

    /**
     * Creates an TranscodeOutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
    public TranscodeOutputSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        this.width = width;
        this.height = height;

        mPixelBuf = ByteBuffer.allocateDirect(width * height * 4);
        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);

        eglSetup(width, height);
//        makeCurrent();

        setup();

        changeFragmentShader(FRAGMENT_SHADER);

    }

//    /**
//     * Creates an TranscodeOutputSurface using the current EGL context.  Creates a Surface that can be
//     * passed to MediaCodec.configure().
//     */
//    public TranscodeOutputSurface() {
//        setup();
//    }

    /**
     * Creates instances of TranscodeTextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private void setup() {
        mTextureRender = new TranscodeTextureRender();
        mTextureRender.surfaceCreated();

        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        if (VERBOSE_TR) Log.d(TAG_TR, Thread.currentThread().getName()+"|_textureID=" + mTextureRender.getTextureId());
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());

        // This doesn't work if TranscodeOutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, TranscodeOutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mSurface = new Surface(mSurfaceTexture);
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private void eglSetup(int width, int height) {
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (!mEGL.eglInitialize(mEGLDisplay, null)) {
            throw new RuntimeException("unable to initialize EGL10");
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE};
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!mEGL.eglChooseConfig(mEGLDisplay, attribList, configs, 1, numConfigs)) {
            throw new RuntimeException("unable to find RGB888+pbuffer EGL onAudioFormatChanged");
        }

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, configs[0], EGL10.EGL_NO_CONTEXT, attrib_list);
        checkEglError("eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        int[] surfaceAttribs = {EGL10.EGL_WIDTH, width, EGL10.EGL_HEIGHT, height, EGL10.EGL_NONE};
        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs);
        checkEglError("eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (mEGL != null) {
            if (mEGL.eglGetCurrentContext().equals(mEGLContext)) {
                // Clear the current context and surface to ensure they are discarded immediately.
                mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            }
            mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
            mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
            //mEGL.eglTerminate(mEGLDisplay);
        }

        mSurface.release();

        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();

        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
        mEGL = null;

        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        if (mEGL == null) {
            throw new RuntimeException("not configured for makeCurrent");
        }
        checkEglError("before makeCurrent");
        if (!mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        mTextureRender.changeFragmentShader(fragmentShader);
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the TranscodeOutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        if (VERBOSE_TR) Log.d(TAG_TR,  caller()+"_|DEAD_LOCK_awaitNewImage Thread:" + Thread.currentThread().getName());
        final int TIMEOUT_MS =  100;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                       Log.w(TAG_EN,"Surface frame wait timed out"+ FormatUtils.generateCallStack());
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    Log.w(TAG_EN,"Surface frame wait timed out"+ie.getMessage()+ FormatUtils.generateCallStack());
                }
            }
            mFrameAvailable = false;
        }

        // Latch the data.
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
        mTextureRender.checkGlError("after updateTexImage");
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage() {
        mTextureRender.drawFrame(mSurfaceTexture);
//        checkEglError("drawImage");
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        if (VERBOSE_TR) Log.d(TAG_TR, caller()+"|DEAD_LOCK_onFrameAvailable_new frame available");
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        if(!GL_DEBUG) return;
        boolean failed = false;
        int error;
        while ((error = mEGL.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG_TR, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }

    public Bitmap copyBitmap() {
        mPixelBuf.rewind();
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mPixelBuf.rewind();
        bmp.copyPixelsFromBuffer(mPixelBuf);
        return bmp;
    }
}
