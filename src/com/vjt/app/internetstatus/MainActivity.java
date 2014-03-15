package com.vjt.app.internetstatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.vjt.app.internetstatus.NetworkConnectivityListener.State;

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
	TextView mTXLimit;
	TextView mRXLimit;
	Button mTXReset;
	Button mRXReset;
	Button mTXSetup;
	Button mRXSetup;
	RelativeLayout mTXLayout;
	RelativeLayout mRXLayout;

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
		mTXLimit = (TextView) findViewById(R.id.stat_tx_limit);
		mRXLimit = (TextView) findViewById(R.id.stat_rx_limit);
		mTXReset = (Button) findViewById(R.id.stat_tx_btn);
		mRXReset = (Button) findViewById(R.id.stat_rx_btn);
		mTXSetup = (Button) findViewById(R.id.stat_set_tx_btn);
		mRXSetup = (Button) findViewById(R.id.stat_set_rx_btn);
		mTXLayout = (RelativeLayout) findViewById(R.id.tx_layout);
		mRXLayout = (RelativeLayout) findViewById(R.id.rx_layout);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();

		mTXReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mOnOff) {
					editor.putBoolean("fire_up", false);
					editor.commit();

					Intent serverService = new Intent(MainActivity.this,
							InternetService.class);
					serverService.setAction(InternetService.ACTION_RESET_TX);
					startService(serverService);
				} else {
					mTXTotal.setText("0");
					editor.putLong("tx_total", 0);
					editor.putBoolean("fire_up", false);
					editor.commit();
					steStatLayoutBorder(true, false);
				}
			}
		});

		mRXReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mOnOff) {
					editor.putBoolean("fire_down", false);
					editor.commit();

					Intent serverService = new Intent(MainActivity.this,
							InternetService.class);
					serverService.setAction(InternetService.ACTION_RESET_RX);
					startService(serverService);
				} else {
					mRXTotal.setText("0");
					editor.putLong("rx_total", 0);
					editor.putBoolean("fire_down", false);
					editor.commit();
					steStatLayoutBorder(false, true);
				}
			}
		});

		mTXSetup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				LayoutInflater li = LayoutInflater.from(MainActivity.this);
				View promptsView = li.inflate(R.layout.limit, null);
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						MainActivity.this);
				alertDialogBuilder.setView(promptsView);
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.input_limit);
				alertDialogBuilder
						.setCancelable(false)
						.setPositiveButton(getString(R.string.stat_ok_label),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										int limit_up;
										try {
											limit_up = Integer
													.parseInt(userInput
															.getText()
															.toString()
															.replaceFirst(
																	"^0+(?!$)",
																	""));
										} catch (Exception e) {
											limit_up = 0;
										}
										SharedPreferences settings = PreferenceManager
												.getDefaultSharedPreferences(MainActivity.this);
										final SharedPreferences.Editor editor = settings
												.edit();
										editor.putInt("limit_up", limit_up);
										editor.putBoolean("fire_up", false);
										editor.commit();

										mTXLimit.setText(limit_up == 0 ? "----"
												: String.valueOf(limit_up));
										steStatLayoutBorder(true, false);
									}
								})
						.setNegativeButton(
								getString(R.string.stat_cancel_label),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});

				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}
		});

		mRXSetup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				LayoutInflater li = LayoutInflater.from(MainActivity.this);
				View promptsView = li.inflate(R.layout.limit, null);
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						MainActivity.this);
				alertDialogBuilder.setView(promptsView);
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.input_limit);
				alertDialogBuilder
						.setCancelable(false)
						.setPositiveButton(getString(R.string.stat_ok_label),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										int limit_down;
										try {
											limit_down = Integer
													.parseInt(userInput
															.getText()
															.toString()
															.replaceFirst(
																	"^0+(?!$)",
																	""));
										} catch (Exception e) {
											limit_down = 0;
										}
										SharedPreferences settings = PreferenceManager
												.getDefaultSharedPreferences(MainActivity.this);
										final SharedPreferences.Editor editor = settings
												.edit();
										editor.putInt("limit_down", limit_down);
										editor.putBoolean("fire_down", false);
										editor.commit();
										mRXLimit.setText(limit_down == 0 ? "----"
												: String.valueOf(limit_down));
										steStatLayoutBorder(false, true);

									}
								})
						.setNegativeButton(
								getString(R.string.stat_cancel_label),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});

				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
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

		int limit_up = settings.getInt("limit_up", 0);
		int limit_down = settings.getInt("limit_down", 0);

		mTXLimit.setText(limit_up == 0 ? "----" : Integer.toString(limit_up));
		mRXLimit.setText(limit_down == 0 ? "----" : Integer
				.toString(limit_down));
		steStatLayoutBorder(true, true);

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

	private void clearNetworkTotal() {
		LogUtil.i(TAG, "clearNetworkTotal");

		mTXTotal.setText("0");
		mRXTotal.setText("0");
		steStatLayoutBorder(true, true);

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putLong("tx_total", 0);
		editor.putLong("rx_total", 0);
		editor.commit();
	}

	// pro
	private void clearNetworkInfo() {
		LogUtil.i(TAG, "clearNetworkInfo");
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
					.equals(InternetService.ACTION_STOPPED)) {
				clearNetworkInfo();
				clearNetworkTotal();
			} else if (intent.getAction()
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
					steStatLayoutBorder(true, true);
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

	private void steStatLayoutBorder(boolean tx, boolean rx) {
		if (tx) {
			try {
				int txLimit = Integer.parseInt(mTXLimit.getText().toString());
				if (txLimit > 0) {
					float txTotal = Float.parseFloat(mTXTotal.getText()
							.toString());
					if (txTotal >= (float) txLimit) {
						mTXLayout
								.setBackgroundResource(R.layout.alert_background);
					} else {
						mTXLayout.setBackground(null);
					}
				}
			} catch (Exception e) {
				mTXLayout.setBackground(null);
			}
		}
		if (rx) {
			try {
				int rxLimit = Integer.parseInt(mRXLimit.getText().toString());
				if (rxLimit > 0) {
					float txTotal = Float.parseFloat(mRXTotal.getText()
							.toString());
					if (txTotal >= (float) rxLimit) {
						mRXLayout
								.setBackgroundResource(R.layout.alert_background);
					} else {
						mRXLayout.setBackground(null);
					}
				}
			} catch (Exception e) {
				mRXLayout.setBackground(null);
			}
		}

	}

}
