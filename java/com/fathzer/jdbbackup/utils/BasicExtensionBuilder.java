package com.fathzer.jdbbackup.utils;

import java.nio.file.Paths;
import java.util.function.Function;

/** A default extension builder that does nothing if an extension is already present and adds one if not.
 */
public class BasicExtensionBuilder implements Function<String,CharSequence> {
	/** An instance that adds sql.gz extension.
	 */
	public static final BasicExtensionBuilder INSTANCE = new BasicExtensionBuilder("sql.gz");
	
	private final String extension;
	
	/** Constructor.
	 * @param extension The extension to add (with no period)
	 */
	public BasicExtensionBuilder(String extension) {
		this.extension = extension;
	}

	@Override
	public CharSequence apply(String path) {
		if (hasExtension(path)) {
			return path;
		} else {
			return path + "." + extension;
		}
	}
	
	/** Test whether a path contains an extension.
	 * @param path The path to test
	 * @return true if the path has an extension
	 */
	public boolean hasExtension(String path) {
		return Paths.get(path).getFileName().toString().contains(".");
	}
}
