package com.mparticle.hello;

public class TimeLogger {

	private static TimeLogger mTimeLogger;

	private TimeLogger() {
	}
	
	public static TimeLogger instance() {
		if (mTimeLogger == null) {
			mTimeLogger = new TimeLogger();
		}
		return mTimeLogger;
	}
	
	public static void LogTime(String reason) {
		
	}
}
