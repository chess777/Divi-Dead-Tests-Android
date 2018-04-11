package com.cs.divideadtest;

import java.io.File;

import com.cs.divideadtest.DL1Arhive.DL1ArhiveException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class BitmapTest extends Activity {
	class RenderView extends View {
		String sState;
		File externalDir;
		File dataFile;
		DL1Arhive SGarhive;
		byte[] baPackedFile;
		byte[] baUnpackedFile;
		BitmapFactory.Options bfoOptions;
		Bitmap BG;
		Bitmap BG2;
		Rect dst = new Rect();

		public RenderView(Context context) {
			super(context);

			sState = Environment.getExternalStorageState();
			if (!sState.equals(Environment.MEDIA_MOUNTED)) {
				Log.d("BitmapTest", "External storage not mounted\n");
			} else {
				externalDir = Environment.getExternalStorageDirectory();
				dataFile = new File(externalDir.getAbsolutePath()
						+ File.separator + "Divi-Dead" + File.separator
						+ "SG.DL1");
				SGarhive = new DL1Arhive(dataFile);
			}

			try {
				bfoOptions = new BitmapFactory.Options();
				bfoOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

				baPackedFile = SGarhive.LoadFile("WAKU_A1.BMP");
				baUnpackedFile = SGarhive.UnpackFile(baPackedFile);
				BG = BitmapFactory.decodeByteArray(baUnpackedFile, 0,
						baUnpackedFile.length, bfoOptions);
				if (BG != null) {
					Log.d("BitmapTest",
							"Loaded AWKU_A1.BMP file: " + BG.getConfig() + "\n");
				} else {
					Log.d("BitmapTest", "Error loading file AWKU_A1.BMP\n");
				}

				baPackedFile = SGarhive.LoadFile("TITLE.BMP");
				baUnpackedFile = SGarhive.UnpackFile(baPackedFile);
				BG2 = BitmapFactory.decodeByteArray(baUnpackedFile, 0,
						baUnpackedFile.length, bfoOptions);
				if (BG2 != null) {
					Log.d("BitmapTest",
							"Loaded TITLE.BMP file: " + BG2.getConfig() + "\n");
				} else {
					Log.d("BitmapTest", "Error loading file TITLE.BMP\n");
				}
			} catch (DL1ArhiveException e) {
				Log.d("BitmapTest", "Error loading file\n");
			} finally {
				SGarhive.Dispose();
			}
		}

		protected void onDraw(Canvas canvas) {
			canvas.drawRGB(0, 0, 0);

			if (BG != null) {
				dst.set(0, 0, BG.getWidth(), BG.getHeight());
				canvas.drawBitmap(BG, null, dst, null);
			}

			/*if (BG2 != null) {
				dst.set(32, 8, 32 + BG2.getWidth(), 8 + BG2.getHeight());
				canvas.drawBitmap(BG2, null, dst, null);
			}*/

			invalidate();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(new RenderView(this));
	}
}
