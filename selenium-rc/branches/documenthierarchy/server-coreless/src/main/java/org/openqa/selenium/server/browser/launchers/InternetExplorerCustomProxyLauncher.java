/*
 * Copyright 2006 ThoughtWorks, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package org.openqa.selenium.server.browser.launchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.openqa.selenium.server.browser.launchers.WindowsUtils.WindowsRegistryException;
import org.openqa.selenium.server.configuration.SeleniumConfiguration;
import org.openqa.selenium.server.configuration.SeleniumConfigurationOption;

public class InternetExplorerCustomProxyLauncher extends
		AbstractBrowserLauncher {

	private static Logger logger = Logger
			.getLogger(InternetExplorerCustomProxyLauncher.class);

	protected static final String REG_KEY_SELENIUM_FOLDER = "HKEY_CURRENT_USER\\Software\\Selenium\\RemoteControl\\";

	protected static final String REG_KEY_BACKUP_READY = REG_KEY_SELENIUM_FOLDER
			+ "BackupReady";

	protected static final String REG_KEY_BACKUP_AUTOCONFIG_URL = REG_KEY_SELENIUM_FOLDER
			+ "AutoConfigURL";

	protected static final String REG_KEY_BACKUP_MAX_CONNECTIONS_PER_1_0_SVR = REG_KEY_SELENIUM_FOLDER
			+ "MaxConnectionsPer1_0Server";

	protected static final String REG_KEY_BACKUP_MAX_CONNECTIONS_PER_1_1_SVR = REG_KEY_SELENIUM_FOLDER
			+ "MaxConnectionsPerServer";

	protected static final String REG_KEY_BACKUP_PROXY_ENABLE = REG_KEY_SELENIUM_FOLDER
			+ "ProxyEnable";

	protected static final String REG_KEY_BACKUP_PROXY_OVERRIDE = REG_KEY_SELENIUM_FOLDER
			+ "ProxyOverride";

	protected static final String REG_KEY_BACKUP_PROXY_SERVER = REG_KEY_SELENIUM_FOLDER
			+ "ProxyServer";

	protected static final String REG_KEY_BACKUP_AUTOPROXY_RESULT_CACHE = REG_KEY_SELENIUM_FOLDER
			+ "EnableAutoproxyResultCache";

	protected static final String REG_KEY_BACKUP_POPUP_MGR = REG_KEY_SELENIUM_FOLDER
			+ "PopupMgr";

	protected static final String REG_KEY_BACKUP_MIME_EXCLUSION_LIST_FOR_CACHE = REG_KEY_SELENIUM_FOLDER
			+ "MimeExclusionListForCache";

	protected static String REG_KEY_BASE = "HKEY_CURRENT_USER";

	protected static final String REG_KEY_POPUP_MGR = "\\Software\\Microsoft\\Internet Explorer\\New Windows\\PopupMgr";

	protected static final String REG_KEY_AUTOCONFIG_URL = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\AutoConfigURL";

	protected static final String REG_KEY_KEEP_ALIVE_TIMEOUT = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\KeepAliveTimeout";
	
	protected static final String REG_KEY_MAX_CONNECTIONS_PER_1_0_SVR = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\MaxConnectionsPer1_0Server";

	protected static final String REG_KEY_MAX_CONNECTIONS_PER_1_1_SVR = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\MaxConnectionsPerServer";

	protected static final String REG_KEY_PROXY_ENABLE = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\ProxyEnable";

	protected static final String REG_KEY_PROXY_OVERRIDE = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\ProxyOverride";

	protected static final String REG_KEY_PROXY_SERVER = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\ProxyServer";

	protected static final String REG_KEY_AUTOPROXY_RESULT_CACHE = "\\Software\\Policies\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\EnableAutoproxyResultCache";

	protected static final String REG_KEY_MIME_EXCLUSION_LIST_FOR_CACHE = "\\Software\\Policies\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\MimeExclusionListForCache";

	protected static final String REG_KEY_WARN_ON_FORM_SUBMIT = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\3\\1601";

	protected static Class popupMgrType;

	private static ArrayList<RegKeyBackup> keys = null;

	private File customProxyPACDir;

	private String[] cmdarray;

	private String commandPath;

	private Process process;

	protected boolean customPACappropriate = true;

	public InternetExplorerCustomProxyLauncher(
			SeleniumConfiguration seleniumConfiguration) {
		this(seleniumConfiguration, findBrowserLaunchLocation());
	}

	public InternetExplorerCustomProxyLauncher(
			SeleniumConfiguration seleniumConfiguration,
			String browserLaunchLocation) {
		super(seleniumConfiguration);
		init();
		commandPath = browserLaunchLocation;
	}

	protected void init() {
		if (!isStaticInitDone()) {
			initStatic();
		}
	}

	protected boolean isStaticInitDone() {
		return keys != null;
	}

	protected void initStatic() {
		keys = new ArrayList<RegKeyBackup>();
		handleEvilPopupMgrBackup();
		addRegistryKeyToBackupList(REG_KEY_BASE + REG_KEY_POPUP_MGR,
				REG_KEY_BACKUP_POPUP_MGR, popupMgrType);
		addRegistryKeyToBackupList(REG_KEY_BASE + REG_KEY_AUTOCONFIG_URL,
				REG_KEY_BACKUP_AUTOCONFIG_URL, String.class);
		addRegistryKeyToBackupList(REG_KEY_BASE
				+ REG_KEY_AUTOPROXY_RESULT_CACHE,
				REG_KEY_BACKUP_AUTOPROXY_RESULT_CACHE, boolean.class);
		addRegistryKeyToBackupList(REG_KEY_BASE
				+ REG_KEY_MIME_EXCLUSION_LIST_FOR_CACHE,
				REG_KEY_BACKUP_MIME_EXCLUSION_LIST_FOR_CACHE, String.class);

		// Only needed for proxy injection mode, but always adding to the list to guarantee they get
		// restored correctly
		addRegistryKeyToBackupList(REG_KEY_BASE + REG_KEY_PROXY_ENABLE,
				REG_KEY_BACKUP_PROXY_ENABLE, boolean.class);
		addRegistryKeyToBackupList(REG_KEY_BASE + REG_KEY_PROXY_OVERRIDE,
				REG_KEY_BACKUP_PROXY_OVERRIDE, boolean.class);
		addRegistryKeyToBackupList(REG_KEY_BASE + REG_KEY_PROXY_SERVER,
				REG_KEY_BACKUP_PROXY_SERVER, String.class);
		addRegistryKeyToBackupList(REG_KEY_BASE
				+ REG_KEY_MAX_CONNECTIONS_PER_1_0_SVR,
				REG_KEY_BACKUP_MAX_CONNECTIONS_PER_1_0_SVR, int.class);
		addRegistryKeyToBackupList(REG_KEY_BASE
				+ REG_KEY_MAX_CONNECTIONS_PER_1_1_SVR,
				REG_KEY_BACKUP_MAX_CONNECTIONS_PER_1_1_SVR, int.class);
	}

	// IE7 changed the type of the popup mgr key to DWORD (int/boolean) from String (which could be
	// "yes" or "no")
	protected void handleEvilPopupMgrBackup() {
		// this will return String (REG_SZ), int (REG_DWORD), or null if the key is missing
		popupMgrType = WindowsUtils.discoverRegistryKeyType(REG_KEY_BASE
				+ REG_KEY_POPUP_MGR);
		Class backupPopupMgrType = WindowsUtils
				.discoverRegistryKeyType(REG_KEY_BACKUP_POPUP_MGR);
		if (popupMgrType == null) { // if official PopupMgr key is missing
			if (backupPopupMgrType == null) {
				// we don't know which type it should be; let's take a guess
				// IE6 can deal with a DWORD 0
				popupMgrType = boolean.class;
				return;
			}
			// non-null backup type is our best guess
			popupMgrType = backupPopupMgrType;
			return;
		}
		if (popupMgrType.equals(backupPopupMgrType))
			return;

		// if we're here, we know the current type of pop-up manager,
		// and the backup has a different (wrong) type
		if (backupPopupMgrType != null) {
			WindowsUtils.deleteRegistryValue(REG_KEY_BACKUP_POPUP_MGR);
		}
		if (!backupIsReady()) {
			return;
		}

		// assume they wanted it off
		turnOffPopupBlocking(REG_KEY_BACKUP_POPUP_MGR);
	}

	protected void turnOffPopupBlocking(String key) {
		if (WindowsUtils.doesRegistryValueExist(key)) {
			WindowsUtils.deleteRegistryValue(key);
		}
		if (popupMgrType.equals(String.class)) {
			WindowsUtils.writeStringRegistryValue(key, "no");
		} else {
			WindowsUtils.writeBooleanRegistryValue(key, false);
		}
	}

	protected void addRegistryKeyToBackupList(String regKey,
			String backupRegKey, Class clazz) {
		keys.add(new RegKeyBackup(regKey, backupRegKey, clazz));
	}

	public static void setBaseRegKey(String base) {
		REG_KEY_BASE = base;
	}

	protected static String findBrowserLaunchLocation() {
		String defaultPath = System.getProperty("internetExplorerDefaultPath");
		if (defaultPath == null) {
			defaultPath = WindowsUtils.getProgramFilesPath()
					+ "\\Internet Explorer\\iexplore.exe";
		}
		File defaultLocation = new File(defaultPath);
		if (defaultLocation.exists()) {
			return defaultLocation.getAbsolutePath();
		}
		File iexploreEXE = AsyncExecute.whichExec("iexplore.exe");
		if (iexploreEXE != null)
			return iexploreEXE.getAbsolutePath();
		throw new RuntimeException(
				"Internet Explorer couldn't be found in the path!\n"
						+ "Please add the directory containing iexplore.exe to your PATH environment\n"
						+ "variable, or explicitly specify a path to IE like this:\n"
						+ "*iexplore c:\\blah\\iexplore.exe");
	}

	public void launch(String url) {
		try {
			if (WindowsUtils.thisIsWindows()) {
				backupRegistrySettings();
				changeRegistrySettings();
				cmdarray = new String[] { commandPath, "-new", url };
			} else {
				cmdarray = new String[] { commandPath, url };
			}
			logger.info("Launching Internet Explorer...");
			AsyncExecute exe = new AsyncExecute();
			exe.setCommandline(cmdarray);
			process = exe.asyncSpawn();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void changeRegistrySettings() throws IOException {
		if (!customPACappropriate) {
			if (WindowsUtils.doesRegistryValueExist(REG_KEY_BASE
					+ REG_KEY_AUTOCONFIG_URL)) {
				WindowsUtils.deleteRegistryValue(REG_KEY_BASE
						+ REG_KEY_AUTOCONFIG_URL);
			}
		} else {
			customProxyPACDir = LauncherUtils
					.createCustomProfileDir(getSession());
			if (customProxyPACDir.exists()) {
				LauncherUtils.recursivelyDeleteDir(customProxyPACDir);
			}
			customProxyPACDir.mkdir();

			boolean isAlwaysProxy = false;
			
			SeleniumConfiguration configuration = getSeleniumConfiguration();
			
			if (configuration.isOptionValueSpecified(SeleniumConfigurationOption.ALWAYS_PROXY)) {
				isAlwaysProxy = configuration.isAlwaysProxy();
			}
			
			int port = 8180;
			
			if (configuration.isOptionValueSpecified(SeleniumConfigurationOption.PORT)) {
				port = configuration.getPort();
			}
			else {
				logger.warn("port is not set.  Launching Internet Explorer with default proxy port 8180.");
			} 
			
			File proxyPAC = LauncherUtils.makeProxyPAC(customProxyPACDir, port, isAlwaysProxy);

			logger.info("Modifying registry settings...");

			String newURL = "file://"
					+ proxyPAC.getAbsolutePath().replace('\\', '/');
			WindowsUtils.writeStringRegistryValue(REG_KEY_BASE
					+ REG_KEY_AUTOCONFIG_URL, newURL);
		}

		// Disabling automatic proxy caching
		// http://support.microsoft.com/?kbid=271361
		// Otherwise, *all* requests will go through our proxy, rather than just */selenium-server/*
		// requests
		WindowsUtils.writeBooleanRegistryValue(REG_KEY_BASE
				+ REG_KEY_AUTOPROXY_RESULT_CACHE, false);

		// Disable caching of html
		WindowsUtils
				.writeStringRegistryValue(REG_KEY_BASE
						+ REG_KEY_MIME_EXCLUSION_LIST_FOR_CACHE,
						"multipart/mixed multipart/x-mixed-replace multipart/x-byteranges text/html");

		// Disable pop-up blocking
		turnOffPopupBlocking(REG_KEY_BASE + REG_KEY_POPUP_MGR);

		WindowsUtils.writeIntRegistryValue(REG_KEY_BASE
				+ REG_KEY_WARN_ON_FORM_SUBMIT, 0);
		
		// Change default keep alive timeout value
		WindowsUtils.writeStringRegistryValue(REG_KEY_BASE + REG_KEY_KEEP_ALIVE_TIMEOUT, "120000");

		if (WindowsUtils.doesRegistryValueExist(REG_KEY_BASE
				+ REG_KEY_PROXY_OVERRIDE)) {
			WindowsUtils.deleteRegistryValue(REG_KEY_BASE
					+ REG_KEY_PROXY_OVERRIDE);
		}

		// TODO Do we want to make these preferences configurable somehow?
		// TODO Disable security warnings
		// TODO Disable "do you want to remember this password?"
	}

	public void backupRegistrySettings() {
		// Don't clobber our old backup if we
		// never got the chance to restore for some reason
		if (backupIsReady())
			return;
		logger.info("Backing up registry settings...");
		for (RegKeyBackup key : keys) {
			key.backup();
		}
		backupReady(true);
	}

	public void restoreRegistrySettings() {
		// Backup really should be ready, but if not, skip it
		try {
			if (!backupIsReady()) {
				return;
			}
		}
		catch (WindowsRegistryException ex) {
			logger.error("Problem when checking if selenium backup key was ready.", ex);
		}
		logger.info("Restoring registry settings (won't affect running browsers)...");
		for (RegKeyBackup key : keys) {
			try {
				key.restore();
			}
			catch (WindowsRegistryException ex) {
				logger.error("Problem restoring key " + key, ex);
			}
		}
		backupReady(false);
	}

	private boolean backupIsReady() {
		if (!WindowsUtils.doesRegistryValueExist(REG_KEY_BACKUP_READY))
			return false;
		return WindowsUtils.readBooleanRegistryValue(REG_KEY_BACKUP_READY);
	}

	private void backupReady(boolean backupReady) {
		WindowsUtils.writeBooleanRegistryValue(REG_KEY_BACKUP_READY,
				backupReady);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean close() {
		Exception taskKillException = null;
		if (WindowsUtils.thisIsWindows()) {
			restoreRegistrySettings();
		}
		if (process == null)
			return false;
		if (false) {
			try {
				// try to kill with windows taskkill
				WindowsUtils.kill(cmdarray);
			} catch (Exception e) {
				taskKillException = e;
			}
		}
		int exitStatus = AsyncExecute.killProcess(process);
		if (customPACappropriate) {
			try {
				LauncherUtils.recursivelyDeleteDir(customProxyPACDir);
			} catch (RuntimeException e) {
				if (taskKillException != null) {
					e.printStackTrace();
					System.err.print("Perhaps caused by: ");
					taskKillException.printStackTrace();
					throw new RuntimeException(
							"Couldn't delete custom IE "
									+ "proxy directory, presumably because task kill failed; "
									+ "see stderr!", e);
				}
				throw e;
			}
		}
		
		boolean closedSuccessfully = (exitStatus == 0) || (exitStatus == 1);
		
		return closedSuccessfully;
	}

	public Process getProcess() {
		return process;
	}

	/**
	 * A data wrapper class to manage backup/restore of registry keys
	 */
	private static class RegKeyBackup {
		private String keyOriginal;

		private String keyBackup;

		private Class type;

		public RegKeyBackup(String keyOriginal, String keyBackup, Class type) {
			this.keyOriginal = keyOriginal;
			this.keyBackup = keyBackup;
			this.type = type;
		}

		private boolean backupExists() {
			return WindowsUtils.doesRegistryValueExist(keyBackup);
		}

		private boolean originalExists() {
			return WindowsUtils.doesRegistryValueExist(keyOriginal);
		}

		private void backup() {
			if (originalExists()) {
				copy(keyOriginal, keyBackup);
			} else {
				clear(keyBackup);
			}
		}

		private void restore() {
			if (backupExists()) {
				copy(keyBackup, keyOriginal);
			} else {
				clear(keyOriginal);
			}
		}

		private void clear(String key) {
			if (WindowsUtils.doesRegistryValueExist(key)) {
				WindowsUtils.deleteRegistryValue(key);
			}
		}

		private void copy(String source, String dest) {
			if (type.equals(String.class)) {
				copyString(source, dest);
				return;
			} else if (type.equals(boolean.class)) {
				copyBoolean(source, dest);
				return;
			} else if (type.equals(int.class)) {
				copyInt(source, dest);
				return;
			}
			throw new RuntimeException("Bad type: " + type.getName());
		}

		private void copyString(String source, String dest) {
			String data = WindowsUtils.readStringRegistryValue(source);
			WindowsUtils.writeStringRegistryValue(dest, data);
		}

		private void copyBoolean(String source, String dest) {
			boolean data = WindowsUtils.readBooleanRegistryValue(source);
			WindowsUtils.writeBooleanRegistryValue(dest, data);
		}

		private void copyInt(String source, String dest) {
			int data = WindowsUtils.readIntRegistryValue(source);
			WindowsUtils.writeIntRegistryValue(dest, data);
		}
	}
}