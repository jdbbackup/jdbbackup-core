package com.fathzer.jdbbackup.cmd;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.fathzer.jdbbackup.JDbBackup;
import com.fathzer.jdbbackup.managers.dropbox.ProxySettingsConverter;
import com.fathzer.jdbbackup.utils.ProxySettings;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** A command line tool to perform backup.
 */
@Command(name="java com.fathzer.jdbbackup.cmd.JDbBackupCmd", mixinStandardHelpOptions = true, description = "Saves a database to a destination", usageHelpWidth = 160)
public class JDbBackupCmd implements Callable<Integer> {
	@Parameters(index="0", description="Data base address (for example mysql://user:pwd@host:port/db")
    private String db;
	@Parameters(index="1", description = "Destination (example sftp://user:pwd@host/filepath)")
    private String dest;
	@Option(names={"-p","--proxy"}, description="The proxy used for the backup, format is [user[:pwd]@]host:port", converter = ProxySettingsConverter.class)
	private ProxySettings proxy;
	
	public static void main(String... args) {
		System.exit(new CommandLine(new JDbBackupCmd()).execute(args));
    }
	
	@SuppressWarnings("java:S106")
	public Integer call() throws Exception {
		try {
			new JDbBackup().backup(proxy, db, dest);
			return 0;
        } catch (IllegalArgumentException e) {
        	err(e);
        	return 1;
        } catch (IOException e) {
        	err(e);
        	return 2;
        }
	}
	
	@SuppressWarnings("java:S106")
	public static void out(String message) {
		System.out.println(message);
	}
	
	@SuppressWarnings("java:S106")
	public static void err(String message) {
		System.err.println(message);
	}
	
	@SuppressWarnings("java:S4507")
	public static void err(Throwable e) {
		e.printStackTrace();
	}
}
