package com.vjt.app.internetstatus;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.vjt.app.internetstatus.NetworkConnectivityListener.State;

public class InternetService extends Service {

	private static final String TAG = "InternetService";

	public static final String ACTION_STARTED = "com.vjt.app.internetstatus.STARTED";
	public static final String ACTION_STOPPED = "com.vjt.app.internetstatus.STOPPED";
	public static final String ACTION_ONLINE = "com.vjt.app.internetstatus.ONLINE";
	public static final String ACTION_OFFLINE = "com.vjt.app.internetstatus.OFFLINE";
	public static final String ACTION_BAD = "com.vjt.app.internetstatus.BAD";

	// stat
	public static final String ACTION_STAT = "com.vjt.app.internetstatus.STAT";
	public static final String ACTION_RESET_TX = "com.vjt.app.internetstatus.RESET_TX";
	public static final String ACTION_RESET_RX = "com.vjt.app.internetstatus.RESET_RX";
	public static final String ACTION_RESET_TXWF = "com.vjt.app.internetstatus.RESET_TXWF";
	public static final String ACTION_RESET_RXWF = "com.vjt.app.internetstatus.RESET_RXWF";

	public static final String ACTION_SCREEN_ON = "screen_on";
	public static final String ACTION_SCREEN_OFF = "screen_off";

	private static final int STATUS_NONE = 0;
	private static final int STATUS_ON = 1;
	private static final int STATUS_OFF = 2;
	private static final int STATUS_BAD = 3;

	private static final int STATE_NONE = 0;
	private static final int STATE_WAITING = 1;

	private static final int MSG_CHECK_TIMEOUT = 1;
	// pro
	private static final int MSG_NETWORK_CHANGED = 2;
	// stat
	private static final int MSG_NET_STAT = 3;
	private static final int TRAFFIC_NONE = 0;
	private static final int TRAFFIC_LOW = 1;
	private static final int TRAFFIC_HIGH = 2;

	private static final int NET_STAT_INTERVAL = 1000;
	private static final int NET_STAT_HIGH_THRESHOLD = 1024 * 512;

	private static final int NOTIFICATIONID = 7696;
	public static final int ALERTUPID = 7697;
	public static final int ALERTDOWNID = 7698;
	public static final int ALERTUPWFID = 7699;
	public static final int ALERTDOWNWFID = 7700;

	private static int serviceStatus = STATUS_NONE;
	private static int serviceState = STATE_NONE;
	private static State mConnectivityState = State.UNKNOWN;
	private static boolean isThisTimeBad;

	private final Handler mHandler = new MainHandler(this);
	private static int mInterval;
	private static String mURL;
	private static boolean mOnOff;
	private Notification mNoti = new Notification();

	// pro
	public static NetworkConnectivityListener mNetworkConnectivityListener;

	// stat
	private static long mTxTotal;
	private static long mRxTotal;
	private static long mTxSec;
	private static long mRxSec;
	private static int mTrafficStatus;
	private static boolean mSupport = true;
	private static long mTXTotal;
	private static long mRXTotal;

	private static long mTxMBTotal;
	private static long mRxMBTotal;
	private static long mTxWFTotal;
	private static long mRxWFTotal;
	private static long mTXWFTotal;
	private static long mRXWFTotal;

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

	private void setupNotification(Context context, int status) {
		String ns = Context.NOTIFICATION_SERVICE;
		int icon, status_label;

		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);

		switch (status) {
		case InternetService.STATUS_ON:
			if (mTrafficStatus == TRAFFIC_HIGH) {
				icon = R.drawable.hightraffic;
			} else if (mTrafficStatus == TRAFFIC_LOW) {
				icon = R.drawable.traffic;
			} else {
				icon = R.drawable.online;
			}
			status_label = R.string.status_online_label;
			break;
		case InternetService.STATUS_OFF:
			icon = R.drawable.offline;
			status_label = R.string.status_offline_label;
			break;
		case InternetService.STATUS_BAD:
			icon = R.drawable.bad;
			status_label = R.string.status_bad_label;
			break;
		default:
			icon = R.drawable.offline;
			status_label = R.string.status_offline_label;
			break;

		}

		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pIntent = PendingIntent
				.getActivity(context, 0, intent, 0);

		if (Build.VERSION.SDK_INT >= 16) {
			Notification.Builder builder = new Notification.Builder(this);

			mNoti = builder
					.setContentTitle(
							context.getString(R.string.status_title_label))
					.setContentIntent(pIntent)
					.setContentText(context.getString(status_label))
					.setSmallIcon(icon).setAutoCancel(false)
					.setPriority(Notification.PRIORITY_HIGH).build();
		} else {
			long when = System.currentTimeMillis();
			CharSequence contentTitle = context
					.getString(R.string.status_title_label);
			CharSequence text = context.getString(R.string.app_name);
			CharSequence contentText = context.getString(status_label);

			mNoti.icon = icon;
			mNoti.when = when;
			mNoti.tickerText = text;
			mNoti.setLatestEventInfo(this, contentTitle, contentText, pIntent);
		}
		mNoti.flags |= Notification.FLAG_NO_CLEAR;
		mNoti.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		nm.notify(NOTIFICATIONID, mNoti);
		startForeground(NOTIFICATIONID, mNoti);
	}

	private void clearNotification(Context context) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);
		nm.cancelAll();
	}

	private int getVibrator() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		int f = (settings.getInt("settings_vibrator", 1));
		switch (f) {
		case 0:
			return Integer.parseInt(getString(R.string.vibration_0_default));
		case 1:
			return Integer.parseInt(getString(R.string.vibration_1_default));
		case 2:
			return Integer.parseInt(getString(R.string.vibration_2_default));
		case 3:
			return Integer.parseInt(getString(R.string.vibration_3_default));
		case 4:
			return Integer.parseInt(getString(R.string.vibration_4_default));
		}
		return 0;
	}

	private int getLight() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		int f = (settings.getInt("settings_light", 1));
		switch (f) {
		case 0:
			return Integer.parseInt(getString(R.string.light_0_default));
		case 1:
			return Integer.parseInt(getString(R.string.light_1_default));
		case 2:
			return Integer.parseInt(getString(R.string.light_2_default));
		case 3:
			return Integer.parseInt(getString(R.string.light_3_default));
		case 4:
			return Integer.parseInt(getString(R.string.light_4_default));
		}
		return 0;
	}

	private int getSound() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		return settings.getInt("settings_sound", 0);
	}

	private void setupAlert(boolean isUp, int limit) {
		String ns = Context.NOTIFICATION_SERVICE;
		int icon;
		String status_label;

		NotificationManager nm = (NotificationManager) getSystemService(ns);
		if (isUp) {
			icon = R.drawable.limit_up;
			status_label = String.format(
					getResources().getString(
							R.string.stat_limit_up_exceed_label),
					Integer.toString(limit));
		} else {
			icon = R.drawable.limit_down;
			status_label = String.format(
					getResources().getString(
							R.string.stat_limit_down_exceed_label),
					Integer.toString(limit));
		}

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		if (Build.VERSION.SDK_INT >= 16) {
			Notification.Builder builder = new Notification.Builder(this);

			int vibrator = getVibrator();
			if (vibrator > 0) {
				builder.setVibrate(new long[] { 0, vibrator });
			}
			int light = getLight();
			if (light > 0) {
				builder.setLights(Color.RED, light, light).build();
			}
			if (getSound() > 0) {
				Uri uri = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				builder.setSound(uri).build();
			}
			mNoti = builder
					.setContentTitle(getString(R.string.stat_limit_alert))
					.setContentIntent(pIntent).setContentText(status_label)
					.setSmallIcon(icon).setAutoCancel(false)
					.setPriority(Notification.PRIORITY_HIGH).build();
		} else {
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.stat_limit_alert);
			CharSequence text = status_label;
			CharSequence contentText = status_label;

			mNoti.icon = icon;
			mNoti.when = when;
			mNoti.tickerText = text;
			if (getVibrator() > 0) {
				mNoti.defaults |= Notification.DEFAULT_VIBRATE;
			}
			if (getLight() > 0) {
				mNoti.defaults |= Notification.DEFAULT_LIGHTS;
			}
			if (getSound() > 0) {
				mNoti.defaults |= Notification.DEFAULT_SOUND;
			}
			mNoti.setLatestEventInfo(this, contentTitle, contentText, pIntent);
		}
		mNoti.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		mNoti.flags |= Notification.FLAG_AUTO_CANCEL;
		if (isUp) {
			nm.notify(ALERTUPID, mNoti);
		} else {
			nm.notify(ALERTDOWNID, mNoti);
		}
	}

	private void setupAlertWF(boolean isUp, int limit) {
		String ns = Context.NOTIFICATION_SERVICE;
		int icon;
		String status_label;

		NotificationManager nm = (NotificationManager) getSystemService(ns);
		if (isUp) {
			icon = R.drawable.limit_up;
			status_label = String.format(
					getResources().getString(
							R.string.stat_wf_limit_up_exceed_label),
					Integer.toString(limit));
		} else {
			icon = R.drawable.limit_down;
			status_label = String.format(
					getResources().getString(
							R.string.stat_wf_limit_down_exceed_label),
					Integer.toString(limit));
		}

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		if (Build.VERSION.SDK_INT >= 16) {
			Notification.Builder builder = new Notification.Builder(this);

			int vibrator = getVibrator();
			if (vibrator > 0) {
				builder.setVibrate(new long[] { 0, vibrator });
			}
			int light = getLight();
			if (light > 0) {
				builder.setLights(Color.RED, light, light).build();
			}
			if (getSound() > 0) {
				Uri uri = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				builder.setSound(uri).build();
			}
			mNoti = builder
					.setContentTitle(getString(R.string.stat_limit_alert))
					.setContentIntent(pIntent).setContentText(status_label)
					.setSmallIcon(icon).setAutoCancel(false)
					.setPriority(Notification.PRIORITY_HIGH).build();
		} else {
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.stat_limit_alert);
			CharSequence text = status_label;
			CharSequence contentText = status_label;

			mNoti.icon = icon;
			mNoti.when = when;
			mNoti.tickerText = text;
			if (getVibrator() > 0) {
				mNoti.defaults |= Notification.DEFAULT_VIBRATE;
			}
			if (getLight() > 0) {
				mNoti.defaults |= Notification.DEFAULT_LIGHTS;
			}
			if (getSound() > 0) {
				mNoti.defaults |= Notification.DEFAULT_SOUND;
			}
			mNoti.setLatestEventInfo(this, contentTitle, contentText, pIntent);
		}
		mNoti.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		mNoti.flags |= Notification.FLAG_AUTO_CANCEL;
		if (isUp) {
			nm.notify(ALERTUPWFID, mNoti);
		} else {
			nm.notify(ALERTDOWNWFID, mNoti);
		}
	}

	public class NetTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			InetAddress addr = null;
			try {
				synchronized (this) {
					serviceState = STATE_WAITING;
					mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIMEOUT, Integer
							.valueOf(getString(R.string.bad_interval_default)));

					addr = InetAddress.getByName(params[0]);
					serviceState = STATE_NONE;
				}
			} catch (UnknownHostException e) {
				return null;
			} catch (Exception e) {
				return null;
			}
			return addr.getHostAddress();
		}

		@Override
		protected void onPostExecute(String netAddress) {
			if (netAddress == null) {
				if (serviceStatus != STATUS_OFF)
					setupNotification(InternetService.this,
							InternetService.STATUS_OFF);
				serviceStatus = STATUS_OFF;
				sendBroadcast(new Intent(ACTION_OFFLINE));
				LogUtil.d(TAG, "Offline !!!");
			} else {
				if (!isThisTimeBad) {
					if (serviceStatus != STATUS_ON)
						setupNotification(InternetService.this,
								InternetService.STATUS_ON);
					serviceStatus = STATUS_ON;
				}
				LogUtil.d(TAG, netAddress);
			}
			setWatchdog(mInterval * 1000);
		}
	}

	BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				return;

			Intent serviceIntent = new Intent(context, InternetService.class);

			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				LogUtil.d(TAG, "Receive Screen on");
				serviceIntent.setAction(ACTION_SCREEN_ON);
				startService(serviceIntent);
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				LogUtil.d(TAG, "Receive Screen off");
				serviceIntent.setAction(ACTION_SCREEN_OFF);
				startService(serviceIntent);
			}

		}
	};

	@Override
	public void onCreate() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(receiver, filter);

		// pro
		mNetworkConnectivityListener = new NetworkConnectivityListener();
		mNetworkConnectivityListener.registerHandler(mHandler,
				MSG_NETWORK_CHANGED);
		mNetworkConnectivityListener.startListening(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		cancelWatchdog();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		mOnOff = settings.getString("onoff", "off").equals("on");

		if (!pm.isScreenOn() || mConnectivityState != State.CONNECTED) {
			resetStatus();
			return START_REDELIVER_INTENT;
		}

		if (!mOnOff) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		if (intent.getAction() == null
				|| intent.getAction().equals(ACTION_SCREEN_ON)) {

			int f = (settings.getInt("interval", 2));
			switch (f) {
			case 0:
				mInterval = Integer
						.parseInt(getString(R.string.interval_0_default));
				break;
			case 1:
				mInterval = Integer
						.parseInt(getString(R.string.interval_1_default));
				break;
			case 2:
				mInterval = Integer
						.parseInt(getString(R.string.interval_2_default));
				break;
			case 3:
				mInterval = Integer
						.parseInt(getString(R.string.interval_3_default));
				break;
			case 4:
				mInterval = Integer
						.parseInt(getString(R.string.interval_4_default));
				break;
			}

			mURL = settings.getString("url", getString(R.string.url_default));
			mTXTotal = settings.getLong("tx_total", 0);
			mRXTotal = settings.getLong("rx_total", 0);
			mTXWFTotal = settings.getLong("txwf_total", 0);
			mRXWFTotal = settings.getLong("rxwf_total", 0);
			LogUtil.i(TAG, "onStartCommand");
			LogUtil.i(TAG, "mRXTotal = " + mRXTotal);
			LogUtil.i(TAG, "mTXTotal = " + mTXTotal);
			LogUtil.i(TAG, "mRXWFTotal = " + mRXWFTotal);
			LogUtil.i(TAG, "mTXWFTotal = " + mTXWFTotal);

			isThisTimeBad = false;
			doCheck();
		} else if (intent.getAction().equals(ACTION_STOPPED)) {
			stopSelf(startId);
			return START_NOT_STICKY;
		} else if (intent.getAction().equals(ACTION_SCREEN_OFF)) {
			resetStatus();
			return START_REDELIVER_INTENT;
		} else if (intent.getAction().equals(ACTION_RESET_TX)) {
			resetStatTx();
			if (serviceStatus == STATUS_NONE) {
				stopSelf(startId);
				return START_NOT_STICKY;
			} else {
				return START_REDELIVER_INTENT;
			}
		} else if (intent.getAction().equals(ACTION_RESET_RX)) {
			resetStatRx();
			if (serviceStatus == STATUS_NONE) {
				stopSelf(startId);
				return START_NOT_STICKY;
			} else {
				return START_REDELIVER_INTENT;
			}
		} else if (intent.getAction().equals(ACTION_RESET_TXWF)) {
			resetStatTxWF();
			if (serviceStatus == STATUS_NONE) {
				stopSelf(startId);
				return START_NOT_STICKY;
			} else {
				return START_REDELIVER_INTENT;
			}
		} else if (intent.getAction().equals(ACTION_RESET_RXWF)) {
			resetStatRxWF();
			if (serviceStatus == STATUS_NONE) {
				stopSelf(startId);
				return START_NOT_STICKY;
			} else {
				return START_REDELIVER_INTENT;
			}
		}

		if (mSupport) {
			doStat(true, intent.getAction() != null);
		} else {
			Intent i = new Intent(ACTION_STAT);
			i.putExtra("support", false);
			sendBroadcast(i);
		}
		sendBroadcast(new Intent(ACTION_STARTED));
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

	private void doBadCheck(Context service) {
		if (serviceState == STATE_WAITING && serviceStatus == STATUS_ON) {
			if (serviceStatus != STATUS_BAD && service != null)
				setupNotification(this, InternetService.STATUS_BAD);
			serviceStatus = STATUS_BAD;
			isThisTimeBad = true;
			LogUtil.d(TAG, "Bad connection !!!");
		}
	}

	private void sendStat() {
		Intent i = new Intent(ACTION_STAT);
		i.putExtra("rx", mRxSec);
		i.putExtra("tx", mTxSec);
		i.putExtra("rx_total", mRXTotal);
		i.putExtra("tx_total", mTXTotal);
		i.putExtra("rxwf_total", mRXWFTotal);
		i.putExtra("txwf_total", mTXWFTotal);
		i.putExtra("support", mSupport);
		sendBroadcast(i);
		saveData();
	}

	private void resetStatTx() {
		mTXTotal = 0;
		sendStat();
	}

	private void resetStatRx() {
		mRXTotal = 0;
		sendStat();
	}

	private void resetStatTxWF() {
		mTXWFTotal = 0;
		sendStat();
	}

	private void resetStatRxWF() {
		mRXWFTotal = 0;
		sendStat();
	}

	private void resetStatus() {
		serviceStatus = STATUS_NONE;
		mTrafficStatus = TRAFFIC_NONE;
		cancelWatchdog();
		mHandler.removeMessages(MSG_CHECK_TIMEOUT);
		mHandler.removeMessages(MSG_NET_STAT);
	}

	private void doCheck() {
		try {

			new NetTask().execute(mURL);

		} catch (Exception e) {
			if (serviceStatus != STATUS_OFF)
				setupNotification(InternetService.this,
						InternetService.STATUS_OFF);
			serviceStatus = STATUS_OFF;
			sendBroadcast(new Intent(ACTION_OFFLINE));
			LogUtil.d(TAG, "Offline !!!");
		}
	}

	private void saveData() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = settings.edit();

		editor.putLong("tx_total", mTXTotal);
		editor.putLong("rx_total", mRXTotal);
		editor.putLong("txwf_total", mTXWFTotal);
		editor.putLong("rxwf_total", mRXWFTotal);
		editor.commit();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// pro
		mNetworkConnectivityListener.unregisterHandler(mHandler);
		mNetworkConnectivityListener.stopListening();
		mNetworkConnectivityListener = null;
		sendBroadcast(new Intent(ACTION_STOPPED));

		saveData();
		resetStatus();
		unregisterReceiver(receiver);
		clearNotification(this);
		stopForeground(true);
	}

	private void handleNetworkChange() {
		if (mConnectivityState == State.CONNECTED) {
			Intent serverService = new Intent(this, InternetService.class);
			startService(serverService);
		} else {
			if (serviceStatus != STATUS_OFF)
				setupNotification(InternetService.this,
						InternetService.STATUS_OFF);
			serviceStatus = STATUS_OFF;
			sendBroadcast(new Intent(ACTION_OFFLINE));
			LogUtil.d(TAG, "Offline !!!");

			resetStatus();
		}
	}

	// stat
	private static void getTxRX() {
		mTxTotal = TrafficStats.getTotalTxBytes();
		mRxTotal = TrafficStats.getTotalRxBytes();
		mTxMBTotal = TrafficStats.getMobileTxBytes();
		mRxMBTotal = TrafficStats.getMobileRxBytes();
		mTxWFTotal = (mTxTotal - mTxMBTotal < 0) ? 0 : mTxTotal - mTxMBTotal;
		mRxWFTotal = (mRxTotal - mRxMBTotal < 0) ? 0 : mRxTotal - mRxMBTotal;

		LogUtil.i(TAG, "TX = " + mTxTotal);
		LogUtil.i(TAG, "RX = " + mRxTotal);
		LogUtil.i(TAG, "TXMB = " + mTxMBTotal);
		LogUtil.i(TAG, "RXMB = " + mRxMBTotal);
		LogUtil.i(TAG, "TXWF = " + mTxWFTotal);
		LogUtil.i(TAG, "RXWF = " + mRxWFTotal);
	}

	private void limitCheck() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		boolean fire_up = settings.getBoolean("fire_up", false);
		int limit_up = settings.getInt("limit_up", 0);
		if (!fire_up && limit_up > 0 && mTXTotal >= limit_up * 1000000) {
			setupAlert(true, limit_up);

			final SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("fire_up", true);
			editor.commit();
		}

		boolean fire_down = settings.getBoolean("fire_down", false);
		int limit_down = settings.getInt("limit_down", 0);
		if (!fire_down && limit_down > 0 && mRXTotal >= limit_down * 1000000) {
			setupAlert(false, limit_down);

			final SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("fire_down", true);
			editor.commit();
		}

		boolean fire_up_wf = settings.getBoolean("fire_up_wf", false);
		int limit_up_wf = settings.getInt("limit_up_wf", 0);
		if (!fire_up_wf && limit_up_wf > 0
				&& mTXWFTotal >= limit_up_wf * 1000000) {
			setupAlertWF(true, limit_up_wf);

			final SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("fire_up_wf", true);
			editor.commit();
		}

		boolean fire_down_wf = settings.getBoolean("fire_down_wf", false);
		int limit_down_wf = settings.getInt("limit_down_wf", 0);
		if (!fire_down_wf && limit_down_wf > 0
				&& mRXWFTotal >= limit_down_wf * 1000000) {
			setupAlertWF(false, limit_down);

			final SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("fire_down_wf", true);
			editor.commit();
		}
	}

	private void doStat(boolean isFirst, boolean countOldData) {

		long rxTotal = mRxTotal;
		long txTotal = mTxTotal;
		long rxMBTotal = mRxMBTotal;
		long txMBTotal = mTxMBTotal;
		long rxWFTotal = mRxWFTotal;
		long txWFTotal = mTxWFTotal;

		getTxRX();
		if (isFirst && mRxTotal > 0 && mRxTotal > 0) {
			if (countOldData && (rxTotal > 0 || txTotal > 0)) {
				long delta = 0;

				if (mNetworkConnectivityListener != null) {
					if (mNetworkConnectivityListener.getNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI) {
						delta = mRxWFTotal - rxWFTotal < 0 ? 0 : mRxWFTotal
								- rxWFTotal;
						mRXWFTotal += delta;
						delta = mTxWFTotal - txWFTotal < 0 ? 0 : mTxWFTotal
								- txWFTotal;
						mTXWFTotal += delta;
					} else {
						delta = mRxMBTotal - rxMBTotal < 0 ? 0 : mRxMBTotal
								- rxMBTotal;
						mRXTotal += delta;
						delta = mTxMBTotal - txMBTotal < 0 ? 0 : mTxMBTotal
								- txMBTotal;
						mTXTotal += delta;
					}
					limitCheck();
					Intent i = new Intent(ACTION_STAT);
					i.putExtra("rx", mRxSec);
					i.putExtra("tx", mTxSec);
					i.putExtra("rx_total", mRXTotal);
					i.putExtra("tx_total", mTXTotal);
					i.putExtra("rxwf_total", mRXWFTotal);
					i.putExtra("txwf_total", mTXWFTotal);
					i.putExtra("support", mSupport);
					sendBroadcast(i);
					saveData();
				}
			}
			if (mHandler.hasMessages(MSG_NET_STAT))
				return;
		}

		if (mRxTotal < 0 || mTxTotal < 0) {
			mSupport = false;
			Intent i = new Intent(ACTION_STAT);
			i.putExtra("support", false);
			sendBroadcast(i);
			return;
		} else {
			mSupport = true;
		}

		if (!isFirst) {
			mRxSec = mRxTotal - rxTotal;
			mTxSec = mTxTotal - txTotal;
			long delta = 0;

			if (mNetworkConnectivityListener != null) {
				if (mNetworkConnectivityListener.getNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI) {
					delta = mRxWFTotal - rxWFTotal < 0 ? 0 : mRxWFTotal
							- rxWFTotal;
					mRXWFTotal += delta;
					delta = mTxWFTotal - txWFTotal < 0 ? 0 : mTxWFTotal
							- txWFTotal;
					mTXWFTotal += delta;
				} else {
					delta = mRxMBTotal - rxMBTotal < 0 ? 0 : mRxMBTotal
							- rxMBTotal;
					mRXTotal += delta;
					delta = mTxMBTotal - txMBTotal < 0 ? 0 : mTxMBTotal
							- txMBTotal;
					mTXTotal += delta;
				}
				limitCheck();
				Intent i = new Intent(ACTION_STAT);
				i.putExtra("rx", mRxSec);
				i.putExtra("tx", mTxSec);
				i.putExtra("rx_total", mRXTotal);
				i.putExtra("tx_total", mTXTotal);
				i.putExtra("rxwf_total", mRXWFTotal);
				i.putExtra("txwf_total", mTXWFTotal);
				i.putExtra("support", mSupport);
				sendBroadcast(i);
				saveData();
			}

			int oldTrafficStatus = mTrafficStatus;

			if (mRxSec == 0 && mTxSec == 0) {
				mTrafficStatus = TRAFFIC_NONE;
			} else if (mRxSec >= NET_STAT_HIGH_THRESHOLD
					|| mTxSec >= NET_STAT_HIGH_THRESHOLD) {
				mTrafficStatus = TRAFFIC_HIGH;
			} else {
				mTrafficStatus = TRAFFIC_LOW;
			}

			if (oldTrafficStatus != mTrafficStatus
					&& serviceStatus == STATUS_ON) {
				setupNotification(InternetService.this,
						InternetService.STATUS_ON);
			}
		}
		mHandler.sendEmptyMessageDelayed(MSG_NET_STAT, NET_STAT_INTERVAL);
	}

	private class MainHandler extends Handler {
		private final WeakReference<InternetService> mService;

		MainHandler(InternetService service) {
			mService = new WeakReference<InternetService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			InternetService service = mService.get();

			switch (msg.what) {
			case MSG_CHECK_TIMEOUT:
				doBadCheck(service);
				break;
			// pro
			case MSG_NETWORK_CHANGED:
				if (mNetworkConnectivityListener != null) {
					mConnectivityState = mNetworkConnectivityListener
							.getState();
					handleNetworkChange();
				}
				break;
			// stat
			case MSG_NET_STAT:
				doStat(false, true);
				break;
			}
		}
	}

}
