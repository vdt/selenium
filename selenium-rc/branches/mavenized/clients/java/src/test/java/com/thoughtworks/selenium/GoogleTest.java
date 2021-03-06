package com.thoughtworks.selenium;

import junit.framework.*;

import org.openqa.selenium.server.*;

public class GoogleTest extends TestCase
{
   private Selenium selenium;

   public void setUp() throws Exception {
        String url = "http://www.google.com";
       selenium = new DefaultSelenium("localhost", SeleniumServer.DEFAULT_PORT, "*firefox", url);
       selenium.start();
    }
   
   protected void tearDown() throws Exception {
       selenium.stop();
   }
   
   public void testGoogleTestSearch() throws Throwable {
		selenium.open("http://www.google.com/webhp");
        String[] windowNames = selenium.getAllWindowNames();
        for (String windowName : windowNames) {
            System.out.println("Window Name: " + windowName);
        }
        selenium.selectWindow(null);
        String[] windowIds = selenium.getAllWindowIds();
        for (String windowId : windowIds) {
            System.out.println("Window Id: " + windowId);
        }
        String[] windowTitles = selenium.getAllWindowTitles();
        for (String windowTitle : windowTitles) {
            System.out.println("Window Title: " + windowTitle);
        }
        //selenium.setSpeed("500");
        selenium.windowFocus("");
        selenium.windowMaximize("title=Google");
        selenium.windowMaximize("title=exact:Google");
        selenium.windowMaximize("title=regexp:Google");
        selenium.windowMaximize("title=glob:Google");
//        selenium.setSpeed("0");
        selenium.windowMaximize("regexp:");
        selenium.windowMaximize("regexp:.*");
        assertEquals("Google", selenium.getTitle());
		selenium.type("q", "Selenium OpenQA");
		assertEquals("Selenium OpenQA", selenium.getValue("q"));
        String s = selenium.getLogMessages();
        System.out.println("The log messages are the following:\n" + s);
		selenium.click("btnG");
		selenium.waitForPageToLoad("5000");
		
		assertEquals("Selenium OpenQA - Google Search", selenium.getTitle());
	}
	
}
