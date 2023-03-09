package com.fathzer.jdbbackup.managers.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.fathzer.jdbbackup.DestinationManager;
import com.fathzer.jdbbackup.utils.ProxySettings;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * A destination manager that saves the backups to a sftp server.
 */
public class SFTPManager implements DestinationManager<SFTPDestination> {
	private ProxyHTTP proxy;

	public SFTPManager() {
		super();
	}

	@Override
	public void setProxy(ProxySettings options) {
		if (options != null) {
			proxy = new ProxyHTTP(options.getHost(), options.getPort());
			if (options.getLogin() != null) {
				proxy.setUserPasswd(options.getLogin().getUser(), options.getLogin().getPassword());
			}
		} else {
			proxy = null;
		}
	}

	@Override
	public SFTPDestination validate(String fileName, Function<String,CharSequence> extensionBuilder) {
		return new SFTPDestination(fileName, extensionBuilder);
	}

	@Override
	public String send(InputStream in, long size, SFTPDestination dest) throws IOException {
		try {
			final JSch jsch = new JSch();
			final Session session = jsch.getSession(dest.getUser(), dest.getHost(), dest.getPort());
			if (proxy != null) {
				session.setProxy(proxy);
			}
			session.setPassword(dest.getPassword());
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			try {
				send(session, dest, in);
				final String fullPath = dest.getPath() == null ? dest.getFilename() : dest.getPath() + URI_PATH_SEPARATOR + dest.getFilename();
				return "Sent to " + dest.getUser() + "@" + dest.getHost() + ": " + fullPath;
			} finally {
				session.disconnect();
			}
		} catch (JSchException e) {
			throw new IOException(e);
		}
	}

	void send(final Session session, SFTPDestination dest, InputStream in) throws JSchException, IOException {
		ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
		channel.connect();
		try {
			if (dest.getPath() != null) {
				mkdirs(channel, dest.getPath());
				channel.cd(dest.getPath());
			}
			channel.put(in, dest.getFilename());
		} catch (SftpException e) {
			throw new IOException(e);
		} finally {
			channel.exit();
			channel.disconnect();
		}
	}

	public static void mkdirs(ChannelSftp ch, String path) throws SftpException {
		final List<String> folders = new ArrayList<>(Arrays.asList(path.split("/")));
		StringBuilder fullPath;
		if (folders.get(0).isEmpty()) {
			// Absolute path
			fullPath = new StringBuilder("/");
			folders.remove(0);
		} else {
			// Relative Path
			fullPath = new StringBuilder("./");
		}
		for (String folder : folders) {
			final Collection<?> ls = ch.ls(fullPath.toString());
			if (!exists(ls, folder) && !folder.isEmpty()) {
				ch.mkdir(fullPath + folder);
			}
			fullPath.append(folder);
			fullPath.append("/");
		}
	}

	private static boolean exists(final Collection<?> ls, String folder) {
		boolean isExist = false;
		for (Object o : ls) {
			if (o instanceof LsEntry) {
				LsEntry e = (LsEntry) o;
				if (e.getAttrs().isDir() && e.getFilename().equals(folder)) {
					isExist = true;
				}
			}
		}
		return isExist;
	}

	@Override
	public String getProtocol() {
		return "sftp";
	}
}
