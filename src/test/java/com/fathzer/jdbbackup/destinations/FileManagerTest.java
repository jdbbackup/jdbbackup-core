package com.fathzer.jdbbackup.destinations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.utils.ProxySettings;

class FileManagerTest {

	@Test
	void test() {
		FileManager manager = new FileManager();
		assertEquals("file",manager.getScheme());
		// Test no error if setProxy called
		manager.setProxy(ProxySettings.fromString("127.0.0.1:3128"));
	}

}
