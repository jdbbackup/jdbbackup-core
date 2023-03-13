package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class FilesTest {

	@Test
	void test() throws IOException {
		final File nonExisting = mock(File.class);
		assertEquals(0, Files.toURL(nonExisting, ".jar", 1).length);

		final File notJar = mock(File.class);
		when(notJar.isFile()).thenReturn(true);
		when(notJar.getName()).thenReturn("myFile.txt");
		assertEquals(0, Files.toURL(notJar, ".jar", 1).length);

		final File fileJar = mock(File.class);
		when(fileJar.isFile()).thenReturn(true);
		when(fileJar.getName()).thenReturn("myLib.jar");
		when(fileJar.toURI()).thenReturn(URI.create("file:/home/myLib.jar"));
		
		URL[] urls = Files.toURL(fileJar, ".jar", 1);
		assertArrayEquals(new URL[] {new URL("file:/home/myLib.jar")},urls);

		final File dir = mock(File.class);
		when(dir.isDirectory()).thenReturn(true);
		final Path jarPath = mock(Path.class);
		when(jarPath.toUri()).thenReturn(URI.create("file:/home/myLib.jar"));
		try (MockedStatic<java.nio.file.Files> mockStatic = mockStatic(java.nio.file.Files.class)) {
			mockStatic.when(() -> java.nio.file.Files.find(any(), anyInt(), any())).thenReturn(Arrays.asList(jarPath).stream());
			assertArrayEquals(new URL[] {new URL("file:/home/myLib.jar")}, Files.toURL(dir, ".jar", 1));
		}
	}
	
	@Test
	void testPathMatcher() {
		// Test not dir
		final BasicFileAttributes attr = mock(BasicFileAttributes.class);
		when(attr.isRegularFile()).thenReturn(true);

		final Path jarPath = mock(Path.class);
		when(jarPath.toString()).thenReturn("/home/myLib.jar");
		assertTrue(Files.IS_JAR.test(jarPath, attr));
		
		final Path notJar = mock(Path.class);
		when(jarPath.toString()).thenReturn("/home/myFile.txt");
		assertFalse(Files.IS_JAR.test(notJar, attr));

		// Test dir
		when(attr.isRegularFile()).thenReturn(false);
		assertFalse(Files.IS_JAR.test(jarPath, attr));
		assertFalse(Files.IS_JAR.test(notJar, attr));
	}
}
