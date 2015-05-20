package com.simpity.android.media.video;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.utils.DefaultMenu;

public class RtspEditActivity extends Activity {

	final static public String URL_DATA = "url";
	final static public String DESCRIPTION_DATA = "description";
	final static public String CATEGORY_DATA = "category";
	
	EditText mDescription, mURL;
	Spinner mCategory;
	
	private Ad mAdView;

	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		
		setContentView(Res.layout.rtsp_edit_dialog);
		
		initAdView();
		init();
		
		mDescription.setText(intent.getStringExtra(DESCRIPTION_DATA));
		mURL.setText(intent.getStringExtra(URL_DATA));
		String catToSet = intent.getStringExtra(CATEGORY_DATA);
		if (catToSet != null) {
			for (int i=0; i < mCategory.getCount(); i++) {
				Object item = mCategory.getItemAtPosition(i);
				if (item != null && catToSet.equalsIgnoreCase(item.toString())) {
					mCategory.setSelection(i);
					break;
				}
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
	
	//--------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		//SAVE
		int savedFocusedID = -1;
		
		String savedDescription = mDescription.getText().toString();
		if(mDescription.isFocused())
			savedFocusedID = mDescription.getId();
		
		String savedUrl = mURL.getText().toString();
		if(mURL.isFocused()){
			savedFocusedID = mURL.getId();
		}
		
		int savedCategoryPos = mCategory.getSelectedItemPosition();
		if(mCategory.isFocused()){
			savedFocusedID = mCategory.getId();
		}
		
		if(findViewById(Res.id.rtsp_dialog_ok_button).isFocused()){
			savedFocusedID = Res.id.rtsp_dialog_ok_button;
		}
		
		if(findViewById(Res.id.rtsp_dialog_cancel_button).isFocused()){
			savedFocusedID = Res.id.rtsp_dialog_cancel_button;
		}
		
		
		setContentView(Res.layout.rtsp_edit_dialog);
		initAdView();
		init();
		
		//RESTORE
		mDescription.setText(savedDescription);
		mURL.setText(savedUrl);
		mCategory.setSelection(savedCategoryPos);
		
		if(savedFocusedID!=-1){
			View v = findViewById(savedFocusedID);
			if(v != null){
				v.requestFocus();
			}
		}
	}
	
	//--------------------------------------------------------------------------
	private void init(){
		findViewById(Res.id.rtsp_dialog_ok_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = getIntent();
				intent.putExtra(DESCRIPTION_DATA, mDescription.getText().toString());
				intent.putExtra(URL_DATA, mURL.getText().toString());
				intent.putExtra(CATEGORY_DATA, mCategory.getSelectedItem().toString());
				setResult(StreamMediaActivity.SUCCESS, intent);
				finish();
			}
		});
		findViewById(Res.id.rtsp_dialog_cancel_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(StreamMediaActivity.CANCEL);
				finish();
			}
		});
		
		mDescription = (EditText) findViewById(Res.id.rtsp_dialog_description_editor);
		mURL = (EditText) findViewById(Res.id.rtsp_dialog_url_editor);
		mCategory = (Spinner)findViewById(Res.id.category);
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
	
	private void initAdView()
    {
    	if(mAdView != null)
    		mAdView.destroy();
 
        mAdView = new Ad(this);
    }
}
