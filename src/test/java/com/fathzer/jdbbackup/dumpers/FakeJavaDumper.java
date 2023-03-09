package com.fathzer.jdbbackup.dumpers;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fathzer.jdbbackup.utils.BasicExtensionBuilder;

public final class FakeJavaDumper extends DBDumperFromProcess {
	public static final List<String> CONTENT = Arrays.asList("Hello,","This is a fake db dump");
	public static boolean shouldFail;
	@Override
	public String getScheme() {
		return "java";
	}

	@Override
	protected List<String> getCommand(String params) {
		List<String> args = shouldFail ? Arrays.asList("java",FakeJavaDumper.class.getName()) :
			Arrays.asList("java","-cp","./target/classes"+File.pathSeparator+"./target/test-classes",FakeJavaDumper.class.getName());
		return args;
	}
	
	
	
	@Override
	public Function<String, CharSequence> getExtensionBuilder() {
		return new BasicExtensionBuilder("txt.gz");
	}

	public static void main(String[] args) {
		CONTENT.forEach(System.out::println);
	}
}