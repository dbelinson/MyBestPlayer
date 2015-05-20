package com.simpity.android.media.video;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;

public class StreamVideoActivity extends Activity {

	//private Tabs mTabs;
	
	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		/*setContentView(R.layout.stream_video_select);
		
		TabButton[] buttons = new TabButton[3];
		buttons[0] = new TabButton(R.drawable.icon_favorites, R.string.favorites, R.id.VideoFavoritesScroll);
		buttons[1] = new TabButton(R.drawable.icon_folder, R.string.history, R.id.VideoHistoryScroll);
		buttons[2] = new TabButton(R.drawable.icon_edit, R.string.new_record, R.id.VideoNewScroll);
		
		mTabs = (Tabs) findViewById(R.id.VideoPageTabs);
		mTabs.setPages(findViewById(R.id.VideoPageContainer), buttons);*/
		
		// TODO
	}
}
