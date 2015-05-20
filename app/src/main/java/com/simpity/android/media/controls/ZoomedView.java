package com.simpity.android.media.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.simpity.android.media.Res;
//import android.content.res.TypedArray;
//import android.graphics.BitmapFactory;

public class ZoomedView extends ImageView {

	public ZoomedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		scrollX = 0;
		scrollY = 0;
		scale = 1.0f;
		modifierValue = 50;

		ViewPaint = new Paint();
/*
		TypedArray a = context.obtainStyledAttributes(attrs,
				Res.styleable.ZN5ScrollView);
*/
		LoadedBitmap = BitmapFactory.decodeResource(getResources(), Res.drawable.vlc_player_config1);

		IMAGE_WIDTH = LoadedBitmap.getWidth();
		IMAGE_HEIGHT = LoadedBitmap.getHeight();

		ViewBitmap = Bitmap.createBitmap(LoadedBitmap);
		handleView(ZOOM_IN);
		//handleScroll(10,10);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		SCREEN_WIDTH = w;
		SCREEN_HEIGHT = h;

		if (IMAGE_WIDTH < SCREEN_WIDTH) {
			IMAGE_WIDTH = SCREEN_WIDTH - scrollX;
		}
		if (IMAGE_HEIGHT < SCREEN_HEIGHT) {
			IMAGE_HEIGHT = SCREEN_HEIGHT - scrollY;
		}

	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.scale(scale, scale);
		canvas.drawBitmap(ViewBitmap, 0f, 0f, ViewPaint);
	}

	public void handleView(int zoomType) {
		switch (zoomType) {
		case ZOOM_IN:
			if (scale <= 1.5f) {
				scale = scale + 0.1f;
			}
			break;
		case ZOOM_OUT:
			if (scale > 1.0f) {
				scale = scale - 0.1f;
			}
			break;
		}
		invalidate();

	}

	public void handleScroll(float distX, float distY) {
		/* X-Axis */
		if (distX > 6.0) {
			if (scrollX < IMAGE_WIDTH) {
				scrollX = Math.min(IMAGE_WIDTH - SCREEN_WIDTH, scrollX
						+ modifierValue);
			}
		} else if (distX < -6.0) {
			if (scrollX >= 50) {
				scrollX = Math.min(IMAGE_WIDTH + SCREEN_WIDTH, scrollX
						- modifierValue);
			} else {
				scrollX = 0;
			}
		}

		/* Y-Axis */
		if (distY > 6.0) {
			if (scrollY < IMAGE_HEIGHT) {
				scrollY = Math.min(IMAGE_HEIGHT - SCREEN_HEIGHT, scrollY
						+ modifierValue);
			}
		} else if (distY < -6.0) {
			if (scrollY >= 50) {
				scrollY = Math.min(IMAGE_HEIGHT + SCREEN_HEIGHT, scrollY
						- modifierValue);
			} else {
				scrollY = 0;
			}
		}

		if ((scrollX <= IMAGE_WIDTH) && (scrollY <= IMAGE_HEIGHT)) {
			ViewBitmap = Bitmap.createBitmap(LoadedBitmap, scrollX, scrollY,
					SCREEN_WIDTH, SCREEN_HEIGHT);
			invalidate();
		}

	}

	private int modifierValue;

	private float scale;
	public final int ZOOM_IN = 1;
	public final int ZOOM_OUT = 2;

	private int SCREEN_WIDTH;
	private int SCREEN_HEIGHT;

	private int IMAGE_WIDTH;
	private int IMAGE_HEIGHT;

	private int scrollX;
	private int scrollY;

	private Bitmap LoadedBitmap;
	private Bitmap ViewBitmap;
	private Paint ViewPaint;
}
