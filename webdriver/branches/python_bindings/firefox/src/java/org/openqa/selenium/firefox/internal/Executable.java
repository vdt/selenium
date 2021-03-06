package org.openqa.selenium.firefox.internal;

import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.util.Map;

/**
 * Wrapper around our runtime environment requirements.
 * Performs discovery of firefox instances.
 * 
 * <p>NOTE: System and platform binaries will only be discovered at class initialization.
 * 
 * @author gregory.block@google.com (Gregory Block)
 */
public class Executable {
  private static final File SYSTEM_BINARY = locateFirefoxBinaryFromSystemProperty();
  private static final File PLATFORM_BINARY = locateFirefoxBinaryFromPlatform();
  
  private final File binary;
  
  public Executable(File userSpecifiedBinaryPath) {
    if (userSpecifiedBinaryPath != null) {
      
      // It should exist and be a file.
      if (userSpecifiedBinaryPath.exists() && userSpecifiedBinaryPath.isFile()) {
        binary = userSpecifiedBinaryPath;
        return;
      }
      
      throw new WebDriverException(
          "Specified firefox binary location does not exist or is not a real file: " + 
          userSpecifiedBinaryPath);
    }

    if (SYSTEM_BINARY != null && SYSTEM_BINARY.exists()) {
      binary = SYSTEM_BINARY;
      return;
    }

    if (PLATFORM_BINARY != null && PLATFORM_BINARY.exists()) {
      binary = PLATFORM_BINARY;
      return;
    }
    
    throw new WebDriverException("Cannot find firefox binary in PATH. " +
        "Make sure firefox is installed. OS appears to be: " + Platform.getCurrent());
  }
  
  public File getFile() {
    return binary;
  }
  
  public String getPath() {
    return binary.getAbsolutePath();
  }
  
  public void setLibraryPath(ProcessBuilder builder, Map<String, String> extraEnv) {
    final String propertyName = getLibraryPathPropertyName();
    StringBuilder libraryPath = new StringBuilder();
    
    // If we have an env var set for the path, use it.
    String env = getEnvVar(propertyName, null);
    if (env != null) {
        libraryPath.append(env).append(File.pathSeparator);
    }
    
    // Check our extra env vars for the same var, and use it too.
    env = extraEnv.get(propertyName);
    if (env != null) {
        libraryPath.append(env).append(File.pathSeparator);
    }

    // Last, add the contents of the specified system property, defaulting to the binary's path.
    String firefoxLibraryPath = System.getProperty("webdriver.firefox.library.path", 
        binary.getParentFile().getAbsolutePath());
    libraryPath.append(firefoxLibraryPath).append(File.pathSeparator).append(libraryPath);

    // Add the library path to the builder.
    builder.environment().put(propertyName, libraryPath.toString());
  }
  
  /**
   * Locates the firefox binary from a system property.
   */
  private static File locateFirefoxBinaryFromSystemProperty() {
      String binaryName = System.getProperty("webdriver.firefox.bin");
      if (binaryName == null)
          return null;
  
      File binary = new File(binaryName);
      if (binary.exists())
          return binary;
  
      switch (Platform.getCurrent()) {
          case WINDOWS:
          case VISTA:
          case XP:
              return null;
  
          case MAC:
              if (!binaryName.endsWith(".app"))
                  binaryName += ".app";
              binaryName += "/Contents/MacOS/firefox";
              return new File(binaryName);
  
          default:
              return findBinary(binaryName);
      }
  }
  
  /**
   * Locates the firefox binary by platform.
   */
  private static File locateFirefoxBinaryFromPlatform() {
    switch (Platform.getCurrent()) {
      case WINDOWS:
      case VISTA:
      case XP:
          return new File(getEnvVar("PROGRAMFILES", "\\Program Files") + "\\Mozilla Firefox\\firefox.exe");
      case MAC:
          return new File("/Applications/Firefox.app/Contents/MacOS/firefox");
      default:
          return findBinary("firefox3", "firefox2", "firefox");
    }
  }
  
  /**
   * Retrieve an env var; if no var is set, returns the default
   * 
   * @param name the name of the variable
   * @param defaultValue the default value of the variable
   * @return the env var
   */
  private static final String getEnvVar(String name, String defaultValue) {
    final String value = System.getenv(name);
    if (value != null) {
      return value;
    }
    return defaultValue;
  }
  
  /**
   * Retrieves the platform specific env property name which contains the library path.
   */
  private static String getLibraryPathPropertyName() {
    switch (Platform.getCurrent()) {
      case MAC:
          return "DYLD_LIBRARY_PATH";
      case WINDOWS:
          return "PATH";
      default:
          return "LD_LIBRARY_PATH";
    }
  }

  /**
   * UNIXy-only: walk a PATH to locate binaries with a specified name.
   * Binaries will be searched for in the order they are provided.
   * 
   * TODO(gblock): Consider using this on Win32
   * @param binaryNames the binary names to search for
   * @return the first binary found matching that name.
   */
  private static File findBinary(String... binaryNames) {
    final String[] paths = System.getenv("PATH").split(File.pathSeparator);
    for (String binaryName : binaryNames) {
      for (String path : paths) {
        File file = new File(path, binaryName);
        if (file.exists()) {
          return file;
        }
      }
    }
    return null;
  }
}
