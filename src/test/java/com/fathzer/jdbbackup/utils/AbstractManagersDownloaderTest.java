package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

class AbstractManagersDownloaderTest {
	private static final String PLUGINS_JAR_URI_PATH = "/plugins/test.jar";
	private static final String MISSING_JAR_PLUGIN_KEY = "missing";
	private static final String VALID_PLUGIN_KEY = "test";
	private static final String REPOSITORY_OK_CONTENT = "repositoryOk";
	private static final String REPOSITORY_PATH = "/repository";
	private static final String FAKE_JAR_FILE_CONTENT = "A fake jar file";
	private static final String CUSTOM_HEADER = "myHeader";
	private static final String REPOSITORY_HEADER_VALUE = "repository";
	private static final String JAR_HEADER_VALUE = "jar";

	private final class TestPluginDownloader extends AbstractManagersDownloader {
		private final Map<String,URI> map;
		
		private TestPluginDownloader(URI uri, Path localDirectory) {
			super(uri, localDirectory);
			map = new HashMap<>();
			map.put(VALID_PLUGIN_KEY, getUri().resolve(PLUGINS_JAR_URI_PATH));
			map.put(MISSING_JAR_PLUGIN_KEY, getUri().resolve("/plugins/missing.jar"));
		}

		@Override
		protected Map<String, URI> getURIMap(InputStream in) throws IOException {
			final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			if (REPOSITORY_OK_CONTENT.equals(content)) {
				return map;
			} else {
				throw new IOException(content);
			}
		}

		@Override
		protected HttpRequest.Builder getRepositoryRequestBuilder() {
			HttpRequest.Builder requestBuilder = super.getRepositoryRequestBuilder();
			requestBuilder.header(CUSTOM_HEADER, REPOSITORY_HEADER_VALUE);
			return requestBuilder;
		}

		@Override
		protected HttpRequest.Builder getJarRequestBuilder(URI uri) {
			HttpRequest.Builder requestBuilder = super.getJarRequestBuilder(uri);
			requestBuilder.header(CUSTOM_HEADER, JAR_HEADER_VALUE);
			return requestBuilder;
		}

		@Override
		public Path getDownloadTarget(URI uri) {
			return super.getDownloadTarget(uri);
		}

		@Override
		public void downloadFile(URI uri, Path path) throws IOException {
			super.downloadFile(uri, path);
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
		            case REPOSITORY_PATH:
		                return new MockResponse().setResponseCode(200).setBody(REPOSITORY_OK_CONTENT);
		            case "/repositoryKo":
		                return new MockResponse().setResponseCode(200).setBody("repositoryKo");
		            case PLUGINS_JAR_URI_PATH:
		                return new MockResponse().setResponseCode(200).setBody(FAKE_JAR_FILE_CONTENT);
		        }
		        return new MockResponse().setResponseCode(404);
		    }
		};
		server = new MockWebServer();
		// Start the server.
		server.setDispatcher(dispatcher);
		server.start();
		previousLogLevel = LogUtils.setLevel((SimpleLogger) LoggerFactory.getLogger(AbstractManagersDownloader.class), "off");
	}
	
	@AfterAll
	static void cleanUp() throws IOException {
		LogUtils.setLevel((SimpleLogger) LoggerFactory.getLogger(AbstractManagersDownloader.class), previousLogLevel);
		server.close();
	}
	
	@Test
	void testUnknownURI(@TempDir Path dir) throws IOException {
		{
			final AbstractManagersDownloader downloader = new TestPluginDownloader(server.url("/repositoryKo").uri(), dir);
			assertThrows (IOException.class, () -> downloader.getURIMap());
		}

		final AbstractManagersDownloader downloader = new TestPluginDownloader(server.url("/repositoryUnknown").uri(), dir);
		assertThrows (IOException.class, () -> downloader.getURIMap());
	}

	@Test
	void test(@TempDir Path dir) throws Exception {
		final TestPluginDownloader downloader = new TestPluginDownloader(server.url(REPOSITORY_PATH).uri(), dir);
		
		// Test getting remote plugins map is correct
		clearRequests();
		final Map<String, URI> map = downloader.getURIMap();
		assertEquals(new HashSet<>(Arrays.asList(VALID_PLUGIN_KEY,MISSING_JAR_PLUGIN_KEY)), map.keySet());
		RecordedRequest request = server.takeRequest();
		assertEquals(REPOSITORY_HEADER_VALUE,request.getHeader(CUSTOM_HEADER));

		// Test load of a key missing in repository
		assertThrows(IllegalArgumentException.class, () -> downloader.download("Not in repository"));

		// Test load nothing doesn't throw exception
		assertTrue(downloader.download().isEmpty());
	}
	
	@Test
	void testEmptyDir(@TempDir Path dir) throws Exception {
		final TestPluginDownloader downloader = new TestPluginDownloader(server.url(REPOSITORY_PATH).uri(), dir);
		assertTrue(Files.deleteIfExists(dir), "Problem while deleting temp dir");
		assertFalse(downloader.clean()); // Test no exception is thrown when dir does not exists
		final URI uri = server.url(PLUGINS_JAR_URI_PATH).uri();
		final Path path = downloader.getDownloadTarget(uri);
		downloader.downloadFile(uri, path);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
	}
	
	@Test
	void testDownloadAndClean(@TempDir Path dir) throws IOException, InterruptedException {
		final TestPluginDownloader downloader = new TestPluginDownloader(server.url(REPOSITORY_PATH).uri(), dir);
		final URI existingURI = server.url(PLUGINS_JAR_URI_PATH).uri();
		clearRequests();
		Path path = downloader.getDownloadTarget(existingURI);
		downloader.downloadFile(existingURI, path);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		// Test already downloaded jar is not reloaded
		assertFalse(downloader.shouldLoad(existingURI, path));
		
		RecordedRequest request = server.takeRequest();
		assertEquals(JAR_HEADER_VALUE, request.getHeader(CUSTOM_HEADER));
		
		assertTrue(downloader.clean());
		assertFalse(Files.isRegularFile(path));

		final URI missingURI = server.url("/plugins/missing.jar").uri();
		final Path path2 = downloader.getDownloadTarget(missingURI);
		assertThrows(IOException.class, ()->downloader.downloadFile(missingURI, path2));
	}

	private void clearRequests() throws InterruptedException {
		do {} while(server.takeRequest(100, TimeUnit.MILLISECONDS)!=null);
	}
}
