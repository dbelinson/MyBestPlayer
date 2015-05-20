package com.simpity.android.media.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.simpity.android.media.Ad;
import com.simpity.android.media.Res;
import com.simpity.android.media.StreamMediaActivity;
import com.simpity.android.media.utils.DefaultMenu;

public class JpegEditActivity extends Activity {

	final static public String URL_DATA = "url";
	final static public String DESCRIPTION_DATA = "description";
	final static public String REFRESH_DATA = "refresh";
	
	private Ad mAdView;

	EditText mDescrEditor, mUrlEditor;
	Spinner mRefreshTime;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(Res.layout.jpeg_edit_dialog);
        
        mAdView = new Ad(this);

        init();
        
		Intent intent = getIntent();

		mDescrEditor.setText(intent.getStringExtra(DESCRIPTION_DATA));
		mUrlEditor.setText(intent.getStringExtra(URL_DATA));
		
        int refresh = Integer.parseInt(intent.getStringExtra(REFRESH_DATA));
        String[] refreshTimes = getResources().getStringArray(Res.arrays.camera_refresh_time_values);
        for(int i=0; i < refreshTimes.length-1; i++) {
        	if(refresh <= Integer.parseInt(refreshTimes[i])) {
        		mRefreshTime.setSelection(i);
        		return;
        	}
        }

        mRefreshTime.setSelection(refreshTimes.length - 1);
	}
    
    //--------------------------------------------------------------------------
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	
    	//SAVE
		int savedFocusedID = -1;
		
		String savedDescription = mDescrEditor.getText().toString();
		if(mDescrEditor.isFocused())
			savedFocusedID = mDescrEditor.getId();
		
		String savedUrl = mUrlEditor.getText().toString();
		if(mUrlEditor.isFocused()){
			savedFocusedID = mUrlEditor.getId();
		}
		
		int savedRefreshTimePos = mRefreshTime.getSelectedItemPosition();
		if(mRefreshTime.isFocused()){
			savedFocusedID = mRefreshTime.getId();
		}
		
		if(findViewById(Res.id.jpeg_dialog_ok_button).isFocused()){
			savedFocusedID = Res.id.jpeg_dialog_ok_button;
		}
		
		if(findViewById(Res.id.jpeg_dialog_cancel_button).isFocused()){
			savedFocusedID = Res.id.jpeg_dialog_cancel_button;
		}
		
		
		setContentView(Res.layout.jpeg_edit_dialog);
		init();
		
		//RESTORE
		mDescrEditor.setText(savedDescription);
		mUrlEditor.setText(savedUrl);
		mRefreshTime.setSelection(savedRefreshTimePos);
		
		if(savedFocusedID!=-1){
			View v = findViewById(savedFocusedID);
			if(v != null){
				v.requestFocus();
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
    private void init(){
    	findViewById(Res.id.jpeg_dialog_ok_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();

				EditText editor = (EditText)findViewById(Res.id.jpeg_dialog_description_editor);
				intent.putExtra(DESCRIPTION_DATA, editor.getText().toString());

				editor = (EditText)findViewById(Res.id.jpeg_dialog_url_editor);
				intent.putExtra(URL_DATA, editor.getText().toString());

				//editor = (EditText)findViewById(Res.id.jpeg_dialog_refresh_editor);
				//intent.putExtra(REFRESH_DATA, editor.getText().toString());
				Spinner spinner = (Spinner)findViewById(Res.id.jpeg_dialog_refresh_spinner);
				int pos = spinner.getSelectedItemPosition();
				int refresh = pos != AdapterView.INVALID_POSITION ?
						Integer.parseInt(getResources().getStringArray(Res.arrays.camera_refresh_time_values)[pos]) : 10;

				intent.putExtra(REFRESH_DATA, Integer.toString(refresh));

			 	setResult(StreamMediaActivity.SUCCESS, intent);
			 	finish();
			}
		});
		findViewById(Res.id.jpeg_dialog_cancel_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			 	setResult(StreamMediaActivity.CANCEL);
			 	finish();
			}
		});
		mDescrEditor = (EditText)findViewById(Res.id.jpeg_dialog_description_editor);
		mUrlEditor = (EditText)findViewById(Res.id.jpeg_dialog_url_editor);
		mRefreshTime = (Spinner)findViewById(Res.id.jpeg_dialog_refresh_spinner);
		//ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, Res.layout.spinner_text_view, JpegCameraActivity.REFRESH_TIMES);
        //mRefreshTime.setAdapter(adapter);
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
}
