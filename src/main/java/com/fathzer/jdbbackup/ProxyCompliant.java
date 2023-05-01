package com.fathzer.jdbbackup;

import java.net.PasswordAuthentication;
import java.net.Proxy;

/** A class that can communicate through a proxy.
 */
public interface ProxyCompliant {
	/** Sets the proxy.
	 * @param proxy The proxy to use to connect to destination ({@link Proxy#NO_PROXY} for disabling proxy).
	 * @param auth The proxy authentication (null if the proxy does not require authentication).
	 * @throws IllegalArgumentException
	 * The default implementation throws an exception if proxy is null or if auth is not null and proxy is {@link Proxy#NO_PROXY}.
	 */
	default void setProxy(final Proxy proxy, final PasswordAuthentication auth) {
		if (proxy==null) {
			throw new IllegalArgumentException("Use Proxy.NO_PROXY instead of null");
		}
		if (Proxy.NO_PROXY.equals(proxy) && auth!=null) {
			throw new IllegalArgumentException("Can't set no proxy with login");
		}
	}
}
