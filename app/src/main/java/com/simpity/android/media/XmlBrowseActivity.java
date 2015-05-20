package com.simpity.android.media;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.simpity.android.media.Res;
import com.simpity.android.media.controls.SdCardBrowserBase;
import com.simpity.android.media.controls.SdCardXMLBrowser;

public class XmlBrowseActivity extends Activity implements
		SdCardBrowserBase.OnFolderChangeListener,
		SdCardBrowserBase.OnFileClickListener  {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(Res.layout.xml_browser);

        SdCardXMLBrowser browser = (SdCardXMLBrowser)findViewById(Res.id.sdcard_xml_browser);
        browser.setOnFolderChangeListener(this);
        browser.setOnFileClickListener(this);

        setResult(StreamMediaActivity.CANCEL);
	}

	@Override
	public void onChange(File folder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(File file) {
		Intent intent = new Intent();
		intent.putExtra(XmlFileSelectActivity.PATH, file.getAbsolutePath());

		setResult(StreamMediaActivity.SUCCESS, intent);
		finish();
	}
}
