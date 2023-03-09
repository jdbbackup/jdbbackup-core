package com.fathzer.jdbbackup.dumpers;

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
	}
	
	private void expect(List<String> command, String user, String pwd, String host, int port, String db) {
		assertAll(command.toString(),
				() -> assertEquals("mysqldump", command.get(0)),
				() -> assertTrue(command.contains("--user="+user)),
				() -> assertTrue(command.contains("--password="+pwd)),
				() -> assertTrue(command.contains("--host="+host)),
				() -> assertTrue(command.contains("--port="+port)),
				() -> assertTrue(command.contains(db))
		);
	}
}
