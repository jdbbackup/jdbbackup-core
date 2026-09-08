package com.fathzer.jdbbackup.sources;

import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fathzer.plugin.loader.utils.LoginParser;

/** A source manager that dumps MYSQL database.
 * <br>It requires mysqldump to be installed on the machine.
 * <br>The URI format is {@code mysql://<i>user</i>:<i>pwd</i>@<i>host</i>[:<i>port</i>]/<i>database</i>[?<i>query</i>]}
 * <br>Default port is 3306.
 * <br>The following optional query parameters are supported (all are opt-in, none is enabled by default):
 * <ul>
 *   <li><b>single-transaction</b> &ndash; adds {@code --single-transaction} to mysqldump. This creates a consistent
 *       snapshot of InnoDB tables without locking them. Recommended for InnoDB databases. Has no effect on MyISAM tables.</li>
 *   <li><b>routines</b> &ndash; adds {@code --routines} to mysqldump. Includes stored procedures and functions in the dump.</li>
 *   <li><b>events</b> &ndash; adds {@code --events} to mysqldump. Includes scheduled events in the dump.</li>
 *   <li><b>hex-blob</b> &ndash; adds {@code --hex-blob} to mysqldump. Dumps binary columns in hexadecimal notation,
 *       which is safer for binary data but produces larger output.</li>
 *   <li><b>default-character-set</b>=<i>charset</i> &ndash; adds {@code --default-character-set=<i>charset</i>} to mysqldump.
 *       Forces the character set used for the dump (e.g. {@code utf8mb4}).</li>
 * </ul>
 * <br>Boolean parameters (single-transaction, routines, events, hex-blob) can be set to {@code false} to explicitly
 * disable them, e.g. {@code ?single-transaction=false}. Their absence is equivalent to {@code false}.
 * <br>Example: {@code mysql://user:pwd@host:3306/mydb?single-transaction&routines&events&default-character-set=utf8mb4}
 */
public class MySQLDumper extends SourceManagerFromProcess {
	@Override
	protected List<String> getCommand(String source) {
		URI params = URI.create(source);
		if (!getScheme().equals(params.getScheme())) {
			throw new IllegalArgumentException("Does not support "+params.getScheme()+" scheme");
		}
		final int port = getPort(params);
		final String dbName = getDBName(params);
		final PasswordAuthentication login = LoginParser.fromString(params.getUserInfo());  
		if (isEmpty(dbName) || isEmpty(params.getHost()) || port<=0 || login==null || isEmpty(login.getUserName()) || login.getPassword().length==0) {
			throw new IllegalArgumentException("Invalid URI");
		}
		final List<String> commands = new ArrayList<>();
		commands.add("mysqldump");
		commands.add("--host="+params.getHost());
		commands.add("--port="+port);
		commands.add("--user="+login.getUserName());
		commands.add("--password="+new String(login.getPassword()));
		commands.add("--add-drop-database");
		commands.add("--databases");
		// Optional parameters from query string
		String query = params.getQuery();
		if (query != null) {
			addOption(commands, query, "single-transaction", "--single-transaction");
			addOption(commands, query, "routines", "--routines");
			addOption(commands, query, "events", "--events");
			addOption(commands, query, "hex-blob", "--hex-blob");
			addOption(commands, query, "default-character-set", "--default-character-set");
		}
		commands.add(dbName);
		return commands;
	}

	/** Adds an option to the command if the corresponding query parameter is present and not set to false.
	 * @param commands the command list to append to.
	 * @param query the raw query string.
	 * @param paramName the query parameter name.
	 * @param option the mysqldump option to add (without value).
	 * @param ignored not used for boolean options.
	 */
	private void addOption(List<String> commands, String query, String paramName, String option) {
		String value = getQueryParam(query, paramName);
		if (value != null && !isFalse(value)) {
			if (paramName.equals("default-character-set")) {
				commands.add(option + "=" + value);
			} else {
				commands.add(option);
			}
		}
	}

	/** Extracts the value of a query parameter from a raw query string.
	 * @param query the raw query string (e.g. "single-transaction&routines=false&default-character-set=utf8mb4").
	 * @param paramName the parameter name to look for.
	 * @return the parameter value (empty string for flag-style params like "single-transaction"), or null if not present. */
	private static String getQueryParam(String query, String paramName) {
		for (String pair : query.split("&")) {
			int eq = pair.indexOf('=');
			String name = eq >= 0 ? pair.substring(0, eq) : pair;
			String value = eq >= 0 ? pair.substring(eq + 1) : "";
			if (name.equals(paramName)) {
				return value;
			}
		}
		return null;
	}

	/** Returns true if the value represents "false" (case-insensitive). */
	private static boolean isFalse(String value) {
		return "false".equalsIgnoreCase(value);
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
