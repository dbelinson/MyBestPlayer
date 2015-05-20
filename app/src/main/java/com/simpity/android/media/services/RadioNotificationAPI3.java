package com.simpity.android.media.services;

import android.app.Notification;
import android.app.Service;

public class RadioNotificationAPI3 extends RadioNotification {

	//-------------------------------------------------------------------------
	public RadioNotificationAPI3(Service context) {
		super(context);
	}
	
	//-------------------------------------------------------------------------
	@Override
	public void notify(Notification notification) {
		mNotificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
	}
}
