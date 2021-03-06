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

package org.openqa.selenium.firefox.internal;

import org.openqa.selenium.Platform;
import org.openqa.selenium.firefox.Command;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxLauncher;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NewProfileExtensionConnection extends AbstractExtensionConnection {
  private static long TIMEOUT_IN_SECONDS = 20;
  private static long MILLIS_IN_SECONDS = 1000;
  private FirefoxBinary process;
  private Socket lockSocket;
  private FirefoxProfile profile;

  public NewProfileExtensionConnection(FirefoxBinary binary, FirefoxProfile profile, String host) throws IOException {
    this.profile = profile;
    getLock(profile.getPort());
        try {
          int portToUse = determineNextFreePort(host, profile.getPort());

          process = new FirefoxLauncher(binary).startProfile(profile, portToUse);

          setAddress(host, portToUse);

          connectToBrowser(TIMEOUT_IN_SECONDS * MILLIS_IN_SECONDS);
        } finally {
          releaseLock();
        }
    }

    protected void getLock(int port) throws IOException {
    InetSocketAddress address = getAddressForLock(port);

    lockSocket = new Socket();
    long maxWait = System.currentTimeMillis() + 45000;  // 45 seconds

    while (System.currentTimeMillis() < maxWait) {
      try {
        if (isLockFree(address))
          return;
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    throw new IOException("Unable to bind to locking port");
  }

  protected InetSocketAddress getAddressForLock(int port) {
    int lockPort = port - 1;
    return new InetSocketAddress("localhost", lockPort);
  }

  private boolean isLockFree(InetSocketAddress address) throws IOException {
    try {
      lockSocket.bind(address);
      return true;
    } catch (BindException e) {
      return false;
    }
  }

  protected void releaseLock() throws IOException {
    if (lockSocket != null && lockSocket.isBound())
      lockSocket.close();
  }  

  protected int determineNextFreePort(String host, int port) throws IOException {
    // Attempt to connect to the given port on the host
    // If we can't connect, then we're good to use it
    int newport;

    for (newport = port; newport < port + 200; newport++) {
      Socket socket = new Socket();
      InetSocketAddress address = new InetSocketAddress(host, newport);

      try {
        socket.bind(address);
        return newport;
      } catch (BindException e) {
        // Port is already bound. Skip it and continue
      } finally {
        socket.close();
      }
    }

    throw new RuntimeException(String.format("Cannot find free port in the range %d to %d ", port, newport));
  }

  public void quit() {
        try {
            sendMessageAndWaitForResponse(RuntimeException.class, new Command(null, "quit"));
        } catch (Exception e) {
            // this is expected
        }

        if (Platform.getCurrent().is(Platform.WINDOWS)) {
        	quitOnWindows();
        } else {
            quitOnOtherPlatforms();
        }

        profile.clean();    
    }

	private void quitOnOtherPlatforms() {
		// Wait for process to die and return
		try {
		    process.waitFor();
		} catch (InterruptedException e) {
		    throw new RuntimeException(e);
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void quitOnWindows() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}