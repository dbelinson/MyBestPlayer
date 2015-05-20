package com.simpity.android.media.controls;

import android.content.Context;
import android.util.AttributeSet;

import com.simpity.android.media.Res;

public class SdCardBrowser extends SdCardBrowserBase {

	public final static String[] DEFAULT_EXTENSIONS = new String[] {".3gp", ".mp4", ".wmv"};
	private static String[] exts = new String[] {".3gp", ".mp4", ".wmv"};

	public SdCardBrowser(Context context) {
		super(context);
	}

	public SdCardBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SdCardBrowser(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public String[] getExtensions() {
		return exts;
	}

	public void setExtensions(String[] value)
	{
		exts = value;
		super.reInit();
	}

	protected int getIconResource(String filename) {
		int pos = filename.lastIndexOf('.');
		
		if (pos > 0) {
			String ext = filename.substring(pos+1).toLowerCase();
			
			if (ext.equals("3gp") || ext.equals("mp4") || ext.equals("wmv") || ext.equals("avi"))
				return Res.drawable.icon_video_file;
		}
		
		return Res.drawable.icon_file;
	}
}
