package com.simpity.android.media.services;

import android.app.Notification;
import android.app.Service;

public class RadioNotificationAPI5 extends RadioNotification {

	//-------------------------------------------------------------------------
	public RadioNotificationAPI5(Service context) {
		super(context);
	}

	//-------------------------------------------------------------------------
	@Override
	public void notify(Notification notification) {
		mContext.startForeground(ONGOING_NOTIFICATION_ID, notification);
	}

	//-------------------------------------------------------------------------
	@Override
	public void cancelNotify() {
		mContext.stopForeground(true);
		mNotificationManager.cancel(ONGOING_NOTIFICATION_ID);
	}

}
