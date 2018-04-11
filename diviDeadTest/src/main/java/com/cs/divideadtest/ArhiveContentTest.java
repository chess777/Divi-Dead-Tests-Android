package com.cs.divideadtest;

import java.io.File;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

public class ArhiveContentTest extends Activity {
	TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		textView = new TextView(this);
		setContentView(textView);
		textView.setText("DemoText\ntext2");

		String sState;
		File externalDir;
		File dataFile;
		DL1Arhive SGarhive;
		DL1Arhive WVarhive;
		StringBuilder builder;
		int iIdx;
		long lStartTime;
		long lEndTime;

		sState = Environment.getExternalStorageState();
		if (!sState.equals(Environment.MEDIA_MOUNTED)) {
			textView.setText("No external storage mounted");
		} else {
			externalDir = Environment.getExternalStorageDirectory();
			dataFile = new File(externalDir.getAbsolutePath() + File.separator
					+ "Divi-Dead" + File.separator + "SG.DL1");
			lStartTime = System.nanoTime();
			SGarhive = new DL1Arhive(dataFile);
			lEndTime = System.nanoTime();
			builder = new StringBuilder();

			builder.append("File: ");
			builder.append(dataFile.getAbsolutePath());
			builder.append("\nLoad time: ");
			builder.append((double) (lEndTime - lStartTime) / 1.0e9);
			builder.append("\nSignature: ");
			try {
				builder.append(new String(SGarhive.m_dahArhiveHeader.m_bSign,
						"UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			builder.append("\nEntry count: ");
			builder.append(SGarhive.m_dahArhiveHeader.m_sEntryCount);
			builder.append("\nFile table offset: ");
			builder.append(SGarhive.m_dahArhiveHeader.m_iFileTableOffset);
			builder.append("\n");
			for (iIdx = 0; iIdx < SGarhive.m_lArhiveEntries.size() && iIdx < 5; iIdx++) {
				builder.append("File No: ");
				builder.append(String.format("%04d", iIdx));
				builder.append("; File name: ");
				builder.append(SGarhive.m_lArhiveEntries.get(iIdx).m_sFileName);
				builder.append("; File size: ");
				builder.append(SGarhive.m_lArhiveEntries.get(iIdx).m_iFileSize);
				builder.append("; File offset: ");
				builder.append(SGarhive.m_lArhiveEntries.get(iIdx).m_iFileOffset);
				builder.append("\n");
			}
			
			dataFile = new File(externalDir.getAbsolutePath() + File.separator
					+ "Divi-Dead" + File.separator + "WV.DL1");
			lStartTime = System.nanoTime();
			WVarhive = new DL1Arhive(dataFile);
			lEndTime = System.nanoTime();
			
			builder.append("\nFile: ");
			builder.append(dataFile.getAbsolutePath());
			builder.append("\nLoad time: ");
			builder.append((double) (lEndTime - lStartTime) / 1.0e9);
			builder.append("\nSignature: ");
			try {
				builder.append(new String(WVarhive.m_dahArhiveHeader.m_bSign,
						"UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			builder.append("\nEntry count: ");
			builder.append(WVarhive.m_dahArhiveHeader.m_sEntryCount);
			builder.append("\nFile table offset: ");
			builder.append(WVarhive.m_dahArhiveHeader.m_iFileTableOffset);
			builder.append("\n");
			for (iIdx = 0; iIdx < WVarhive.m_lArhiveEntries.size() && iIdx < 5; iIdx++) {
				builder.append("File No: ");
				builder.append(String.format("%04d", iIdx));
				builder.append("; File name: ");
				builder.append(WVarhive.m_lArhiveEntries.get(iIdx).m_sFileName);
				builder.append("; File size: ");
				builder.append(WVarhive.m_lArhiveEntries.get(iIdx).m_iFileSize);
				builder.append("; File offset: ");
				builder.append(WVarhive.m_lArhiveEntries.get(iIdx).m_iFileOffset);
				builder.append("\n");
			}

			textView.setText(builder.toString());

		}
	}
}
