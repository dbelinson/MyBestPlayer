package com.simpity.android.media;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.simpity.android.media.Res;

public class WaitDialog implements Runnable {

	private AlertDialog mDialog;
	private View mView;
	private ProgressBar mProgress;
	private boolean mClose = false;
	private int mIndicator = 0;
	private final Handler mHandler = new Handler();

	//--------------------------------------------------------------------------
	public WaitDialog(Context context, String text) {

		mView = LayoutInflater.from(context).inflate(Res.layout.wait_dialog2, null);

		mProgress = (ProgressBar)mView.findViewById(Res.id.WaitProgressBar);
		mProgress.setMax(10);

		TextView text_view = (TextView)mView.findViewById(Res.id.WaitText);
		text_view.setText(text);

		mHandler.postDelayed(this, 100);
	}

	//--------------------------------------------------------------------------
	public static WaitDialog show(Context context, String text) {
		WaitDialog dialog = new WaitDialog(context, text);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setView(dialog.mView);

		dialog.mDialog = builder.show();

		return dialog;
	}

	//--------------------------------------------------------------------------
	public static WaitDialog show(Context context, int text_res) {
		return show(context, context.getString(text_res));
	}

	//--------------------------------------------------------------------------
	@Override
	public void run() {
		if (!mClose) {
			mIndicator = (mIndicator + 1) % mProgress.getMax();
			mProgress.setProgress(mIndicator);
			mHandler.postDelayed(this, 100);
		}
	}


	//--------------------------------------------------------------------------
	private class CloseRun implements Runnable {

		private final Runnable mFinishRun;

		CloseRun(Runnable finish_run) {
			mFinishRun = finish_run;
		}

		@Override
		public void run() {
			close();
			if (mFinishRun != null)
				mFinishRun.run();
		}
	}

	//--------------------------------------------------------------------------
	public void postClose(Runnable finish_run) {
		mHandler.post(new CloseRun(finish_run));
	}

	//--------------------------------------------------------------------------
	public void close() {
		mClose = true;
		try {
			mDialog.dismiss();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
