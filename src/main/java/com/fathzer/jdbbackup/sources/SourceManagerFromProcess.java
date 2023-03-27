package com.fathzer.jdbbackup.sources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.jdbbackup.SourceManager;

/** A class able to data source obtain through a command line to a compressed (.gz) file.
 */
public abstract class SourceManagerFromProcess implements SourceManager {
	/** Constructor.
	 */
	protected SourceManagerFromProcess() {
		super();
	}
	
	@Override
	public void save(String source, File destFile) throws IOException {
		final List<String> commands = getCommand(source);

		final ProcessBuilder pb = new ProcessBuilder(commands);
		final Process process = pb.start();
		final ProcessContext context = new ProcessContext(process);
		final Compressor compressor = new Compressor(destFile, context);
		final Thread compressThread = new Thread(compressor);
		compressThread.start();
		final InputStream err = process.getErrorStream();
		Thread errorThread = new Thread(() -> {
				final Logger logger = LoggerFactory.getLogger(SourceManagerFromProcess.this.getClass());
				try (BufferedReader bufErr = new BufferedReader(new InputStreamReader(err))) {
					for (String line = bufErr.readLine(); line!=null; line = bufErr.readLine()) {
						logger.warn(line);
					}
				} catch (IOException e) {
					if (!context.isKilled()) {
						context.kill();
						logger.error("Error while reading error stream", e);
					}
				}
			});
		errorThread.start();
		try {
			final int result = process.waitFor();
			compressThread.join();
			errorThread.join();
			if (compressor.getError()!=null) {
				throw compressor.getError();
			}
			if (result!=0) {
				throw new IOException ("Process failed with code "+result);
			}
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(getClass()).warn("Backup was interrupted", e);
			Thread.currentThread().interrupt();
			throw new InterruptedIOException();
		}
    }

	/** Gets the command line to execute to save the data source.
	 * @param source The data source access URI
	 * @return The list of the tokens that compose the command
	 * @throws IllegalArgumentException if source is invalid 
	 */
	protected abstract List<String> getCommand(String source);
}