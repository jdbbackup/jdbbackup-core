package com.fathzer.jdbbackup.cmd;

import com.fathzer.jdbbackup.utils.ProxySettings;

import picocli.CommandLine.ITypeConverter;

/** A converter that converts a String to a ProxySettings instance.
 */
public class ProxySettingsConverter implements ITypeConverter<ProxySettings> {
	@Override
	public ProxySettings convert(String value) throws Exception {
		return ProxySettings.fromString(value);
	}
}
