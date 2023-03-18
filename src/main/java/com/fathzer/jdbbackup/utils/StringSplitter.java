package com.fathzer.jdbbackup.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** An iterator of delimited fields that allow to ignore escaped delimiters.
 * <br>It allows to easily parse fields separated by / in "a/{file=/home/titi}/b" where escaped text is between curly brackets.
 * <br>You only have to define a function that will detect the escaped zone.
 * <br>Here is the example with the example above:<br>
 * <pre><code>
 * StringSplitter.EscapeZoneChecker checker = (s,p) -> s.charAt(p) == '{' ?  s.indexOf('}', p) : -1;
 * StringSplitter split = new StringSplitter("a/{file=/home/titi}/b",'/', checker);
 * while (split.hasNext()) {
 *   String next = split.next();
 * }
 * </code></pre>
 * <br>Particular cases:<ul>
 * <li>An empty string contains one empty field</li>
 * <li>A string starting with a delimiter starts with an empty field</li>
 * <li>A string ending with a delimiter ends with an empty field</li>
 * </ul>
 */
public class StringSplitter implements Iterator<String> {
	/** A function that detects escaped zones.
	 */
	@FunctionalInterface
	public static interface EscapeZoneChecker {
		/** Tests whether the char at <i>position</i> in <i>input</i> starts a escaped zone.
		 * @param input The string
		 * @param position The position to test
		 * @return -1 if the position does not starts an escaped zone. Otherwise, the index of last char of the zone
		 */
		int check(String input, int position);
	}
	
	private final String input;
	private final char delimiter;
	private final EscapeZoneChecker escapedZoneChecker;
	private int start;
	
	/** Constructor.
	 * @param input The string to split
	 * @param delimiter The field delimiter
	 * @param escapedZoneChecker the function that detects escaped zones
	 */
	public StringSplitter(String input, char delimiter, EscapeZoneChecker escapedZoneChecker) {
		this.input = input;
		this.delimiter = delimiter;
		this.escapedZoneChecker = escapedZoneChecker;
		this.start = 0;
	}
	
	@Override
	public boolean hasNext() {
		return start<=input.length();
	}

	@Override
	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		for (int current = start; current < input.length(); current++) {
		    final int escapedEnd = escapedZoneChecker.check(input, current);
			if (escapedEnd>0) {
	    		current = escapedEnd;
		    } else if (input.charAt(current) == delimiter) {
		    	// Quit because of delimiter encountered
		        final String result = input.substring(start, current);
		        start = current + 1;
		        return result;
		    }
		}
		// End of input
        final String result = input.substring(start);
        start=input.length()+1;
        return result;
	}
	
	/** Gets the remaining characters of the String.
	 * <br>This method has no impact on {@link #hasNext()} and {@link #next()} methods.
	 * @return a String or null if the {@link #hasNext()} is false.
	 */
	public String getRemaining() {
		return hasNext() ? input.substring(start) : null;
	}
}