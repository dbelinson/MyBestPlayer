package com.simpity.android.media.player;

import android.util.Log;

public class HttpProxy {

	public native static void start() throws Exception;
	public native static void stop();

	public native static int connect(String url);
	public native static void close(String url);
	public native static boolean isClosed(String url);

	public native static boolean isTitleUpdated(String url);
	public native static String getTitle(String url);

	public native static String getLocation(String url);

	public native static int getHttpAnswer(String url);

    static {
    	try
    	{
    	Log.d("Http", "loading Library httpproxy");
        System.loadLibrary("httpproxy");
    	}
    	catch(UnsatisfiedLinkError e)
    	{
    		Log.d("Http", e.getMessage());
    	}
    }

	/*public final static int STATE_UNKNOWN		= -1;
	public final static int STATE_CLOSED		= 0;
	public final static int STATE_CONNECTING	= 1;
	public final static int STATE_BUFFERING		= 2;
	public final static int STATE_PLAYING		= 3;*/

}
