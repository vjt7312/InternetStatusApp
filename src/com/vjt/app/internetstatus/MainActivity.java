package com.vjt.app.internetstatus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	private static final String MY_AD_UNIT_ID = "a152e3523d2367b";

	private ToggleButton mOnOffButton;
	private EditText mURL;
	private EditText mInterval;

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
		mURL = (EditText) findViewById(R.id.url);

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
		startService(serverService);
	}

	private void stopServer() {
		Intent serverService = new Intent(this, InternetService.class);
		serverService.setAction(InternetService.ACTION_STOPPED);
		startService(serverService);
	}

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
			sendBroadcast(new Intent(InternetService.ACTION_STOPPED));
			stopServer();
		}

	}

	@Override
	public void onDestroy() {
		adView.destroy();
		super.onDestroy();
	}

}
