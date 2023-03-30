package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.LogUtils;
import org.slf4j.simple.SimpleLogger;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

class AbstractPluginsDownloaderTest {
	private static final String MISSING_JAR_PLUGIN_KEY = "missing";
	private static final String VALID_PLUGIN_KEY = "test";
	private static final String REGISTRY_OK_CONTENT = "registryOk";
	private static final String REGISTRY_PATH = "/registry";
	private static final String FAKE_JAR_FILE_CONTENT = "A fake jar file";
	private static final String CUSTOM_HEADER = "myHeader";
	private static final String REGISTRY_HEADER_VALUE = "registry";
	private static final String JAR_HEADER_VALUE = "jar";

	private final class TestPluginDownloader extends AbstractPluginsDownloader {
		private final Map<String,URI> map;
		
		private TestPluginDownloader(PluginRegistry<?> registry, URI uri, Path localDirectory) {
			super(registry, uri, localDirectory);
			map = new HashMap<>();
			map.put(VALID_PLUGIN_KEY, getUri().resolve("/plugins/test.jar"));
			map.put(MISSING_JAR_PLUGIN_KEY, getUri().resolve("/plugins/missing.jar"));
		}

		@Override
		protected Map<String, URI> getURIMap(InputStream in) throws IOException {
			final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			if (REGISTRY_OK_CONTENT.equals(content)) {
				return map;
			} else {
				throw new IOException(content);
			}
		}

		@Override
		protected void customizeRegistryRequest(Builder requestBuilder) {
			super.customizeRegistryRequest(requestBuilder);
			requestBuilder.header(CUSTOM_HEADER, REGISTRY_HEADER_VALUE);
		}

		@Override
		protected void customizeJarRequest(Builder requestBuilder) {
			super.customizeJarRequest(requestBuilder);
			requestBuilder.header(CUSTOM_HEADER, JAR_HEADER_VALUE);
		}
	}

	private static MockWebServer server;
	private static int previousLogLevel;
	
	@BeforeAll
	static void init() throws IOException {
		final Dispatcher dispatcher = new Dispatcher() {
		    @Override
		    public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
		        switch (request.getPath()) {
		            case REGISTRY_PATH:
		                return new MockResponse().setResponseCode(200).setBody(REGISTRY_OK_CONTENT);
		            case "/registryKo":
		                return new MockResponse().setResponseCode(200).setBody("registryKo");
		            case "/plugins/test.jar":
		                return new MockResponse().setResponseCode(200).setBody(FAKE_JAR_FILE_CONTENT);
		        }
		        return new MockResponse().setResponseCode(404);
		    }
		};
		server = new MockWebServer();
		// Start the server.
		server.setDispatcher(dispatcher);
		server.start();
		previousLogLevel = LogUtils.setLevel((SimpleLogger) LoggerFactory.getLogger(AbstractPluginsDownloader.class), "off");
	}
	
	@AfterAll
	static void cleanUp() throws IOException {
		LogUtils.setLevel((SimpleLogger) LoggerFactory.getLogger(AbstractPluginsDownloader.class), previousLogLevel);
		server.close();
	}
	
	@Test
	void testUnknownURI() throws IOException {
		final PluginRegistry<?> plugins = new PluginRegistry<>(Object.class, Object::toString);
		{
			final AbstractPluginsDownloader downloader = new TestPluginDownloader(plugins, server.url("/registryKo").uri(), null);
			assertThrows (IOException.class, () -> downloader.getURIMap());
		}

		final AbstractPluginsDownloader downloader = new TestPluginDownloader(plugins, server.url("/registryUnknown").uri(), null);
		assertThrows (IOException.class, () -> downloader.getURIMap());
	}

	@Test
	void test(@TempDir Path dir) throws IOException, InterruptedException {
		@SuppressWarnings("unchecked")
		PluginRegistry<? super Object> registry = mock(PluginRegistry.class);
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(registry, server.url(REGISTRY_PATH).uri(), dir);
		
		// Test getting remote plugins map is correct
		clearRequests();
		final Map<String, URI> map = downloader.getURIMap();
		assertEquals(new HashSet<>(Arrays.asList(VALID_PLUGIN_KEY,MISSING_JAR_PLUGIN_KEY)), map.keySet());
		RecordedRequest request = server.takeRequest();
		assertEquals(REGISTRY_HEADER_VALUE,request.getHeader(CUSTOM_HEADER));

		// Test load of a valid key
		when(registry.get(VALID_PLUGIN_KEY)).thenReturn("ok");
		downloader.load(Collections.singleton(VALID_PLUGIN_KEY));
		
		// Test load of a key missing in registry
		assertThrows(IllegalArgumentException.class, () -> downloader.load(Collections.singleton("Not in registry")));

		// Test load of a key in registry, but with missing jar
		assertThrows(IOException.class, () -> downloader.load(Collections.singleton(MISSING_JAR_PLUGIN_KEY)));
	}
	
	@Test
	void testDownloadAndClean(@TempDir Path dir) throws IOException, InterruptedException {
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(null, server.url(REGISTRY_PATH).uri(), dir);
		final URI existingURI = server.url("/plugins/test.jar").uri();
		clearRequests();
		Path path = downloader.download(existingURI);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		RecordedRequest request = server.takeRequest();
		assertEquals(JAR_HEADER_VALUE, request.getHeader(CUSTOM_HEADER));
		
		downloader.clean();
		assertFalse(Files.isRegularFile(path));

		final URI missingURI = server.url("/plugins/missing.jar").uri();
		assertThrows(UncheckedIOException.class, ()->downloader.download(missingURI));
	}

	private void clearRequests() throws InterruptedException {
		do {} while(server.takeRequest(100, TimeUnit.MILLISECONDS)!=null);
	}
}
