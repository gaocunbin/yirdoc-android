package com.example.yirdoc_atomizer.ble.utility;

import android.util.Log;

import com.example.yirdoc_atomizer.BuildConfig;


public class DebugLogger {
	public static void v(final String tag, final String text) {
		if (BuildConfig.DEBUG)
			Log.v(tag, text);
	}

	public static void d(String tag, String text) {
		if (BuildConfig.DEBUG) {
			Log.d(tag, text);
		}
	}

	public static void i(final String tag, final String text) {
		if (BuildConfig.DEBUG)
			Log.i(tag, text);
	}

	public static void w(String tag, String text) {
		if (BuildConfig.DEBUG) {
			Log.w(tag, text);
		}
	}

	public static void e(final String tag, final String text) {
		if (BuildConfig.DEBUG)
			Log.e(tag, text);
	}

	public static void wtf(String tag, String text) {
		if (BuildConfig.DEBUG) {
			Log.wtf(tag, text);
		}
	}
}
