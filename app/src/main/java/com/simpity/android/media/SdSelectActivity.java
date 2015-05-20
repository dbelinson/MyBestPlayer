package com.simpity.android.media;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.controls.SdCardBrowser;
import com.simpity.android.media.controls.SdCardBrowserBase;
import com.simpity.android.media.player.PlayerActivity;
import com.simpity.android.media.utils.DefaultMenu;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;

public class SdSelectActivity extends Activity implements
		SdCardBrowserBase.OnFolderChangeListener,
		SdCardBrowserBase.OnFileClickListener {

	private final static String CURRENT_PATH = "PATH";

	SdCardBrowser mBrowser = null;
	
	private Ad mAdView;
	
	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("SdSelectActivity", "onCreate()");

		super.onCreate(savedInstanceState);

        setContentView(Res.layout.sdcard_browser);
        
        mAdView = new Ad(this);

        if (getWindowManager().getDefaultDisplay().getOrientation() == 0) {
        	if(mAdView != null)
				mAdView.setVisibility(View.VISIBLE);
        	//Ad.Visible(this);
		} else {
			if(mAdView != null)
				mAdView.setVisibility(View.INVISIBLE);
			//Ad.Gone(this);
		}
        mBrowser = (SdCardBrowser)findViewById(Res.id.sdcard_browser);
        mBrowser.setOnFolderChangeListener(this);
        mBrowser.setOnFileClickListener(this);
        if(mBrowser.getExtensions() == null)
        	((CheckBox)findViewById(Res.id.showAllFiles)).setChecked(true);
        else
        	((CheckBox)findViewById(Res.id.showAllFiles)).setChecked(false);

        findViewById(Res.id.showAllFiles).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(((CheckBox)v).isChecked())
				{
					mBrowser.setExtensions(null);
				}
				else
				{
					mBrowser.setExtensions(SdCardBrowser.DEFAULT_EXTENSIONS);
				}
			}
		});


        SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        String path = prefs.getString(CURRENT_PATH, null);
        if(path == null) {
        	path = mBrowser.getCurrentFolder();
        }
        else {
        	File cur_path = new File(path);
        	if(cur_path.exists())
        		mBrowser.setCurrentFolder(path);
        	else
        		path = mBrowser.getCurrentFolder();
        }
        setTitleFolder(path);
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		DefaultMenu.create(menu);
		return true;
	}

	//--------------------------------------------------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultMenu.onItemSelected(this, item);
	}
	
	@Override
    protected void onDestroy() {

		if(mAdView != null)
    		mAdView.destroy();
		
    	super.onDestroy();
    }
	
	//--------------------------------------------------------------------------
	@Override
	public void onChange(File folder) {
		String path = folder.getAbsolutePath();

		setTitleFolder(path);

		SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
		editor.clear();
		editor.putString(CURRENT_PATH, path);
		editor.commit();
	}

	//--------------------------------------------------------------------------
	private void setTitleFolder(String path) {
		StringBuilder title = new StringBuilder();
		title.append(getResources().getString(Res.string.sdcard_select_title));
		title.append(" - ");
		title.append(path);
		setTitle(title.toString());
	}

	//--------------------------------------------------------------------------
	@Override
	public void onClick(File file) {
		/*
		FileInputStream stream;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Message.show(this, Res.string.error, e.getMessage());
			return;
		}

		byte[] hdr = new byte[8];
		try {
			stream.read(hdr);
		} catch (IOException e) {
			Message.show(this, Res.string.error, e.getMessage());
			return;
		}
		finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}

		if(hdr[4] != 'f' || hdr[5] != 't' || hdr[6] != 'y' || hdr[7] != 'p') {
			Message.show(this, Res.string.error, "The file \"" + file.getAbsolutePath() +
					"\" is not mp4 or 3gp");
			return;
		}
		*/

		/*Intent intent = new Intent(this, MediaPlayerActivity.class);
		intent.putExtra(MediaPlayerActivity.URL_DATA, file.getAbsolutePath());
		intent.putExtra(MediaPlayerActivity.SESSION_ID,
				Integer.toHexString((int)(Math.random() * (double)0x7FFFFFFF)));*/
		
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.URL_DATA, file.getAbsolutePath());
		intent.putExtra(PlayerActivity.SESSION_ID,
				Integer.toHexString((int)(Math.random() * (double)0x7FFFFFFF)));
		
		startActivity(intent);
	}
	
	private void upgrateAdView(boolean do_create)
	{
    	if(mAdView != null)
    		mAdView.destroy();
 
    	if(do_create)
    	{
	        mAdView = new Ad(this);
    	}
	}
}
