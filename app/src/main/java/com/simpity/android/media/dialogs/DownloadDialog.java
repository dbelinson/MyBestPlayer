package com.simpity.android.media.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import com.simpity.android.media.Res;

public class DownloadDialog extends AlertDialog {

	public boolean GetDoNotShowQuestionCheckedState(){
		return ((CheckBox)findViewById(Res.id.DoNotShowDownloadQuestion)).isChecked();
	}
	
	public DownloadDialog(Context context) {
		super(context);
		init(context);
	}

	public DownloadDialog(Context context, int theme) {
		super(context, theme);
		init(context);
	}

	public DownloadDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		init(context);
	}

	private void init(Context context) {
		setView(LayoutInflater.from(context).inflate(Res.layout.download_dialog, null));
	}
}
