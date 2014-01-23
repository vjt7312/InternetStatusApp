package com.vjt.app.internetstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Intent in = new Intent("com.vjt.app.internetstatus.InternetService");
			context.startService(in);
		}
	}

}
