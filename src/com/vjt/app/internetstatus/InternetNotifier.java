package com.vjt.app.internetstatus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InternetNotifier extends BroadcastReceiver {

	private static final String TAG = "InternetNotifier";
	private final int NOTIFICATIONID = 7696;

	@Override
	public void onReceive(Context context, Intent intent) {
		// Log.d(TAG, "Action is: " + intent.getAction());
		if (intent.getAction().equals(InternetService.ACTION_ONLINE)) {
			setupNotification(context, InternetService.STATUS_ON);
		} else if (intent.getAction().equals(InternetService.ACTION_OFFLINE)) {
			setupNotification(context, InternetService.STATUS_OFF);
		} else if (intent.getAction().equals(InternetService.ACTION_BAD)) {
			setupNotification(context, InternetService.STATUS_BAD);
		} else if (intent.getAction().equals(InternetService.ACTION_STOPPED)) {
			clearNotification(context);
		}

	}

	private void setupNotification(Context context, int status) {
		String ns = Context.NOTIFICATION_SERVICE;
		int icon, status_label;

		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);

		switch (status) {
		case InternetService.STATUS_ON:
			icon = R.drawable.online;
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

		Notification noti = new Notification.Builder(context)
				.setContentTitle(context.getString(R.string.status_title_label))
				.setContentIntent(pIntent)
				.setContentText(context.getString(status_label))
				.setSmallIcon(icon).setAutoCancel(false).build();
		noti.flags = Notification.FLAG_NO_CLEAR;
		nm.notify(NOTIFICATIONID, noti);

	}

	private void clearNotification(Context context) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);
		nm.cancelAll();
	}

}
