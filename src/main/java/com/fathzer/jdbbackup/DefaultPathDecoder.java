package com.fathzer.jdbbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A path decoder that replaces patterns with their actual value.
 * <br>It accepts patterns that have the format {<i>name</i>=<i>value</i>} where <i>name</i> is a lowercase string that identifies the kind of pattern and
 * <i>value</i> is a string that contains the pattern itself (note that the pattern can not contains '}' character.
 * <br>This class supports the following patterns:<ul>
 *   <li><b>d</b>: The <i>value</i> must be a valid date time pattern as described in
 *     <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.
 *     <br>For example, the pattern {d=yyyy} will be replaced by the year on 4 characters at runtime.</li>
 *   <li><b>e</b>: The <i>value</i> must be an existing environment variable whose value will replace the pattern</li>
 *   <li><b>p</b>: The <i>value</i> must be an existing System property whose value will replace the pattern</li>
 *   <li><b>f</b>: The <i>value</i> must be an existing file whose content will replace the pattern</li>
 * </ul>
 * <br>You can add your own pattern kind by overriding {@link #decode(String, String)} method.
 */
public class DefaultPathDecoder {
	private static final Pattern PATTERN = Pattern.compile("\\{(\\p{Lower}+)=([^\\}]+)\\}");
	
	/** A String splitter that ignores delimiter in pattern.
	 * @see com.fathzer.jdbbackup.utils.StringSplitter
	 */
	public static class StringSplitter extends com.fathzer.jdbbackup.utils.StringSplitter {
		/** Constructor.
		 * @param input The string to split
		 * @param delimiter The field delimiter
		 */
		public StringSplitter(String input, char delimiter) {
			super(input, delimiter, (s,p) -> s.charAt(p) == '{' ?  s.indexOf('}', p) : -1);
		}
	}
	
	/** An instance with the default settings.
	 */
	public static final DefaultPathDecoder INSTANCE = new DefaultPathDecoder();
	
	/** An exception that denotes a illegal pattern in path.
	 */
	public static class IllegalNamePatternException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;

		/** Constructor.
		 * @param message The exception message
		 */
		public IllegalNamePatternException(String message) {
			super(message);
		}
	}

	/** Constructor.
	 */
	public DefaultPathDecoder() {
		super();
	}

	/** Decodes a path.
	 * @param path The encoded path
	 * @return The decoded path
	 * @throws IllegalNamePatternException if the path has wrong format
	 */
	public String decodePath(String path) {
		Matcher m = PATTERN.matcher(path);
		StringBuilder sb = new StringBuilder();
		int previous = 0;
		while (m.find()) {
			if (previous!=m.start()) {
				sb.append(path.substring(previous, m.start()));
			}
			sb.append(decode(m.group(1), m.group(2)));
			previous = m.end();
		}
		if (previous<path.length()) {
			sb.append(path.substring(previous));
		}
		return sb.toString();
	}

	/** Decodes a path and adds extension if needed.
	 * @param path The encoded path
	 * @param extensionManager A function responsible for adding (or not) an extension to the path
	 * @return The decoded path
	 * @throws IllegalNamePatternException if the path has wrong format
	 */
	public String decodePath(String path, Function<String,CharSequence> extensionManager) {
		return extensionManager.apply(decodePath(path)).toString();
	}

	/** Decodes a pattern.
	 * <br>See {@link DefaultPathDecoder class comments} to learn what names are supported.
	 * @param name The pattern name.
	 * @param value The pattern value.
	 * @return The decoded pattern.
	 * @throws IllegalNamePatternException If the name in not a valid name or value is not a wlid valid for <i>name</i> pattern.
	 */
	protected CharSequence decode(String name, String value) {
		if ("d".equals(name)) {
			return decodeDate(value);
		} else if ("e".equals(name)) {
			return decodeEnvVar(value);
		} else if ("p".equals(name)) {
			return decodeSysProperty(value);
		} else if ("f".equals(name)) {
			return decodeFile(value);
		} else {
			throw new IllegalNamePatternException(name+" is not a valid pattern name");
		}
	}

	private CharSequence decodeFile(String value) {
		final Path path = Paths.get(value);
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("File "+value+" does not exists");
		}
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to read file "+value, e);
		}
	}

	private CharSequence decodeSysProperty(String value) {
		final String v = System.getProperty(value);
		if (v==null) {
			throw new IllegalArgumentException("No "+value+" system property defined");
		} else {
			return v;
		}
	}

	private CharSequence decodeEnvVar(String value) {
		final String v = System.getenv(value);
		if (v==null) {
			throw new IllegalArgumentException("No "+value+" environment variable defined");
		} else {
			return v;
		}
	}

	private CharSequence decodeDate(String value) {
		try {
			return new SimpleDateFormat(value).format(new Date());
		} catch (IllegalArgumentException e) {
			throw new IllegalNamePatternException(value+" is not a valid value for date pattern");
		}
	}
}
