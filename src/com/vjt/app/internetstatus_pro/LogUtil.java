package com.vjt.app.internetstatus_pro;

import android.util.Log;

public class LogUtil {

	public static final boolean enable = false;

	public static int v(String tag, String msg) {
		if (enable)
			return Log.v(tag, msg);
		else
			return 0;
	}

	public static int d(String tag, String msg) {
		if (enable)
			return Log.d(tag, msg);
		else
			return 0;
	}

	public static int i(String tag, String msg) {
		if (enable)
			return Log.i(tag, msg);
		else
			return 0;
	}

	public static int w(String tag, String msg) {
		if (enable)
			return Log.w(tag, msg);
		else
			return 0;
	}

	public static int e(String tag, String msg) {
		if (enable)
			return Log.e(tag, msg);
		else
			return 0;
	}
}
