package com.fathzer.jdbbackup;

import java.net.PasswordAuthentication;
import java.net.Proxy;

/** A class that can communicate through a proxy.
 */
public interface ProxyCompliant {
	/** Sets the proxy.
	 * @param proxy The proxy to use to connect to destination ({@link Proxy#NO_PROXY} for disabling proxy).
	 */
	void setProxy(final Proxy proxy);
	
	/** Sets the proxy authentication.
	 * @param auth The proxy authentication (null if the proxy does not require authentication).
	 */
	void setProxyAuth(final PasswordAuthentication auth);
}
