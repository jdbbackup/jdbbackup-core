package com.fathzer.jdbbackup.managers.dropbox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.TokenAccessType;
import com.fathzer.jdbbackup.cmd.JDbBackupCmd;
import com.fathzer.jdbbackup.utils.ProxySettings;

import picocli.CommandLine;
import picocli.CommandLine.Option;

/** A helper class to obtain a token usable with DropBoxManager
 */
public class DropBoxTokenCmd extends DropBoxBase implements Callable<Integer> {
	@Option(names={"-p","--proxy"}, description="The proxy used to communicate with Dropbox, format is [user[:pwd]@]host:port", converter = ProxySettingsConverter.class)
	private ProxySettings proxy;
	
	public static void main(String... args) {
		System.exit(new CommandLine(new DropBoxTokenCmd()).execute(args));
    }

	public Integer call() throws Exception {
		setProxy(proxy);
		getToken();
		return 0;
	}

	private void getToken() {
	    DbxAppInfo appInfo = getAppInfo();
	    DbxWebAuth auth = new DbxWebAuth(getConfig(), appInfo);
	    DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
	             .withNoRedirect()
	             .withTokenAccessType(TokenAccessType.OFFLINE)
	             .build();
        String authorizeUrl = auth.authorize(authRequest);
        JDbBackupCmd.out("1. Go to: " + authorizeUrl);
        JDbBackupCmd.out("2. Click \"Allow\" (you might have to log in first)");
        JDbBackupCmd.out("3. Enter the authorization code there:");
		try {
			String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
			JDbBackupCmd.out("Please wait ...");
	        DbxAuthFinish authFinish = auth.finishFromCode(code);
	        String accessToken = authFinish.getRefreshToken();
	        JDbBackupCmd.out("Your token is: "+REFRESH_PREFIX+accessToken);
	        JDbBackupCmd.out("Keep it in a secure place as it allows to access to your backup folder on Dropbox");
		} catch (Exception e) {
			JDbBackupCmd.err ("Sorry, an error occurred:");
			JDbBackupCmd.err(e);
		}
	}
}
