package com.simpity.android.media.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.simpity.android.media.Res;
import com.simpity.android.media.radio.RadioSelectActivity;

public abstract class RadioNotification {
	
	public final static String ACTION_BRING_TO_FRONT = "ACTION_BRING_TO_FRONT";
	protected final int ONGOING_NOTIFICATION_ID = 1;
	
	protected final NotificationManager mNotificationManager;
	protected Service mContext = null;

	//-------------------------------------------------------------------------
	public RadioNotification(Service context) {
		mContext = context;
		mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	//-------------------------------------------------------------------------
	public void ongoingNotify(String msg, String title, int drawableID) {
		int color = 0xFF000000;
		//mNotificationManager.cancel(ONGOING_NOTIFICATION_ID);

		Notification notification = new Notification(drawableID, null, System.currentTimeMillis());
		//notification.tickerText = title;
		RemoteViews contentView = new RemoteViews(mContext.getPackageName(), Res.layout.notifylayout);
		contentView.setImageViewResource(Res.id.NotificationIcon, drawableID);
		if (msg != null) {
			contentView.setTextViewText(Res.id.NotificationMessage, msg);
		}
		contentView.setTextColor(Res.id.NotificationMessage, color);
		contentView.setTextViewText(Res.id.NotificationTitle, title);
		contentView.setTextColor(Res.id.NotificationTitle, color);

		notification.contentView = contentView;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		Intent setIntent = new Intent(mContext.getApplicationContext(), RadioSelectActivity.class);  //RadioActivity.class);
		//setIntent.putExtra(RadioActivity.URL_STRING, url);
		setIntent.setAction(ACTION_BRING_TO_FRONT);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		
		notification.contentIntent = PendingIntent.getActivity(mContext, 4097, setIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		notify(notification);
	}

	//-------------------------------------------------------------------------
	public abstract void notify(Notification notification);
	
	//-------------------------------------------------------------------------
	public void cancelNotify() {
		mNotificationManager.cancel(ONGOING_NOTIFICATION_ID);
	}
}
