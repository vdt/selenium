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

package com.thoughtworks.selenium.outbedded;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.DefaultSeleneseCommand;
import com.thoughtworks.selenium.SeleniumException;

/**
 * Sends and retrieves results of commands sent over HTTP to CommandBridge servlet.
 * @see com.thoughtworks.selenium.outbedded.CommandBridge
 * @author Ben Griffiths, Jez Humble
 */
public class CommandBridgeClient implements CommandProcessor {

    private String pathToServlet;
    private String sessionId;

    /** Specifies the URL to the CommandBridge servlet */
    public CommandBridgeClient(String pathToServlet) {
        this.pathToServlet = pathToServlet;
    }

    public String doCommand(String commandName, String field, String value) {
        DefaultSeleneseCommand command = new DefaultSeleneseCommand(commandName,field,value);
        return executeCommandOnServlet(command.getCommandString());
    }

    /** Does nothing */
    public void start() {
    }

    /** Does nothing */
    public void stop() {
    }

    /** Sends the specified command string to the bridge servlet */  
    public String executeCommandOnServlet(String command) {
        InputStream is = null;
        try {
            is = getCommandResponse(command, is);
            return stringContentsOfInputStream(is);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Catch body broken: IOException from " + command + " -> " + e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String stringContentsOfInputStream(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = is.read()) != -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    private InputStream getCommandResponse(String command, InputStream is) throws IOException {
        int responsecode = 301;
        while (responsecode == 301) {
            URL result = buildCommandURL(command, pathToServlet);
            HttpURLConnection uc = (HttpURLConnection) result.openConnection();
            uc.setInstanceFollowRedirects(false);
            responsecode = uc.getResponseCode();
            if (responsecode == 301) {
                pathToServlet = uc.getRequestProperty("Location");
            } else if (responsecode != 200) {
                throw new SeleniumException(uc.getResponseMessage());
            } else {
                is = uc.getInputStream();
            }
        }
        return is;
    }

    private URL buildCommandURL(String command, String servletUrl) throws MalformedURLException {
        StringBuffer sb = new StringBuffer();
        sb.append(servletUrl);
        sb.append("?");
        sb.append("commandRequest=");
        sb.append(URLEncoder.encode(command));
        if (sessionId != null) {
            sb.append("&sessionId=");
            sb.append(URLEncoder.encode(sessionId));
        }
        URL result = new URL(sb.toString());
        return result;
    }
    
    /** Specifies the session ID that this bridge client will use to communicate
     * with the Selenium Server.  Multiple session IDs may be used to communicate
     * with multiple Selenese command queues
     * @param id - the session ID to use
     */
    public void setSessionId(long id) {
        sessionId = Long.toString(id);
    }
}
