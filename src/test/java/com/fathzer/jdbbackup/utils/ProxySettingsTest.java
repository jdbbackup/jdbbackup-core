package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

class ProxySettingsTest {

	@Test
	void test() {
		ProxySettings settings = ProxySettings.fromString("user:pwd@host:3128");
		assertEquals("user", settings.getLogin().getUser());
		assertEquals("pwd", settings.getLogin().getPassword());
		assertEquals("host", settings.getHost());
		assertEquals(3128, settings.getPort());
		
		settings = new ProxySettings("host", 3128, new Login("user", "pwd")); 
		assertEquals("user", settings.getLogin().getUser());
		assertEquals("pwd", settings.getLogin().getPassword());
		assertEquals("host", settings.getHost());
		assertEquals(3128, settings.getPort());
		
		// Test toString hides the password
		ProxySettings fromToString = ProxySettings.fromString(settings.toString());
		assertEquals(settings.getLogin().getUser(), fromToString.getLogin().getUser());
		assertEquals(settings.getHost(), fromToString.getHost());
		assertEquals(settings.getPort(), fromToString.getPort());
		assertNotEquals(settings.getLogin().getPassword(), fromToString.getLogin().getPassword());

		// Test with no password
		settings = ProxySettings.fromString("user@host:2000");
		assertEquals("user", settings.getLogin().getUser());
		assertNull(settings.getLogin().getPassword());
		assertEquals("host", settings.getHost());
		assertEquals(2000, settings.getPort());
		assertEquals("user@host:2000", settings.toString());
		
		// Test with no user
		settings = ProxySettings.fromString("host:3128");
		assertEquals("host", settings.getHost());
		assertEquals(3128, settings.getPort());
		assertEquals("host:3128", settings.toString());
		
		InetSocketAddress proxy = (InetSocketAddress) ProxySettings.fromString("127.0.0.1:3128").toProxy().address();
		assertEquals(3128, proxy.getPort());
		assertArrayEquals(new byte[] {127,0,0,1}, proxy.getAddress().getAddress());
		
		// Empty or null String
		assertNull(ProxySettings.fromString(" "));
		assertNull(ProxySettings.fromString(null));
	}

	@Test
	void testIllegal() {
		// Illegal arguments
		assertThrows(IllegalArgumentException.class, () -> ProxySettings.fromString("host"));
		assertThrows(IllegalArgumentException.class, () -> ProxySettings.fromString("host:3128:11"));
		assertThrows(IllegalArgumentException.class, () -> ProxySettings.fromString("host:xxx"));
		assertThrows(IllegalArgumentException.class, () -> ProxySettings.fromString("u:p@:3128"));
		assertThrows(IllegalArgumentException.class, () -> ProxySettings.fromString("u:p@myHost{1}:3128"));
	}

	@Test
	void loginTest() {
		assertNull(Login.fromString(" "));
		assertNull(Login.fromString(null));
	}
}
