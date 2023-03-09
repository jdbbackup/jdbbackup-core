package com.fathzer.jdbbackup.dumpers;

import java.io.InputStream;

class ProcessContext {
	private Process process;
	private boolean isKilled;
	
	public ProcessContext(Process process) {
		super();
		this.process = process;
		this.isKilled = false;
	}

	public synchronized void kill() {
		isKilled = true;
		process.destroy();
	}

	public synchronized boolean isKilled() {
		return isKilled;
	}

	public InputStream getInputStream() {
		return process.getInputStream();
	}
	
	
}
