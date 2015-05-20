package com.simpity.android.media.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlayerLayout extends ViewGroup {

	private VideoAspectRatio mAspectRatio = VideoAspectRatio.Original;
	private PlayerActivity mActivity;
	
	private boolean mLockedControls;
	
	//--------------------------------------------------------------------------
	public PlayerLayout(Context context) {
		super(context);
		init(context);
	}

	//--------------------------------------------------------------------------
	public PlayerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	//--------------------------------------------------------------------------
	public PlayerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	//--------------------------------------------------------------------------
	private void init(Context context) {
		if (context instanceof PlayerActivity) {
			mActivity = (PlayerActivity)context;
		}
		mLockedControls = false;
	}
	
	//--------------------------------------------------------------------------
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int count = super.getChildCount();
		int width = right - left, height = bottom - top;
		int title_height = 0, controls_height = 0;
		SurfaceView video_view = null;

		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);

			if (child instanceof SurfaceView) {
		
				video_view = (SurfaceView)child;
				
			} else if (child.getVisibility() != GONE) {
				
				//int h = child.getMeasuredHeight();
				int h = child.getHeight();
				int y;
				
				if (child instanceof TextView) {
					y = 0;
					title_height = h;
				} else {
					y = height - h;
					controls_height = h;
				}
				
				child.layout(0, y, width, y+h);
			}
		}
		
		if (video_view != null) {
			int w = mAspectRatio.Width, h = mAspectRatio.Height;
			int x, y, video_width, video_height;
			MediaPlayer player = mActivity.getPlayer();
			
			if (player != null) {
				switch(mAspectRatio) {
				case Original:
					video_width  = 0;
					video_height = 0;
					
					try{
						video_width = player.getVideoWidth();
						video_height = player.getVideoHeight();
					}catch (IllegalStateException e) {
						e.printStackTrace();
						Log.e("PlayerLayout", "Can't determine video width and height, set OnVideoSizeChangedListener");
						player.setOnVideoSizeChangedListener(videoSizeResolvedListener);
					}
					
					if (video_width > 0 && video_height > 0) {
						w = video_width;
						h = video_height;
					}else{
						player.setOnVideoSizeChangedListener(videoSizeResolvedListener);
					}
					break;
					
				/*case ByWidth:
				case ByWidth:
					video_width  = player.getVideoWidth();
					video_height = player.getVideoHeight();
					if (video_width > 0 && video_height > 0) {
						
					}
					break;
					*/
				}
			}
			
			video_width  = width;
			video_height = video_width * h / w;
			
			if (width < height) {
				
				x = 0;
				y = (height + title_height - controls_height - video_height) / 2;
				
			} else {
				
				if (mLockedControls) {
					
					int visible_height = height - title_height - controls_height;
					
					if (video_height > height) {
						
						video_height = visible_height; 
						video_width  = video_height * w / h;
						y = controls_height;
						x = (width - video_width) / 2; 
						
					} else {
						
						x = 0;
						y = (height + title_height - controls_height - video_height) / 2;
					}

				} else {
					
					if (video_height > height) {
						
						video_height = height; 
						video_width  = video_height * w / h;
						y = 0;
						x = (width - video_width) / 2; 
						
					} else {
						
						x = 0;
						y = (height - video_height) / 2;
					}
				}
			}
			
			video_view.layout(x, y, x+video_width, y+video_height);
		}
	}
	
	MediaPlayer.OnVideoSizeChangedListener videoSizeResolvedListener = new MediaPlayer.OnVideoSizeChangedListener() {
		@Override
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
			mp.setOnVideoSizeChangedListener(null);

            //Change after porting to Android Studio ===========================
			layout(getLeft(), getTop(), getRight(), getBottom());
            //onLayout(false, getLeft(), getTop(), getRight(), getBottom());
            //==================================================================
		}
	};

	//--------------------------------------------------------------------------
	public void setAspectRatio(VideoAspectRatio aspect_ratio) {
		mAspectRatio = aspect_ratio;
	}

	//--------------------------------------------------------------------------
	public VideoAspectRatio getAspectRatio() {
		return mAspectRatio;
	}
}
