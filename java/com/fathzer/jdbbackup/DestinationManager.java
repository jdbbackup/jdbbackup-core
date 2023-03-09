package com.fathzer.jdbbackup;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import com.fathzer.jdbbackup.utils.ProxySettings;

/** An abstract class to manage where backup are saved.
 */
public interface DestinationManager<T> {
	public static final char URI_PATH_SEPARATOR = '/';
	
	/** Sets the proxy configuration of this manager.
	 * @param options The proxy options
	 */
	void setProxy(final ProxySettings options);
	
	/** Gets the string that identifies the protocol.
	 * <br>Example file, dropbox, sftp. This destination manager will have to process all file transfers related to destinations that begins with <i>protocol</i>:// 
	 * @return a String
	 */
	String getProtocol();

	/** Tests whether a destination is valid. 
	 * @param path The destination path.
	 * @param extensionBuilder a function that transforms a path that may contain or not an file extension (like .gz) to a path with the extension (To simplify, it adds the extension if needed).
	 * @return An internal representation of where the backup will be saved.
	 * @throws IllegalArgumentException If the path is not valid.
	 */
	T validate(final String path, Function<String,CharSequence> extensionBuilder);
	
	/** Sends the backup file to its final destination at the path passed in {@link #setDestinationPath}.
	 * <br>It is guaranteed that {@link DestinationManager#setProxy(ProxyOptions)} will be called before this method.
	 * @param in The input stream on the temporary file to save
	 * @param size The number of bytes to save (the size of the temporary file). The manager is free to ignore this information and save all bytes available in the input stream.
	 * @param destination The destination that was returned by {@link #validate(URI, Function)}
	 * @return a message indicating where the file was sent
	 * @throws IOException If an error occurs while sending the file
	 * @see #setProxy(ProxyOptions)
	 */
	String send(final InputStream in, long size, T destination) throws IOException;
}
