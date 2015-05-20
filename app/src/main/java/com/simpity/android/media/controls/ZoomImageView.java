package com.simpity.android.media.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {

	public final static int ZOOM_IN = 1;
	public final static int ZOOM_OUT = 2;

	private final float zoom_step = 0.3f;

	float scaleX = 1;
	float scaleY = 1;

	public ZoomImageView(Context context)
	{
		super(context);
		setFocusable(true);
		setScrollContainer(true);
	}

	public ZoomImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setScrollContainer(true);
		setFocusable(true);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.scale(scaleX, scaleY);
		super.onDraw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if(changed){
			scaleX = 1;
			scaleY = 1;
			scrollTo(0,0);
		}
		super.onLayout(changed, left, top, right, bottom);
	};

	public void changeZoom(int zoomType){
		int ScrollX = getScrollX();
		int ScrollY = getScrollY();
		switch (zoomType) {
		case ZOOM_IN:
			scaleX += zoom_step;
			scaleY += zoom_step;
			ScrollX += getWidth()*zoom_step/2;
			ScrollY += getHeight()*zoom_step/2;
			break;
		case ZOOM_OUT:
			scaleX -= zoom_step;
			scaleY -= zoom_step;
			ScrollX -= getWidth()*zoom_step/2;
			ScrollY -= getHeight()*zoom_step/2;
			break;
		default:
			break;
		}
		invalidate();
		scrollTo(ScrollX, ScrollY);
	}
}
