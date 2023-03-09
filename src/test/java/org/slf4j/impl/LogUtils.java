package org.slf4j.impl;

public class LogUtils {
	public static int setLevel(SimpleLogger logger, String level) {
		return setLevel(logger, SimpleLoggerConfiguration.stringToLevel(level));
	}

	public static int setLevel(SimpleLogger logger, int level) {
		final int previous = logger.currentLogLevel;
		logger.currentLogLevel = level;
		return previous;
	}
}
