package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

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
	private static final String FAKE_JAR_FILE_CONTENT = "A fake jar file";

	private final class TestPluginDownloader extends AbstractPluginsDownloader {
		private TestPluginDownloader(PluginRegistry<?> registry, URI uri, Path localDirectory) {
			super(registry, uri, localDirectory);
		}

		@Override
		protected Map<String, URI> getURIMap(InputStream in) throws IOException {
			final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			if ("registryOk".equals(content)) {
				return Collections.singletonMap("key", getUri().resolve("/plugins/test.jar"));
			} else if ("registryUnknownJar".equals(content)) {
				return Collections.singletonMap("key", getUri().resolve("/plugins/unknown.jar"));
			} else {
				throw new IOException(content);
			}
		}

		@Override
		protected void customize(Builder requestBuilder) {
			super.customize(requestBuilder);
			requestBuilder.header("myHeader", "aValue");
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
		            case "/registry":
		                return new MockResponse().setResponseCode(200).setBody("registryOk");
		            case "/registryKo":
		                return new MockResponse().setResponseCode(200).setBody("registryKo");
		            case "/registryUnknownJar":
		                return new MockResponse().setResponseCode(200).setBody("registryUnknownJar");
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
	void testGetURIMap(@TempDir Path dir) throws IOException, InterruptedException {
		final URI registryURI = server.url("/registry").uri();
		final PluginRegistry<?> plugins = new PluginRegistry<>(Object.class, Object::toString); 
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(plugins, registryURI, null);
		
		final Map<String, URI> map = downloader.getURIMap();
		assertEquals(Collections.singletonMap("key", server.url("/plugins/test.jar").uri()), map);
		RecordedRequest request = server.takeRequest();
		assertEquals("aValue",request.getHeader("myHeader"));
	}
	
	@Test
	void testDownloadAndClean(@TempDir Path dir) throws IOException {
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(null, null, dir);
		final URI existingURI = server.url("/plugins/test.jar").uri();
		Path path = downloader.download(existingURI);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		downloader.clean();
		assertFalse(Files.isRegularFile(path));

		final URI missingURI = server.url("/plugins/missing.jar").uri();
		assertThrows(UncheckedIOException.class, ()->downloader.download(missingURI));
	}
}
