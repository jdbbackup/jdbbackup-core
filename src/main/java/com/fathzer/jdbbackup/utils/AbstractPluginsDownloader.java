package com.fathzer.jdbbackup.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.Plugins;
import com.fathzer.plugin.loader.jar.JarPluginLoader;
import com.fathzer.plugin.loader.utils.PluginRegistry;

/** A class that loads plugins from an Internet remote repository.
 */
public abstract class AbstractPluginsDownloader<T> extends com.fathzer.plugin.loader.utils.AbstractPluginsDownloader<T> {
	private static final Logger log = LoggerFactory.getLogger(AbstractPluginsDownloader.class);
	
	/** Constructor.
	 * @param registry The registry where plugins are loaded.
	 * @param uri The uri where to load the remote plugin registry.
	 * @param localDirectory The folder where plugins jar files will be loaded.
	 */
	protected AbstractPluginsDownloader(PluginRegistry<T> registry, URI uri, Path localDirectory, Class<T> pluginClass) {
		super(registry, uri, localDirectory, pluginClass);
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
	protected void load(Collection<Path> paths) throws IOException {
		final PluginLoader<Path> loader = new JarPluginLoader();
		final Plugins<T> plugins = new Plugins<>(getPluginClass());
		for (Path path : paths) {
			log.info("Loading repository plugins from {}",path);
			final Plugins<T> pathPlugins = loader.getPlugins(path, getPluginClass());
			pathPlugins.getExceptions().forEach(e -> log.warn(getPluginTypeWording(), e));
			plugins.add(pathPlugins);
		}
		plugins.getInstances().stream().forEach(this.getRegistry()::register);
		log.info("registry plugins are loaded");
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
	protected Map<String, URI> getURIMap() throws IOException {
		log.info("Downloading {} registry at {}", getPluginTypeWording(), getUri());
		return super.getURIMap();
	}
}
