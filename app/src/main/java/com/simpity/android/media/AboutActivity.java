package com.simpity.android.media;

//import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class AboutActivity extends Activity {

    //--------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		setContentView(Res.layout.about_view);
		setTitle(Res.string.about);
		String version = getString(Res.string.version );
		try {
			String verName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
			if(verName != null)
				version += " " +  verName;
			else
				version += " " +  getString(Res.string.versionName);
		} catch (NameNotFoundException e) {
			version += " 1.0";
			e.printStackTrace();
		}
		
		((TextView)findViewById(Res.id.version)).setText(version);
		findViewById(Res.id.about_psa_link).setOnClickListener(linkClickListener);
		findViewById(Res.id.about_psa_inc).setOnClickListener(linkClickListener);
    }

    View.OnClickListener linkClickListener = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
			Intent launcher = new Intent("android.intent.action.VIEW",
					Uri.parse("http://www.simpity.by/"));
	        startActivity(launcher);
		}
    };

    public void onStart(){
		//FlurryAgent.onStartSession(this, StreamMediaActivity.FLURRY_AGENT_APP_CODE);
		//FlurryAgent.onEvent("AboutActivity");
	   super.onStart();
	}

	public void onStop(){
	   //FlurryAgent.onEndSession(this);
	   super.onStop();
	}
}
