/*
 * Copyright 2004 ThoughtWorks, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.thoughtworks.selenium.launchers;

import com.thoughtworks.selenium.BrowserLauncher;

/**
 * Runs the browser launcher specified in the <code>selenium-browser-path</code> Java System property,
 * or runs the SystemDefaultBrowserLauncher if no property was specified
 * 
 * @see System#getProperties()
 * @author Paul Hammant
 * @version $Revision$
 */
public class SystemPropertiesBrowserLauncher implements BrowserLauncher {

    private BrowserLauncher delegate;
    private static final String browserPath = System.getProperty("selenium-browser-path");

    public SystemPropertiesBrowserLauncher() {
        if (browserPath != null && !browserPath.equals("")) {
            delegate = new SpecifiedPathBrowserLauncher(browserPath);
        } else {
            delegate = new SystemDefaultBrowserLauncher();
        }
    }

    public void launch(String url) {
        delegate.launch(url);
    }

    public void close() {
        delegate.close();
    }
}
