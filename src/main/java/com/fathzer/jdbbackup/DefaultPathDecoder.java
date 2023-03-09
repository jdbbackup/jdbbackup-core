package com.fathzer.jdbbackup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A path decoder that replaces patterns with their actual value.
 * <br>It accepts patterns that have the format {<i>name</i>=<i>value</i>} where <i>name</i> is a lowercase string that identifies the kind of pattern and
 * <i>value</i> is a string that contains the pattern itself (note that the pattern can not contains '}' character.
 * <br>This class only supports one pattern kind: <b>d</b>. The <i>value</i> should be a valid date time pattern
 * as described in <a href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.
 * <br>For example, the pattern {d=yyyy} should be replaced by the year on 4 characters at runtime.
 * <br>You can add your own pattern kind by overriding {@link #decode(String, String)} method.
 */
public class DefaultPathDecoder {
	private static final Pattern PATTERN = Pattern.compile("\\{(\\p{Lower}+)=([^\\}]+)\\}");
	public static final DefaultPathDecoder INSTANCE = new DefaultPathDecoder();

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
	public String decodePath(String path) throws IllegalNamePatternException {
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
	public String decodePath(String path, Function<String,CharSequence> extensionManager) throws IllegalNamePatternException {
		return extensionManager.apply(decodePath(path)).toString();
	}

	/** Decodes a pattern.
	 * <br>See {@link DefaultPathDecoder class comments} to learn what names are supported.
	 * @param name The pattern name.
	 * @param value The pattern value.
	 * @return The decoded pattern.
	 * @throws IllegalNamePatternException If the name in not a valid name or value is not a wlid valid for <i>name</i> pattern.
	 */
	protected CharSequence decode(String name, String value) throws IllegalNamePatternException {
		if ("d".equals(name)) {
			try {
				return new SimpleDateFormat(value).format(new Date());
			} catch (IllegalArgumentException e) {
				throw new IllegalNamePatternException(value+" is not a valid value for "+name+" pattern");
			}
		} else {
			throw new IllegalNamePatternException(name+" is not a valid pattern name");
		}
	}
}
