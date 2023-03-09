package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class DefaultPathDecoderTest {

	@Test
	void test() {
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decodePath("x{v=jh}"));
		assertThrows(IllegalArgumentException.class, () -> DefaultPathDecoder.INSTANCE.decodePath("x{d=jh}"));
		try (MockedConstruction<Date> mock = mockConstruction(Date.class)) {
			// instances of Date used by the PathDecoder will be replaced by mockito with a Date that corresponds to 1/1/1970.
			assertEquals("y1970/m01", DefaultPathDecoder.INSTANCE.decodePath("y{d=YYYY}/m{d=MM}"));
			assertEquals("197001", DefaultPathDecoder.INSTANCE.decodePath("{d=YYYY}{d=MM}"));
		}
	}
}
