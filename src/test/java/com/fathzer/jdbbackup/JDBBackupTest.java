package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.LogUtils;
import org.slf4j.simple.SimpleLogger;

import com.fathzer.jdbbackup.sources.FakeJavaSource;

class JDBBackupTest {
	private static final String DEST_PATH = "./tmpTestFile.gz";
	
	private static class ObservableJDbBackup extends JDbBackup {
		private File tmpFile;

		@Override
		protected File createTempFile() throws IOException {
			this.tmpFile = super.createTempFile();
			return tmpFile;
		}
	}
	
	@AfterEach
	void cleanup() {
		new File(DEST_PATH).delete();
	}

	@Test
	@EnabledIf("com.fathzer.jdbbackup.JavaProcessAvailabilityChecker#available")
	void testOk() throws IOException {
		final ObservableJDbBackup b = new ObservableJDbBackup();
		// No destination
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, null, (String)null));
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, null, new String[0]));
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, "java://", (String)null));
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, "java://", new String[0]));
		String dest = "file://"+DEST_PATH;

		// No DbName
		assertTrue(b.tmpFile==null || !b.tmpFile.exists());
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, null, dest));
		assertTrue(b.tmpFile==null || !b.tmpFile.exists());
		
		// Unknown DB Type
		assertThrows(IllegalArgumentException.class, () -> b.backup(null, "unknown://jmkjmkl", dest));
		assertTrue(b.tmpFile==null || !b.tmpFile.exists());
		
		
		FakeJavaSource.shouldFail = false;
		String db = "java://";
		b.backup(null, db, dest);
		assertTrue(b.tmpFile==null || !b.tmpFile.exists());
		
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(DEST_PATH)))))) {
			final List<String> lines = reader.lines().collect(Collectors.toList());
			assertEquals(FakeJavaSource.CONTENT, lines);
		}
	}

	@Test
	@EnabledIf("com.fathzer.jdbbackup.JavaProcessAvailabilityChecker#available")
	void testKo() throws IOException {
		final ObservableJDbBackup b = new ObservableJDbBackup();
		String dest = "file://"+DEST_PATH;
		String db = "java://";
		FakeJavaSource.shouldFail = true;
		SimpleLogger log = (SimpleLogger) LoggerFactory.getLogger(com.fathzer.jdbbackup.sources.FakeJavaSource.class);
		final int previous = LogUtils.setLevel(log, "off");
		try {
			assertThrows(IOException.class, () -> b.backup(null, db, dest));
		} finally {
			LogUtils.setLevel(log, previous);
		}
	}
}
