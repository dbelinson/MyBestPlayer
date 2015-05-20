package com.simpity.android.media.widgets.camera;

interface ICameraWidgetServiceInterface {
	void startJpegCamera(int widgetId, String url, int refreshTime, String username, String password);
	void stopJpegCamera(int widgetId);
}
