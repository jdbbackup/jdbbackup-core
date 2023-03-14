package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fathzer.jdbbackup.DBDumper;
import com.fathzer.jdbbackup.dumpers.MySQLDumper;

class PluginRegistryTest {

	private static final class FakePlugin implements DBDumper {
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
		PluginRegistry<DBDumper> registry = new PluginRegistry<>(DBDumper.class, DBDumper::getScheme);
		assertTrue(registry.load(ClassLoader.getSystemClassLoader()));
		assertFalse(registry.load(ClassLoader.getSystemClassLoader()));
		assertFalse(registry.register(new MySQLDumper()));
		assertNull(registry.get("test"));
		assertTrue(registry.register(new FakePlugin("test")));
		assertNotNull(registry.get("test"));
		assertFalse(registry.register(new FakePlugin("test")));
		assertTrue(registry.register(new FakePlugin(new MySQLDumper().getScheme())));
	}

}
