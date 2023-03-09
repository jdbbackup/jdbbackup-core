package com.fathzer.jdbbackup.dumpers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class ProcessContextTest {
	@Test
	void test() {
		Process process = mock(Process.class);
		ProcessContext context = new ProcessContext(process);
		assertFalse(context.isKilled());
		context.kill();
		assertTrue(context.isKilled());
		verify(process).destroy();
	}
}
