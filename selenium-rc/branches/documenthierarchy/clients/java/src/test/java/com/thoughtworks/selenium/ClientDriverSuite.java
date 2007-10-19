/*
 * Created on Feb 25, 2006
 *
 */
package com.thoughtworks.selenium;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.configuration.SeleniumConfiguration;

import com.thoughtworks.selenium.corebased.*;

/**
 * The wrapper test suite for these tests, which spawns an in-process Selenium
 * Server for simple integration testing.
 * 
 * <p>
 * Normally, users should start the Selenium Server out-of-process, and just
 * leave it up and running, available for the tests to use. But, if you like,
 * you can do what we do here and start a Selenium Server before launching the
 * tests.
 * </p>
 * 
 * <p>
 * Note that we don't recommend starting and stopping the entire server during
 * each test's setUp and tearDown for these Integration tests; it shouldn't be
 * necessary, and doing so may conceal bugs in the server.
 * </p>
 * 
 * 
 * @author Dan Fabulich
 * 
 */
public class ClientDriverSuite extends TestCase {

//	protected static List<Class<? extends SeleniumClientTestBase>> seleniumClientTestList = new ArrayList<Class<? extends SeleniumClientTestBase>>();
//	protected static List<Browser> testableBrowserList = new ArrayList<Browser>();
	
//	static {		
//		seleniumClientTestList.add(GoogleTest.class);
//	}
//	
//	static {
//		testableBrowserList.add(BrowserType.Browser.FIREFOX);
//		testableBrowserList.add(BrowserType.Browser.IEXPLORE);
//	}
	
    /**
     * Construct a test suite containing the other integration tests, wrapping
     * them up in a TestSetup object that will launch the Selenium Server
     * in-proc.
     * 
     * @return a test suite containing tests to run
     */
    public static Test suite() {
        boolean isProxyInjectionMode = System.getProperty("selenium.proxyInjectionMode") != null
        && System.getProperty("selenium.proxyInjectionMode").equals("true");

        String forcedBrowserMode = System.getProperty("selenium.forcedBrowserMode");

        return generateSuite(isProxyInjectionMode, forcedBrowserMode);
    }

    public static TestSuite generateSuite(boolean isProxyInjectionMode, String forcedBrowserMode) {
        try {
            // TODO This class extends TestCase to workaround MSUREFIRE-113
            // http://jira.codehaus.org/browse/MSUREFIRE-113
            // Once that bug is fixed, this class should be a TestSuite, not a
            // TestCase
            TestSuite supersuite = new TestSuite(ClientDriverSuite.class
                    .getName());
            TestSuite suite = new TestSuite(ClientDriverSuite.class.getName());
            
            
            if (isProxyInjectionMode) {
            	 suite.addTestSuite(TestModalDialog.class);            	 
            }

//          suite.addTestSuite(RealDealIntegrationTest.class);


            // Working tests
         
            suite.addTestSuite(TestFramesNested.class);
            suite.addTestSuite(TestFramesSpecialTargets.class);
            suite.addTestSuite(TestFramesClickJavascriptHref.class);
            suite.addTestSuite(TestFramesClick.class);            
            suite.addTestSuite(TestFramesOpen.class);   
            suite.addTestSuite(WindowNamesTest.class);
            suite.addTestSuite(ApacheMyFacesSuggestTest.class);
            suite.addTestSuite(TestJavascriptParameters.class);
            suite.addTestSuite(TestErrorChecking.class);
            suite.addTestSuite(GoogleTestSearch.class);
            suite.addTestSuite(GoogleTest.class);
            suite.addTestSuite(TestCheckUncheck.class);
            suite.addTestSuite(TestXPathLocators.class);
            suite.addTestSuite(TestClickJavascriptHref.class);
            suite.addTestSuite(TestCommandError.class);
            suite.addTestSuite(TestComments.class);
            suite.addTestSuite(TestFailingAssert.class);
            suite.addTestSuite(TestFailingVerifications.class);
            suite.addTestSuite(TestFocusOnBlur.class);
            suite.addTestSuite(TestGoBack.class);            
            suite.addTestSuite(TestImplicitLocators.class);
            suite.addTestSuite(TestLocators.class);
            suite.addTestSuite(TestOpen.class);
            suite.addTestSuite(TestPatternMatching.class);
            suite.addTestSuite(TestStore.class);
            suite.addTestSuite(TestSubmit.class);
            suite.addTestSuite(TestType.class);
            suite.addTestSuite(TestVerifications.class);
            suite.addTestSuite(TestSelect.class);
            suite.addTestSuite(TestEditable.class);
            suite.addTestSuite(TestRefresh.class);
            suite.addTestSuite(TestVisibility.class);
            suite.addTestSuite(TestMultiSelect.class);
            suite.addTestSuite(TestWaitFor.class);
            suite.addTestSuite(TestWaitForNot.class);
            suite.addTestSuite(TestClick.class);
            suite.addTestSuite(TestSelectWindow.class); 
            suite.addTestSuite(TestWaitInPopupWindow.class);
            suite.addTestSuite(TestPrompt.class);
            suite.addTestSuite(TestConfirmations.class);
            suite.addTestSuite(TestAlerts.class);
            suite.addTestSuite(TestPause.class);
            suite.addTestSuite(TestWait.class);
            
            // new
            suite.addTestSuite(TestEvilClosingWindow.class);
            suite.addTestSuite(TestSelectWindowTitle.class);
            suite.addTestSuite(TestSelectMultiLevelFrame.class);
            suite.addTestSuite(TestOpenInTargetFrame.class);
            // not working

//			  // only works in IE right now
//			  suite.addTest(I18nTest.suite());
            
            // Test all browsers
//            for (Browser browser : testableBrowserList) {
//            	System.out.println("Running all tests in suite for browser " + browser);
//            	
//            	for (Class<? extends SeleniumClientTestBase> testClass : seleniumClientTestList) {
////            		suite.addTestSuite(testClass);
//            		try {
//						Constructor<? extends SeleniumClientTestBase> constructor = testClass.getConstructor(BrowserType.class);
//						SeleniumClientTestBase testBase = constructor.newInstance(new Object[] { BrowserType.getBrowserType(browser.toString()) });
//						
//						//testBase.setName(testBase.getClass().getName());
//						suite.addTest(testBase);
//					} catch (NoSuchMethodException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (InstantiationException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IllegalAccessException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (InvocationTargetException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//            	}
//            }
            
            ClientDriverTestSetup setup = new ClientDriverTestSetup(suite);
            supersuite.addTest(setup);
            return supersuite;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * A TestSetup decorator that runs a super setUp and tearDown at the
     * beginning and end of the entire run: in this case, we use it to startup
     * and shutdown the in-process Selenium Server.
     * 
     * 
     * @author danielf
     * 
     */
    static class ClientDriverTestSetup extends TestSetup {
        SeleniumServer server;

        public ClientDriverTestSetup(Test test) {
            super(test);
        }

        public void setUp() throws Exception {
            try {
            	//System.setProperty("selenium.debugMode", "true");
            	
                SeleniumServer.startServer(new String[0]);
                SeleniumServer seleniumServer = SeleniumServer.getInstance();
                SeleniumConfiguration configuration = seleniumServer.getSeleniumConfiguration();
                
                System.out.println("Starting the Selenium Server listening on port " + configuration.getPort()
                        + " as part of global setup...");

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        public void tearDown() throws Exception {
            try {
            	System.clearProperty("selenium.debugMode");
                SeleniumServer.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

    }
}