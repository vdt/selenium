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
package com.thoughtworks.selenium.runner;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.httpclient.HttpException;

import com.thoughtworks.selenium.launch.WindowsIEBrowserLauncher;

/**
 * @author Darren Cotterill
 * @author Ajit George
 */
public class SeleniumRunner {

    private final SeleniumRunnerConfig config;
    
    public SeleniumRunner(SeleniumRunnerConfig initalConfig) {
        config = initalConfig;
    }
    
    public static void main(String[] argv) throws Exception {
        
        Properties properties = System.getProperties();

        SeleniumRunnerConfig config = new SeleniumRunnerConfig();      
        config.setResultsURL(properties.getProperty("test.selenium.results"));
        config.setTestRunnerURL(properties.getProperty("test.selenium.runner"));
        config.setBrowserVisible(visible(properties.getProperty("test.selenium.browser.visible")));
        String logDirectoryPath = properties.getProperty("test.selenium.log.dir");
        config.setOutputFile(createLogFile(logDirectoryPath));
        config.setMaxPollAttempts(Integer.parseInt(properties.getProperty("test.selenium.maximum.poll.attempts")));
        config.setBrowserLauncher(new WindowsIEBrowserLauncher());

        new SeleniumRunner(config).run();
    }

    public void run() throws HttpException, IOException {
        
        String resultsURL = config.getResultsURL();
        boolean isBrowserVisible = config.isBrowserVisible();
        WindowsIEBrowserLauncher browserLauncher = config.getBrowserLauncher();
                                      
        new SeleniumInitializer(resultsURL).initialize();
        browserLauncher.launch(config.getTestRunnerURL(), isBrowserVisible);

        boolean succeeded = new SeleniumPoller(resultsURL, config
                .getMaxPollAttempts(), config.getOutputFile()).poll();
        if (!succeeded) {
            throw new RuntimeException("selenium tests failed");
        }
        if (succeeded || !isBrowserVisible) {
            browserLauncher.close();
        }
    }

    private static boolean visible(String visibleString) {
        boolean isPropertySet = visibleString != null;
        String normalizedVisibleString = isPropertySet ? visibleString.toLowerCase().trim() : "";
        return !"false".equals(normalizedVisibleString);
    }

    private static File createLogFile(String logDirectoryPath) {
        File logDirectory = new File(logDirectoryPath);
        if (!(logDirectory.exists() && logDirectory.isDirectory())) {
            throw new RuntimeException("cannot write to log directory '" + logDirectory + "'");
        }
        File outputFile = new File(logDirectoryPath, "selenium-results.html");
        return outputFile;
    }
}
