package com.fathzer.jdbbackup.utils;

/** A User/password login.
 */
public class Login {
	private final String user;
	private final String password;
	
	public Login(String user, String password) {
		super();
		this.user = user;
		this.password = password;
	}

	/** Converts a user:pwd formatted string to a Login. 
	 * @param userAndPwd The string. Its format should be user:pwd. pwd can be omitted.
	 * @return The login or null if the input String is empty or null
	 */
	public static Login fromString(String userAndPwd) {
		if (userAndPwd!=null && !userAndPwd.trim().isEmpty()) {
			final int index = userAndPwd.indexOf(':');
			return index<0 ? new Login(userAndPwd, null) : new Login(userAndPwd.substring(0,index), userAndPwd.substring(index+1));
		} else {
			return null;
		}
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.user);
		if (this.password!=null) {
			builder.append(":*******");
		}
		return builder.toString(); 
	}
	
	
}
