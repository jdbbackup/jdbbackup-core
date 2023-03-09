package com.fathzer.jdbbackup;

public class IllegalNamePatternException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public IllegalNamePatternException(String message) {
		super(message);
	}

}
