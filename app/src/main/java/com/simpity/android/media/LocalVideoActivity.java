package com.simpity.android.media;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Video;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.controls.SdCardBrowser;
import com.simpity.android.media.controls.SdCardBrowserBase;
import com.simpity.android.media.player.PlayerActivity;
import com.simpity.android.media.utils.Command;
import com.simpity.android.media.utils.DefaultMenu;
import com.simpity.android.media.utils.VideoGalleryAdapter;
//import android.view.View.OnClickListener;
//import android.widget.CheckBox;

public class LocalVideoActivity extends FourTabActivity implements
		SdCardBrowserBase.OnFolderChangeListener,
		SdCardBrowserBase.OnFileClickListener {

	private final static String CURRENT_PATH = "PATH";
	final static public String GALLERY_STATE = "GALLERY_STATE";
	final static public String CARD_BROWSER_STATE = "CARD_BROWSER_STATE";
	
	private SdCardBrowser mBrowser;
	private ListView mGallery;
	private VideoGalleryAdapter mGalleryAdapter;
	
	private Ad mAdView;
	
	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(Res.layout.local_video_select);
		
		mAdView = new Ad(this);
		
		initTabs(savedInstanceState, null);
        
		mBrowser = (SdCardBrowser)findViewById(Res.id.LocalVideoBrowser);
		mBrowser.setOnFolderChangeListener(this);
		mBrowser.setOnFileClickListener(this);
		
		String[] toSetFilter = SdCardBrowser.DEFAULT_EXTENSIONS;
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(Res.string.pref_local_media_show_all_files_key), false)){
			toSetFilter = null;
		}
		mBrowser.setExtensions(toSetFilter);
		/*
        CheckBox checkBox = (CheckBox) findViewById(Res.id.LocalVideoShowAllFiles);
        checkBox.setChecked(mBrowser.getExtensions() == null);

        checkBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(((CheckBox)v).isChecked()) {					
					mBrowser.setExtensions(null);
				} else {
					mBrowser.setExtensions(SdCardBrowser.DEFAULT_EXTENSIONS);
				}
			}
		});
        */

        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        String path = prefs.getString(CURRENT_PATH, null);
        if (path == null) {
        	path = mBrowser.getCurrentFolder();
        } else {
        	File cur_path = new File(path);
        	if (cur_path.exists())
        		mBrowser.setCurrentFolder(path);
        	else
        		path = mBrowser.getCurrentFolder();
        }
        
        mGalleryAdapter = new VideoGalleryAdapter(this);
        mGallery = (ListView) findViewById(Res.id.LocalVideoGallery);
        mGallery.setAdapter(mGalleryAdapter);
        mGallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
				Cursor cursor = getContentResolver().query(
						Video.Media.EXTERNAL_CONTENT_URI, 
						new String[] { Video.Media.DATA }, 
						Video.Media._ID + '=' + id, null, null);
				
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						try {
							int data_index = cursor.getColumnIndexOrThrow(Video.Media.DATA);
							String path = cursor.getString(data_index);
							
							startPlayer(path);
							
						} catch (IllegalArgumentException ex) {
							ex.printStackTrace();
						}
					}
					
					cursor.close();
				}
			}
		});
	
        if (savedInstanceState != null) {
        	Parcelable savedListState = savedInstanceState.getParcelable(GALLERY_STATE);
			if(savedListState != null){
				mGallery.onRestoreInstanceState(savedListState);
			}
			savedListState = savedInstanceState.getParcelable(CARD_BROWSER_STATE);
			if(savedListState != null){
				mBrowser.onRestoreInstanceState(savedListState);
			}
		}
	}
	
	@Override
    protected void onDestroy() {
		
		if (mAdView != null) {
			mAdView.destroy();
		}

    	super.onDestroy();
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(GALLERY_STATE, mGallery.onSaveInstanceState());
		outState.putParcelable(CARD_BROWSER_STATE, mBrowser.onSaveInstanceState());
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == Command.SETTINGS)
		{
			String[] toSetFilter = SdCardBrowser.DEFAULT_EXTENSIONS;
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(Res.string.pref_local_media_show_all_files_key), false)){
				toSetFilter = null;
			}
			mBrowser.setExtensions(toSetFilter);
		}
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryPageId() {
		return Res.id.LocalVideoBrowserPage;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getHistoryTabId() {
		return Res.id.LocalVideoBrowserTab;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewPageId() {
		return Res.id.LocalVideoGallery;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getNewTabId() {
		return Res.id.LocalVideoGalleryTab;
	}
	
	//--------------------------------------------------------------------------
	@Override
	public void onChange(File folder) {
		String path = folder.getAbsolutePath();

		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);		
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear();
		editor.putString(CURRENT_PATH, path);
		editor.commit();
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

		startPlayer(file.getAbsolutePath());
	}
	
	//--------------------------------------------------------------------------
	private void startPlayer(String path) {
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.URL_DATA, path);
		intent.putExtra(PlayerActivity.SESSION_ID,
				Integer.toHexString((int)(Math.random() * (double)0x7FFFFFFF)));

		startActivity(intent);
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksPageId() {
		return -1;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getAllLinksTabId() {
		return -1;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getFavoritesPageId() {
		return -1;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getFavoritesTabId() {
		return -1;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchTabId() {
		// TODO Auto-generated method stub
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getSearchViewId() {
		// TODO Auto-generated method stub
		return 0;
	}
}
