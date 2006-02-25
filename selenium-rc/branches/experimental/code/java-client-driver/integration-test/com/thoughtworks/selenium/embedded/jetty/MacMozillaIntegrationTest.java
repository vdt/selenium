package com.thoughtworks.selenium.embedded.jetty;

import com.thoughtworks.selenium.BrowserLauncher;
import com.thoughtworks.selenium.launchers.MacMozillaBrowserLauncher;

/**
 * @author Paul Hammant
 * @version $Revision: 1.8 $
 */
public class MacMozillaIntegrationTest extends RealDealIntegrationTest {
    protected BrowserLauncher getBrowserLauncher() {
        return new MacMozillaBrowserLauncher();
    }
}
