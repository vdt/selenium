/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

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

package org.openqa.selenium.ie;

import static org.openqa.selenium.ie.ExportedWebDriverFunctions.SUCCESS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.Speed;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.ReturnedCookie;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

public class InternetExplorerDriver implements WebDriver, SearchContext, JavascriptExecutor {
    private static ExportedWebDriverFunctions lib;
    private Pointer driver;
    private ErrorHandler errors = new ErrorHandler();

    public InternetExplorerDriver() {
      intializeLib();
      PointerByReference ptr = new PointerByReference();
      int result = lib.wdNewDriverInstance(ptr);
      if (result != SUCCESS) {
        throw new IllegalStateException("Cannot create new browser instance: " + result);
      }
      driver = ptr.getValue();
    }

    public String getPageSource() {
      PointerByReference wrapper = new PointerByReference();
      int result = lib.wdGetPageSource(driver, wrapper);
      
      errors.verifyErrorCode(result, "Unable to get page source");
      
      return new StringWrapper(lib, wrapper).toString();
    }

    public void close() {
      int result = lib.wdClose(driver);
      if (result != SUCCESS) {
        throw new IllegalStateException("Unable to close driver: " + result);
      }
    }
    
    public void quit() {
    	close();  // Not a good implementation, but better than nothing
    }

  public Set<String> getWindowHandles() {
    return Collections.singleton("1");
  }

  public String getWindowHandle() {
    return "1";
  }

  public Object executeScript(String script, Object... args) {
    PointerByReference scriptArgsRef = new PointerByReference();
    int result = lib.wdNewScriptArgs(scriptArgsRef, args.length);
    errors.verifyErrorCode(result, "Unable to create new script arguments array");
    Pointer scriptArgs = scriptArgsRef.getValue();
    
    try {
      populateArguments(result, scriptArgs, args);
      
      script = "(function() { return function(){" + script + "};})();";
      
      PointerByReference scriptResultRef = new PointerByReference();
      result = lib.wdExecuteScript(driver, new WString(script), scriptArgs, scriptResultRef);
      
      errors.verifyErrorCode(result, "Cannot execute script");
      Object toReturn = extractReturnValue(scriptResultRef);
      return toReturn;
    } finally {
      lib.wdFreeScriptArgs(scriptArgs);
    }
  }

  public boolean isJavascriptEnabled() {
    return true;
  }

  private Object extractReturnValue(PointerByReference scriptResultRef) {
    int result;
    Pointer scriptResult = scriptResultRef.getValue();
    
    IntByReference type = new IntByReference();
    result = lib.wdGetScriptResultType(scriptResult, type);
    
    errors.verifyErrorCode(result, "Cannot determine result type");
    
    try {
      Object toReturn;
      switch (type.getValue()) {
      case 1:
        PointerByReference wrapper = new PointerByReference();
        result = lib.wdGetStringScriptResult(scriptResult, wrapper);
        errors.verifyErrorCode(result, "Cannot extract string result");
        toReturn = new StringWrapper(lib, wrapper).toString();
        break;
        
      case 2:
        NativeLongByReference value = new NativeLongByReference();
        result = lib.wdGetNumberScriptResult(scriptResult, value);
        errors.verifyErrorCode(result, "Cannot extract number result");
        toReturn = value.getValue().longValue();
        break;
        
      case 3:
        IntByReference boolVal = new IntByReference();
        result = lib.wdGetBooleanScriptResult(scriptResult, boolVal);
        errors.verifyErrorCode(result, "Cannot extract boolean result");
        toReturn = boolVal.getValue() == 1 ? Boolean.TRUE : Boolean.FALSE;
        break;
        
      case 4:
        PointerByReference element = new PointerByReference();
        result = lib.wdGetElementScriptResult(scriptResult, driver, element);
        errors.verifyErrorCode(result, "Cannot extract element result");
        toReturn = new InternetExplorerElement(lib, driver, element.getValue());
        break;
        
      case 5:
        toReturn = null;
        break;
        
      case 6:
        PointerByReference message = new PointerByReference();
        result = lib.wdGetStringScriptResult(scriptResult, message);
        errors.verifyErrorCode(result, "Cannot extract string result");
        throw new WebDriverException(new StringWrapper(lib, message).toString());
        
      default:
        throw new WebDriverException("Cannot determine result type");
      }
      return toReturn;
    } finally {
      lib.wdFreeScriptResult(scriptResult);
    }
  }

  private int populateArguments(int result, Pointer scriptArgs, Object... args) {
    for (Object arg : args) {
      if (arg instanceof String) {
        result = lib.wdAddStringScriptArg(scriptArgs, new WString((String) arg));
      } else if (arg instanceof Boolean) {
        Boolean param = (Boolean) arg;
        result = lib.wdAddBooleanScriptArg(scriptArgs, param == null || !param ? 0 : 1);      
      } else if (arg instanceof Number) {
        long number = ((Number) arg).longValue();
        result = lib.wdAddNumberScriptArg(scriptArgs, new NativeLong(number));
      } else if (arg instanceof InternetExplorerElement) {
        result = ((InternetExplorerElement) arg).addToScriptArgs(scriptArgs);
      } else {
        throw new IllegalArgumentException("Parameter is not of recognized type: " + arg);
      }
      
      errors.verifyErrorCode(result, ("Unable to add argument: " + arg));
    }
    return result;
  }
    
    
    public void get(String url) {
      int result = lib.wdGet(driver, new WString(url));
      if (result != SUCCESS) {
        throw new IllegalStateException(String.format("Cannot get \"%s\": %s", url, result));
      }
    }

    public String getCurrentUrl() {
      PointerByReference ptr = new PointerByReference();
      int result = lib.wdGetCurrentUrl(driver, ptr);
      if (result != SUCCESS) {
        throw new IllegalStateException("Unable to get current URL: " + result);
      }
      
      return new StringWrapper(lib, ptr).toString();
    }

    public String getTitle() {
      PointerByReference ptr = new PointerByReference();
      int result = lib.wdGetTitle(driver, ptr);
      if (result != SUCCESS) {
        throw new IllegalStateException("Unable to get current URL: " + result);
      }
      
      return new StringWrapper(lib, ptr).toString();
    }

    /**
     * Is the browser visible or not?
     *
     * @return True if the browser can be seen, or false otherwise
     */
    public boolean getVisible() {
      IntByReference toReturn = new IntByReference();
      int result = lib.wdGetVisible(driver, toReturn);
      
      errors.verifyErrorCode(result, "Unable to determine if browser is visible");
      
      return toReturn.getValue() == 1;
    }

  /**
   * Make the browser visible or not. 
   * 
   * @param visible Set whether or not the browser is visible
   */  
   public void setVisible(boolean visible) {
     int result = lib.wdSetVisible(driver, visible ? 1 : 0);
     
     errors.verifyErrorCode(result, "Unable to change the visibility of the browser");
   }

    public List<WebElement> findElements(By by) {
    	return new Finder(lib, driver, null).findElements(by);
    }

    public WebElement findElement(By by) {
        return new Finder(lib, driver, null).findElement(by);
    }

    @Override
    public String toString() {
        return getClass().getName() + ": Implement me!";
    }

    public TargetLocator switchTo() {
        return new InternetExplorerTargetLocator();
    }


    public Navigation navigate() {
        return new InternetExplorerNavigation();
    }

    public Options manage() {
        return new InternetExplorerOptions();
    }

    protected native void waitForLoadToComplete();

    @Override
    protected void finalize() throws Throwable {
    	if (driver != null) {
    	  lib.wdFreeDriver(driver);
    	}
    }
    
    private class InternetExplorerTargetLocator implements TargetLocator {
        public WebDriver frame(int frameIndex) {
            return frame(String.valueOf(frameIndex));
        }

        public WebDriver frame(String frameName) {
          int result = lib.wdSwitchToFrame(driver, new WString(frameName));
          
          errors.verifyErrorCode(result, ("Unable to switch to frame: " + frameName));
          
        	return InternetExplorerDriver.this;
        }

        public WebDriver window(String windowName) {
            throw new NoSuchWindowException("Unable to switch to window: " + windowName);
        }

      public WebDriver defaultContent() {
            return frame("");
        }


        public WebElement activeElement() {
          PointerByReference element = new PointerByReference();
          int result = lib.wdSwitchToActiveElement(driver, element);
          
          errors.verifyErrorCode(result, "Unable to find active element");
          
          return new InternetExplorerElement(lib, driver, element.getValue());
        }

        public Alert alert() {
            throw new UnsupportedOperationException("alert");
        }
    }
    
    private class InternetExplorerNavigation implements Navigation {
		public void back() {
		  int result = lib.wdGoBack(driver);
      errors.verifyErrorCode(result, "Unable to go back");
		}
		
		public void forward() {
			int result = lib.wdGoForward(driver);
			errors.verifyErrorCode(result, "Unable to go forward");
		}

		public void to(String url) {
			get(url);
		}
      
        public void to(URL url) {
            get(String.valueOf(url));
        }

      public void refresh() {
        throw new UnsupportedOperationException("refresh");
      }
    }
    
    private class InternetExplorerOptions implements Options {
      
		public void addCookie(Cookie cookie) {
		  int result = lib.wdAddCookie(driver, new WString(cookie.toString()));
		 
		  errors.verifyErrorCode(result, ("Unable to add cookie: " + cookie));
		}

		public void deleteAllCookies() {
			Set<Cookie> cookies = getCookies();
			for (Cookie cookie : cookies) {
				deleteCookie(cookie);
			}
		}

		public void deleteCookie(Cookie cookie) {
			Date dateInPast = new Date(0);
			Cookie toDelete = new ReturnedCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), dateInPast, false);
			addCookie(toDelete);
		}

		public void deleteCookieNamed(String name) {
			deleteCookie(new ReturnedCookie(name, "", getCurrentHost(), "", null, false));
		}

		public Set<Cookie> getCookies() {
			String currentUrl = getCurrentHost();
			
			PointerByReference wrapper = new PointerByReference();
			int result = lib.wdGetCookies(driver, wrapper);
			
			errors.verifyErrorCode(result, "Unable to extract visible cookies");
			
			Set<Cookie> toReturn = new HashSet<Cookie>();
			String allDomainCookies = new StringWrapper(lib, wrapper).toString(); 
			
			String[] cookies = allDomainCookies.split("; ");
			for (String cookie : cookies) {
				String[] parts = cookie.split("=");
				if (parts.length != 2) {
					continue;
				}
				
				toReturn.add(new ReturnedCookie(parts[0], parts[1], currentUrl, "", null, false));
			}
			
	        return toReturn;
		}

		private String getCurrentHost() {
			try {
				URL url = new URL(getCurrentUrl());
				return url.getHost();
			} catch (MalformedURLException e) {
				return "";
			}
		}

        public Speed getSpeed() {
            throw new UnsupportedOperationException();
        }

        public void setSpeed(Speed speed) {
//            doSetMouseSpeed(speed.getTimeOut());
        }
    }

    private synchronized void intializeLib() {
      if (lib != null) {
        return;
      }
      
      StringBuilder jnaPath = new StringBuilder();
      jnaPath.append(System.getProperty("java.class.path"));
      jnaPath.append(File.pathSeparator);
      
      
      // We need to do this before calling any JNA methods because 
      // the map of paths to search is static. Apparently.
      File dll = writeResourceToDisk("InternetExplorerDriver.dll");
      String driverLib = dll.getName().replace(".dll", "");
      jnaPath.append(dll.getParent());
      
      System.setProperty("jna.library.path", jnaPath.toString());
      
      try {
        lib = (ExportedWebDriverFunctions)  Native.loadLibrary("InternetExplorerDriver", ExportedWebDriverFunctions.class);
      } catch (UnsatisfiedLinkError e) {
        lib = (ExportedWebDriverFunctions)  Native.loadLibrary(driverLib, ExportedWebDriverFunctions.class);
      }
    }
    
    private File writeResourceToDisk(String resourceName) throws UnsatisfiedLinkError {
      InputStream is = InternetExplorerDriver.class.getResourceAsStream(resourceName);
      if (is == null) 
          is = InternetExplorerDriver.class.getResourceAsStream("/" + resourceName);
          if (is == null) {
              throw new UnsatisfiedLinkError("Could not find " + resourceName);
          }
          FileOutputStream fos = null;
          
      try {
          File dll = File.createTempFile("webdriver", ".dll");
          dll.deleteOnExit();
          fos = new FileOutputStream(dll);
          
          int count;
          byte[] buf = new byte[4096];
          while ((count = is.read(buf, 0, buf.length)) > 0) {
              fos.write(buf, 0, count);
          }
          
          return dll;
      } catch(IOException e) {
          throw new UnsatisfiedLinkError("Could not create temporary DLL: " + e.getMessage());
      }
      finally {
          if (is != null) {
              try { is.close(); } catch(IOException ignored) { }
          }
          if (fos != null) {
              try { fos.close(); } catch(IOException ignored) { }
          }
      }
    }
}
