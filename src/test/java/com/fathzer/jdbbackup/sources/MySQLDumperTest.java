package com.fathzer.jdbbackup.sources;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class MySQLDumperTest {
	private static class MySQLObservableDumper extends MySQLDumper {
		@Override
		public List<String> getCommand(String params) {
			return super.getCommand(params);
		}
	}
	
	@Test
	void test() {
		MySQLObservableDumper d = new MySQLObservableDumper();
		assertEquals("mysql",d.getScheme());
		List<String> command = d.getCommand("mysql://u:p@host:4502/db/more");
		expect(command, "u","p","host",4502,"db/more");
		command = d.getCommand("mysql://user:pwd@localhost/db");
		expect(command, "user","pwd","localhost",3306,"db");
		
		final String wrongPort = "mysql://u:p@host:-5/db/more";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(wrongPort));
		final String noLogin = "mysql://host:-5/db/more";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(noLogin));
		final String noDb = "mysql://u:p@host";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(noDb));
		final String noPassword = "mysql://u@host/db";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(noPassword));
		final String noUser = "mysql://:p@host/db";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(noUser));
		final String wrongProtocol = "http://u:p@host/db";
		assertThrows(IllegalArgumentException.class, () -> d.getCommand(wrongProtocol));
		
		assertEquals("x.sql.gz", new MySQLDumper().getExtensionBuilder().apply("x"));
	}

	@Test
	void databasesOptionIsPresent() {
		MySQLObservableDumper d = new MySQLObservableDumper();
		List<String> command = d.getCommand("mysql://u:p@host:3306/db");
		assertTrue(command.contains("--databases"), "Command should contain --databases");
		assertTrue(command.contains("--add-drop-database"), "Command should contain --add-drop-database");
	}

	@Test
	void queryParameters() {
		MySQLObservableDumper d = new MySQLObservableDumper();
		List<String> command = d.getCommand("mysql://u:p@host:3306/db?single-transaction&routines&events&hex-blob&default-character-set=utf8mb4");
		assertTrue(command.contains("--single-transaction"), "Should contain --single-transaction");
		assertTrue(command.contains("--routines"), "Should contain --routines");
		assertTrue(command.contains("--events"), "Should contain --events");
		assertTrue(command.contains("--hex-blob"), "Should contain --hex-blob");
		assertTrue(command.contains("--default-character-set=utf8mb4"), "Should contain --default-character-set=utf8mb4");
	}

	@Test
	void queryParametersDisabled() {
		MySQLObservableDumper d = new MySQLObservableDumper();
		List<String> command = d.getCommand("mysql://u:p@host:3306/db?single-transaction=false");
		assertFalse(command.contains("--single-transaction"), "Should not contain --single-transaction");
	}

	@Test
	void noQueryParameters() {
		MySQLObservableDumper d = new MySQLObservableDumper();
		List<String> command = d.getCommand("mysql://u:p@host:3306/db");
		assertFalse(command.contains("--single-transaction"), "Should not contain --single-transaction by default");
		assertFalse(command.contains("--routines"), "Should not contain --routines by default");
		assertFalse(command.contains("--events"), "Should not contain --events by default");
		assertFalse(command.contains("--hex-blob"), "Should not contain --hex-blob by default");
	}
	
	private void expect(List<String> command, String user, String pwd, String host, int port, String db) {
		assertAll(command.toString(),
				() -> assertEquals("mysqldump", command.get(0)),
				() -> assertTrue(command.contains("--user="+user)),
				() -> assertTrue(command.contains("--password="+pwd)),
				() -> assertTrue(command.contains("--host="+host)),
				() -> assertTrue(command.contains("--port="+port)),
				() -> assertTrue(command.contains("--databases")),
				() -> assertTrue(command.contains(db))
		);
	}
}
