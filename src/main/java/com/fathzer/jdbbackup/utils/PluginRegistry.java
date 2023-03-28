package com.fathzer.jdbbackup.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

/** A class to manage plugins identified by a key String.
 * <br>This class is not thread safe
 * @param <T> The class of the plugins. 
 */
public class PluginRegistry<T> {
	private final Map<String, T> pluginsMap;
	private final Function<T, String> keyFunction;
	private final Class<T> aClass;
	
	/** Constructor.
	 * <br>The built registry is empty. Use {@link #load(ClassLoader...)} to populate it.
	 * @param aClass Class instance of T type 
	 * @param keyFunction A function that get the plugin's key.
	 * @see #load(ClassLoader...)
	 */
	public PluginRegistry(Class<T> aClass, Function<T, String> keyFunction) {
		this.aClass = aClass;
		this.pluginsMap = new HashMap<>();
		this.keyFunction = keyFunction;
	}
	
	/** Loads plugins in this registry.
	 * <br>They are loaded using the {@link java.util.ServiceLoader} mechanism.
	 * @param classLoaders The class loaders used to load the plugins. For instance a class loader over jar files in a directory is exposed in <a href="https://stackoverflow.com/questions/16102010/dynamically-loading-plugin-jars-using-serviceloader">The second option exposed in this question</a>).
	 * @return List of new (The ones that were not already registered) plugins loaded.
	 * @see Files#getJarURL(java.io.File, int)
	 * @see java.net.URLClassLoader#newInstance(java.net.URL[])
	 * @see ClassLoader#getSystemClassLoader()
	 * @see #register(Object)
	 */
	public List<T> load(ClassLoader... classLoaders) {
		final List<T> found = new ArrayList<>();
		for (ClassLoader classLoader:classLoaders) {
			ServiceLoader.load(aClass, classLoader).forEach(x -> {
				if (register(x)) {
					found.add(x);
				}
			});
		}
		return found;
	}
	
	/** Register a plugin.
	 * @param plugin The plugin to register.
	 * @return true if no plugin of the same class was already registered.
	 */
	public boolean register(T plugin) {
		T previous = pluginsMap.put(keyFunction.apply(plugin),plugin);
		return (previous==null || !plugin.getClass().equals(previous.getClass()));
	}
	
	/** Gets a plugin by its key.
	 * @param key The plugin's key
	 * @return The plugin or null if the plugin does not exists.
	 */
	public T get(String key) {
		return pluginsMap.get(key);
	}
	
	/** Gets all available plugins.
	 * @return An unmodifiable map.
	 */
	public Map<String, T> getLoaded() {
		return Collections.unmodifiableMap(pluginsMap);
	}
}
