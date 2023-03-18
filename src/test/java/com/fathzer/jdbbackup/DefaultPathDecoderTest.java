package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.*;

import com.fathzer.jdbbackup.DefaultPathDecoder.StringSplitter;

class DefaultPathDecoderTest {

	@Test
	void testDates() {
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decodePath("x{v=jh}"));
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decodePath("x{d=jh}"));
		try (MockedConstruction<Date> mock = mockConstruction(Date.class)) {
			// instances of Date used by the PathDecoder will be replaced by mockito with a Date that corresponds to 1/1/1970.
			assertEquals("y1970/m01", DefaultPathDecoder.INSTANCE.decodePath("y{d=YYYY}/m{d=MM}"));
			assertEquals("197001", DefaultPathDecoder.INSTANCE.decodePath("{d=YYYY}{d=MM}"));
		}
	}

	@Test
	void testEnv() {
		// Mockito can't mock System class and we can't add env prop. So let's try with existing one ...
		Optional<Entry<String, String>> any = System.getenv().entrySet().stream().findAny();
		if (any.isPresent()) {
			assertEquals("-"+any.get().getValue()+"-",DefaultPathDecoder.INSTANCE.decodePath("-{e="+any.get().getKey()+"}-"), "DefaultPathDecoder with env pattern failed on "+any.get().getKey()+" env var");
		} else {
			System.err.println("WARNING: no env var set, can't test DefaultPathDecoder with env pattern");
		}
		// Now let's find a missing env
		String env = "-";
		while(System.getenv(env)!=null) {
			env = env + "-";
		}
		final String envVar = env;
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decode("e",envVar));
	}
	
	@Test
	void testSysProp() {
		final String myProp = "DefaultPathDecoderTestProp";
		final String previous = System.getProperty(myProp);
		final String value = "should be removed after test";
		System.setProperty(myProp, value);
		try {
			assertEquals("-"+value+"-",DefaultPathDecoder.INSTANCE.decodePath("-{p="+myProp+"}-"));
			System.clearProperty(myProp);
			assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decodePath("-{p="+myProp+"}-"));
		} finally {
			if (previous==null) {
				System.clearProperty(myProp);
			} else {
				System.setProperty(myProp, previous);
			}
		}
	}
	
	@Test
	void testFile() {
		assertEquals("-File content",DefaultPathDecoder.INSTANCE.decodePath("-{f=src/test/resources/DefaultPathDecoderTest.txt}"));
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decode("f","NotExisting"));
	}
	
	@Test
	void testSplitter() {
		{
		String input = "{f=/home/user/toto}";
		final StringSplitter s = new StringSplitter(input, '/');
		assertEquals(input, s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals(input,s.next());
		assertFalse(s.hasNext());
		assertNull(s.getRemaining());
		assertThrows(NoSuchElementException.class, () -> s.next());
		}
		
		{
		// Empty string
		final StringSplitter s = new StringSplitter("", '/');
		assertTrue(s.hasNext());
		assertEquals("", s.getRemaining());
		assertEquals("", s.next());
		assertFalse(s.hasNext());
		}
		
		{
		// Test empty tokens
		final StringSplitter s = new StringSplitter("/a/", '/');
		assertEquals("/a/",s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals("",s.next());
		assertEquals("a/", s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals("a",s.next());
		assertEquals("", s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals("",s.next());
		assertFalse(s.hasNext());
		}
		
		{
		// Test non closed pattern
		final StringSplitter s = new StringSplitter("{a/b", '/');
		assertEquals("{a/b",s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals("{a",s.next());
		assertEquals("b", s.getRemaining());
		assertTrue(s.hasNext());
		assertEquals("b",s.next());
		assertEquals(null, s.getRemaining());
		}
	}
}
