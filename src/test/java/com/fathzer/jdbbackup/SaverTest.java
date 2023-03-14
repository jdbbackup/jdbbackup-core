package com.fathzer.jdbbackup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.fathzer.jdbbackup.utils.PluginRegistry;
import com.fathzer.jdbbackup.utils.ProxySettings;

class SaverTest {

	@Test
	void test() throws Exception {
		Destination dest = new Destination("unknown://klm");
		assertThrows(IllegalArgumentException.class, () -> new Saver<>(dest));
		
		DestinationManager<?> manager = mock(DestinationManager.class);
		when(manager.getScheme()).thenReturn("known");
		
		register(manager);
		
		Saver<?> s = new Saver<>(new Destination("known://klm"));
		ProxySettings proxy = ProxySettings.fromString("127.0.0.1:3128");
		s.setProxy(proxy);
		verify(manager).setProxy(proxy);
	}

	private void register(DestinationManager<?> manager) throws NoSuchFieldException, IllegalAccessException {
		final Field field = Saver.class.getDeclaredField("MANAGERS");
		field.setAccessible(true);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		PluginRegistry<DestinationManager> plugins = (PluginRegistry<DestinationManager>) field.get(null);
		plugins.register(manager);
	}
}
