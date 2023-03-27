package com.fathzer.jdbbackup;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import com.fathzer.jdbbackup.utils.BasicExtensionBuilder;

/** A class able to dump a data source to a file.
 */
public interface SourceManager {

	/** Gets the extension builder of this manager.
	 * <br>The returned instance is responsible for adding (or not) an extension to the target path.
	 * @return a function
	 */
	default Function<String,CharSequence> getExtensionBuilder() {
		return BasicExtensionBuilder.INSTANCE;
	}
	
	/** Gets the scheme used in the URL to identify the type of data source this manager can dump.
	 * @return a string (example mysql)
	 */
	String getScheme();
	
	
	/** Saves a data source to the specified location.
	 * @param source The address of the data source to save (Its format depends on the SourceManager)
	 * @param destFile the backup destination file.
	 * @throws IOException If something went wrong
	 */
	void save(String source, File destFile) throws IOException;
}
