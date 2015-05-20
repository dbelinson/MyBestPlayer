package com.simpity.android.media.statistic;

public class StatSummary {

	public final int count_app_launches;
	public final int count_radio_launches;
	public final int count_video_launches;
	public final int count_camera_launches;
	
	public final long time_video;
	public final long time_radio;
	public final long time_camera;
	
	public StatSummary( int app_launches, 
						
						int radio_launches, 
						int video_launches, 
						int camera_launches,
						
						long radio_time,
						long video_time, 
						long camera_time){
		
		count_app_launches = app_launches;
		count_radio_launches = radio_launches;
		count_video_launches = video_launches;
		count_camera_launches = camera_launches;
		
		time_radio = radio_time;
		time_video = video_time;
		time_camera = camera_time;
	}
	
	@Override
	public String toString() {
		return String.format("App launch count = %d;\nRadio launch count = %d;\nVideo launch count = %d;\nCamera launch count = %d;\nRadio time = %d;\nVideo time = %d;\nCamera time = %d", 
				count_app_launches,
				count_radio_launches,
				count_video_launches,
				count_camera_launches,
				time_radio,
				time_video,
				time_camera);
	}
}
