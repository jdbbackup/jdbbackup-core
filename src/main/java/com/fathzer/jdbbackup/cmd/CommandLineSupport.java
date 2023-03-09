package com.fathzer.jdbbackup.cmd;

/** A class that acts as a command line.
 */
public interface CommandLineSupport {
	
	/** Output a message on the standard output.
	 * @param message The message to output
	 */
	@SuppressWarnings("java:S106")
	default void out(String message) {
		System.out.println(message);
	}
	
	/** Output a message on the standard error.
	 * @param message The message to output
	 */
	@SuppressWarnings("java:S106")
	default void err(String message) {
		System.err.println(message);
	}
	
	/** Output an exception on the standard error.
	 * @param e The exception to output
	 */
	@SuppressWarnings("java:S4507")
	default void err(Throwable e) {
		e.printStackTrace();
	}

}
