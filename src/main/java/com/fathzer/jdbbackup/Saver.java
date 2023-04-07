package com.fathzer.jdbbackup;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import com.fathzer.plugin.loader.utils.PluginRegistry;
import com.fathzer.plugin.loader.utils.ProxySettings;

class Saver<T> {
	@SuppressWarnings("rawtypes")
	private static final PluginRegistry<DestinationManager> MANAGERS = new PluginRegistry<>(DestinationManager::getScheme);

	private final Destination d;
	private T dest;
	private final DestinationManager<T> manager;
	
	@SuppressWarnings("unchecked")
	Saver(Destination d) {
		this.manager = MANAGERS.get(d.getScheme());
		if (manager==null) {
			throw new IllegalArgumentException("Unknown protocol: "+d.getScheme());
		}
		this.d = d;
	}
	
	@SuppressWarnings("rawtypes")
	static PluginRegistry<DestinationManager> getManagers() {
		return MANAGERS;
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
