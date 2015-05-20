package com.simpity.android.media;

interface IMediaServiceInterface {

	int getUpdateState();
	int getUpdatedLinkCount(int type);
	
	void startNewRadio(String url);
	void startCurrentRadio();
	void startRadio(int radio_record_id);
	boolean startRadioPlaylist(int playlist_id);
	void stopRadio();
	void nextRadio();
	boolean isRadioPlaying();
	boolean isRadioPlaylistPlaying();
	//void stopReconnecting();
	int getRadioCurrentAction();
	int getCurrentRadioId();
	String getRadioInfo();
	String getRadioComposition();
}
