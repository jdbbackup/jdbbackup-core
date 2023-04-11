package com.fathzer.jdbbackup.destinations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FileManagerTest {

	@Test
	void test() {
		FileManager manager = new FileManager();
		assertEquals("file",manager.getScheme());
	}
}
