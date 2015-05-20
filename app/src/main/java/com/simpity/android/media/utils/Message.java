package com.simpity.android.media.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public final class Message {

	public static void show(Context context, int title, String message) {
    	AlertDialog.Builder dlg = new AlertDialog.Builder(context);
    	dlg.setTitle(title);
    	dlg.setMessage(message);
    	dlg.setCancelable(true);
    	dlg.show();
    }

	//--------------------------------------------------------------------------
	public static void show(Context context, int title, int message) {
    	AlertDialog.Builder dlg = new AlertDialog.Builder(context);
    	dlg.setTitle(title);
    	dlg.setMessage(message);
    	dlg.setCancelable(true);
    	dlg.show();
    }

	//--------------------------------------------------------------------------
	public static void show(Context context, int title, String message,
			DialogInterface.OnCancelListener onCancelListener) {
    	AlertDialog.Builder dlg = new AlertDialog.Builder(context);
    	dlg.setTitle(title);
    	dlg.setMessage(message);
    	dlg.setCancelable(true);
    	if(onCancelListener != null)
    		dlg.setOnCancelListener(onCancelListener);
    	dlg.show();
    }

}
