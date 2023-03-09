package com.fathzer.jdbbackup.managers.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

import com.fathzer.jdbbackup.DefaultPathDecoder;
import com.fathzer.jdbbackup.DestinationManager;
import com.fathzer.jdbbackup.utils.ProxySettings;

/** A destination manager that saves the backups locally.
 * <br>It uses an instance of {@link DefaultPathDecoder} in order to build the destination path.
 */
public class FileManager implements DestinationManager<Path> {
	public FileManager() {
		super();
	}

	@Override
	public void setProxy(ProxySettings options) {
		// Ignore proxy as there's no network access there.
	}

	@Override
	public Path validate(String fileName, Function<String,CharSequence> extensionBuilder) {
		return new File(DefaultPathDecoder.INSTANCE.decodePath(fileName, extensionBuilder)).toPath();
	}

	@Override
	public String send(InputStream in, long size, Path dest) throws IOException {
	    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
		return "Saved to: "+dest.toFile().getAbsolutePath();
	}

	@Override
	public String getProtocol() {
		return "file";
	}
}
