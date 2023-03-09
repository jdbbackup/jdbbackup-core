package com.fathzer.jdbbackup;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

public class JavaProcessAvailabilityChecker {
	private static Boolean javaSupported;

	public static boolean available() {
		if (javaSupported == null) {
			try {
				final Process process = new ProcessBuilder("java", "-version").redirectError(Redirect.DISCARD)
						.redirectOutput(Redirect.DISCARD).start();
				process.waitFor();
				final int exitValue = process.exitValue();
				if (exitValue != 0) {
					System.err.println("WARNING, java -version returns code " + exitValue);
					javaSupported = Boolean.FALSE;
				} else {
					javaSupported = true;
				}
			} catch (IOException e) {
				System.err.println("WARNING, can't launch java");
				e.printStackTrace();
				javaSupported = false;
			} catch (InterruptedException e) {
				System.err.println("WARNING, java process waiting was interrupted");
				javaSupported = false;
				Thread.currentThread().interrupt();
			}
		}
		return javaSupported;
	}

}
