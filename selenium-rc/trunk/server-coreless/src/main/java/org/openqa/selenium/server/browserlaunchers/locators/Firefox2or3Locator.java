package org.openqa.selenium.server.browserlaunchers.locators;

import org.apache.commons.logging.Log;
import org.mortbay.log.LogFactory;

/**
 * Discovers a valid Firefox installation on local system.
 */
public class Firefox2or3Locator {

    private static Log LOGGER = LogFactory.getLog(Firefox2or3Locator.class);

    public String findBrowserLaunchLocationOrFail() {
        LOGGER.debug("Dicovering Firefox 2...");
        final String firefox2Location = new Firefox2Locator().findBrowserLocation();
        if (null != firefox2Location) {
            return firefox2Location;
        }

        LOGGER.debug("Did not find Firefox 2, now dicovering Firefox 3...");
        final String firefox3Location = new Firefox3Locator().findBrowserLocation();
        if (null != firefox3Location) {
            return firefox3Location;
        }

        throw new RuntimeException(couldNotFindFirefoxMessage());
    }

    private String couldNotFindFirefoxMessage() {
        return new Firefox3Locator().couldNotFindAnyInstallationMessage();
    }

}