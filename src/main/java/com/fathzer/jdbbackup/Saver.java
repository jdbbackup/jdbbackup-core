package com.fathzer.jdbbackup;

import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Map;
import java.util.function.Function;

class Saver<T> implements ProxyCompliant {
	private final Destination d;
	private T dest;
	private final DestinationManager<T> manager;
	
	@SuppressWarnings("unchecked")
	Saver(Destination d, @SuppressWarnings("rawtypes") Map<String, DestinationManager> map) {
		this.manager = map.get(d.getScheme());
		if (manager==null) {
			throw new IllegalArgumentException("Unknown protocol: "+d.getScheme());
		}
		this.d = d;
	}

	@Override
	public void setProxy(Proxy proxy, PasswordAuthentication auth) {
		ProxyCompliant.super.setProxy(proxy, auth);
		if (manager instanceof ProxyCompliant) {
			((ProxyCompliant)manager).setProxy(proxy, auth);
		}
	}
	
	void prepare(Function<String,CharSequence> extensionBuilder) {
		this.dest = manager.validate(d.getPath(), extensionBuilder);
	}
	
	void send(InputStream in, long size) throws IOException {
		manager.send(in, size, dest);
	}
}
