package com.fathzer.jdbbackup;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.fathzer.jdbbackup.utils.ProxySettings;

class Saver<T> {
	private static final Map<String, DestinationManager<?>> MANAGERS = new HashMap<>();

	private final Destination d;
	private T dest;
	private final DestinationManager<T> manager;
	
	@SuppressWarnings("unchecked")
	Saver(Destination d) {
		this.manager = (DestinationManager<T>) MANAGERS.get(d.getScheme());
		if (manager==null) {
			throw new IllegalArgumentException("Unknown protocol: "+d.getScheme());
		}
		this.d = d;
	}
	
	static boolean loadPlugins(ClassLoader... classLoaders) {
		final AtomicBoolean found = new AtomicBoolean();
		for (ClassLoader classLoader:classLoaders) {
			ServiceLoader.load(DestinationManager.class, classLoader).forEach((x) -> {
				found.set(true);
				register(x);
			});
		}
		return found.get();
	}
	
	static void register(DestinationManager<?> manager) {
		MANAGERS.put(manager.getScheme(),manager);
	}

	void setProxy(ProxySettings proxySettings) {
		manager.setProxy(proxySettings);
	}
	
	void prepare(Function<String,CharSequence> extensionBuilder) {
		this.dest = manager.validate(d.getPath(), extensionBuilder);
	}
	
	void send(InputStream in, long size) throws IOException {
		manager.send(in, size, dest);
	}
}
