package com.zp.libvideoedit.GPUImage.Core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AndroidResourceManager {
	
	private static Context mContext = null;
	
	private AndroidResourceManager() {
		
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public String readStringFromAssets(String fileName) {
		if (mContext != null) {
			try {
				InputStreamReader inputReader = new InputStreamReader(mContext
						.getResources().getAssets().open(fileName));
				BufferedReader bufReader = new BufferedReader(inputReader);
				String line = "";
				String Result = "";
				while ((line = bufReader.readLine()) != null)
					Result += line;
				return Result;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public Bitmap readBitmapFromAssets(String fileName) {
		InputStream bitmapStream = null;
		try {
			bitmapStream = mContext.getAssets().open(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream);
		return bitmap;
	}
	
	public static synchronized AndroidResourceManager getAndroidResourceManager(Context context) {
		if (mAndroidResourceManager == null) {
			mAndroidResourceManager = new AndroidResourceManager();
			mContext = context;
		}
		return mAndroidResourceManager;
	}
	
	private static AndroidResourceManager mAndroidResourceManager = null;
}
