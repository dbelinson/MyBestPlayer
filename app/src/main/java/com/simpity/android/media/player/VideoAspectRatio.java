package com.simpity.android.media.player;

public enum VideoAspectRatio {
	Original(4,3), FourToTree(4,3), FiveToFour(5,4), SixteenToNine(16,9);
	
	public final int Width, Height;
	
	//--------------------------------------------------------------------------
	VideoAspectRatio(int width, int height) {
		Width = width;
		Height = height;
	}
}
