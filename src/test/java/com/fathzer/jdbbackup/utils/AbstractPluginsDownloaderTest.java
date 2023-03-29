package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
			server.setDispatcher(buildDispatcher());
			
			final URI registryURI = server.url("/registry").uri();
			final PluginRegistry<?> plugins = new PluginRegistry<>(Object.class, Object::toString); 
			new AbstractPluginsDownloader(plugins, registryURI, directory) {
				@Override
				protected Map<String, URI> getURIMap(InputStream in) throws IOException {
					new String(in.readAllBytes(), StandardCharsets.UTF_8);
					// TODO Auto-generated method stub
					return null;
				}
			};
			
			// Schedule some responses.
//			server.enqueue(new MockResponse().setBody("hello, world!"));
//			server.enqueue(new MockResponse().setBody("sup, bra?"));
//			server.enqueue(new MockResponse().setBody("yo dog"));

			// Start the server.
			server.start();

			// Ask the server for its URL. You'll need this to make HTTP requests.
			URI baseUrl = server.url("/v1/chat/").uri();

			final HttpClient client = HttpClient.newBuilder().build();
			HttpResponse<String> resp = client.send(HttpRequest.newBuilder(baseUrl).build(), BodyHandlers.ofString());
			System.out.println(resp.body());
			resp = client.send(HttpRequest.newBuilder(baseUrl.resolve("xxx")).build(), BodyHandlers.ofString());
			System.out.println(resp.body());
			resp = client.send(HttpRequest.newBuilder(baseUrl.resolve("xxx")).build(), BodyHandlers.ofString());
			System.out.println(resp.body());
			RecordedRequest request = server.takeRequest();
			System.out.println(request.getPath());
			request = server.takeRequest();
			System.out.println(request.getPath());
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
