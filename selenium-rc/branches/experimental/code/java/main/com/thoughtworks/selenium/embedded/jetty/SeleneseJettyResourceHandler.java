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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.StringUtil;

import com.thoughtworks.selenium.SeleneseCommand;
import com.thoughtworks.selenium.SeleneseQueue;

/**
 * The Jetty class that actually handles Selenese GET requests.
 * @author Paul Hammant
 * @version $Revision$
 */
public class SeleneseJettyResourceHandler extends ResourceHandler {

    private SeleneseQueue seleneseQueue = new SeleneseQueue();

    /** Provides implementation for <code>JettyCommandProcessor.doCommand</code>
     * 
     * @see JettyCommandProcessor#doCommand(String, String, String)
     */
    public String doCommand(String command, String field, String value) {
        return seleneseQueue.doCommand(command, field, value);
    }

    private String getParam(HttpRequest req, String name) {
        List parameterValues = req.getParameterValues(name);
        if (parameterValues == null) {
            return null;
        }
        return (String) parameterValues.get(0);
    }

    /** Handles the Jetty request, by accepting the previous command's result (if any)
     * and sending along the next command to run
     */
    public void handle(String s, String s1, HttpRequest req, HttpResponse res) throws HttpException, IOException {
        res.setField(HttpFields.__ContentType, "text/plain");
        setNoCacheHeaders(res); 
        
        OutputStream out = res.getOutputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1000);
        Writer writer = new OutputStreamWriter(buf, StringUtil.__ISO_8859_1);
        String seleniumStart = getParam(req, "seleniumStart");
        String commandResult = getParam(req, "commandResult");
        if (commandResult != null || (seleniumStart != null && seleniumStart.equals("true")) ) {
            SeleneseCommand sc = seleneseQueue.handleCommandResult(commandResult);
            writer.flush();
            writer.write(sc.getCommandString());
            for (int pad = 998 - buf.size(); pad-- > 0;) {
                writer.write(" ");
            }
            writer.write("\015\012");
            writer.flush();
            buf.writeTo(out);
            req.setHandled(true);
        } else {
            req.setHandled(false);
        }
    }

    private void setNoCacheHeaders(HttpResponse res) {
        res.setField(HttpFields.__CacheControl, "no-cache");
        res.setField(HttpFields.__Pragma, "no-cache");
        res.setField(HttpFields.__Expires, "-1");
    }
}
