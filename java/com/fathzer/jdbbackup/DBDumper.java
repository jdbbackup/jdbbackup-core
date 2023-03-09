package com.fathzer.jdbbackup;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import com.fathzer.jdbbackup.utils.BasicExtensionBuilder;

/** A class able to dump a database to a file.
 */
public interface DBDumper {

	/** Gets the extension builder of this dumper.
	 * <br>The returned instance is responsible for adding (or not) an extension to the target path.
	 * @return a function
	 */
	default Function<String,CharSequence> getExtensionBuilder() {
		return BasicExtensionBuilder.INSTANCE;
	}
	
	/** Gets the scheme used in the URL to identify the type of database this saver can dump.
	 * @return a string (example mysql)
	 */
	String getScheme();
	
	
	/** Saves a database to the specified location.
	 * @param source The address of the database to save (Its format depends on the DBDumper)
	 * @param destFile the backup destination file or null to backup database in a temporary file.
	 * @throws IOException If something went wrong
	 */
	void save(String source, File destFile) throws IOException;
}
