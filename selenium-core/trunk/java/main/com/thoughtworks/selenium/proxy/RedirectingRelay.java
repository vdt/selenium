package com.thoughtworks.selenium.proxy;
/*
  Copyright 2004 ThoughtWorks, Inc.

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

import com.thoughtworks.selenium.utils.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.Socket;

/**
 * @version $Id: RedirectingRelay.java,v 1.3 2004/11/13 04:46:57 ahelleso Exp $
 */
public class RedirectingRelay implements Relay {
    private static final Log LOG = LogFactory.getLog(RedirectingRelay.class);
    private static final String proxy = (String) System.getProperties().get("http.proxyHost");
    private static final String proxyPort = (String) System.getProperties().get("http.proxyPort");
    private final RequestInput requestInput;
    private final ResponseStream responseStream;
    private final RequestModificationCommand requestModificationCommand;

    public RedirectingRelay(RequestInput requestInput,
                            ResponseStream responseStream,
                            RequestModificationCommand requestModificationCommand) {
        Assert.assertIsTrue(requestInput != null, "requestInputcan't be null");
        Assert.assertIsTrue(responseStream != null, "responseStream can't be null");
        Assert.assertIsTrue(requestModificationCommand != null, "requestModificationCommand can't be null");
        this.requestInput = requestInput;
        this.responseStream = responseStream;
        this.requestModificationCommand = requestModificationCommand;
    }


    public void relay() throws IOException {
        HTTPRequest request = requestInput.readRequest();
        if (!hasBeenRedirected(request.getUri())) {
            byte[] redirectMessage = redirectResponse(request).getBytes();
            responseStream.write(redirectMessage, redirectMessage.length);
            responseStream.flush();
        } else {
            sendToIntendedTarget(request);
        }
    }

    private boolean hasBeenRedirected(String uri) {
        return uri.startsWith(HTTPRequest.SELENIUM_REDIRECT_PROTOCOL + HTTPRequest.SELENIUM_REDIRECT_SERVER);
    }

    private void sendToIntendedTarget(HTTPRequest request) {
        requestModificationCommand.execute(request);
        String requestToDest = request.getRequest();
        LOG.debug("Writing\n" + requestToDest);
        int port = getPort(request.getDestinationPort(), request.getDestinationServer());
        String dest = getServer(request.getDestinationServer());
        LOG.debug("Connecting to " + dest + " on " + port);
        try {
            Socket destinationSocket = new Socket(dest, port);
            OutputStream destStream = destinationSocket.getOutputStream();
            InputStream backStream = destinationSocket.getInputStream();
            destStream.write(requestToDest.getBytes());
            destStream.flush();
            getResponse(backStream, responseStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getPort(int destinationPort, String destinationServer) {
        if (proxyPort == null || destinationServer.startsWith(HTTPRequest.SELENIUM_REDIRECT_SERVERNAME)) {
            return destinationPort;
        }
        return Integer.parseInt(proxyPort);
    }

    private String getServer(String destinationServer) {
        if (proxy == null || destinationServer.startsWith(HTTPRequest.SELENIUM_REDIRECT_SERVERNAME)) {
            return destinationServer;
        }
        return proxy;
    }

    private void getResponse(InputStream inputStream, ResponseStream clientOutputStream) throws IOException {
        int bytesRead = 0;
        byte[] response = new byte[2048];
        while (bytesRead > -1) {
            bytesRead = inputStream.read(response);
            if (bytesRead > -1) {
                LOG.debug("Waiting");
                clientOutputStream.write(response, bytesRead);
                LOG.debug("Number of bytes returned = " + bytesRead);
            }
        }
        clientOutputStream.flush();
    }

    private String redirectResponse(HTTPRequest request) {
        String response = request.getProtocol() + " 302 Moved Temporarily\r\nLocation: " +
                          HTTPRequest.SELENIUM_REDIRECT_URI  +
                          request.getUri().substring(7) + "\r\n";
        LOG.debug("Redirected Response\n" + response);
        return response;
    }

}
