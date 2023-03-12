package com.fathzer.jdbbackup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.LoggerFactory;

import com.fathzer.jdbbackup.utils.ProxySettings;

/** A class able to perform a database backup.
 */
public class JDbBackup {
	private static final Map<String, DestinationManager<?>> MANAGERS = new HashMap<>();
	private static final Map<String, DBDumper> SAVERS = new HashMap<>();
	
	static {
		loadPlugins(ClassLoader.getSystemClassLoader());
	}
	
	/** Loads extra plugins.
	 * <br>Plugins allow you to extends this library to dump sources not supported natively by this library or accept new destinations.
	 * <br>They are loaded using the {@link java.util.ServiceLoader} mechanism.
	 * @param classLoaders The class loaders used to load the plugins. For instance a class loader over jar files in a directory is exposed in <a href="https://stackoverflow.com/questions/16102010/dynamically-loading-plugin-jars-using-serviceloader">The second option exposed in this question</a>).
	 * @see DBDumper
	 * @see DestinationManager
	 */
	public static void loadPlugins(ClassLoader... classLoaders) {
		for (ClassLoader classLoader:classLoaders) {
			ServiceLoader.load(DestinationManager.class, classLoader).forEach(m -> add(MANAGERS, m.getScheme(), m));
			ServiceLoader.load(DBDumper.class, classLoader).forEach(s -> add(SAVERS, s.getScheme(), s));
		}
	}
	
	private static <T> void add(Map<String, T> map, String key, T instance) {
		map.put(key, instance);
	}
	
	/** Makes a backup.
	 * @param proxySettings The proxy used to save the data base content
	 * @param source The address of the data base source (its format depends on the data base type)
	 * @param destination The address of the backup destination (its format depends on the data base type)
	 * @return An information string
	 * @throws IOException If something went wrong.
	 * @throws IllegalArgumentException if arguments are wrong.
	 */
	public String backup(ProxySettings proxySettings, String source, String destination) throws IOException {
		if (source==null || destination==null) {
			throw new IllegalArgumentException();
		}
		final Destination dest = new Destination(destination);
		final DestinationManager<?> manager = getDestinationManager(dest);
		if (proxySettings!=null) {
			manager.setProxy(proxySettings);
		}
		final File tmpFile = createTempFile();
		try {
			return backup(source, manager, dest, tmpFile);
		} finally {
			Files.delete(tmpFile.toPath());
		}
	}
	
	/** Creates the temporary file that will be used by the DBDumper to create the database backup.
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
	
	private <T> String backup(String dbURI, DestinationManager<T> manager, Destination destination, File tmpFile) throws IOException {
		DBDumper dumper = getDBDumper(new Destination(dbURI).getScheme());
		T destFile = manager.validate(destination.getPath(), dumper.getExtensionBuilder());
		dumper.save(dbURI, tmpFile);
		try (InputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
			return manager.send(in, tmpFile.length(), destFile);
		}
	}
	
	private <T> DestinationManager<T> getDestinationManager(Destination destination) {
		@SuppressWarnings("unchecked")
		final DestinationManager<T> manager = (DestinationManager<T>) MANAGERS.get(destination.getScheme());
		if (manager==null) {
			throw new IllegalArgumentException("Unknown protocol: "+destination.getScheme());
		}
		return manager;
	}
	
	private DBDumper getDBDumper(String dbType) {
		final DBDumper saver = SAVERS.get(dbType);
		if (saver==null) {
			throw new IllegalArgumentException("Unknown database type: "+dbType);
		}
		return saver;
	}
}
