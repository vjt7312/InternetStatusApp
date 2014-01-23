package com.vjt.app.internetstatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class InternetService extends Service {

	static public final String ACTION_STARTED = "com.vjt.app.internetstatus.STARTED";
	static public final String ACTION_STOPPED = "com.vjt.app.internetstatus.STOPPED";
	static public final String ACTION_ONLINE = "com.vjt.app.internetstatus.ONLINE";
	static public final String ACTION_OFFLINE = "com.vjt.app.internetstatus.OFFLINE";

	private static boolean isRunning = false;

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

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		String url = settings.getString("url", getString(R.string.url_default));
		String netAddress = null;

		try {
			netAddress = new NetTask().execute(url).get();
			if (netAddress == null) {
				sendBroadcast(new Intent(ACTION_OFFLINE));
			} else {
				sendBroadcast(new Intent(ACTION_ONLINE));
			}
		} catch (Exception e) {
			sendBroadcast(new Intent(ACTION_OFFLINE));
		}

		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		isRunning = false;
		sendBroadcast(new Intent(ACTION_STOPPED));
	}

	public static boolean isRunning() {
		return isRunning;
	}
}
