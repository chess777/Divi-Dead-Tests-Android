package com.cs.divideadtest;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;

public class RenderViewTest extends Activity implements SurfaceHolder.Callback,
		OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener {
	private static final String	TAG						= "RenderViewTest";
	private int					mVideoWidth;
	private int					mVideoHeight;
	private boolean				mIsVideoSizeKnown		= false;
	private boolean				mIsVideoReadyToBePlayed	= false;
	private MediaPlayer			mMediaPlayer;
	private FastRenderView		renderView;
	private String				sState;
	private String				mPath;
	private File				externalDir;

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!LibsChecker.checkVitamioLibs(this)) {
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		renderView = new FastRenderView(this);
		setContentView(renderView);
		this.getWindow().getDecorView()
				.setBackgroundColor(Color.argb(255, 0, 0, 0));
		if (VERSION.SDK_INT < 11) {
			renderView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		renderView.holder.addCallback(RenderViewTest.this);

		sState = Environment.getExternalStorageState();
		if (!sState.equals(Environment.MEDIA_MOUNTED)) {
			Log.d(TAG, "External storage not mounted\n");
			mPath = null;
		}
		else {
			externalDir = Environment.getExternalStorageDirectory();
			mPath = externalDir.getAbsolutePath() + File.separator
					+ "Divi-Dead" + File.separator + "OPEN.AVI";
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		playVideo();
	}

	protected void onResume() {
		super.onResume();
		renderView.resume();
	}

	protected void onPause() {
		super.onPause();
		releaseMediaPlayer();
		doCleanUp();
		renderView.pause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaPlayer();
		doCleanUp();
	}

	class FastRenderView extends SurfaceView implements Runnable {
		Thread				renderThread	= null;
		SurfaceHolder		holder;
		volatile boolean	running			= false;

		@SuppressLint("NewApi")
		public FastRenderView(Context context) {
			super(context);

			holder = getHolder();
		}

		public void resume() {
			// running = true;
			// renderThread = new Thread(this);
			// renderThread.start();
		}

		public void run() {
			/*
			 * while (running) { if (!holder.getSurface().isValid()) continue;
			 * 
			 * Canvas canvas = holder.lockCanvas(); canvas.drawRGB(255, 0, 0);
			 * holder.unlockCanvasAndPost(canvas); }
			 */
		}

		public void pause() {
			running = false;
			/*
			 * while (true) { // try { // renderThread.join(); return; } catch
			 * (InterruptedException e) { // retry } }
			 */
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void onVideoSizeChanged(io.vov.vitamio.MediaPlayer p_MediaPlayer,
			int p_Width, int p_Height) {
		if (p_Width == 0 || p_Height == 0) {
			Log.e(TAG, "invalid video width(" + p_Width + ") or height("
					+ p_Height + ")");
			return;
		}
		Log.e(TAG, "Video width(" + p_Width + "), height(" + p_Height + ")");
		mIsVideoSizeKnown = true;
		mVideoWidth = p_Width;
		mVideoHeight = p_Height;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	@Override
	public void onPrepared(io.vov.vitamio.MediaPlayer p_MediaPlayer) {
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	@Override
	public void onCompletion(io.vov.vitamio.MediaPlayer p_MediaPlayer) {

	}

	private void playVideo() {
		doCleanUp();
		try {
			// Create a new media player and set the listeners
			mMediaPlayer = new MediaPlayer(this);
			mMediaPlayer.setDataSource(mPath);
			mMediaPlayer.setDisplay(renderView.holder);
			mMediaPlayer.setScreenOnWhilePlaying(true);
			mMediaPlayer.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
			mMediaPlayer.prepare();
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

		}
		catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private void doCleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
	}

	private void startVideoPlayback() {
		LayoutParams lp;

		lp = new LayoutParams(mVideoWidth, mVideoHeight);
		lp.setMargins(187, 120, mVideoWidth, mVideoHeight);
		renderView.setLayoutParams(lp);
		
		mMediaPlayer.start();
	}
}
