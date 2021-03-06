package org.openqa.selenium.iphone;

import junit.framework.TestCase;

public class CreateIPhoneDriverTest extends TestCase {
  public void testCreateDriver() throws Exception {
    new IPhoneDriver();
  }

  public void testDeleteSession() throws Exception {
    IPhoneDriver driver = new IPhoneDriver();
    driver.close();
  }
  
  public void testCreateDriverWithTrailingSlash() throws Exception {
    new IPhoneDriver("http://localhost:16000/hub/");
  }
}
