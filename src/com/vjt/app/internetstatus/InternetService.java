package com.vjt.app.internetstatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;

public class InternetService extends Service implements Callback {

	static public final String ACTION_STARTED = "com.vjt.app.internetstatus.STARTED";
	static public final String ACTION_STOPPED = "com.vjt.app.internetstatus.STOPPED";
	static public final String ACTION_ONLINE = "com.vjt.app.internetstatus.ONLINE";
	static public final String ACTION_OFFLINE = "com.vjt.app.internetstatus.OFFLINE";

	private static final int MSG_CHECK_TIMEOUT = 1;

	private static boolean isRunning = false;
	private Handler mHandler;
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		sendBroadcast(new Intent(ACTION_STARTED));
		isRunning = true;
		mHandler = new Handler(this);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		mInterval = Integer.valueOf(settings.getString("interval",
				getString(R.string.interval_default)));
		mURL = settings.getString("url", getString(R.string.url_default));
		doCheck();

		mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT, mInterval * 1000);

		return START_REDELIVER_INTENT;
	}

	private void doCheck() {
		String netAddress = null;

		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		try {
			netAddress = new NetTask().execute(mURL).get();
			if (netAddress == null) {
				sendBroadcast(new Intent(ACTION_OFFLINE));
			} else {
				sendBroadcast(new Intent(ACTION_ONLINE));
			}
		} catch (Exception e) {
			sendBroadcast(new Intent(ACTION_OFFLINE));
		}
		mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT, mInterval * 1000);
	}

	@Override
	public void onDestroy() {
		isRunning = false;
		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		sendBroadcast(new Intent(ACTION_STOPPED));
	}

	public static boolean isRunning() {
		return isRunning;
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
