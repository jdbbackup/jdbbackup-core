package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fathzer.jdbbackup.SourceManager;
import com.fathzer.jdbbackup.sources.MySQLDumper;

class PluginRegistryTest {

	private static final class FakePlugin implements SourceManager {
		private String scheme;

		private FakePlugin(String scheme) {
			super();
			this.scheme = scheme;
		}

		@Override
		public void save(String source, File destFile) throws IOException {
			//Fake plugin
		}

		@Override
		public String getScheme() {
			return scheme;
		}
	}

	@Test
	void test() {
		PluginRegistry<SourceManager> registry = new PluginRegistry<>(SourceManager.class, SourceManager::getScheme);
		assertFalse(registry.load(ClassLoader.getSystemClassLoader()).isEmpty());
		assertTrue(registry.load(ClassLoader.getSystemClassLoader()).isEmpty());
		assertFalse(registry.register(new MySQLDumper()));
		assertNull(registry.get("test"));
		assertTrue(registry.register(new FakePlugin("test")));
		assertNotNull(registry.get("test"));
		assertFalse(registry.register(new FakePlugin("test")));
		assertTrue(registry.register(new FakePlugin(new MySQLDumper().getScheme())));
	}

}
