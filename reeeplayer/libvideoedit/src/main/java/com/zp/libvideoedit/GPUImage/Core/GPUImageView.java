package com.zp.libvideoedit.GPUImage.Core;

import android.content.Context;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.modle.ViewportRange;

import java.nio.FloatBuffer;


public class GPUImageView extends SurfaceView implements SurfaceHolder.Callback, GPUImageInput {
	public static final String TAG = GPUImageView.class.getSimpleName();
	
	public GPUImageView(Context context) {
		super(context);
		commonInit();
	}
	
	public GPUImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		commonInit();
	}
	
	public GPUImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		commonInit();
	}
	
	private void commonInit() {
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		
		setFocusable(true);
	
		mInputRotation = GPUImageRotationMode.kGPUImageNoRotation;
		mEnabled = true;
		
//		GPUImageContext.sharedImageProcessingContexts().setmSurfaceHolder(getHolder());
		GPUImageContext.sharedImageProcessingContexts().setViewSurface(getHolder().getSurface());
	}
	
	public void setSurfaceHolderCallBack(SurfaceHolder.Callback holderCallback) {
		mHolderCallback = holderCallback;
	}
	
	public void initializeAttributes() {
		mDisplayProgram.addAttribute("position");
		mDisplayProgram.addAttribute("inputTextureCoordinate");
	}
	
	public void setDisplayFramebuffer() {
	    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	    GLES20.glViewport(0, 0, mSizeInPixels.width, mSizeInPixels.height);
	}
	
	public void presentFramebuffer() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		GPUImageContext.sharedImageProcessingContexts().presentBufferForDisplay();
	}
	
	public void setBackgroundColor(float red, float green, float blue, float alpha) {
		mBackgroundColorRed = red;
	    mBackgroundColorGreen = green;
	    mBackgroundColorBlue = blue;
	    mBackgroundColorAlpha = alpha;
	}
	
	public void createDisplayFramebuffer() {
		GPUImageContext.useImageProcessingContext();
		
//		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);     
//
//		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, getWidth(), getHeight());
		
//		IntBuffer backingWidth = IntBuffer.allocate(1);
//		IntBuffer backingHeight = IntBuffer.allocate(1);
//		GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_WIDTH, backingWidth);
//		GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_HEIGHT, backingHeight);
		
//		if (backingWidth.get(0) == 0
//			|| backingHeight.get(0) == 0) {
//			Log.e(TAG, "Get The render buffer width : " + backingWidth.get(0) + " render buffer height : " + backingHeight.get(0));
//		}
		
		mSizeInPixels = new GPUSize(getWidth(), getHeight());
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
//		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
//			@Override
//			public void run() {
				GPUImageContext.useImageProcessingContext();
				
				mDisplayProgram = GPUImageContext.sharedImageProcessingContexts()
												 .programForVertexShaderStringFragmentShaderString(GPUImageFilter.kGPUImageVertexShaderString,
														 										   GPUImageFilter.kGPUImagePassthroughFragmentShaderString);
				if (!mDisplayProgram.ismInitialized()) {
					initializeAttributes();
					
					if (!mDisplayProgram.link())
		            {
		                Log.e(TAG, "Program link log: " + mDisplayProgram.getmProgramLog());
		                Log.e(TAG, "Fragment shader compile log: " + mDisplayProgram.getmFragmentShaderLog());
		                Log.e(TAG, "Vertex shader compile log: "+ mDisplayProgram.getmVertexShaderLog());
		                mDisplayProgram = null;
		            }
				}
				
				mDisplayPositionAttribute = mDisplayProgram.attributeIndex("position");
				mDisplayTextureCoordinateAttribute = mDisplayProgram.attributeIndex("inputTextureCoordinate");
				mDisplayInputTextureUniform = mDisplayProgram.uniformIndex("inputImageTexture");
				
				GPUImageContext.setActiveShaderProgram(mDisplayProgram);

		        GLES20.glEnableVertexAttribArray(mDisplayPositionAttribute);
		        GLES20.glEnableVertexAttribArray(mDisplayTextureCoordinateAttribute);
		        
		        mFillMode = GPUImageFillModeType.kGPUImageFillModePreserveAspectRatio;
		        setBackgroundColor(1.0f, 0.0f, 0.0f, 1.0f);
		        
		        createDisplayFramebuffer();
//			}
//		});
		
		if (mHolderCallback != null) {
			mHolderCallback.surfaceCreated(holder);
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mHolderCallback != null) {
			mHolderCallback.surfaceChanged(holder, format, width, height);
		}
//		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
//			@Override
//			public void run() {
//				createDisplayFramebuffer();
//			}
//		});
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mHolderCallback != null) {
			mHolderCallback.surfaceDestroyed(holder);
		}
	}
	
	private void recalculateViewGeometry() {
		mImageVertices = GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.imageVertices);
	}
	
	@Override
	public void newFrameReadyAtTime(long frameTime, int textureIndex) {
//		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
//			@Override
//			public void run() {
				GPUImageContext.setActiveShaderProgram(mDisplayProgram);
				setDisplayFramebuffer();
				
				GLES20.glClearColor(mBackgroundColorRed, mBackgroundColorGreen, mBackgroundColorBlue, mBackgroundColorAlpha);
				GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		        
				GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInputFramebufferForDisplay.getTexture());
				GLES20.glUniform1i(mDisplayInputTextureUniform, 4);
		        
				GLES20.glVertexAttribPointer(mDisplayPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mImageVertices);
				GLES20.glVertexAttribPointer(mDisplayTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUImageFilter.textureCoordinatesForRotation(mInputRotation));
		        
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
				
				presentFramebuffer();
//			}
//		});
	}


	@Override
	public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer,
			int textureIndex) {
		mInputFramebufferForDisplay = newInputFramebuffer;
	}

	@Override
	public int nextAvailableTextureIndex() {
		return 0;
	}

	@Override
	public void setInputSize(final GPUSize newSize, int index) {
//		GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable(){
//			@Override
//			public void run() {
				if (mInputImageSize == null || !mInputImageSize.equals(newSize)) {
					mInputImageSize = newSize;
					recalculateViewGeometry();
				}
//			}
//		});
	}

	@Override
	public void setInputRotation(GPUImageRotationMode newInputRotation,
			int textureIndex) {
		
	}

	@Override
	public GPUSize maximumOutputSize() {
		return null;
	}

	@Override
	public void endProcessing() {
		
	}

	@Override
	public boolean shouldIgnoreUpdatesToThisTarget() {
		return false;
	}

	@Override
	public boolean enabled() {
		return false;
	}

	@Override
	public boolean wantsMonochromeInput() {
		return false;
	}

	@Override
	public void setCurrentlyReceivingMonochromeInput(boolean newValue) {
		
	}

	@Override
	public void setViewportRange(ViewportRange viewportRange) {

	}

	public static enum GPUImageFillModeType {
		 kGPUImageFillModeStretch,                       // Stretch to fill the full view, which may distort the image outside of its normal aspect ratio
		 kGPUImageFillModePreserveAspectRatio,           // Maintains the aspect ratio of the source image, adding bars of the specified background color
		 kGPUImageFillModePreserveAspectRatioAndFill     // Maintains the aspect ratio of the source image, zooming in on its center to fill the view
	}

	private SurfaceHolder mHolder = null;
	private GPUImageRotationMode mInputRotation = null;
	private boolean mEnabled = false;
	private GPUSize mSizeInPixels = null;
	private GLProgram mDisplayProgram = null;
	private int mDisplayPositionAttribute = -1;
	private int mDisplayTextureCoordinateAttribute = -1;
	private int mDisplayInputTextureUniform = -1;
    private GPUImageFillModeType mFillMode = null;
    private float mBackgroundColorRed = 0.0f;
    private float mBackgroundColorGreen = 0.0f;
    private float mBackgroundColorBlue = 0.0f;
    private float mBackgroundColorAlpha = 0.0f;
    private GPUSize mBoundsSizeAtFrameBufferEpoch = null;
    private SurfaceHolder.Callback mHolderCallback = null;
    private GPUSize mInputImageSize = null;
    private GPUImageFrameBuffer mInputFramebufferForDisplay;
    private FloatBuffer mImageVertices = null;
}
