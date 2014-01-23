package com.vjt.app.internetstatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class InternetService extends Service implements Callback {

	private static final String TAG = "InternetService";

	static public final String ACTION_STARTED = "com.vjt.app.internetstatus.STARTED";
	static public final String ACTION_STOPPED = "com.vjt.app.internetstatus.STOPPED";
	static public final String ACTION_ONLINE = "com.vjt.app.internetstatus.ONLINE";
	static public final String ACTION_OFFLINE = "com.vjt.app.internetstatus.OFFLINE";

	static public final String ACTION_SCREEN_ON = "screen_on";
	static public final String ACTION_SCREEN_OFF = "screen_off";

	private static final int MSG_CHECK_TIMEOUT = 1;

	private static final int STATUS_STOP = 0;
	private static final int STATUS_ON = 1;
	private static final int STATUS_OFF = 2;

	private static int serviceStatus;
	private Handler mHandler = new Handler(this);
	private static int mInterval;
	private static String mURL;

	private final IBinder binder = new InternetServiceBinder();

	public class InternetServiceBinder extends Binder {

		/**
		 * Gets the service.
		 * 
		 * @return the service
		 */
		public InternetServiceBinder getService() {
			return InternetServiceBinder.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public class NetTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName(params[0]);
			}

			catch (UnknownHostException e) {
				return null;
			}
			return addr.getHostAddress();
		}
	}

	BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				return;

			Intent serviceIntent = new Intent(context, InternetService.class);

			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				Log.d(TAG, "Receive Screen on");
				serviceIntent.setAction(ACTION_SCREEN_ON);
				startService(serviceIntent);
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Log.d(TAG, "Receive Screen off");
				serviceIntent.setAction(ACTION_SCREEN_OFF);
				startService(serviceIntent);
			}

		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		sendBroadcast(new Intent(ACTION_STARTED));

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);

		registerReceiver(receiver, filter);

		if (intent.getAction() != null
				&& intent.getAction().equals(ACTION_SCREEN_OFF)) {
			mHandler.removeMessages(MSG_CHECK_TIMEOUT);
			return START_REDELIVER_INTENT;
		} else {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);

			mInterval = Integer.valueOf(settings.getString("interval",
					getString(R.string.interval_default)));
			mURL = settings.getString("url", getString(R.string.url_default));
			doCheck();

			mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT,
					mInterval * 1000);

			return START_REDELIVER_INTENT;
		}
	}

	private void doCheck() {
		String netAddress = null;

		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		try {
			netAddress = new NetTask().execute(mURL).get();
			if (netAddress == null) {
				if (serviceStatus != STATUS_OFF)
					sendBroadcast(new Intent(ACTION_OFFLINE));
				serviceStatus = STATUS_OFF;
			} else {
				if (serviceStatus != STATUS_ON)
					sendBroadcast(new Intent(ACTION_ONLINE));
				serviceStatus = STATUS_ON;
				Log.d(TAG, netAddress);
			}
		} catch (Exception e) {
			if (serviceStatus != STATUS_OFF)
				sendBroadcast(new Intent(ACTION_OFFLINE));
			serviceStatus = STATUS_OFF;
		}
		mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT, mInterval * 1000);
	}

	@Override
	public void onDestroy() {
		serviceStatus = STATUS_STOP;
		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		sendBroadcast(new Intent(ACTION_STOPPED));
		unregisterReceiver(receiver);
	}

	public static boolean isRunning() {
		return serviceStatus != STATUS_STOP;
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_CHECK_TIMEOUT:
			doCheck();
			break;
		default:
			return false;
		}
		return true;
	}

}
