package com.thoughtworks.webdriver.firefox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.thoughtworks.webdriver.AbstractDriverTestCase;
import com.thoughtworks.webdriver.By;
import com.thoughtworks.webdriver.Ignore;
import com.thoughtworks.webdriver.NeedsFreshDriver;
import com.thoughtworks.webdriver.NoSuchElementException;
import com.thoughtworks.webdriver.WebElement;

public class FirefoxDriverTest extends AbstractDriverTestCase {
    public void testShouldContinueToWorkIfUnableToFindElementById() {
        driver.get(formPage);

        try {
            driver.findElement(By.id("notThere"));
            fail("Should not be able to select element by id here");
        } catch (NoSuchElementException e) {
            // This is expected
        }

        // Is this works, then we're golden
        driver.get(xhtmlTestPage);
    }

    public void testShouldSwitchFocusToANewWindowWhenItIsOpenedAndNotStopFutureOperations() {
        driver.get(xhtmlTestPage);

        driver.findElement(By.linkText("Open new window")).click();
        assertThat(driver.getTitle(), equalTo("XHTML Test Page"));

        driver.switchTo().window("result");
        assertThat(driver.getTitle(), equalTo("We Arrive Here"));

        driver.get(iframePage);
        driver.findElement(By.id("iframe_page_heading"));
    }

    @NeedsFreshDriver
    @Ignore(value = "firefox", reason = "Need to figure out how to open a new browser instance mid-test")
    public void testShouldWaitUntilBrowserHasClosedProperly() throws Exception {
      driver.get(simpleTestPage);
      driver.close();

      setUp();

      driver.get(formPage);
      WebElement textarea = driver.findElement(By.id("withText"));
      String expectedText = "I like cheese\n\nIt's really nice";
      textarea.setValue(expectedText);

      String seenText = textarea.getValue();
      assertThat(seenText, equalTo(expectedText));
    }
}