package com.fathzer.jdbbackup.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.plugin.loader.utils.AbstractPluginsDownloader;

/** A class that loads plugins from an Internet remote repository.
 */
public abstract class AbstractManagersDownloader extends AbstractPluginsDownloader {
	private static final Logger log = LoggerFactory.getLogger(AbstractManagersDownloader.class);
	
	/** Constructor.
	 * @param uri The uri where to load the remote plugin registry.
	 * @param localDirectory The folder where plugins jar files will be loaded.
	 */
	protected AbstractManagersDownloader(URI uri, Path localDirectory) {
		super(uri, localDirectory);
	}

	
	/** Deletes all files in local directory.
	 * @throws IOException If something went wrong
	 */
	@Override
	public boolean clean() throws IOException {
		final boolean result = super.clean();
		if (result) {
			log.info("Existing downloaded {} deleted from {}", getPluginTypeWording(), getLocalDirectory());
		}
		return result;
	}
	
	@Override
	protected boolean shouldLoad(URI uri, Path path) {
		final boolean result = super.shouldLoad(uri, path);
		if (result) {
			log.info("Downloading {} to file {}",uri, path);
		} else {
			log.info("{} was already downloaded to file {}",uri, path);
		}
		return result;
	}

	@Override
	public Map<String, URI> getURIMap() throws IOException {
		log.info("Downloading {} registry from {}", getPluginTypeWording(), getUri());
		return super.getURIMap();
	}
}
