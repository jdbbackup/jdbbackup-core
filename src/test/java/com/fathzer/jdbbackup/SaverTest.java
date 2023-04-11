package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.utils.PluginRegistry;
import com.fathzer.plugin.loader.utils.ProxySettings;

class SaverTest {
	
	private static class KnownManager implements DestinationManager<Object>, ProxyCompliant {
		private Proxy proxy = Proxy.NO_PROXY;
		private PasswordAuthentication proxyAuth;
		
		@Override
		public void setProxy(Proxy proxy) {
			this.proxy = proxy;
		}

		@Override
		public void setProxyAuth(PasswordAuthentication auth) {
			this.proxyAuth = auth;
		}

		@Override
		public String getScheme() {
			return "known";
		}

		@Override
		public Object validate(String path, Function<String, CharSequence> extensionBuilder) {
			return null;
		}

		@Override
		public void send(InputStream in, long size, Object destination) throws IOException {
		}
	}

	@Test
	void test() throws Exception {
		Destination dest = new Destination("unknown://klm");
		assertThrows(IllegalArgumentException.class, () -> new Saver<>(dest));
		
		KnownManager manager = new KnownManager();
		register(manager);
		
		final Saver<?> s = new Saver<>(new Destination("known://klm"));
		ProxySettings proxy = ProxySettings.fromString("127.0.0.1:3128");
		s.setProxy(proxy.toProxy(), proxy.getLogin());
		assertEquals(new InetSocketAddress("127.0.0.1",3128), manager.proxy.address());
		assertNull(manager.proxyAuth);

		proxy = ProxySettings.fromString("a:b@x.com:4128");
		s.setProxy(proxy.toProxy(), proxy.getLogin());
		assertEquals(new InetSocketAddress("x.com",4128), manager.proxy.address());
		assertEquals("a",manager.proxyAuth.getUserName());
		assertArrayEquals(new char[] {'b'},manager.proxyAuth.getPassword());
		assertThrows(IllegalArgumentException.class, () -> s.setProxy(Proxy.NO_PROXY, manager.proxyAuth));

		s.setProxy(proxy.toProxy(), null);
		assertNull(manager.proxyAuth);
		
		assertThrows(IllegalArgumentException.class, () -> s.setProxy(null, null));
	}

	private void register(DestinationManager<?> manager) throws NoSuchFieldException, IllegalAccessException {
		final Field field = Saver.class.getDeclaredField("MANAGERS");
		field.setAccessible(true);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		PluginRegistry<DestinationManager> plugins = (PluginRegistry<DestinationManager>) field.get(null);
		plugins.register(manager);
	}
}
