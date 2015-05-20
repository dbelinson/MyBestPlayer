package com.simpity.android.media.controls;

import android.content.Context;
import android.util.AttributeSet;

import com.simpity.android.media.Res;

public class SdCardXMLBrowser extends SdCardBrowserBase {

	public SdCardXMLBrowser(Context context) {
		super(context);
	}

	public SdCardXMLBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SdCardXMLBrowser(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	protected String[] getExtensions() {
		return new String[] {".xml"};
	}

	protected int getIconResource(String filename) {
		return Res.drawable.xml_icon;
	}
}
