/*
 * Copyright 2006 ThoughtWorks, Inc.
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

package org.openqa.selenium.server;

import java.io.*;
import java.net.*;

/**
 * The default implementation of the SeleneseCommand interface
 * @author Paul Hammant
 * @version $Revision: 184 $
 */
public class DefaultSeleneseCommand implements SeleneseCommand {
    // as we have beginning and ending pipes, we will have 1 more entry than we need
    private static final int NUMARGSINCLUDINGBOUNDARIES = 4;
    private static final int FIRSTINDEX = 1;
    private static final int SECONDINDEX = 2;
    private static final int THIRDINDEX = 3;
    private final String command;
    private final String field;
    private final String value;
    private final String piggybackedJavaScript;

    public DefaultSeleneseCommand(String command, String field, String value) {
        this.command = command;
        this.field = field;
        this.value = value;
        this.piggybackedJavaScript = null;
    }

    public DefaultSeleneseCommand(String command, String field, String value, String piggybackedJavaScript) {
        this.command = command;
        this.field = field;
        this.value = value;
        if (piggybackedJavaScript!=null && !"".equals(piggybackedJavaScript)) {
            this.piggybackedJavaScript = piggybackedJavaScript;
        }
        else {
            this.piggybackedJavaScript = null;
        }
    }

    public String getCommandURLString() {
        return "cmd=" + urlEncode(command) + "&1=" + urlEncode(field) + "&2=" + urlEncode(value);
    }

    public String getCommand() {
        return command;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        if (piggybackedJavaScript==null) {
            return getCommandURLString();
        }
        return getCommandURLString() + "\n" + getPiggybackedJavaScript();
    }

    /** Factory method to create a SeleneseCommand from a wiki-style input string */
    public static SeleneseCommand parse(String inputLine) {
        if (inputLine == null) throw new IllegalArgumentException("inputLine must not be null");

        String[] values = inputLine.split("\\|");
        if (values.length != NUMARGSINCLUDINGBOUNDARIES) {
            throw new IllegalStateException("Cannot parse invalid line: " + inputLine + values.length);
        }
        return new DefaultSeleneseCommand(values[FIRSTINDEX], values[SECONDINDEX], values[THIRDINDEX]);
    }
    
    /** Encodes the text as an URL using UTF-8.
     * 
     * @param text the text too encode
     * @return the encoded URI string
     * @see URLEncoder#encode(java.lang.String, java.lang.String)
     */
    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPiggybackedJavaScript() {
        return piggybackedJavaScript;
    }
}
