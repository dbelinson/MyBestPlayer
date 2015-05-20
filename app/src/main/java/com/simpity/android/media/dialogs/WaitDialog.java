package com.simpity.android.media.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class WaitDialog extends Dialog {

	public WaitDialog(Context context) {
		super(context);
		init();
	}

	public WaitDialog(Context context, int theme) {
		super(context, theme);
		init();
	}

	public WaitDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		init();
	}

	private void init() {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(Res.layout.wait_dialog);
		ProgressBar pBar = (ProgressBar)findViewById(Res.id.progress);
		pBar.setIndeterminate(true);
		pBar.setVisibility(View.VISIBLE);
		resetPercent();
	}

	public void invalidatePercent(int percent){
		TextView view = (TextView) findViewById(Res.id.progressValue);
		String currTxt = view.getText().toString();
		int value = Integer.parseInt(currTxt.substring(0, currTxt.length()-1));
		if(value < percent){
			view.setText(String.valueOf(percent).concat("%"));
			view.invalidate();
		}
	}

	@Override
	public void show() {
		ProgressBar pBar = (ProgressBar)findViewById(Res.id.progress);
		pBar.setIndeterminate(true);
		pBar.setVisibility(View.GONE);
		pBar.setVisibility(View.VISIBLE);
		pBar.setAnimation(pBar.getAnimation());
		pBar.invalidate();
		findViewById(Res.id.progressValue).setVisibility(View.GONE);
		super.show();
	};
	
	public void show(String message) {
		ProgressBar pBar = (ProgressBar)findViewById(Res.id.progress);
		pBar.setIndeterminate(true);
		pBar.setVisibility(View.GONE);
		pBar.setVisibility(View.VISIBLE);
		pBar.setAnimation(pBar.getAnimation());
		pBar.invalidate();
		findViewById(Res.id.progressValue).setVisibility(View.GONE);
		((TextView)findViewById(Res.id.wait_dlg_message)).setText(message);
		super.show();
	};

	public void show(boolean isShowPercent){
		if(isShowPercent){
			ProgressBar pBar = (ProgressBar)findViewById(Res.id.progress);
			pBar.setIndeterminate(true);
			pBar.setVisibility(View.GONE);
			pBar.setVisibility(View.VISIBLE);
			pBar.setAnimation(pBar.getAnimation());
			resetPercent();
			pBar.invalidate();
			findViewById(Res.id.progressValue).setVisibility(View.VISIBLE);
			super.show();
		}
		else
			this.show();
	}

	private void resetPercent(){
		TextView view = (TextView) findViewById(Res.id.progressValue);
		view.setText("0%");
		view.invalidate();
	}
}
