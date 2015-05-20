package com.simpity.android.media.utils;

import android.os.Build;

public class AndroidVersionHelper {

	static int getVersion(){
		return Build.VERSION.SDK_INT;
	}
}
