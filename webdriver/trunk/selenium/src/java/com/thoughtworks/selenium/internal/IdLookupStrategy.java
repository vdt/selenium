package com.thoughtworks.selenium.internal;

import com.thoughtworks.webdriver.WebDriver;
import com.thoughtworks.webdriver.WebElement;

public class IdLookupStrategy implements LookupStrategy {
	public WebElement find(WebDriver driver, String use) {
		return driver.selectElement("id=" + use);
	}
}