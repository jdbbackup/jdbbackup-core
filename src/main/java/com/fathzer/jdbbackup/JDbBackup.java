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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.classloader.ClassLoaderPluginLoader;

/** A class able to perform a data source backup.
 */
public class JDbBackup {
	private static final Logger log = LoggerFactory.getLogger(JDbBackup.class);
	
	private final Map<String, SourceManager> sources;
	@SuppressWarnings("rawtypes")
	private final Map<String, DestinationManager> destinations;
	private Proxy proxy;
	private PasswordAuthentication auth;
	
	/** Constructor.
	 * <br>All source and destination managers available on the calling thread class loader are loaded.
	 * <br>To load extra plugins, you can use {@link java.util.ServiceLoader} or {@link com.fathzer.plugin.loader.PluginLoader} to instantiate them,
	 * and then adding them to the maps returned by {@link #getSourceManagers()} and {@link #getDestinationManagers()} methods.
	 * @see #getDestinationManagers()
	 * @see #getSourceManagers()
	 */
	public JDbBackup() {
		sources = new HashMap<>();
		destinations = new HashMap<>();
		this.proxy = Proxy.NO_PROXY;
		final PluginLoader<ClassLoader> loader = new ClassLoaderPluginLoader().withExceptionConsumer(e -> log.warn("An error occurred while loading plugins", e));
		try {
			loader.getPlugins(null, SourceManager.class).forEach(s -> sources.put(s.getScheme(), s));
			loader.getPlugins(null, DestinationManager.class).forEach(d -> destinations.put(d.getScheme(), d));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/** Sets the proxy.
	 * @param proxy The proxy to use to connect to destination ({@link Proxy#NO_PROXY} for disabling proxy).
	 * @param auth The proxy authentication (null if the proxy does not require authentication).
	 * @throws IllegalArgumentException if proxy is null or if auth is not null and proxy is {@link Proxy#NO_PROXY}.
	 */
	public void setProxy(Proxy proxy, PasswordAuthentication auth) {
		// Test parameters are ok
		new ProxyCompliant(){}.setProxy(proxy, auth);
		this.proxy = proxy;
		this.auth = auth;
	}

	/** Gets the source managers registry.
	 * @return a map that links schemes to their source managers
	 */
	public Map<String, SourceManager> getSourceManagers() {
		return sources;
	}
	
	/** Gets the destination managers registry.
	 * @return a map that links schemes to their destination managers
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, DestinationManager> getDestinationManagers() {
		return destinations;
	}
	
	/** Makes a backup.
	 * @param source The address of the data base source (its format depends on the data base type)
	 * @param destinations The addresses of the backup destinations (their format depends on the data base type)
	 * @throws IOException If something went wrong.
	 * @throws IllegalArgumentException if arguments are wrong.
	 */
	public void backup(String source, String... destinations) throws IOException {
		if (source==null || destinations==null || destinations.length==0) {
			throw new IllegalArgumentException();
		}
		final List<Saver<?>> dest = Arrays.stream(destinations).map(Destination::new).map(d -> new Saver<>(d, this.destinations)).collect(Collectors.toList());
		final File tmpFile = createTempFile();
		try {
			backup(source, tmpFile, dest);
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
	
	private void backup(String source, File tmpFile, Collection<Saver<?>> savers) throws IOException {
		final SourceManager sourceManager = getSourceManager(new Destination(source).getScheme());
		if (sourceManager instanceof ProxyCompliant) {
			((ProxyCompliant)sourceManager).setProxy(proxy, auth);
		}
		savers.forEach(s->s.prepare(sourceManager.getExtensionBuilder()));
		sourceManager.save(source, tmpFile);
		for (Saver<?> s : savers) {
			s.setProxy(proxy, auth);
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
