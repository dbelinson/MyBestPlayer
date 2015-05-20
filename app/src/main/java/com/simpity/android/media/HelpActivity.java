package com.simpity.android.media;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.simpity.android.media.Res;
import com.simpity.android.media.dialogs.WhatsNewDlg;

public class HelpActivity extends Activity {

	//--------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		setContentView(Res.layout.help_view);
		setTitle(Res.string.help);

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

		findViewById(Res.id.supported_formats_link).setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					//((View)v.getParent()).setBackgroundColor(Color.parseColor("#FFFFFFFF"));
					v.setBackgroundColor(Color.parseColor("#FFFFFF"));
				} else {
					//((View)v.getParent()).setBackgroundColor(Color.parseColor("#FFFFB400"));
					v.setBackgroundColor(Color.parseColor("#FF8C00"));
				}
			}
		});
		findViewById(Res.id.supported_formats_link).setOnTouchListener(mTouchListener);
		findViewById(Res.id.supported_formats_link).setOnClickListener(linkClickListener);

		//findViewById(Res.id.about_psa_inc).setOnClickListener(linkClickListener);
		//findViewById(Res.id.testPanel).setOnClickListener(linkClickListener);
		findViewById(Res.id.pnlToClick1).setOnClickListener(linkClickListener1);
		findViewById(Res.id.pnlToClick2).setOnClickListener(linkClickListener2);
		findViewById(Res.id.pnlToClick3).setOnClickListener(linkClickListener3);
		findViewById(Res.id.pnlToClick4).setOnClickListener(linkClickListener4);
		
		View v = findViewById(Res.id.whats_new_link);
		v.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					v.setBackgroundColor(Color.parseColor("#FFFFFF"));
				} else {
					v.setBackgroundColor(Color.parseColor("#FF8C00"));
				}
			}
		});
		v.setOnTouchListener(mTouchListener);
		v.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				WhatsNewDlg.show(HelpActivity.this, Res.raw.whats_new, Res.string.whats_new_title, true, null);
			}
		});
    };

    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				v.setBackgroundColor(Color.parseColor("#FF8C00"));
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				v.setBackgroundColor(Color.parseColor("#FFFFFF"));
				break;
			}
			return false;
		}
    };

    View.OnClickListener linkClickListener = new View.OnClickListener() {

    	@Override
		public void onClick(View v) {
			Intent launcher = new Intent("android.intent.action.VIEW",
					Uri.parse("http://developer.android.com/guide/appendix/media-formats.html"));
	        startActivity(launcher);
		}
    };

    View.OnClickListener linkClickListener1 = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
    		switch (findViewById(Res.id.pnlToCollapse1).getVisibility()) {
			case View.VISIBLE:
				findViewById(Res.id.pnlToCollapse1).setVisibility(View.GONE);
				((ImageView)findViewById(Res.id.img1)).setImageResource(Res.drawable.expander_ic_maximized);
				break;
			case View.GONE:
				findViewById(Res.id.pnlToCollapse1).setVisibility(View.VISIBLE);
				((ImageView)findViewById(Res.id.img1)).setImageResource(Res.drawable.expander_ic_minimized);
				break;
			default:
				break;
			}
		}
    };
    View.OnClickListener linkClickListener2 = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
    		switch (findViewById(Res.id.pnlToCollapse2).getVisibility()) {
			case View.VISIBLE:
				findViewById(Res.id.pnlToCollapse2).setVisibility(View.GONE);
				((ImageView)findViewById(Res.id.img2)).setImageResource(Res.drawable.expander_ic_maximized);
				break;
			case View.GONE:
				findViewById(Res.id.pnlToCollapse2).setVisibility(View.VISIBLE);
				((ImageView)findViewById(Res.id.img2)).setImageResource(Res.drawable.expander_ic_minimized);
				break;
			default:
				break;
			}
		}
    };
    View.OnClickListener linkClickListener3 = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
    		switch (findViewById(Res.id.pnlToCollapse3).getVisibility()) {
			case View.VISIBLE:
				findViewById(Res.id.pnlToCollapse3).setVisibility(View.GONE);
				((ImageView)findViewById(Res.id.img3)).setImageResource(Res.drawable.expander_ic_maximized);
				break;
			case View.GONE:
				findViewById(Res.id.pnlToCollapse3).setVisibility(View.VISIBLE);
				((ImageView)findViewById(Res.id.img3)).setImageResource(Res.drawable.expander_ic_minimized);
				break;
			default:
				break;
			}
		}
    };
    View.OnClickListener linkClickListener4 = new View.OnClickListener() {
    	@Override
		public void onClick(View v) {
    		switch (findViewById(Res.id.pnlToCollapse4).getVisibility()) {
			case View.VISIBLE:
				findViewById(Res.id.pnlToCollapse4).setVisibility(View.GONE);
				((ImageView)findViewById(Res.id.img4)).setImageResource(Res.drawable.expander_ic_maximized);
				break;
			case View.GONE:
				findViewById(Res.id.pnlToCollapse4).setVisibility(View.VISIBLE);
				((ImageView)findViewById(Res.id.img4)).setImageResource(Res.drawable.expander_ic_minimized);
				break;
			default:
				break;
			}
		}
    };
}
