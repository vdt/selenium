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
package org.openqa.selenium.server.browser.launchers;

import org.apache.commons.logging.Log;
import org.mortbay.log.LogFactory;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.client.Session;
import org.openqa.selenium.server.configuration.SeleniumConfiguration;

import java.io.File;
import java.io.IOException;

public class InternetExplorerCustomProxyLauncher extends AbstractBrowserLauncher {

    static Log log = LogFactory.getLog(InternetExplorerCustomProxyLauncher.class);

    private File customProxyPACDir;
    private String[] cmdarray;
    private String commandPath;
    private Process process;
    protected boolean customPACappropriate = true;
    protected WindowsProxyManager wpm;

    private static boolean alwaysChangeMaxConnections = false;
    protected boolean changeMaxConnections = alwaysChangeMaxConnections;

    public InternetExplorerCustomProxyLauncher(SeleniumConfiguration seleniumConfiguration) {
        this(seleniumConfiguration, findBrowserLaunchLocation());
    }

    public InternetExplorerCustomProxyLauncher(SeleniumConfiguration seleniumConfiguration, String browserLaunchLocation) {
        super(seleniumConfiguration);
        commandPath = browserLaunchLocation;
    }
    
    @Override
    public void init(Session session) {
    	super.init(session);
    	
        wpm = new WindowsProxyManager(true, getSession(), getSeleniumConfiguration().getPort());
    }

    protected void changeRegistrySettings() throws IOException {
        wpm.changeRegistrySettings(getSeleniumConfiguration());
    }
    
    protected static String findBrowserLaunchLocation() {
        String defaultPath = System.getProperty("internetExplorerDefaultPath");
        if (defaultPath == null) {
            defaultPath = WindowsUtils.getProgramFilesPath() + "\\Internet Explorer\\iexplore.exe";
        }
        File defaultLocation = new File(defaultPath);
        if (defaultLocation.exists()) {
            return defaultLocation.getAbsolutePath();
        }
        File iexploreEXE = AsyncExecute.whichExec("iexplore.exe");
        if (iexploreEXE != null) return iexploreEXE.getAbsolutePath();
        throw new RuntimeException("Internet Explorer couldn't be found in the path!\n" +
                "Please add the directory containing iexplore.exe to your PATH environment\n" +
                "variable, or explicitly specify a path to IE like this:\n" +
                "*iexplore c:\\blah\\iexplore.exe");
    }

    @Override
    public void launch(String url) {
        try {
        	log.info("launch(" + url + ")...");
        	log.info("Windows? " + (WindowsUtils.thisIsWindows() ? "Yes" : "No") );
        	log.info("OS Name: '" + System.getProperty("os.name") + "'" );
        	log.info("Windows 2000?: " + (System.getProperty("os.name").equals("Windows 2000") ? "Yes" : "No"));
        	boolean useKillableProcess = WindowsUtils.thisIsWindows() &&
        	System.getProperty("os.name").equals("Windows 2000") == false &&
        	getSeleniumConfiguration().isKillableProcessEnabled() == true;
        	log.info("Using killable process?: "+ (useKillableProcess ? "Yes" : "No") );
        	
        	// Backup/set registry settings if windows
        	if (WindowsUtils.thisIsWindows()) {
        		wpm.backupRegistrySettings();
                changeRegistrySettings();
        	}
        	
        	// Use killable process if windows
            if (useKillableProcess) {
                customProxyPACDir = wpm.getCustomProxyPACDir();
                File killableProcessWrapper = new File(customProxyPACDir, "killableprocess.exe");
                ResourceExtractor.extractResourcePath(InternetExplorerCustomProxyLauncher.class, "/killableprocess/killableprocess.exe", killableProcessWrapper);
                cmdarray = new String[]{killableProcessWrapper.getAbsolutePath(), commandPath, "-new", url};
            } else {
                // DGF IEs4Linux, perhaps?  It could happen!
                cmdarray = new String[]{commandPath, url};
            }
            log.info("Launching Internet Explorer...");
            AsyncExecute exe = new AsyncExecute();
            exe.setCommandline(cmdarray);
            process = exe.asyncSpawn();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean close() {
        Exception taskKillException = null;
        if (WindowsUtils.thisIsWindows()) {
            wpm.restoreRegistrySettings(getSeleniumConfiguration());
        }
        if (process == null) return true;
        if (false) {
            try {
                // try to kill with windows taskkill
                WindowsUtils.kill(cmdarray);
            } catch (Exception e) {
                taskKillException = e;
            }
        }
        try { // DGF killableprocess.exe should commit suicide if we send it a newline
            process.getOutputStream().write('\n');
            process.getOutputStream().flush();
            Thread.sleep(200);
        } catch (Exception ignored) {}
        AsyncExecute.killProcess(process);
        if (customPACappropriate) {
            try {
                LauncherUtils.recursivelyDeleteDir(customProxyPACDir);
            } catch (RuntimeException e) {
                if (taskKillException != null) {
                    log.error("Couldn't delete custom IE proxy directory", e);
                    log.error("Perhaps IE proxy delete error was caused by this exception", taskKillException);
                    throw new RuntimeException("Couldn't delete custom IE " +
                            "proxy directory, presumably because task kill failed; " +
                            "see error log!", e);
                }
                throw e;
            }
        }
        
        return true;
    }

    public Process getProcess() {
        return process;
    }

//    public static void main(String[] args) {
//        InternetExplorerCustomProxyLauncher l = new InternetExplorerCustomProxyLauncher(SeleniumServer.DEFAULT_PORT, "CUSTIE");
//        l.launch("http://www.google.com/");
//        int seconds = 5;
//        System.out.println("Killing browser in " + Integer.toString(seconds) + " seconds");
//        AsyncExecute.sleepTight(seconds * 1000);
//        l.close();
//        System.out.println("He's dead now, right?");
//    }


    public static void setChangeMaxConnections(boolean changeMaxConnections) {
        InternetExplorerCustomProxyLauncher.alwaysChangeMaxConnections = changeMaxConnections;
    }
}