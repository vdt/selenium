/*
 * Copyright 2006 ThoughtWorks, Inc.
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
package org.openqa.selenium.server;

import java.io.File;

import junit.framework.TestCase;

import org.openqa.selenium.server.browser.launchers.html.HTMLLauncher;
import org.openqa.selenium.server.browser.launchers.html.HTMLResultsListener;
import org.openqa.selenium.server.browser.launchers.html.HTMLTestResults;
import org.openqa.selenium.server.configuration.DefaultSeleniumConfiguration;
import org.openqa.selenium.server.configuration.SeleniumConfiguration;

public abstract class HTMLRunnerTestBase extends TestCase implements HTMLResultsListener {

    SeleniumServer server;
    HTMLLauncher launcher;
    HTMLTestResults results = null;
    protected String suiteName = "TestSuite.html";
    protected String browserURL; 
    File output;
    
    public HTMLRunnerTestBase() {
        super();
    }
    
    public HTMLRunnerTestBase(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        output = new File(getName() + "-results.html");
        System.out.println("Will print results to " + output.getAbsolutePath());
//        HttpRequest.__maxFormContentSize = 400000;
        
    }
    
    protected void runHTMLSuite(String browser, boolean slowResources) throws Exception {
    	SeleniumConfiguration configuration = new DefaultSeleniumConfiguration();
    	
    	if (slowResources) {
    		configuration.setSlowResourcesMode(slowResources);
    	}
    	
    	configuration.setMultiWindowMode(true);
    	
        server = new SeleniumServer();
        server.start(configuration);
        
        launcher = new HTMLLauncher(configuration);
        
        browserURL = "http://localhost:" + configuration.getPort();
        String testURL = browserURL + "/selenium-server/tests/" + suiteName;
        int timeout = 60 * 15; // fifteen minutes
//        String result = launcher.runHTMLSuite(browser, browserURL, testURL, output, timeout);
//        assertTrue("Results file doesn't exist: " + output.getAbsolutePath(), output.exists());
//        assertEquals("Tests didn't pass, check HTML output for details: " + output.getAbsolutePath(), "PASSED", result);
    }
    
    
    public void tearDown() throws Exception {
        if (server != null) {
        	server.stop();
        }
    }

    public void processResults(HTMLTestResults results) {
        this.results = results;
    }
}
