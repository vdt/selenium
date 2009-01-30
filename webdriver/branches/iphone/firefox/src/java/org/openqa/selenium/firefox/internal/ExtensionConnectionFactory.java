/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.firefox.internal;

import org.openqa.selenium.firefox.ExtensionConnection;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;

public class ExtensionConnectionFactory {
    public static ExtensionConnection connectTo(FirefoxBinary binary, FirefoxProfile profile, String host) {
        boolean isDev = Boolean.getBoolean("webdriver.firefox.useExisting");
        if (isDev) {
            try {
                return new RunningInstanceConnection(host, profile.getPort());
            } catch (IOException e) {
                // Fine. No running instance
            }
        }

        try {
          return new NewProfileExtensionConnection(binary, profile, host);
        } catch (Exception e) {
          // Tell the world what went wrong
          e.printStackTrace();
        }

        return new DisconnectedExtension();
    }
}