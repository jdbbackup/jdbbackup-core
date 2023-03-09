package com.fathzer.jdbbackup.managers.sftp;

import static com.fathzer.jdbbackup.DestinationManager.URI_PATH_SEPARATOR;

import java.util.function.Function;

import com.fathzer.jdbbackup.DefaultPathDecoder;

public class SFTPDestination {
	private String user;
	private String password;
	private String host;
	private int port;
	private String path;
	private String filename;

	/** Constructor.
	 * @param destination The destination in its string format: <i>user:pwd[@host[:port]][/path]/filename</i>
	 */
	public SFTPDestination(String destination, Function<String,CharSequence> extensionBuilder) {
		int index = destination.indexOf(URI_PATH_SEPARATOR);
		if (index < 0) {
			badFileName(destination);
		}
		parseConnectionData(destination, destination.substring(0, index));
		parsePath(destination, destination.substring(index + 1), extensionBuilder);
	}

	private void parseConnectionData(String fileName, String cData) {
		int index = cData.indexOf('@');
		if (index < 0) {
			this.host = "127.0.0.1";
			this.port = 22;
			parseUserData(fileName, cData);
		} else if (index==cData.length()-1) {
			badFileName(fileName);
		} else {
			parseUserData(fileName, cData.substring(0, index));
			parseHostData(fileName, cData.substring(index + 1));
		}
	}

	private void parseUserData(String fileName, String userData) {
		int index = userData.indexOf(':');
		if (index < 0) {
			badFileName(fileName);
		}
		this.user = userData.substring(0, index);
		this.password = userData.substring(index + 1);
	}

	private void parseHostData(String fileName, String hostData) {
		int index = hostData.indexOf(':');
		if (index < 0) {
			this.port = 22;
			this.host = hostData;
		} else {
			this.host = hostData.substring(0, index);
			try {
				this.port = Integer.parseInt(hostData.substring(index + 1));
			} catch (NumberFormatException e) {
				badFileName(fileName);
			}
		}
	}

	private void parsePath(String fileName, String path, Function<String,CharSequence> extensionBuilder) {
		int index = path.lastIndexOf(URI_PATH_SEPARATOR);
		if (index < 0) {
			this.filename = path;
		} else {
			this.path = DefaultPathDecoder.INSTANCE.decodePath(path.substring(0, index),s->s);
			this.filename = path.substring(index + 1);
		}
		if (filename.isEmpty()) {
			badFileName(fileName);
		}
		this.filename = DefaultPathDecoder.INSTANCE.decodePath(this.filename, extensionBuilder);
	}

	private void badFileName(String fileName) {
		throw new IllegalArgumentException(fileName + " does not match format user:pwd[@host[:port]][/path]/filename");
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}

	public String getFilename() {
		return filename;
	}
}