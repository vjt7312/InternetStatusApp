package com.vjt.app.internetstatus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class InternetService extends Service {

	private static final String TAG = "InternetService";

	static public final String ACTION_STARTED = "com.vjt.app.internetstatus.STARTED";
	static public final String ACTION_STOPPED = "com.vjt.app.internetstatus.STOPPED";
	static public final String ACTION_ONLINE = "com.vjt.app.internetstatus.ONLINE";
	static public final String ACTION_OFFLINE = "com.vjt.app.internetstatus.OFFLINE";
	static public final String ACTION_BAD = "com.vjt.app.internetstatus.BAD";

	static public final String ACTION_SCREEN_ON = "screen_on";
	static public final String ACTION_SCREEN_OFF = "screen_off";

	static public final int STATUS_NONE = 0;
	static public final int STATUS_ON = 1;
	static public final int STATUS_OFF = 2;
	static public final int STATUS_BAD = 3;

	static public final int STATE_NONE = 0;
	static public final int STATE_WAITING = 1;

	private static final int MSG_CHECK_TIMEOUT = 1;

	private static int serviceStatus = STATUS_NONE;
	private static int serviceState = STATE_NONE;
	private static boolean isThisTimeBad;

	private final Handler mHandler = new MainHandler();
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
		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		cancelWatchdog();

		if (intent.getAction() == null
				|| intent.getAction().equals(ACTION_SCREEN_ON)) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);

			mInterval = Integer.valueOf(settings.getString("interval",
					getString(R.string.interval_default)));
			mURL = settings.getString("url", getString(R.string.url_default));
			synchronized (this) {
				serviceState = STATE_WAITING;
				isThisTimeBad = false;
				mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT, Integer
						.valueOf(getString(R.string.bad_interval_default)));
				doCheck();
				serviceState = STATE_NONE;
			}
			setWatchdog(mInterval * 1000);
		} else if (intent.getAction().equals(ACTION_STOPPED)) {
			resetStatus();
		}
		// stopSelf(startId);
		return START_REDELIVER_INTENT;
	}

	PendingIntent createAlarmIntent() {
		Intent i = new Intent();
		i.setClass(this, InternetService.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}

	private void cancelWatchdog() {
		PendingIntent pi = createAlarmIntent();
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	private void setWatchdog(int delay) {
		PendingIntent pi = createAlarmIntent();
		long timeNow = SystemClock.elapsedRealtime();

		long nextCheckTime = timeNow + delay;
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextCheckTime, pi);
	}

	private void doBadCheck() {
		if (serviceState == STATE_WAITING) {
			if (serviceStatus != STATUS_BAD)
				sendBroadcast(new Intent(ACTION_BAD));
			serviceStatus = STATUS_BAD;
			isThisTimeBad = true;
			Log.d(TAG, "Bad connection !!!");
		}
	}

	private void resetStatus() {
		serviceStatus = STATUS_NONE;
		cancelWatchdog();
	}

	private void doCheck() {
		String netAddress = null;

		try {
			netAddress = new NetTask().execute(mURL).get();
			if (netAddress == null) {
				if (serviceStatus != STATUS_OFF)
					sendBroadcast(new Intent(ACTION_OFFLINE));
				serviceStatus = STATUS_OFF;
			} else {
				if (!isThisTimeBad) {
					if (serviceStatus != STATUS_ON)
						sendBroadcast(new Intent(ACTION_ONLINE));
					serviceStatus = STATUS_ON;
				}
				Log.d(TAG, netAddress);
			}
		} catch (Exception e) {
			if (serviceStatus != STATUS_OFF)
				sendBroadcast(new Intent(ACTION_OFFLINE));
			serviceStatus = STATUS_OFF;
		}
	}

	@Override
	public void onDestroy() {
		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		unregisterReceiver(receiver);
	}

	private class MainHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_CHECK_TIMEOUT:
				doBadCheck();
				break;
			}
		}
	}

}
