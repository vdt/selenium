package org.openqa.selenium.server.browser.launchers;

import org.openqa.selenium.server.client.Session;
import org.openqa.selenium.server.configuration.SeleniumConfiguration;
import org.openqa.selenium.server.configuration.SeleniumConfigurationOption;

/**
 * Runs the specified command path to start the browser, and kills the process to quit.
 * 
 * @author Paul Hammant
 * @author Matthew Purland
 */
public abstract class AbstractBrowserLauncher implements BrowserLauncher {

	private Session session;

	private SeleniumConfiguration seleniumConfiguration;

	public AbstractBrowserLauncher(SeleniumConfiguration seleniumConfiguration) {
		this.seleniumConfiguration = seleniumConfiguration;
	}
	
	/**
	 * Initialize the browser launcher with the given session.
	 * 
	 * @param session The session
	 */
	public void init(Session session) {
		this.session = session;
	}
	
	/**
	 * Get the session for the browser launcher.
	 * 
	 * @return Get the session associated with the browser launcher.
	 */
	public Session getSession() {
		return session;
	}
	
	/**
	 * Get the selenium configuration.
	 * 
	 * @return Get the selenium configuration.
	 */
	public SeleniumConfiguration getSeleniumConfiguration() {
		return seleniumConfiguration;
	}

	protected abstract void launch(String url);

	public void launchHTMLSuite(String suiteUrl, String browserURL,
			boolean multiWindow) {
		launch(LauncherUtils.getDefaultHTMLSuiteUrl(browserURL, suiteUrl,
				multiWindow, getSeleniumConfiguration().getPort(), (String) SeleniumConfigurationOption.LOG_LEVEL.getDefaultValue()));
	}

	public void launchRemoteSession(String browserURL, boolean multiWindow, boolean debugMode) {
		launch(LauncherUtils.getDefaultRemoteSessionUrl(browserURL, session,
				multiWindow, debugMode, getSeleniumConfiguration().isProxyInjectionMode(), getSeleniumConfiguration().getPort(), getSeleniumConfiguration().getHostname()));
	}



}
