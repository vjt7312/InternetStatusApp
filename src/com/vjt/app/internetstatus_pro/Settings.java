package com.vjt.app.internetstatus_pro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.SeekBar;

public class Settings extends Activity {

	SeekBar mVibrator;
	SeekBar mLight;
	SeekBar mSound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		mVibrator = (SeekBar) findViewById(R.id.vibrator);
		mLight = (SeekBar) findViewById(R.id.light);
		mSound = (SeekBar) findViewById(R.id.sound);

		mVibrator
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						final SharedPreferences settings = PreferenceManager
								.getDefaultSharedPreferences(Settings.this);
						final SharedPreferences.Editor editor = settings.edit();
						editor.putInt("settings_vibrator", progress);
						editor.commit();
					}
				});
		mLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(Settings.this);
				final SharedPreferences.Editor editor = settings.edit();
				editor.putInt("settings_light", progress);
				editor.commit();
			}
		});
		mSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(Settings.this);
				final SharedPreferences.Editor editor = settings.edit();
				editor.putInt("settings_sound", progress);
				editor.commit();
			}
		});

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(Settings.this);
		mVibrator.setProgress(settings.getInt("settings_vibrator", 1));
		mLight.setProgress(settings.getInt("settings_light", 1));
		mSound.setProgress(settings.getInt("settings_sound", 0));
	}
}
