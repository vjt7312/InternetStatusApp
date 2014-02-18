package com.vjt.app.internetstatus_pro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.vjt.app.internetstatus_pro.NetworkConnectivityListener.State;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	private static final String TAG = "MainActivity";

	ToggleButton mOnOffButton;
	EditText mURL;
	EditText mInterval;
	// pro
	TextView mNtwType;
	TextView mNtwState;
	TextView mNtwRoaming;
	// stat
	TextView mTX;
	TextView mRX;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mOnOffButton = (ToggleButton) findViewById(R.id.running_state_toogle_button);
		mOnOffButton.setOnCheckedChangeListener(this);

		mInterval = (EditText) findViewById(R.id.interval);
		mURL = (EditText) findViewById(R.id.url);

		// pro
		mNtwType = (TextView) findViewById(R.id.ntw_type);
		mNtwState = (TextView) findViewById(R.id.ntw_state);
		mNtwRoaming = (TextView) findViewById(R.id.ntw_roam);

		// stat
		mTX = (TextView) findViewById(R.id.stat_tx);
		mRX = (TextView) findViewById(R.id.stat_rx);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();
		mInterval.setText(settings.getString("interval",
				getString(R.string.interval_default)));

		mURL.setText(settings.getString("url", getString(R.string.url_default)));

		if (settings.getString("onoff", getString(R.string.onoff_default))
				.equals("on")) {
			mURL.setEnabled(false);
			mInterval.setEnabled(false);
			mOnOffButton.setChecked(true);
			editor.putString("onoff", "on");
			editor.commit();
			startServer();
		} else {
			mURL.setEnabled(true);
			mInterval.setEnabled(true);
			mOnOffButton.setChecked(false);
			editor.putString("onoff", "off");
			editor.commit();
			stopServer();
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
				if (interval <= 1 || 65535 < interval || clear) {
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
		startService(serverService);
	}

	private void stopServer() {
		Intent serverService = new Intent(this, InternetService.class);
		serverService.setAction(InternetService.ACTION_STOPPED);
		startService(serverService);
	}

	// pro
	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(InternetService.ACTION_STARTED);
		filter.addAction(InternetService.ACTION_STOPPED);
		filter.addAction(InternetService.ACTION_OFFLINE);
		filter.addAction(InternetService.ACTION_STAT);
		registerReceiver(internetServerReceiver, filter);
	}

	// pro
	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(internetServerReceiver);
	}

	// pro
	private void setNetworkInfo(NetworkInfo info) {
		if (info == null) {
			clearNetworkInfo();
			return;
		}
		mNtwType.setText(info.getTypeName()
				+ ((info.getSubtypeName() == null) ? "" : ("["
						+ info.getSubtypeName() + "]")));
		mNtwState.setText(info.getState().toString());
		mNtwRoaming.setText(info.isRoaming() ? "True" : "False");
	}

	// pro
	private void clearNetworkInfo() {
		mNtwType.setText(getString(R.string.ntw_none_label));
		mNtwState.setText(getString(R.string.ntw_none_label));
		mNtwRoaming.setText(getString(R.string.ntw_none_label));
		
		// stat
		mTX.setText("0");
		mRX.setText("0");
	}

	// pro
	BroadcastReceiver internetServerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LogUtil.v(TAG,
					"InternetService action received: " + intent.getAction());
			if (intent.getAction().equals(InternetService.ACTION_STARTED)) {
				if (InternetService.mNetworkConnectivityListener != null
						&& InternetService.mNetworkConnectivityListener
								.getState() == State.CONNECTED) {
					setNetworkInfo(InternetService.mNetworkConnectivityListener
							.getNetworkInfo());
				}
			} else if (intent.getAction()
					.equals(InternetService.ACTION_STOPPED)
					|| intent.getAction()
							.equals(InternetService.ACTION_OFFLINE)) {
				clearNetworkInfo();
			} else if (intent.getAction()
					.equals(InternetService.ACTION_STAT)) {
				if (intent.getLongExtra("tx", -1) == -1) {
					mTX.setText(R.string.stat_unsupport);
				} else {
					mTX.setText(Long.toString(intent.getLongExtra("tx", -1)));
				}
				if (intent.getLongExtra("rx", -1) == -1) {
					mRX.setText(R.string.stat_unsupport);
				} else {
					mRX.setText(Long.toString(intent.getLongExtra("rx", -1)));
				}
			}
		}
	};

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if (arg1) {
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putString("url", mURL.getText().toString());
			editor.putString("interval", mInterval.getText().toString());
			editor.putString("onoff", "on");
			editor.commit();

			mInterval.setEnabled(false);
			mURL.setEnabled(false);
			startServer();
		} else {
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putString("onoff", "off");
			editor.commit();

			mInterval.setEnabled(true);
			mURL.setEnabled(true);
			stopServer();
		}

	}

}
