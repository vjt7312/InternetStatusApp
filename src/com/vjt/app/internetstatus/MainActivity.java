package com.vjt.app.internetstatus;

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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.vjt.app.internetstatus.NetworkConnectivityListener.State;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	private static final String TAG = "MainActivity";

	private static final String MY_AD_UNIT_ID = "a152e3523d2367b";

	ToggleButton mOnOffButton;
	// EditText mURL;
	EditText mInterval;
	// pro
	TextView mNtwType;
	TextView mNtwState;
	TextView mNtwRoaming;
	// stat
	TextView mTX;
	TextView mRX;

	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ---------------------------------------------------------------------
		// 建立 adView
		adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);

		// 查詢 LinearLayout (假設您已經提供)
		// 屬性是 android:id="@+id/mainLayout"
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainLayout);

		// 在其中加入 adView
		layout.addView(adView);

		// 請求測試廣告
		// AdRequest adRequest = new AdRequest();
		// adRequest.addTestDevice(AdRequest.TEST_EMULATOR); // 模擬工具
		// adRequest.addTestDevice("TEST_DEVICE_ID");
		// adView.loadAd(adRequest);

		// 啟用泛用請求，並隨廣告一起載入
		adView.loadAd(new AdRequest());
		// ---------------------------------------------------------------------

		mOnOffButton = (ToggleButton) findViewById(R.id.running_state_toogle_button);
		mOnOffButton.setOnCheckedChangeListener(this);

		mInterval = (EditText) findViewById(R.id.interval);
		// mURL = (EditText) findViewById(R.id.url);

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

		// mURL.setText(settings.getString("url",
		// getString(R.string.url_default)));

		if (settings.getString("onoff", getString(R.string.onoff_default))
				.equals("on")) {
			// mURL.setEnabled(false);
			mInterval.setEnabled(false);
			mOnOffButton.setChecked(true);
			editor.putString("onoff", "on");
			editor.commit();
			startServer();
		} else {
			// mURL.setEnabled(true);
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

		// mURL.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// String url = mURL.getText().toString();
		//
		// if (URLUtil.isValidUrl(url)) {
		// Toast.makeText(MainActivity.this,
		// R.string.url_validation_error, Toast.LENGTH_LONG)
		// .show();
		// return;
		// }
		// editor.putString("url", mURL.getText().toString());
		// editor.commit();
		// }
		// });
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
				} else {
					mTX.setText(Long.toString(intent.getLongExtra("tx", -1)));
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

			// editor.putString("url", mURL.getText().toString());
			editor.putString("interval", mInterval.getText().toString());
			editor.putString("onoff", "on");
			editor.commit();

			mInterval.setEnabled(false);
			// mURL.setEnabled(false);
			startServer();
		} else {
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = settings.edit();

			editor.putString("onoff", "off");
			editor.commit();

			mInterval.setEnabled(true);
			// mURL.setEnabled(true);
			stopServer();
		}

	}

	@Override
	public void onDestroy() {
		adView.destroy();
		super.onDestroy();
	}

}
