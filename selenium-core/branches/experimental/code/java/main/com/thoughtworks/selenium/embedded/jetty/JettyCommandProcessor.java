/*
 * Copyright 2004 ThoughtWorks, Inc.
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

package com.thoughtworks.selenium.embedded.jetty;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.RequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.MultiException;

import com.thoughtworks.selenium.CommandProcessor;

/**
 * Constructs an embedded Jetty server to drive Selenium; <i>end users should primarily interact with this object</i>.
 * @author Paul Hammant
 * @version $Revision$
 */
public class JettyCommandProcessor implements CommandProcessor {

    private Server server;
    private SeleneseJettyResourceHandler seleneseJettyResourceHandler;
    private static final int PORT = 8080;

    /** 
     * Specifies the webapp and context directory to expose, but exposes
     * no static content
     * @param webAppRoot the root webapp that will be served by Jetty
     * @param seleniumContext the name of the virtual (context) directory 
     */
    public JettyCommandProcessor(File webAppRoot, String seleniumContext) {
        this(webAppRoot, seleniumContext, new NullStaticContentHandler());
    }

    /** 
     * Specifies the webapp, context directory, and static content to be exposed
     * @param webAppRoot the root webapp that will be served by Jetty
     * @param seleniumContext the name of the virtual (context) directory 
     * @param staticContentHandler the static content handler used to expose images and JavaScript using Jetty
     * 
     * @see StaticContentHandler interface and objects that implement it 
     */
    public JettyCommandProcessor(File webAppRoot, String seleniumContext, StaticContentHandler staticContentHandler) {

        configureServer();
        seleneseJettyResourceHandler = new SeleneseJettyResourceHandler();

        ServletHttpContext context = (ServletHttpContext) server.getContext("localhost",
                                                                            "/" + seleniumContext + "/*");

        staticContentHandler.addStaticContent(context);
        context.addHandler(seleneseJettyResourceHandler);
        context.setContextPath("/" + seleniumContext + "/*");
        server.addContext("locahost", context);
        addServerApplication(webAppRoot);
    }

    private void addServerApplication(File webAppRoot) {
        if (webAppRoot != null) {
            try {
                server.addWebApplication("localhost", "/", webAppRoot.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Exception instantiating Jetty", e);
            }
        }
    }

    private void configureServer() {
        server = new Server();
        try {
            server.addListener(new InetAddrPort("localhost", PORT));
        } catch (IOException e) {
            throw new RuntimeException("Exception instantiating Jetty", e);
        }

        server.setRequestLog(new RequestLog() {
            public void log(HttpRequest httpRequest, HttpResponse httpResponse, int i) {
            }

            public void start() throws Exception {
            }

            public void stop() {
            }

            public boolean isStarted() {
                return false;
            }
        });
    }

    public String doCommand(String command, String field, String value) {
        return seleneseJettyResourceHandler.doCommand(command, field, value);
    }

    public void start() {
        try {
            server.start();
        } catch (MultiException e) {
            List excpts = e.getExceptions();
            for (int i = 0; i < excpts.size(); i++) {
                Exception exception = (Exception) excpts.get(i);
                System.out.println("[Jetty MultiException] " + i + " : " + exception);
            }

            throw new RuntimeException("Exception starting Jetty. Port blocked by another process?", e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException("Jetty Interrupted during stop", e);
        }
    }


}
