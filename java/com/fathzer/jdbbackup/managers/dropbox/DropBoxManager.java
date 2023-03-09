package com.fathzer.jdbbackup.managers.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadBuilder;
import com.dropbox.core.v2.files.WriteMode;

import com.fathzer.jdbbackup.DefaultPathDecoder;
import com.fathzer.jdbbackup.DestinationManager;

/** A destination manager that saves the backups to a dropbox account.
 * <br>Destination paths have the following format dropbox://<i>token</i>/<i>filePath</i>
 */
public class DropBoxManager extends DropBoxBase implements DestinationManager<DropBoxManager.DropBoxDestination> {

	static class DropBoxDestination {
		private String token;
		private String path;
		
		String getToken() {
			return token;
		}
		String getPath() {
			return path;
		}
	}
	
	/** Constructor.
	 */
	public DropBoxManager() {
		super();
	}

	@Override
	public String send(final InputStream in, long size, DropBoxDestination dest) throws IOException {
		DbxClientV2 client = new DbxClientV2(getConfig(), getCredential(dest.token));
		UploadBuilder builder = client.files().uploadBuilder(dest.path);
		builder.withMode(WriteMode.OVERWRITE);
		try {
			FileMetadata data = builder.uploadAndFinish(in, size);
			return "Sent to Dropbox: "+data.getPathDisplay()+" (rev: "+data.getRev()+")";
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}
	
	DbxCredential getCredential(String token) {
		if (token.startsWith(REFRESH_PREFIX)) {
			final DbxAppInfo info = getAppInfo();
			return new DbxCredential("fake", 0L, token.substring(REFRESH_PREFIX.length()), info.getKey(), info.getSecret());
		} else {
			return new DbxCredential(token);
		}
	}
	
	@Override
	public DropBoxDestination validate(final String fileName, Function<String,CharSequence> extensionBuilder) {
		int index = fileName.indexOf(URI_PATH_SEPARATOR);
		if (index<=0) {
			throw new IllegalArgumentException("Unable to locate token. "+"FileName should conform to the format access_token/path");
		}
		DropBoxDestination dest = new DropBoxDestination();
		dest.token = fileName.substring(0, index);
		dest.path = fileName.substring(index+1);
		if (dest.path.isEmpty()) {
			throw new IllegalArgumentException("Unable to locate destination path. Path should conform to the format access_token/path");
		}
		dest.path = DefaultPathDecoder.INSTANCE.decodePath(dest.path, extensionBuilder);
		if (dest.path.charAt(0)!=URI_PATH_SEPARATOR) {
			// Dropbox requires path that starts with a /
			dest.path = URI_PATH_SEPARATOR+dest.path;
		}
		// Check destination with Dropbox constraint on path
		try {
			CommitInfo.newBuilder(dest.path);
		} catch (IllegalArgumentException e) {
			// Throw a more explicit exception
			throw new IllegalArgumentException("The path does not match Dropbox path pattern",e);
		}
		return dest;
	}

	@Override
	public String getProtocol() {
		return "dropbox";
	}
}
