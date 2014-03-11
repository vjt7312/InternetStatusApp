package com.vjt.app.internetstatus_pro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.vjt.app.internetstatus_pro.NetworkConnectivityListener.State;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	private static final String TAG = "MainActivity";

	ToggleButton mOnOffButton;
	SeekBar mInterval;
	// pro
	TextView mNtwType;
	TextView mNtwState;
	TextView mNtwRoaming;
	// stat
	TextView mTX;
	TextView mRX;
	TextView mTXTotal;
	TextView mRXTotal;
	Button mTXReset;
	Button mRXReset;

	boolean mOnOff;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mOnOffButton = (ToggleButton) findViewById(R.id.running_state_toogle_button);
		mOnOffButton.setOnCheckedChangeListener(this);

		mInterval = (SeekBar) findViewById(R.id.interval);

		// pro
		mNtwType = (TextView) findViewById(R.id.ntw_type);
		mNtwState = (TextView) findViewById(R.id.ntw_state);
		mNtwRoaming = (TextView) findViewById(R.id.ntw_roam);

		// stat
		mTX = (TextView) findViewById(R.id.stat_tx);
		mRX = (TextView) findViewById(R.id.stat_rx);
		mTXTotal = (TextView) findViewById(R.id.stat_tx_total);
		mRXTotal = (TextView) findViewById(R.id.stat_rx_total);
		mTXReset = (Button) findViewById(R.id.stat_tx_btn);
		mRXReset = (Button) findViewById(R.id.stat_rx_btn);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();

		mTXReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mOnOff) {
					Intent serverService = new Intent(MainActivity.this,
							InternetService.class);
					serverService.setAction(InternetService.ACTION_RESET_TX);
					startService(serverService);
				} else {
					mTXTotal.setText("0");
					editor.putLong("tx_total", 0);
					editor.commit();
				}
			}
		});

		mRXReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mOnOff) {
					Intent serverService = new Intent(MainActivity.this,
							InternetService.class);
					serverService.setAction(InternetService.ACTION_RESET_RX);
					startService(serverService);
				} else {
					mRXTotal.setText("0");
					editor.putLong("rx_total", 0);
					editor.commit();
				}
			}
		});

		try {
			mInterval.setProgress(settings.getInt("interval", 2));
		} catch (Exception e) {
			mInterval.setProgress(2);
		}
		float tx_total = settings.getLong("tx_total", 0) / 1000000.0f;
		tx_total = (float) Math.round(tx_total * 100) / 100;
		float rx_total = settings.getLong("rx_total", 0) / 1000000.0f;
		rx_total = (float) Math.round(rx_total * 100) / 100;

		mTXTotal.setText(Float.toString(tx_total));
		mRXTotal.setText(Float.toString(rx_total));

		mOnOff = settings.getString("onoff", getString(R.string.onoff_default))
				.equals("on");
		if (mOnOff) {
			mInterval.setEnabled(false);
			mOnOffButton.setChecked(true);
			editor.putString("onoff", "on");
			editor.commit();
			startServer();
		} else {
			mInterval.setEnabled(true);
			mOnOffButton.setChecked(false);
			editor.putString("onoff", "off");
			editor.commit();
			stopServer();
		}
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

	private String getNetworkInfoName(int type) {
		switch (type) {
		case ConnectivityManager.TYPE_MOBILE:
			return getString(R.string.mobile_label);
		case ConnectivityManager.TYPE_WIFI:
			return getString(R.string.wifi_label);
		}
		return null;
	}

	private String getNetworkInfoState(NetworkInfo.State state) {
		switch (state) {
		case CONNECTING:
			return getString(R.string.type_0_label);
		case CONNECTED:
			return getString(R.string.type_1_label);
		case SUSPENDED:
			return getString(R.string.type_2_label);
		case DISCONNECTING:
			return getString(R.string.type_3_label);
		case DISCONNECTED:
			return getString(R.string.type_4_label);
		case UNKNOWN:
			return getString(R.string.type_5_label);
		}
		return null;
	}

	private String getNetworkInfoIsRoaming(boolean isRoaming) {
		if (isRoaming) {
			return getString(R.string.true_label);
		} else {
			return getString(R.string.false_label);
		}
	}

	// pro
	private void setNetworkInfo(NetworkInfo info) {
		if (info == null) {
			clearNetworkInfo();
			return;
		}
		String name = getNetworkInfoName(info.getType());
		String state = getNetworkInfoState(info.getState());
		String isRoaming = getNetworkInfoIsRoaming(info.isRoaming());

		mNtwType.setText(name
				+ ((info.getSubtypeName() == null) ? "" : ("["
						+ info.getSubtypeName() + "]")));
		mNtwState.setText(state);
		mNtwRoaming.setText(isRoaming);
	}

	// pro
	private void clearNetworkInfo() {
		mNtwType.setText(getString(R.string.ntw_none_label));
		mNtwState.setText(getString(R.string.ntw_none_label));
		mNtwRoaming.setText(getString(R.string.ntw_none_label));

		// stat
		mTX.setText("0");
		mRX.setText("0");
		mTXTotal.setText("0");
		mRXTotal.setText("0");

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putLong("tx_total", 0);
		editor.putLong("rx_total", 0);
		editor.commit();
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
			} else if (intent.getAction().equals(InternetService.ACTION_STAT)) {
				if (intent.getBooleanExtra("support", false) == false) {
					mTX.setText(R.string.stat_unsupport);
					mRX.setText(R.string.stat_unsupport);
					mTXTotal.setText(R.string.stat_unsupport);
					mRXTotal.setText(R.string.stat_unsupport);
				} else {
					mTX.setText(Long.toString(intent.getLongExtra("tx", 0)));
					mRX.setText(Long.toString(intent.getLongExtra("rx", 0)));

					float tx_total = intent.getLongExtra("tx_total", 0) / 1000000.0f;
					tx_total = (float) Math.round(tx_total * 100) / 100;
					float rx_total = intent.getLongExtra("rx_total", 0) / 1000000.0f;
					rx_total = (float) Math.round(rx_total * 100) / 100;

					mTXTotal.setText(Float.toString(tx_total));
					mRXTotal.setText(Float.toString(rx_total));
				}
			}
		}
	};

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if (arg1) {
			mOnOff = true;
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putInt("interval", mInterval.getProgress());
			editor.putString("onoff", "on");
			editor.commit();

			mInterval.setEnabled(false);
			startServer();
		} else {
			mOnOff = false;
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putString("onoff", "off");
			editor.commit();

			mInterval.setEnabled(true);
			stopServer();
		}

	}

}
