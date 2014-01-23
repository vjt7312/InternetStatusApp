package com.vjt.app.internetstatus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InternetNotifier extends BroadcastReceiver {

	private final int NOTIFICATIONID = 7696;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(InternetService.ACTION_ONLINE)) {
			setupNotification(context, true);
		} else if (intent.getAction().equals(InternetService.ACTION_OFFLINE)) {
			setupNotification(context, false);
		} else if (intent.getAction().equals(InternetService.ACTION_STOPPED)) {
			clearNotification(context);
		}

	}

	private void setupNotification(Context context, boolean online) {
		String ns = Context.NOTIFICATION_SERVICE;
		int icon, status;

		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);
		if (online) {
			icon = R.drawable.online;
			status = R.string.status_online_label;
		} else {
			icon = R.drawable.offline;
			status = R.string.status_offline_label;
		}

		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pIntent = PendingIntent
				.getActivity(context, 0, intent, 0);

		Notification noti = new Notification.Builder(context)
				.setContentTitle(context.getString(status))
				.setContentIntent(pIntent)
				.setContentText(context.getString(status)).setSmallIcon(icon)
				.setAutoCancel(false).build();
		noti.flags = Notification.FLAG_NO_CLEAR ;
		nm.notify(NOTIFICATIONID, noti);

	}

	private void clearNotification(Context context) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) context
				.getSystemService(ns);
		nm.cancelAll();
	}

}
