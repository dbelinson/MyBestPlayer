package com.simpity.android.media.radio;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;

import com.simpity.android.media.storage.RadioRecord;

import java.io.IOException;


/**
 * Created by dbelinson on 8/18/2015.
 */
public class MusicRadioService extends Service implements OnPreparedListener {



    void play(RadioRecord mRecord) {

        MediaPlayer mMediaPlayer=null;
        try {
            mMediaPlayer= new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(mRecord.getUrl());
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    mp.start();
                }
            });
        } catch (IllegalArgumentException e) {
            // ...
        } catch (IllegalStateException e) {
            // ...
        } catch (IOException e) {
            // ...
        }

    }
    @Override
    public void onPrepared(MediaPlayer mp) {

        mp.start();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }





}
