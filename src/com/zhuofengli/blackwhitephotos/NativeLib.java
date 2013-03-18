package com.zhuofengli.blackwhitephotos;

public class NativeLib {
	static {
		System.loadLibrary("ndk_blackwhitephotos");
	}

	public native int processAndSaveFile(String srcPath, int srcType, int srcWidth,
			int srcHeight, int orientation, int capSize, String destPath, int quality,
			int thumbCapSize, String thumbPath);

}
