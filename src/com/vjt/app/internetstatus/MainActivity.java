package com.vjt.app.internetstatus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	ToggleButton mOnOffButton;
	EditText mURL;
	EditText mInterval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mOnOffButton = (ToggleButton) findViewById(R.id.running_state_toogle_button);
		mOnOffButton.setOnCheckedChangeListener(this);

		mInterval = (EditText) findViewById(R.id.interval);
		mURL = (EditText) findViewById(R.id.url);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();
		mInterval.setText(settings.getString("interval",
				getString(R.string.interval_default)));

		mURL.setText(settings.getString("url", getString(R.string.url_default)));

		if (InternetService.isRunning() == true) {
			mURL.setEnabled(false);
			mInterval.setEnabled(false);
			mOnOffButton.setChecked(true);
		} else {
			mURL.setEnabled(true);
			mInterval.setEnabled(true);
			mOnOffButton.setChecked(false);
		}

		mInterval.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String newInterval = mInterval.getText().toString();
				int interval = 0;
				boolean clear = false;
				try {
					interval = Integer.parseInt(newInterval);
				} catch (Exception e) {
					clear = true;
				}
				if (interval <= 0 || 65535 < interval || clear) {
					Toast.makeText(MainActivity.this,
							R.string.interval_validation_error,
							Toast.LENGTH_LONG).show();
					return;
				}
				editor.putString("interval", mInterval.getText().toString());
				editor.commit();
			}
		});

		mURL.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = mURL.getText().toString();

				if (URLUtil.isValidUrl(url)) {
					Toast.makeText(MainActivity.this,
							R.string.url_validation_error, Toast.LENGTH_LONG)
							.show();
					return;
				}
				editor.putString("url", mURL.getText().toString());
				editor.commit();
			}
		});
	}

	private void startServer() {
		Intent serverService = new Intent(this, InternetService.class);
		if (!InternetService.isRunning()) {
			startService(serverService);
		}
	}

	private void stopServer() {
		Intent serverService = new Intent(this, InternetService.class);
		stopService(serverService);
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(InternetService.ACTION_STARTED);
		filter.addAction(InternetService.ACTION_STOPPED);
		registerReceiver(serverReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(serverReceiver);
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if (arg1) {
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putString("url", mURL.getText().toString());
			editor.putString("interval", mInterval.getText().toString());
			editor.commit();

			mInterval.setEnabled(false);
			mURL.setEnabled(false);
			startServer();
		} else {
			mInterval.setEnabled(true);
			mURL.setEnabled(true);
			stopServer();
		}

	}

	BroadcastReceiver serverReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(InternetService.ACTION_STARTED)) {
				mOnOffButton.setChecked(true);
				mInterval.setEnabled(false);
				mURL.setEnabled(false);
			} else if (intent.getAction()
					.equals(InternetService.ACTION_STOPPED)) {
				mOnOffButton.setChecked(false);
				mInterval.setEnabled(true);
				mURL.setEnabled(true);
			}
		}
	};

}
