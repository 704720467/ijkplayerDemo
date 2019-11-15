package com.zp.libvideoedit.GPUImage.Core;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class GLProgram {
	
	private static final String TAG = GLProgram.class.getSimpleName();
	
	public GLProgram initWithVertexVShaderStringFShaderString(String vShaderString, String fShaderString) {
		mInitialized = false;
		
		mAtrributes = new ArrayList<String>();
		mUniforms = new ArrayList<Integer>();
		mProgram = GLES20.glCreateProgram();

		mVertShader = compileShader(GLES20.GL_VERTEX_SHADER, vShaderString);
		if (mVertShader == -1) {
			Log.e(TAG, "Failed to compile vertex shader");
		}
		
		mFragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fShaderString);
		if (mFragShader == -1) {
			Log.e(TAG, "Failed to compile fragment shader");
		}
		
		GLES20.glAttachShader(mProgram, mVertShader);
		GLES20.glAttachShader(mProgram, mFragShader);
		
		return this;
	}
	
	public GLProgram initWithVertexVShaderStringFShaderFilename(String vShaderString, String fShaderFilename) {
//		String fShaderString = AndroidResourceManager.getAndroidResourceManager().readStringFromAssets(fShaderFilename);
//		if (fShaderString == null) {
//			Log.e(TAG, "ERROR the file name : " + fShaderFilename);
//		}
//		return initWithVertexVShaderStringFShaderString(vShaderString, fShaderString);
		return null;
	}
	
	public GLProgram initWithVertexVShaderFilenameFShaderFilename(String vShaderFilename, String fShaderFilename) {
//		String vShaderString = AndroidResourceManager.getAndroidResourceManager().readStringFromAssets(vShaderFilename);
//		if (vShaderString == null) {
//			Log.e(TAG, "ERROR the file name : " + vShaderFilename);
//		}
//
//		String fShaderString = AndroidResourceManager.getAndroidResourceManager().readStringFromAssets(fShaderFilename);
//		if (fShaderString == null) {
//			Log.e(TAG, "ERROR the file name : " + fShaderFilename);
//		}
//		return initWithVertexVShaderStringFShaderString(vShaderString, fShaderString);
		return null;
	}
	
	public boolean ismInitialized() {
		return mInitialized;
	}

	public String getmVertexShaderLog() {
		return mVertexShaderLog;
	}

	public String getmFragmentShaderLog() {
		return mFragmentShaderLog;
	}

	public String getmProgramLog() {
		return mProgramLog;
	}

	public void addAttribute(String attributeName) {
		int index = 0;
		for (; index < mAtrributes.size(); ++ index) {
			if (attributeName.equals(mAtrributes.get(index))) {
				return ;
			}
		}
		
		mAtrributes.add(attributeName);
		GLES20.glBindAttribLocation(mProgram,
				index,
                attributeName);
	}
	
	public int attributeIndex(String attributeName) {
		int index = -1;
		for (int i = 0; i < mAtrributes.size(); ++i) {
			if (mAtrributes.get(i).equals(attributeName)) {
				index = i;
			}
		}
		return index;
	}
	
	public int uniformIndex(String uniformName) {
		return GLES20.glGetUniformLocation(mProgram, uniformName);
	}
	
	public boolean link() {
		IntBuffer success = IntBuffer.allocate(1);
	    GLES20.glLinkProgram(mProgram);
		
	    GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, success);
	    if (success.get(0) == GLES20.GL_FALSE){
			mProgramLog = GLES20.glGetProgramInfoLog(mProgram);
			return false;

		}

	    if (mVertShader > 0) {
	        GLES20.glDeleteShader(mVertShader);
	        mVertShader = 0;
	    }
	    
	    if (mFragShader > 0)
	    {
	        GLES20.glDeleteShader(mFragShader);
	        mFragShader = 0;
	    }
	    
	    mInitialized = true;
	    
		return true;
	}
	
	public void use() {
		GLES20.glUseProgram(mProgram);
	}
	
	public void validate() {
		IntBuffer success = IntBuffer.allocate(1);
        GLES20.glValidateProgram(mProgram);
        GLES20.glGetProgramiv(mProgram, GLES20.GL_VALIDATE_STATUS, success);
        if (success.get(0) == GLES20.GL_FALSE) {
        	mProgramLog = GLES20.glGetProgramInfoLog(mProgram);
        }
	}
	
	private int compileShader(int shaderType, String shaderSource) {
		if (shaderSource == null) {
			Log.e(TAG, "Error null shader source!");;
		}
		
		int handle = GLES20.glCreateShader(shaderType);

		if (handle == GLES20.GL_FALSE) {
			Log.e(TAG, "Error creating shader!");
		}
		
		// set and compile the shader
		GLES20.glShaderSource(handle, shaderSource);
		GLES20.glCompileShader(handle);

		// check if the compilation was OK
		int[] compileStatus = new int[1];
		GLES20.glGetShaderiv(handle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

		if (compileStatus[0] == 0) {
			String error = GLES20.glGetShaderInfoLog(handle);
			GLES20.glDeleteShader(handle);
			
			if (shaderType == GLES20.GL_VERTEX_SHADER) {
				mVertexShaderLog = error;
			}else {
				mFragmentShaderLog = error;
			}
		}
		
		return handle;
	}

	public int getmProgram() {
		return mProgram;
	}

	private List<String> mAtrributes = null;
	private List<Integer> mUniforms = null;
	private int mProgram = -1;
	private int mVertShader = -1; 
	private int mFragShader = -1;
	private boolean mInitialized = false;
	private String mVertexShaderLog = null;
	private String mFragmentShaderLog = null;
	private String mProgramLog = null;
}
