package org.openqa.selenium;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.openqa.selenium.server.ClasspathResourceLocatorTest;
import org.openqa.selenium.server.FsResourceLocatorTest;
import org.openqa.selenium.server.StaticContentHandlerTest;
import org.openqa.selenium.server.browserlaunchers.WindowsUtilsTest;
import org.openqa.selenium.server.browserlaunchers.FirefoxChromeLauncherTest;

public class UnitTestSuite extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(UnitTestSuite.class.getName());
        suite.addTestSuite(QueueTest.class);
        suite.addTestSuite(ClasspathResourceLocatorTest.class);
        suite.addTestSuite(FsResourceLocatorTest.class);
        suite.addTestSuite(StaticContentHandlerTest.class);
        suite.addTestSuite(WindowsUtilsTest.class);
        suite.addTestSuite(FirefoxChromeLauncherTest.class);
        suite.addTestSuite(HTMLSuiteResultTest.class);
        return suite;
    }
}
