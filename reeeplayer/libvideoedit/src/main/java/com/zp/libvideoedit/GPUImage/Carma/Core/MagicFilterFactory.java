package com.zp.libvideoedit.GPUImage.Carma.Core;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

public class MagicFilterFactory {
	
	private static VNIFilterType filterType = VNIFilterType.NONE;
	
	public static GPUImageFilter initFilters(VNIFilterType type){
		return new GPUImageFilter();
	}
	
	public VNIFilterType getCurrentFilterType(){
		return filterType;
	}
}
