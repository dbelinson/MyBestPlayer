package com.simpity.android.media.dialogs;

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class WhatsNewDlg {
	
	public static void show(Context context, int rawResourceID, int titleResID, boolean isCancelable, final DialogInterface.OnClickListener clickListener) {
		InputStream stream = context.getResources().openRawResource(rawResourceID);
		
		byte[] text;
		
		try {
			text = new byte[stream.available()];
			stream.read(text);			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Spanned spanned = Html.fromHtml(new String(text));
	
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final SpannableString spstr = new SpannableString(spanned);
		Linkify.addLinks(spstr, Linkify.WEB_URLS);
		builder.setMessage(spstr);
		builder.setTitle(titleResID);//"What's new"
		builder.setCancelable(isCancelable);
		if(isCancelable)
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					if(clickListener != null)
						clickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
				}
			});
		builder.setNeutralButton(Res.string.ok, clickListener);

		AlertDialog dlg = builder.show();
		
		// Make the textview clickable. Must be called after show()
		TextView messageText = (TextView)dlg.findViewById(android.R.id.message);
		messageText.setMovementMethod(LinkMovementMethod.getInstance());

		/*=======================
		Spanned spanned = Html.fromHtml(new String(text));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(spanned);
		builder.setTitle(titleResID);//"What's new"
		builder.setCancelable(isCancelable);
		if(isCancelable)
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					if(clickListener != null)
						clickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
				}
			});
		builder.setNeutralButton(Res.string.ok, clickListener);
		builder.show();
		============================*/
	}
}
