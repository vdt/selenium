/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.
Portions copyright 2007 ThoughtWorks, Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.firefox;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.Speed;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.internal.ExtensionConnectionFactory;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.ReturnedCookie;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * An implementation of the {#link WebDriver} interface that drives Firefox. This works through a firefox extension,
 * which can be installed via the {#link FirefoxLauncher}. Important system variables are:
 * <ul>
 *  <li><b>webdriver.firefox.bin</b> - Which firefox binary to use (normally "firefox" on the PATH).</li>
 *  <li><b>webdriver.firefox.profile</b> - The name of the profile to use (normally "WebDriver").</li>
 * </ul>
 *
 * When the driver starts, it will make a copy of the profile it is using, rather than using that profile directly.
 * This allows multiple instances of firefox to be started.
 */
public class FirefoxDriver implements WebDriver, SearchContext, JavascriptExecutor,
        FindsById, FindsByClassName, FindsByLinkText, FindsByName, FindsByTagName, FindsByXPath {
    public static final int DEFAULT_PORT = 7055;

    private final ExtensionConnection extension;
    protected Context context;

    public FirefoxDriver() {
      this(new FirefoxBinary(), null);
    }

    public FirefoxDriver(String profileName) {
      this(new FirefoxBinary(), findProfile(profileName));
    }

    public FirefoxDriver(String profileName, int port) {
      this(new FirefoxBinary(), findProfile(profileName), port);
    }

    public FirefoxDriver(FirefoxProfile profile) {
      this(new FirefoxBinary(), profile);
    }


    private FirefoxDriver(FirefoxBinary binary, FirefoxProfile profile, int port) {
      this(binary, modifyProfile(profile, port));
    }

    private static FirefoxProfile modifyProfile(FirefoxProfile profile, int port) {
        if (profile == null) {
          throw new NullPointerException("You must pass in a profile");
        }
        profile.setPort(port);
        return profile;
    }

    public FirefoxDriver(FirefoxBinary binary, FirefoxProfile profile) {
        if (profile == null) {
            profile = new FirefoxProfile();
        }

        try {
            profile.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        prepareEnvironment();

        extension = connectTo(binary, profile, "localhost");

        if (!extension.isConnected()) {
            throw new RuntimeException("Unable to connect to Firefox.");
        }

        fixId();
    }

    private FirefoxDriver(ExtensionConnection extension, Context context) {
      this.extension = extension;
      this.context = context;
    }

    private static FirefoxProfile findProfile(String profileName) {
      FirefoxProfile profile = new ProfilesIni().getProfile(profileName);
      if (profile == null)
        throw new NullPointerException("No firefox profile found: " + profileName);
      return profile;
    }

    protected ExtensionConnection connectTo(FirefoxBinary binary, FirefoxProfile profile, String host) {
        return ExtensionConnectionFactory.connectTo(binary, profile, host);
    }

    protected void prepareEnvironment() {
      // Does nothing, but provides a hook for subclasses to do "stuff"
    }

    public void close() {
        try {
            sendMessage(RuntimeException.class, "close");
        } catch (Exception e) {
            // All good
        }
    }

    public String getPageSource() {
        return sendMessage(RuntimeException.class, "getPageSource");
    }

    public void get(String url) {
        sendMessage(RuntimeException.class, "get", url);
    }

    public String getCurrentUrl() {
        return sendMessage(RuntimeException.class, "getCurrentUrl");
    }

    public String getTitle() {
        return sendMessage(RuntimeException.class, "title");
    }

  public List<WebElement> findElements(By by) {
    return by.findElements((SearchContext)this);
  }

  public WebElement findElement(By by) {
    return by.findElement((SearchContext)this);
  }

  public WebElement findElementById(String using) {
      return findElement("selectElementById", using);
  }

  public List<WebElement> findElementsById(String using) {
      return findElements("selectElementsUsingId", using);
  }

  public WebElement findElementByLinkText(String using) {
    return findElement("selectElementUsingLink", using);
  }

  public List<WebElement> findElementsByLinkText(String using) {
    return findElements("selectElementsUsingLink", using);
  }

  public WebElement findElementByPartialLinkText(String using) {
      return findElement("selectElementUsingPartialLinkText", using);
  }

  public List<WebElement> findElementsByPartialLinkText(String using) {
    return findElements("selectElementsUsingPartialLinkText", using);
  }

  public List<WebElement> findElementsByXPath(String using) {
      return findElements("selectElementsUsingXPath", using);
  }

  public List<WebElement> findElementsByClassName(String using) {
    return findElements("selectElementsUsingClassName", using);
  }

  public WebElement findElementByClassName(String using) {
    return findElement("selectElementUsingClassName", using);
  }

  public WebElement findElementByName(String using) {
      return findElement("selectElementByName", using);
  }

  public List<WebElement> findElementsByName(String using) {
      return findElements("selectElementsUsingName", using);
  }

  public WebElement findElementByTagName(String using) {
    return findElement("selectElementByTagName", using);
  }

  public List<WebElement> findElementsByTagName(String using) {
    return findElements("selectElementsByTagName", using);
  }
  
  public WebElement findElementByXPath(String using) {
    return findElement("selectElementUsingXPath", using);
  }

  private WebElement findElement(String commandName, String argument) {
    String elementId = sendMessage(NoSuchElementException.class, commandName, argument);

    return new FirefoxWebElement(this, elementId);
  }

  private List<WebElement> findElements(String commandName, String argument) {
    String returnedIds = sendMessage(RuntimeException.class, commandName, argument);
    List<WebElement> elements = new ArrayList<WebElement>();

    if (returnedIds.length() == 0)
        return elements;

    String[] ids = returnedIds.split(",");
    for (String id : ids) {
        elements.add(new FirefoxWebElement(this, id));
    }
    return elements;
  }

    public TargetLocator switchTo() {
        return new FirefoxTargetLocator();
    }


    public Navigation navigate() {
        return new FirefoxNavigation();
    }

    protected WebDriver findActiveDriver() {
        String response = sendMessage(RuntimeException.class, "findActiveDriver");

        Context newContext = new Context(response);
        if (newContext.getDriverId().equals(newContext.getDriverId())) {
            return this;
        }
        return new FirefoxDriver(extension, newContext);
    }

    private String sendMessage(Class<? extends RuntimeException> throwOnFailure, String methodName, Object... parameters) {
        return sendMessage(throwOnFailure, new Command(context, methodName, parameters));
    }

    protected String sendMessage(Class<? extends RuntimeException> throwOnFailure, Command command) {
        Response response = extension.sendMessageAndWaitForResponse(throwOnFailure, command);
        context = response.getContext();
        response.ifNecessaryThrow(throwOnFailure);
        return response.getResponseText();
    }

    private void fixId() {
        String response = sendMessage(RuntimeException.class, "findActiveDriver");
        this.context = new Context(response);
    }

    public void quit() {
        extension.quit();
    }

  public String getWindowHandle() {
    return sendMessage(RuntimeException.class, "getCurrentWindowHandle");
  }

  public Set<String> getWindowHandles() {
    String allHandles = sendMessage(RuntimeException.class, "getAllWindowHandles");
    String[] handles = allHandles.split(",");
    HashSet<String> toReturn = new HashSet<String>();
    for (String handle : handles) {
      toReturn.add(handle);
    }
    return toReturn;
  }

  public Object executeScript(String script, Object... args) {
        // Escape the quote marks
        script = script.replaceAll("\"", "\\\"");

        Object[] convertedArgs = convertToJsObjects(args);

        Command command = new Command(context, null, "executeScript", script, convertedArgs);
    	Response response = extension.sendMessageAndWaitForResponse(RuntimeException.class, command);
        context = response.getContext();
        response.ifNecessaryThrow(RuntimeException.class);

        if ("NULL".equals(response.getExtraResult("resultType")))
          return null;

        String resultType = (String) response.getExtraResult("resultType");
        if ("ELEMENT".equals(resultType))
        	return new FirefoxWebElement(this, response.getResponseText());

        Object result = response.getExtraResult("response");
        if (result instanceof Integer)
          return new Long((String) response.getResponseText());
        return result;
    }

  private Object[] convertToJsObjects(Object[] args) {
    if (args.length == 0)
      return null;

    Object[] converted = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      converted[i] = convertToJsObject(args[i]);
    }

    return converted;
  }

  private Object convertToJsObject(Object arg) {
    Map<String, Object> converted = new HashMap<String, Object>();

    if (arg instanceof String) {
      converted.put("type", "STRING");
      converted.put("value", arg);
    } else if (arg instanceof Number) {
      converted.put("type", "NUMBER");
      converted.put("value", ((Number) arg).longValue());
    } else if (isPrimitiveNumberType(arg)) {
      converted.put("type", "NUMBER");
      converted.put("value", getPrimitiveTypeAsLong(arg));
    } else if (arg instanceof Boolean) {
      converted.put("type", "BOOLEAN");
      converted.put("value", ((Boolean) arg).booleanValue());
    } else if (arg.getClass() == boolean.class) {
      converted.put("type", "BOOLEAN");
      converted.put("value", arg);
    } else if (arg instanceof FirefoxWebElement) {
      converted.put("type", "ELEMENT");
      converted.put("value", ((FirefoxWebElement) arg).getElementId());
    } else {
      throw new IllegalArgumentException("Argument is of an illegal type: " + arg);
    }

    return converted;
  }

  private Long getPrimitiveTypeAsLong(Object arg) {
    return Long.valueOf(String.valueOf(arg)); // Clever
  }

  private boolean isPrimitiveNumberType(Object arg) {
    if (!arg.getClass().isPrimitive()) {
      return false;
    }

    return arg.getClass() == long.class ||
           arg.getClass() == int.class ||
           arg.getClass() == short.class; // And so on. That's the common case done :)
  }

  public Options manage() {
        return new FirefoxOptions();
    }

    private class FirefoxOptions implements Options {
        private final List<String> fieldNames = Arrays.asList("domain", "expiry", "name", "path", "value", "secure");
        private final DateFormat RFC_1123_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'Z", Locale.US);
        //TODO: need to refine the values here
        private final int SLOW_SPEED = 1;
        private final int MEDIUM_SPEED = 10;
        private final int FAST_SPEED = 100;

        public void addCookie(Cookie cookie) {
            sendMessage(RuntimeException.class, "addCookie", convertToJson(cookie));
        }

        private String convertToJson(Cookie cookie) {
          JSONObject json = new JSONObject();

          BeanInfo info;
            try {
                info = Introspector.getBeanInfo(Cookie.class);
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
            PropertyDescriptor[] properties = info.getPropertyDescriptors();

            for (PropertyDescriptor property : properties) {
                if (fieldNames.contains(property.getName())) {
                    Object result;
                    try {
                        result = property.getReadMethod().invoke(cookie);
                        json.put(property.getName(), result);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (cookie.getExpiry() != null)
                try {
                    json.put("expiry",  RFC_1123_DATE_FORMAT.format(cookie.getExpiry()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            return json.toString();
        }

        public Set<Cookie> getCookies() {
            String response = sendMessage(RuntimeException.class, "getCookie").trim();
            Set<Cookie> cookies = new HashSet<Cookie>();

            if(!"".equals(response)) {
                for(String cookieString : response.split("\n")) {
                    if ("".equals(cookieString.trim())) continue;

                    HashMap<String, String> attributesMap = new HashMap<String, String>();
                    attributesMap.put("name", "");
                    attributesMap.put("value", "");
                    attributesMap.put("domain", "");
                    attributesMap.put("path", "");
                    attributesMap.put("expires", "");
                    attributesMap.put("secure", "false");

                    for (String attribute : cookieString.split(";")) {
                        if(attribute.contains("=")) {
                            String[] tokens = attribute.trim().split("=", 2);
                            if(attributesMap.get("name").equals("")) {
                                attributesMap.put("name", tokens[0]);
                                attributesMap.put("value", tokens[1]);
                            } else if("domain".equals(tokens[0])
                                    && tokens[1].trim().startsWith(".")) {
                                //convert " .example.com" into "example.com" format
                                int offset = tokens[1].indexOf(".") + 1;
                                attributesMap.put("domain", tokens[1].substring(offset));
                            } else if (tokens.length > 1) {
                                attributesMap.put(tokens[0], tokens[1]);
                            }
                        } else if (attribute.equals("secure")) {
                            attributesMap.put("secure", "true");
                        }
                    }
                    Date expires = null;
                  String expiry = attributesMap.get("expires");
                  if (expiry != null && !"".equals(expiry) && !expiry.equals("0")) {
                        //firefox stores expiry as number of seconds
                        expires = new Date(Long.parseLong(attributesMap.get("expires")));
                    }

                    cookies.add(new ReturnedCookie(attributesMap.get("name"), attributesMap.get("value"),
                            attributesMap.get("domain"), attributesMap.get("path"),
                            expires, Boolean.parseBoolean(attributesMap.get("secure"))));
                }

            }
            return cookies;
        }

        public void deleteCookieNamed(String name) {
            Cookie toDelete = new Cookie(name, "");
            sendMessage(RuntimeException.class, "deleteCookie", convertToJson(toDelete));
        }

        public void deleteCookie(Cookie cookie) {
            sendMessage(RuntimeException.class, "deleteCookie", convertToJson(cookie));
        }

        public void deleteAllCookies() {
            Set<Cookie> cookies = getCookies();
            for(Cookie c : cookies) {
                deleteCookie(c);
            }
        }

        public Speed getSpeed() {
            int pixelSpeed = Integer.parseInt(sendMessage(RuntimeException.class, "getMouseSpeed"));
            Speed speed;

            // TODO: simon 2007-02-01; Delegate to the enum
            switch (pixelSpeed) {
                case SLOW_SPEED:
                    speed = Speed.SLOW;
                    break;
                case MEDIUM_SPEED:
                    speed = Speed.MEDIUM;
                    break;
                case FAST_SPEED:
                    speed = Speed.FAST;
                    break;
                default:
                    //TODO: log a warning here
                    speed = Speed.FAST;
                    break;
            }
            return speed;
        }

        public void setSpeed(Speed speed) {
            int pixelSpeed;
            // TODO: simon 2007-02-01; Delegate to the enum
            switch(speed) {
                case SLOW:
                    pixelSpeed = SLOW_SPEED;
                    break;
                case MEDIUM:
                    pixelSpeed = MEDIUM_SPEED;
                    break;
                case FAST:
                    pixelSpeed = FAST_SPEED;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            sendMessage(RuntimeException.class, "setMouseSpeed", "" + pixelSpeed);
        }
    }

    private class FirefoxTargetLocator implements TargetLocator {
        public WebDriver frame(int frameIndex) {
            return frame(String.valueOf(frameIndex));
        }

        public WebDriver frame(String frameName) {
            sendMessage(NoSuchFrameException.class, "switchToFrame", frameName);
            return FirefoxDriver.this;
        }

        public WebDriver window(String windowName) {
            String response = sendMessage(NoSuchWindowException.class, "switchToWindow", String.valueOf(windowName));
            if (response == null || "No window found".equals(response)) {
                throw new NoSuchWindowException("Cannot find window: " + windowName);
            }
            try {
                FirefoxDriver.this.context = new Context(response);
            } catch (NumberFormatException e) {
                throw new RuntimeException("When switching to window: " + windowName + " ---- " + response);
            }
            return FirefoxDriver.this;
        }

      public WebDriver defaultContent() {
            sendMessage(RuntimeException.class, "switchToDefaultContent");
            return FirefoxDriver.this;
        }


        public WebElement activeElement() {
            return findElement("switchToActiveElement", "active element");
        }

        public Alert alert() {
            throw new UnsupportedOperationException("alert");
        }
    }

    private class FirefoxNavigation implements Navigation {
      public void back() {
        sendMessage(RuntimeException.class, "goBack");
      }

      public void forward() {
        sendMessage(RuntimeException.class, "goForward");
      }

      public void to(String url) {
        get(url);
      }

      public void to(URL url) {
        get(String.valueOf(url));
      }
    }

  private class FirefoxDriverIterator implements Iterator<WebDriver> {
    private String[] allHandles;
    private int index = 0;

    public FirefoxDriverIterator(String handlesString) {
      allHandles = handlesString.split(",");
    }

    public boolean hasNext() {
      return allHandles.length > index;
    }

    public WebDriver next() {
      // Ugh. The context needs some reworking.
      return new FirefoxDriver(extension, new Context(allHandles[index++] + " ?"));
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }

    /** Saves a screenshot of the current page into the given file. */
    public void saveScreenshot(File pngFile) {
        if (pngFile == null) {
            throw new IllegalArgumentException("Method parameter pngFile must not be null");
        }
        File dir = pngFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Could not create directory " + dir.getAbsolutePath());
        }
        sendMessage(RuntimeException.class, "saveScreenshot", pngFile.getAbsolutePath());
    }
}
