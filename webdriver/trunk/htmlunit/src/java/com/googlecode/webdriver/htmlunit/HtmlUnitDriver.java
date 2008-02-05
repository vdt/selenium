/*
 * Copyright 2007 ThoughtWorks, Inc
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

package com.googlecode.webdriver.htmlunit;

import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpState;
import org.jaxen.JaxenException;
import org.jaxen.XPath;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.WebWindowEvent;
import com.gargoylesoftware.htmlunit.WebWindowListener;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.xpath.HtmlUnitXPath;
import com.googlecode.webdriver.Alert;
import com.googlecode.webdriver.By;
import com.googlecode.webdriver.Cookie;
import com.googlecode.webdriver.NoSuchElementException;
import com.googlecode.webdriver.NoSuchFrameException;
import com.googlecode.webdriver.Speed;
import com.googlecode.webdriver.WebDriver;
import com.googlecode.webdriver.WebElement;
import com.googlecode.webdriver.internal.FindsById;
import com.googlecode.webdriver.internal.FindsByLinkText;
import com.googlecode.webdriver.internal.FindsByXPath;

public class HtmlUnitDriver implements WebDriver, FindsById, FindsByLinkText, FindsByXPath {
    private WebClient webClient;
    private WebWindow currentWindow;

    public HtmlUnitDriver() {
        newWebClient();
        webClient.addWebWindowListener(new WebWindowListener() {
            private boolean waitingToLoad;

            public void webWindowOpened(WebWindowEvent webWindowEvent) {
                waitingToLoad = true;
            }

            public void webWindowContentChanged(WebWindowEvent webWindowEvent) {
                if (waitingToLoad) {
                    waitingToLoad = false;
                    webClient.setCurrentWindow(webWindowEvent.getWebWindow());
                }
            }

            public void webWindowClosed(WebWindowEvent webWindowEvent) {
                pickWindow();
            }
        });
    }

    private HtmlUnitDriver(WebWindow currentWindow) {
        this();
        this.currentWindow = currentWindow;
    }

    protected void newWebClient() {
        webClient = new WebClient();
        webClient.setThrowExceptionOnFailingStatusCode(true);
        webClient.setJavaScriptEnabled(false);
        webClient.setRedirectEnabled(true);
    }

    public void get(String url) {
        try {
            URL fullUrl = new URL(url);
            Page page = webClient.getPage(fullUrl);
            page.initialize();
        } catch (UnknownHostException e) {
          // This should be fine
        } catch (ConnectException e) {
          // This might be expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        pickWindow();
    }

    private void pickWindow() {
        currentWindow = webClient.getCurrentWindow();
        Page page = webClient.getCurrentWindow().getEnclosedPage();

        if (page == null)
          return;

        if (((HtmlPage) page).getFrames().size() > 0) {
            FrameWindow frame = (FrameWindow) ((HtmlPage) page).getFrames().get(0);
            if (!(frame.getFrameElement() instanceof HtmlInlineFrame))
                switchTo().frame(0);
        }
    }

    public String getCurrentUrl() {
        return lastPage().getWebResponse().getUrl().toString();
    }

    public String getTitle() {
        HtmlPage htmlPage = lastPage();
        if (htmlPage == null) {
            return null; // no page so there is no title
        }
        return htmlPage.getTitleText();
    }

    public boolean getVisible() {
        return false;
    }

    public void setVisible(boolean visible) {
        // no-op
    }

    @SuppressWarnings("unchecked")
    public List<WebElement> findElementsByXPath(String selector) {
        try {
            HtmlUnitXPath xpath = new HtmlUnitXPath(selector);
            List<HtmlElement> nodes = xpath.selectNodes(lastPage());
            List<WebElement> elements = new ArrayList<WebElement>();

            for (int i = 0; i < nodes.size(); i++) {
                elements.add(new HtmlUnitWebElement(this, nodes.get(i)));
            }

            return elements;
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
    }

    public WebElement findElement(By by) {
        return by.findElement(this);
    }

    public List<WebElement> findElements(By by) {
        return by.findElements(this);
    }

    public String getPageSource() {
        WebResponse webResponse = lastPage().getWebResponse();
        return webResponse.getContentAsString();
    }

    public void close() {
        newWebClient();
    }

    public void quit() {
    	webClient = null;
    	currentWindow = null;
    }

    public TargetLocator switchTo() {
        return new HtmlUnitTargetLocator();
    }


  public Navigation navigate() {
    return new HtmlUnitNavigation();
  }

  private synchronized HtmlPage lastPage() {
        return (HtmlPage) currentWindow.getEnclosedPage();
    }

    @SuppressWarnings("unchecked")
    public WebElement findElementByLinkText(String selector) {
        int equalsIndex = selector.indexOf('=') + 1;
        String expectedText = selector.substring(equalsIndex).trim();

        List<HtmlAnchor> anchors = lastPage().getAnchors();
        Iterator<HtmlAnchor> allAnchors = anchors.iterator();
        while (allAnchors.hasNext()) {
            HtmlAnchor anchor = allAnchors.next();
            if (expectedText.equals(anchor.asText())) {
                return new HtmlUnitWebElement(this, anchor);
            }
        }
        throw new NoSuchElementException("No link found with text: " + expectedText);
    }

  @SuppressWarnings("unchecked")
  public List<WebElement> findElementsByLinkText(String selector) {
    int equalsIndex = selector.indexOf('=') + 1;
    String expectedText = selector.substring(equalsIndex).trim();

    List<HtmlAnchor> anchors = lastPage().getAnchors();
    Iterator<HtmlAnchor> allAnchors = anchors.iterator();
    List<WebElement> elements = new ArrayList<WebElement>();
    while (allAnchors.hasNext()) {
      HtmlAnchor anchor = allAnchors.next();
      if (expectedText.equals(anchor.asText())) {
        elements.add(new HtmlUnitWebElement(this, anchor));
      }
    }
    return elements;
  }

    public WebElement findElementById(String id) {
        try {
            HtmlElement element = lastPage().getHtmlElementById(id);
            return new HtmlUnitWebElement(this, element);
        } catch (ElementNotFoundException e) {
            throw new NoSuchElementException("Cannot find element with ID: " + id);
        }
    }

    public List<WebElement> findElementsById(String id) {
        return findElementsByXPath("//*[@id='" + id + "']");
    }

    public WebElement findElementByXPath(String selector) {
        try {
            XPath xpath = new HtmlUnitXPath(selector);
            Object node = xpath.selectSingleNode(lastPage());
            if (node == null)
                throw new NoSuchElementException("Cannot locate a node using " + selector);
            return new HtmlUnitWebElement(this, (HtmlElement) node);
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
    }

    private class HtmlUnitTargetLocator implements TargetLocator {
        public WebDriver frame(int frameIndex) {
            HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
            try {
                currentWindow = (WebWindow) page.getFrames().get(frameIndex);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchFrameException("Cannot find frame: " + frameIndex);
            }
            return HtmlUnitDriver.this;
        }

        public WebDriver frame(String name) {
            HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
            WebWindow window = webClient.getCurrentWindow();

            String[] names = name.split("\\.");
            for (String frameName : names) {
                try {
                    int index = Integer.parseInt(frameName);
                    window = (WebWindow) page.getFrames().get(index);
                } catch (NumberFormatException e) {
                    window = null;
                    for (Object frame : page.getFrames()) {
                        FrameWindow frameWindow = (FrameWindow) frame;
                        if (frameName.equals(frameWindow.getFrameElement().getId())) {
                            window = frameWindow;
                            break;
                        } else if (frameName.equals(frameWindow.getName())) {
                            window = frameWindow;
                            break;
                        }
                    }
                    if (window == null) {
                        throw new NoSuchFrameException("Cannot find frame: " + name);
                    }
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchFrameException("Cannot find frame: " + name);
                }

                page = (HtmlPage) window.getEnclosedPage();
            }

            currentWindow = window;
            return HtmlUnitDriver.this;
        }

        public WebDriver window(String windowId) {
            WebWindow window = webClient.getWebWindowByName(windowId);
            webClient.setCurrentWindow(window);
            pickWindow();
            return HtmlUnitDriver.this;
        }

        public WebDriver defaultContent() {
            pickWindow();
            return HtmlUnitDriver.this;
        }


        public WebElement activeElement() {
            Page page = currentWindow.getEnclosedPage();
            if (page instanceof HtmlPage) {
                HtmlElement element = ((HtmlPage) page).getElementWithFocus();
                if (element == null) {
                    List allBodies = ((HtmlPage) page).getDocumentElement().getHtmlElementsByTagName("body");
                    if (allBodies.size() > 0)
                        return new HtmlUnitWebElement(HtmlUnitDriver.this, (HtmlElement) allBodies.get(0));
                }
            }

            throw new NoSuchElementException("Unable to locate element with focus or body tag");
        }

        public Alert alert() {
            return null;
        }
    }

    protected WebDriver findActiveWindow() {
        WebWindow window = webClient.getCurrentWindow();
        HtmlPage page = (HtmlPage) window.getEnclosedPage();

        if (page != null && page.getFrames().size() > 0) {
            FrameWindow frame = (FrameWindow) page.getFrames().get(0);
            if (!(frame.getFrameElement() instanceof HtmlInlineFrame))
                return new HtmlUnitDriver(frame);
        }

        if (currentWindow != null && currentWindow.equals(window))
            return this;
        return new HtmlUnitDriver(window);
    }


    protected WebClient getWebClient() {
        return webClient;
    }

    protected WebWindow getCurrentWindow() {
        return currentWindow;
    }

    private class HtmlUnitNavigation implements Navigation {
      public void back() {
        // This functionality isn't already built into htmlunit. I'm surprised.
        // https://sourceforge.net/tracker/index.php?func=detail&aid=1337174&group_id=47038&atid=448269
        // for more the HtmlUnit feature request.
        throw new UnsupportedOperationException("back");
      }


      public void forward() {
        throw new UnsupportedOperationException("forward");
      }


      public void to(String url) {
        get(url);
      }
    }

    public Options manage() {
        return new HtmlUnitOptions();
    }

    private class HtmlUnitOptions implements Options {
        private HttpState state;

        HtmlUnitOptions() {
            state = webClient.getWebConnection().getState();
        }

        public void addCookie(Cookie cookie) {
            state.addCookie(new org.apache.commons.httpclient.Cookie(cookie.getDomain(),
                    cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiry(),
                    cookie.isSecure()));
        }

        public void deleteCookieNamed(String name) {
            //Assume the cookie either doesn't have a domain or has the same domain as the current
            //page. Delete the cookie for both cases.
            state.addCookie(new org.apache.commons.httpclient.Cookie(getHostName(), name, "", "",
                    new Date(0), false));
            state.addCookie(new org.apache.commons.httpclient.Cookie("", name, "", "", new Date(0),
                    false));
        }

        public void deleteCookie(Cookie cookie) {
            state.addCookie(new org.apache.commons.httpclient.Cookie(cookie.getDomain(),
                    cookie.getName(), cookie.getValue(), cookie.getPath(), new Date(0),
                    cookie.isSecure()));
        }

        public void deleteAllCookies() {
            state.clearCookies();
        }

        public Set<Cookie> getCookies() {
            HttpState state = webClient.getWebConnection().getState();
            org.apache.commons.httpclient.Cookie[] rawCookies = state.getCookies();
            
            Set<Cookie> retCookies = new HashSet<Cookie>();
            for(org.apache.commons.httpclient.Cookie c : rawCookies) {
                if("".equals(c.getDomain()) || getHostName().indexOf(c.getDomain()) != -1) {
                	if (c.getPath() != null && getPath().startsWith(c.getPath())) {
                		retCookies.add(new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(),
                            c.getExpiryDate(), c.getSecure()));
                	}
                }
            }
            return retCookies;  
        }

        private String getHostName() {
            return lastPage().getWebResponse().getUrl().getHost().toLowerCase();
        }
        
        private String getPath() {
        	return lastPage().getWebResponse().getUrl().getPath();
        }

        public Speed getMouseSpeed() {
            throw new UnsupportedOperationException();
        }

        public void setMouseSpeed(Speed speed) {
            throw new UnsupportedOperationException();
        }
    }
}