package com.fathzer.jdbbackup.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

class AbstractPluginsDownloaderTest {

	@Test
	void test() throws IOException, InterruptedException {
		final Dispatcher dispatcher = new Dispatcher() {

		    @Override
		    public MockResponse dispatch (RecordedRequest request) throws InterruptedException {

		        switch (request.getPath()) {
		            case "/v1/chat/":
		                return new MockResponse().setResponseCode(200).setBody("ok");
		            case "/v1/chat/xxx":
		                return new MockResponse().setResponseCode(200).setBody("xxx");
		            case "/v1/profile/info":
		                return new MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}");
		        }
		        return new MockResponse().setResponseCode(404);
		    }
		};

		try (MockWebServer server = new MockWebServer()) {
			server.setDispatcher(dispatcher);
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

}
