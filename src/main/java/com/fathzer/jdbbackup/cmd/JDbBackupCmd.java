package com.fathzer.jdbbackup.cmd;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.fathzer.jdbbackup.JDbBackup;
import com.fathzer.jdbbackup.utils.ProxySettings;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** A command line tool to perform backup.
 */
@Command(name="java com.fathzer.jdbbackup.cmd.JDbBackupCmd", mixinStandardHelpOptions = true, description = "Saves a database to a destination", usageHelpWidth = 160)
public class JDbBackupCmd implements Callable<Integer>, CommandLineSupport {
	@Parameters(index="0", description="Data base address (for example mysql://user:pwd@host:port/db")
    private String db;
	@Parameters(index="1", description = "Destination (example sftp://user:pwd@host/filepath)")
    private String dest;
	@Option(names={"-p","--proxy"}, description="The proxy used for the backup, format is [user[:pwd]@]host:port", converter = ProxySettingsConverter.class)
	private ProxySettings proxy;
	
	/** Launches the command.
	 * @param args The command arguments. Run the class without any arguments to know what are the available arguments.
	 */
	public static void main(String... args) {
		System.exit(new CommandLine(new JDbBackupCmd()).execute(args));
    }
	
	@Override
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
}
