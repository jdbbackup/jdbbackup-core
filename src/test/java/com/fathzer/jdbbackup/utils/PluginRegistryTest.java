package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fathzer.jdbbackup.DBDumper;
import com.fathzer.jdbbackup.dumpers.MySQLDumper;

class PluginRegistryTest {

	@Test
	void test() {
		PluginRegistry<DBDumper> registry = new PluginRegistry<>(DBDumper.class, DBDumper::getScheme);
		assertTrue(registry.load(ClassLoader.getSystemClassLoader()));
		assertFalse(registry.load(ClassLoader.getSystemClassLoader()));
		assertFalse(registry.register(new MySQLDumper()));
		assertTrue(registry.register(new DBDumper() {
			
			@Override
			public void save(String source, File destFile) throws IOException {
				//Fake plugin
			}
			
			@Override
			public String getScheme() {
				return "test";
			}
		}));
		assertNotNull(registry.get("test"));
	}

}
