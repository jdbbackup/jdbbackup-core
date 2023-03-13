package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.fathzer.jdbbackup.utils.ProxySettings;

class SaverTest {

	@Test
	void test() {
		Destination dest = new Destination("unknown://klm");
		assertThrows(IllegalArgumentException.class, () -> new Saver<>(dest));
		
		DestinationManager<?> manager = mock(DestinationManager.class);
		when(manager.getScheme()).thenReturn("known");
		Saver.register(manager);
		Saver<?> s = new Saver<>(new Destination("known://klm"));
		ProxySettings proxy = ProxySettings.fromString("127.0.0.1:3128");
		s.setProxy(proxy);
		verify(manager).setProxy(proxy);
	}

}
