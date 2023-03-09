package com.fathzer.jdbbackup.dumpers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fathzer.jdbbackup.utils.Login;

/** A DBSaver that saves MYSQL database.
 * <br>It requires mysqldump to be installed on the machine.
 * <br>The URI format is mysql://<i>user</i>:<i>pwd</i>@<i>host</i>[:<i>port</i>]/<i>database</i>
 * <br>Default port is 3306
 */
public class MySQLDumper extends DBDumperFromProcess {
	@Override
	protected List<String> getCommand(String source) {
		URI params = URI.create(source);
		if (!getScheme().equals(params.getScheme())) {
			throw new IllegalArgumentException("Does not support "+params.getScheme()+" scheme");
		}
		final int port = getPort(params);
		final String dbName = getDBName(params);
		final Login login = Login.fromString(params.getUserInfo());  
		if (isEmpty(dbName) || isEmpty(params.getHost()) || port<=0 || login==null || isEmpty(login.getUser()) || isEmpty(login.getPassword())) {
			throw new IllegalArgumentException("Invalid URI");
		}
		final List<String> commands = new ArrayList<>();
		commands.add("mysqldump");
		commands.add("--host="+params.getHost());
		commands.add("--port="+port);
		commands.add("--user="+login.getUser());
		commands.add("--password="+login.getPassword());
		commands.add("--add-drop-database");
		commands.add(dbName);
		return commands;
	}
	
	private int getPort(URI uri) {
		int port = uri.getPort();
		if (port==-1) {
			port=3306;
		}
		return port;
	}
	
	private String getDBName(URI uri) {
		String result = uri.getPath();
		if (result.startsWith("/")) {
			// Remove initial /
			result = result.substring(1);
		}
		return result;
	}

	@Override
	public String getScheme() {
		return "mysql";
	}
	
	private boolean isEmpty(String str) {
		return str==null || str.trim().isEmpty();
	}
}
