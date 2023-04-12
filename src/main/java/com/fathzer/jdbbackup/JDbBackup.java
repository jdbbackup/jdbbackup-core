package com.fathzer.jdbbackup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.classloader.ClassLoaderPluginLoader;
import com.fathzer.plugin.loader.utils.PluginRegistry;
import com.fathzer.plugin.loader.utils.ProxySettings;

/** A class able to perform a data source backup.
 */
public class JDbBackup {
	private static final Logger log = LoggerFactory.getLogger(JDbBackup.class);
	
	private final PluginRegistry<SourceManager> sources;
	@SuppressWarnings("rawtypes")
	private final PluginRegistry<DestinationManager> destinations;
	
	/** Constructor.
	 * <br>All source and destination managers available on the calling thread class loader are loaded.
	 * <br>To load extra plugins, you can use {@link java.util.ServiceLoader} or {@link com.fathzer.plugin.loader.PluginLoader} to instantiate them,
	 * and then register them with the {@link PluginRegistry#register(Object)} or {@link PluginRegistry#registerAll(Collection)} methods.
	 * @see #getDestinationManagers()
	 * @see #getSourceManagers()
	 */
	public JDbBackup() {
		sources = new PluginRegistry<>(SourceManager::getScheme);
		destinations = new PluginRegistry<>(DestinationManager::getScheme);
		final PluginLoader<ClassLoader> loader = new ClassLoaderPluginLoader().withExceptionConsumer(e -> log.warn("An error occured while loading plugins", e));
		try {
			loadPlugins(loader, loader, null);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private <T> boolean loadPlugins(PluginLoader<T> dest, PluginLoader<T> src, T source) throws IOException {
		boolean newSources = !getSourceManagers().registerAll(src.getPlugins(source, SourceManager.class)).isEmpty();
		boolean newDestinations = !getDestinationManagers().registerAll(dest.getPlugins(source, DestinationManager.class)).isEmpty();
		return newSources || newDestinations;
	}

	/** Gets the source managers registry.
	 * @return a plugin registry
	 */
	public PluginRegistry<SourceManager> getSourceManagers() {
		return sources;
	}
	
	/** Gets the destination managers registry.
	 * @return a plugin registry
	 */
	@SuppressWarnings("rawtypes")
	public PluginRegistry<DestinationManager> getDestinationManagers() {
		return destinations;
	}
	
	/** Makes a backup.
	 * @param proxySettings The proxy used to save the data base content
	 * @param source The address of the data base source (its format depends on the data base type)
	 * @param destinations The addresses of the backup destinations (their format depends on the data base type)
	 * @throws IOException If something went wrong.
	 * @throws IllegalArgumentException if arguments are wrong.
	 */
	public void backup(ProxySettings proxySettings, String source, String... destinations) throws IOException {
		if (source==null || destinations==null || destinations.length==0) {
			throw new IllegalArgumentException();
		}
		final List<Saver<?>> dest = Arrays.stream(destinations).map(Destination::new).map(d -> new Saver<>(d, this.destinations::get)).collect(Collectors.toList());
		final Proxy proxy = proxySettings==null ? Proxy.NO_PROXY : proxySettings.toProxy();
		final PasswordAuthentication auth = proxySettings==null ? null : proxySettings.getLogin();
		final File tmpFile = createTempFile();
		try {
			backup(source, tmpFile, dest, proxy, auth);
		} finally {
			Files.delete(tmpFile.toPath());
		}
	}
	
	/** Creates the temporary file that will be used by the source manager to create the backup.
	 * @return a File.
	 * @throws IOException If something went wrong.
	 */
	protected File createTempFile() throws IOException {
		final File tmpFile = Files.createTempFile("JDBBackup", ".gz").toFile();
		if(!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
			// On Posix compliant systems, java create tmp files with read/write rights only for user
			// Let do the same on non Posix systems
			final boolean readUserOnly = tmpFile.setReadable(true, true);
			final boolean writeUserOnly = tmpFile.setWritable(true, true);
			if (! (readUserOnly && writeUserOnly)) {
				LoggerFactory.getLogger(getClass()).warn("Fail to apply security restrictions on temporary file. Restrict read to user: {}, restrict write to user: {}", readUserOnly, writeUserOnly);
			}
			if (tmpFile.setExecutable(false, false)) {
				LoggerFactory.getLogger(getClass()).debug("Impossible to set temporary file not executable on this system");
			}
		}
		tmpFile.deleteOnExit();
		return tmpFile;
	}
	
	private void backup(String source, File tmpFile, Collection<Saver<?>> savers, Proxy proxy, PasswordAuthentication proxyAuth) throws IOException {
		final SourceManager sourceManager = getSourceManager(new Destination(source).getScheme());
		if (sourceManager instanceof ProxyCompliant) {
			((ProxyCompliant)sourceManager).setProxy(proxy, proxyAuth);
		}
		savers.forEach(s->s.prepare(sourceManager.getExtensionBuilder()));
		sourceManager.save(source, tmpFile);
		for (Saver<?> s : savers) {
			s.setProxy(proxy, proxyAuth);
			try (InputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
				s.send(in, tmpFile.length());
			}
		}
	}
	
	private SourceManager getSourceManager(String dbType) {
		final SourceManager saver = sources.get(dbType);
		if (saver==null) {
			throw new IllegalArgumentException("Unknown data source type: "+dbType);
		}
		return saver;
	}
}
