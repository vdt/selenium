package com.thoughtworks.selenium.launchers;

import java.io.IOException;

import com.thoughtworks.selenium.BrowserLauncher;
import com.thoughtworks.selenium.SeleniumException;

/**
 * An abstract class to launch the specified command path
 * @author Paul Hammant
 * @version $Revision: 1.8 $
 */
public abstract class RuntimeExecutingBrowserLauncher implements BrowserLauncher {

    protected Process process;
    private String commandPath;

    /** Specifies a command path to run */
    protected RuntimeExecutingBrowserLauncher(String commandPath) {
        this.commandPath = commandPath;
    }

    public void launch(String url) {
        exec(commandPath + " " + url);
    }

    protected void exec(String command) {

        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new SeleniumException("IO Exception:" + e.getMessage());
        }
    }
}
