package com.thoughtworks.selenium.embedded.jetty;

import com.thoughtworks.selenium.BrowserLauncher;
import com.thoughtworks.selenium.launchers.MacSeaMonkeyBrowserLauncher;

/**
 * @author Paul Hammant
 * @version $Revision: 1.8 $
 */
public class MacSeaMonkeyIntegrationTest extends RealDealIntegrationTest {
    protected BrowserLauncher getBrowserLauncher() {
        return new MacSeaMonkeyBrowserLauncher();
    }
}
