package com.fathzer.jdbbackup.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A class that loads plugins from an Internet remote repository.
 */
public abstract class AbstractPluginsDownloader {
	private static final Logger log = LoggerFactory.getLogger(AbstractPluginsDownloader.class);
	
	private final PluginRegistry<?> registry;
	private final URI uri;
	private final Path localDirectory;
	private ProxySettings proxy;
	private String pluginTypeWording = "plugin";
	
	private HttpClient httpClient;
	
	/** Constructor.
	 * @param registry The registry where plugins are loaded.
	 * @param uri The uri where to load the remote plugin registry.
	 * @param localDirectory The folder where plugins jar files will be loaded.
	 */
	public AbstractPluginsDownloader(PluginRegistry<?> registry, URI uri, Path localDirectory) {
		this.uri = uri;
		this.localDirectory = localDirectory;
		this.registry = registry;
	}
	
	public void setProxy(ProxySettings proxy) {
		this.proxy = proxy;
	}
	
	public void setPluginTypeWording(String wording) {
		this.pluginTypeWording = wording;
	}
	
	/** Deletes all files in local directory.
	 * @throws IOException If something went wrong
	 */
	public void clean() throws IOException {
		if (Files.isDirectory(localDirectory)) {
			log.info("Deleting {} already downloaded in {}", pluginTypeWording, localDirectory);
			try (Stream<Path> files = Files.find(localDirectory, 1, (p, bfa) -> bfa.isRegularFile())) {
				final List<Path> toDelete = files.collect(Collectors.toList());
				for (Path p : toDelete) {
					Files.delete(p);
				}
			}
		}
	}
	
	/** Search for plugins in remote registry, then loads them and verify they are not missing anymore.
	 * @param keys The plugin's keys to search
	 * @throws IOException If something went wrong
	 */
	public void load(Set<String> keys) throws IOException {
		if (keys.isEmpty()) {
			return;
		}
		final Map<String, URI> remoteRegistry = getURIMap();
		checkMissingKeys(keys, k -> {return !remoteRegistry.containsKey(k);});
		if (!Files.exists(localDirectory)) {
			Files.createDirectories(localDirectory);
		}
		final Set<URI> toDownload = keys.stream().map(remoteRegistry::get).collect(Collectors.toSet());
		try {
			final URL[] pluginsUrls = PluginRegistry.getURLs(toDownload.stream().map(this::download).collect(Collectors.toList()));
			log.info("Start loading registry plugins from {}",Arrays.asList(pluginsUrls));
			this.registry.load(new URLClassLoader(pluginsUrls));
			log.info("registry plugins are loaded");
			checkMissingKeys(keys, s -> this.registry.get(s)==null);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	/** Downloads an URI to a file.
	 * @param uri The uri to download
	 * @return The path where the URI body was downloaded.
	 * @throws UncheckedIOException if something went wrong
	 */
	protected Path download(URI uri) {
		final Path file = localDirectory.resolve(Paths.get(uri.getPath()).getFileName());
		if (Files.exists(file)) {
			log.info("{} was already downloaded to file {}",uri, file);
		} else {
			log.info("Downloading {} to file {}",uri, file);
			final HttpRequest request = getRequestBuilder().uri(uri).build();
			try {
				final BodyHandler<Path> bodyHandler = (info) -> info.statusCode() == 200 ? BodySubscribers.ofFile(file) : BodySubscribers.replacing(Paths.get("/NULL"));
				final HttpResponse<Path> response = getHttpClient().send(request, bodyHandler);
				if (response.statusCode()!=200) {
					throw new IOException(String.format("Unexpected status code %d received while downloading %s", response.statusCode(), uri));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new UncheckedIOException(new IOException(e));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return file;
	}
	
	/** Checks that plugins are registered in a map.
	 * @param keys The keys to check.
	 * @throws IllegalArgumentException if some keys are missing
	 */
	private void checkMissingKeys(Set<String> keys, Predicate<String> missingDetector) {
		final Set<String> missing = keys.stream().filter(missingDetector).collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(String.format("Unable to find the following %s: %s", pluginTypeWording, missing));
		}
	}
	
	/** Gets the content of the remote registry.
	 * <br>This method gets an input stream from the uri passed to this class constructor, then pass this input stream to {@link #getURIMap(InputStream)} and return its result.
	 * @return A key to uri map.
	 * @throws IOException
	 */
	protected Map<String, URI> getURIMap() throws IOException {
		log.info("Downloading {} registry at {}", pluginTypeWording, uri);
		final HttpRequest.Builder requestBuilder = getRequestBuilder();
		customize(requestBuilder);
		final HttpRequest request = requestBuilder.uri(uri).build();
		try {
			final HttpResponse<InputStream> response = getHttpClient().send(request, BodyHandlers.ofInputStream());
			if (response.statusCode()!=200) {
				throw new IOException(String.format("Unexpected status code %d received while downloading %s registry", pluginTypeWording, response.statusCode()));
			}
			try (InputStream in = response.body()) {
				return getURIMap(in);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}
	
	/** Allows sub-classes to custimize the request used to query the registry.
	 * <br>For example, a sub-class can add headers to the request with this method.
	 * @param requestBuilder The request under construction.
	 */
	protected void customize(HttpRequest.Builder requestBuilder) {
		// Allows customization of request in sub-classes
	}
	
	protected abstract Map<String, URI> getURIMap(InputStream in) throws IOException;

	private HttpRequest.Builder getRequestBuilder() {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				  .version(HttpClient.Version.HTTP_2)
				  .GET();
		if (proxy!=null && proxy.getLogin()!=null) {
			final String login = proxy.getLogin().getUserName()+":"+String.valueOf(proxy.getLogin().getPassword());
			final String encoded = new String(Base64.getEncoder().encode(login.getBytes()));
            builder.setHeader("Proxy-Authorization", "Basic " + encoded);
		}
		return builder;
	}

	private HttpClient getHttpClient() {
		if (httpClient==null) {
			final Builder clientBuilder = HttpClient.newBuilder();
			if (proxy!=null) {
				clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
			}
			clientBuilder.connectTimeout(Duration.ofSeconds(30));
			clientBuilder.followRedirects(Redirect.ALWAYS);
			this.httpClient = clientBuilder.build();
		}
		return httpClient;
	}
}
