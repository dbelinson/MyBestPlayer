package com.simpity.android.media;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public final class Ad {
	
	private AdView mAdView;
	private String AD_UNIT_ID = "a14b3b1e046767e";
	
	public Ad(Activity activity)
	{
		mAdView = new AdView(activity, AdSize.BANNER, AD_UNIT_ID);
		LinearLayout layout = (LinearLayout)activity.findViewById(R.id.AdLayout);
		layout.addView(mAdView);
		
		// Initiate a generic request to load it with an ad
		mAdView.loadAd(new AdRequest());
	}
	
	public void destroy()
	{
		mAdView.destroy();
	}
	
	public void setVisibility(int visibility)
	{
		if(mAdView != null)
			mAdView.setVisibility(visibility);
	}

	public final static void Gone(Activity activity) {
		/*
		View view = activity.findViewById(Res.id.ad);
		if (view != null)
			view.setVisibility(View.GONE);
			*/
	}

	public final static void Visible(Activity activity) {
		/*
		View view = activity.findViewById(Res.id.ad);
		if (view != null)
			view.setVisibility(View.VISIBLE);
			*/
	}
}