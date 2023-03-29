package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

class AbstractPluginsDownloaderTest {
	@TempDir
	private Path directory;  

	@Test
	void test() throws IOException, InterruptedException {
		try (MockWebServer server = new MockWebServer()) {
			// Start the server.
			server.setDispatcher(buildDispatcher());
			server.start();
			
			final URI registryURI = server.url("/registry").uri();
			final PluginRegistry<?> plugins = new PluginRegistry<>(Object.class, Object::toString); 
			final AbstractPluginsDownloader downloader = new AbstractPluginsDownloader(plugins, registryURI, directory) {
				@Override
				protected Map<String, URI> getURIMap(InputStream in) throws IOException {
					final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					if ("registryOk".equals(content)) {
						return Collections.singletonMap("key", registryURI.resolve("/plugins/test.jar"));
					} else {
						throw new IOException("Not the right URI");
					}
				}

				@Override
				protected void customize(Builder requestBuilder) {
					requestBuilder.header("myHeader", "aValue");
				}
			};
			
			final Map<String, URI> map = downloader.getURIMap();
			assertEquals(Collections.singletonMap("key", registryURI.resolve("/plugins/test.jar")), map);
			RecordedRequest request = server.takeRequest();
			assertEquals("aValue",request.getHeader("myHeader"));

			// Ask the server for its URL. You'll need this to make HTTP requests.
//			URI baseUrl = server.url("/v1/chat/").uri();
//
//			final HttpClient client = HttpClient.newBuilder().build();
//			HttpResponse<String> resp = client.send(HttpRequest.newBuilder(baseUrl).build(), BodyHandlers.ofString());
//			System.out.println(resp.body());
//			resp = client.send(HttpRequest.newBuilder(baseUrl.resolve("xxx")).build(), BodyHandlers.ofString());
//			System.out.println(resp.body());
//			resp = client.send(HttpRequest.newBuilder(baseUrl.resolve("xxx")).build(), BodyHandlers.ofString());
//			System.out.println(resp.body());
//			RecordedRequest request = server.takeRequest();
//			System.out.println(request.getPath());
//			request = server.takeRequest();
//			System.out.println(request.getPath());
		}
	}

	private Dispatcher buildDispatcher() {
		final Dispatcher dispatcher = new Dispatcher() {
		    @Override
		    public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
		        switch (request.getPath()) {
		            case "/registry":
		                return new MockResponse().setResponseCode(200).setBody("registryOk");
		            case "/v1/chat/xxx":
		                return new MockResponse().setResponseCode(200).setBody("xxx");
		            case "/v1/profile/info":
		                return new MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}");
		        }
		        return new MockResponse().setResponseCode(404);
		    }
		};
		return dispatcher;
	}
}
