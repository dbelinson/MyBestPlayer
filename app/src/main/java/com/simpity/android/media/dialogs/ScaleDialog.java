package com.simpity.android.media.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.simpity.android.media.Res;


public class ScaleDialog extends Dialog {

	ScaleChangedHandler mHandler = null;
	static int width = 1;
	static int height = 1;
	static int selectedItem = Res.id.none;
	static boolean isUseNative = true;
	Handler ownHandler = new Handler();

	public void reset()
	{
		selectedItem = Res.id.none;
		isUseNative = true;
		((RadioGroup)findViewById(Res.id.radioGroup)).check(selectedItem);
	}

	public double GetScale()
	{
		return (double)width/(double)height;
	}
	public boolean GetIsUseNative()
	{
		return isUseNative;
	}

	public ScaleDialog(Context context) {
		super(context);
		init(context);
	}

	public ScaleDialog(Context context, int theme) {
		super(context, theme);
		init(context);
	}

	public ScaleDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		init(context);
	}

	private void init(Context context) {
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setTitle("Aspect Ratio");
		this.setCancelable(true);
		this.setCanceledOnTouchOutside(true);
		View inflateView = LayoutInflater.from(context).inflate(Res.layout.scale_dialog, null);
		addContentView(inflateView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		RadioGroup r = (RadioGroup)findViewById(Res.id.radioGroup);
		r.check(selectedItem);
		r.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {

				ownHandler.removeCallbacks(dismissRunnable);

				selectedItem = checkedId;
				if(mHandler != null)
				{
					switch (checkedId) {
					case Res.id.none:
						isUseNative = true;
						width = 1;
						height = 1;
						mHandler.ScaleChangedHandle(width,height);
						break;

					case Res.id.four_to_tree:
						isUseNative = false;
						width = 4;
						height = 3;
						mHandler.ScaleChangedHandle(width, height);
						break;

					case Res.id.five_to_four:
						isUseNative = false;
						width = 5;
						height = 4;
						mHandler.ScaleChangedHandle(width, height);
						break;

					case Res.id.sixteen_to_nine:
						isUseNative = false;
						width = 16;
						height = 9;
						mHandler.ScaleChangedHandle(width, height);
						break;

					default:
						break;
					}
				}
				cancel();
			}
		});

		/*
		wSB = (SeekBar) findViewById(Res.id.widthSeekBar);
		wSB.setProgress(width);
		hSB = (SeekBar) findViewById(Res.id.heightSeekBar);
		hSB.setProgress(height);
		wSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(mHandler != null)
				{
					isUseNative = false;
					width = wSB.getProgress();
					height = hSB.getProgress();
					mHandler.ScaleChangedHandle(wSB.getProgress()+1, hSB.getProgress()+1);
				}
			}
		});
		hSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(mHandler != null)
				{
					isUseNative = false;
					width = wSB.getProgress();
					height = hSB.getProgress();
					mHandler.ScaleChangedHandle(wSB.getProgress()+1, hSB.getProgress()+1);
				}
			}
		});
	*/
	}

	public void SetScaleChangedHandler(ScaleChangedHandler handler)
	{
		mHandler = 	handler;
	}

	Runnable dismissRunnable = new Runnable() {
		@Override
		public void run() {
			dismiss();
		}
	};

	public void show(int dismissTime){
		super.show();
		ownHandler.removeCallbacks(dismissRunnable);
		ownHandler.postDelayed(dismissRunnable, dismissTime);
	}

	@Override
	public void show() {
		show(3000);
	}
	
	@Override
	public void dismiss() {
		ownHandler.removeCallbacks(dismissRunnable);
		super.dismiss();
	}
}
