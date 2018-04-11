package com.cs.divideadtest;

import java.io.File;
import java.io.IOException;

import com.cs.divideadtest.DL1Arhive.DL1ArhiveException;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class VoiceTest extends Activity {
	
	String sState;
	File externalDir;
	File dataFile;
	DL1Arhive WVarhive;
	MediaPlayer mediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        setContentView(textView);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = new MediaPlayer();
        try {
            sState = Environment.getExternalStorageState();
			if (!sState.equals(Environment.MEDIA_MOUNTED)) {
				Log.d("BitmapTest", "External storage not mounted\n");
			} else {
				externalDir = Environment.getExternalStorageDirectory();
				dataFile = new File(externalDir.getAbsolutePath()
						+ File.separator + "Divi-Dead" + File.separator
						+ "WV.DL1");
				WVarhive = new DL1Arhive(dataFile);
			}
            
			mediaPlayer.setDataSource(WVarhive.GetArhiveFD(),
            		WVarhive.GetFileOffsetInArhive("AZU0010.WAV"), WVarhive.GetFileSize("AZU0010.WAV"));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
        } catch (IOException e) {
            textView.setText("Couldn't load music file, " + e.getMessage());
            mediaPlayer = null;
        } catch (DL1ArhiveException e) {
            textView.setText("Couldn't load music file, " + e.getMessage());
            mediaPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            if (isFinishing()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
    }
}
