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

package com.thoughtworks.selenium.servlet;

import com.thoughtworks.selenium.SeleneseHandler;
import com.thoughtworks.selenium.SeleneseCommand;

import javax.servlet.ServletContext;
import java.io.Writer;

/**
 * @author Paul Hammant
 * @version $Revision$
 */
public class SeleneseProxyServlet extends AbstractSeleneseServlet {


    protected SeleneseHandler getRemoteSeleneseHandler(ServletContext servletContext, Writer writer) {
        SeleneseProxy seleneseProxy = new SeleneseProxy(host, port);
        servletContext.setAttribute("remote-selenese-handler", seleneseProxy);
        return seleneseProxy;
    }

    protected SeleneseCommand handleCommand(ServletContext servletContext, String commandReply) {
        SeleneseHandler seleneseProxy = (SeleneseHandler) servletContext.getAttribute("remote-selenese-handler");
        if (seleneseProxy == null) {
            throw new IllegalStateException("We expected the attribute 'remote-selenese-handler' to exist");
        }
        SeleneseCommand command = seleneseProxy.handleCommandResult(commandReply);
        return command;
    }

    protected void endTests() {
        //TODO
    }

}
