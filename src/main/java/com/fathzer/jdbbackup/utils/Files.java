package com.fathzer.jdbbackup.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class Files {
    static final BiPredicate<Path, BasicFileAttributes> IS_JAR = (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar");

    private Files() {
		super();
	}
	
	/** Get the URL of files included in a folder with a specified extension. 
	 * @param root The directory to search in or a file. 
	 * @param extension the extension used to select files
	 * @param depth The maximum number of directory levels to search.
	 * <br>A value of 1 means the search is limited to the jars directly under the searched folder.
	 * <br>To set no limit, you should set the depth to Integer.MAX_VALUE
	 * @return an array of URL, possibly empty. If root is a file and has the specified extension, returns the file's URL. 
	 * @throws IOException If something went wrong.
	 */
	public static URL[] toURL(File root, String extension, int depth) throws IOException {
		if (root.isDirectory()) {
			try (Stream<Path> files = java.nio.file.Files.find(root.toPath(), depth, IS_JAR)) {
				return files.map(f -> {
					try {
						return f.toUri().toURL();
					} catch (MalformedURLException e) {
						throw new UncheckedIOException(e);
				}}).toArray(URL[]::new);
		    }
		} else if (root.isFile() && root.getName().endsWith(extension)) {
			return new URL[] {root.toURI().toURL()};
		} else {
			return new URL[0];
		}
	}
}
