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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.StringUtil;
import org.openqa.selenium.server.browserlaunchers.AsyncExecute;
import org.openqa.selenium.server.browserlaunchers.BrowserLauncher;
import org.openqa.selenium.server.browserlaunchers.BrowserLauncherFactory;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;

/**
 * A Jetty handler that takes care of Selenese Driven requests.
 * 
 * Selenese Driven requests are described in detail in the class description for
 * <code>SeleniumProxy</code>
 * @see org.openqa.selenium.server.SeleniumServer
 * @author Paul Hammant
 * @version $Revision: 674 $
 */
public class SeleniumDriverResourceHandler extends ResourceHandler {
    private final Map<String, FrameGroupSeleneseQueueSet> queueSets = new HashMap<String, FrameGroupSeleneseQueueSet>();    
    private final Map<String, BrowserLauncher> launchers = new HashMap<String, BrowserLauncher>();
    private SeleniumServer server;
    private static String lastSessionId = null;
    private Map<String, String> domainsBySessionId = new HashMap<String, String>();
    private Map<String, String> unusedBrowserSessions = new HashMap<String, String>();
    private Map<String, String> sessionIdsToBrowserStrings = new HashMap<String, String>();
    private StringBuffer logMessagesBuffer = new StringBuffer();

    public SeleniumDriverResourceHandler(SeleniumServer server) {
        this.server = server;
    }

    /** Handy helper to retrieve the first parameter value matching the name
     * 
     * @param req - the Jetty HttpRequest
     * @param name - the HTTP parameter whose value we'll return
     * @return the value of the first HTTP parameter whose name matches <code>name</code>, or <code>null</code> if there is no such parameter
     */
    private String getParam(HttpRequest req, String name) {
        List parameterValues = req.getParameterValues(name);
        if (parameterValues == null) {
            return null;
        }
        return (String) parameterValues.get(0);
    }

	public void handle(String pathInContext, String pathParams, HttpRequest req, HttpResponse res) throws HttpException, IOException {
        try {
            res.setField(HttpFields.__ContentType, "text/plain");
            setNoCacheHeaders(res);

            String method = req.getMethod();
            String cmd = getParam(req, "cmd");
            String sessionId = getParam(req, "sessionId");
            String seleniumStart = getParam(req, "seleniumStart");
            boolean justLoaded = (seleniumStart != null && seleniumStart.equals("true"));

            // If this is a browser requesting work for the first time...
            if ("POST".equalsIgnoreCase(method) || justLoaded) {
                FrameAddress frameAddress = FrameGroupSeleneseQueueSet.findFrameAddress(getParam(req, "seleniumWindowName"), 
                        getParam(req, "localFrameAddress"), justLoaded);
                String uniqueId = getParam(req, "uniqueId");
                    String postedData = readPostedData(req, sessionId, uniqueId);
                if (postedData == null || postedData.equals("")) {
                    res.getOutputStream().write("\r\n\r\n".getBytes());
                    req.setHandled(true);
                    return;
                }
                logPostedData(frameAddress, justLoaded, sessionId, postedData, uniqueId);

                FrameGroupSeleneseQueueSet queueSet = getQueueSet(sessionId);
                if (justLoaded) {
                    postedData = "OK";	// assume a new page starting is okay
                    queueSet.markWhetherJustLoaded(frameAddress, true);
                }
                else {
                    queueSet.markWhetherJustLoaded(frameAddress, false);
                }

                SeleneseCommand sc = queueSet.handleCommandResult(postedData, frameAddress, uniqueId);
                if (sc != null) {
                    respond(res, sc);
                }
                req.setHandled(true);
            } else if (cmd != null) {
                SeleniumServer.log(method + ": " + cmd);
                handleCommandRequest(req, res, cmd, sessionId);
            } else if (-1 != req.getRequestURL().indexOf("selenium-server/core/scripts/user-extensions.js") 
                    || -1 != req.getRequestURL().indexOf("selenium-server/tests/html/tw.jpg")){
                // ignore failure to find these items...
            }
            else {
                SeleniumServer.log("Not handling: " + req.getRequestURL() + "?" + req.getQuery());
                req.setHandled(false);
            }
        }
        catch (RuntimeException e) {
            if (!SeleniumServer.isDebugMode()
                && looksLikeBrowserLaunchFailedBecauseFileNotFound(e)) {
                String apparentFile = extractNameOfFileThatCouldntBeFound(e);
                if (apparentFile!=null) {
                    System.err.println("\n\nCould not start browser; it appears that " + apparentFile + " is missing or inaccessible");
                }
            }
            throw e;
        }
    }

	private void logPostedData(FrameAddress frameAddress, boolean justLoaded, String sessionId, String postedData, String uniqueId) {
        StringBuffer sb = new StringBuffer();
        sb.append("Browser " + sessionId + "/" + frameAddress + " " + uniqueId + " posted " + postedData);
        if (!frameAddress.isDefault()) {
            sb.append(" from " + frameAddress);
        }
        if (justLoaded) {
            sb.append(" NEW");
        }
        SeleniumServer.log(sb.toString());
    }

	private void respond(HttpResponse res, SeleneseCommand sc) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1000);
        Writer writer = new OutputStreamWriter(buf, StringUtil.__UTF_8);
        if (sc!=null) {
            writer.write(sc.toString());
        }
        for (int pad = 998 - buf.size(); pad-- > 0;) {
            writer.write(" ");
        }
        writer.write("\015\012");
        writer.close();
        OutputStream out = res.getOutputStream();
        buf.writeTo(out);

    }

    /**
     * extract the posted data from an incoming request, stripping away a piggybacked data
     *
     * @param req
     * @param sessionId
     * @param uniqueId 
     * @return a string containing the posted data (with piggybacked log info stripped)
     * @throws IOException
     */
	private String readPostedData(HttpRequest req, String sessionId, String uniqueId) throws IOException {
        InputStream is = req.getInputStream();
        StringBuffer sb = new StringBuffer();
        InputStreamReader r = new InputStreamReader(is, "UTF-8");
        int c;
        while ((c = r.read()) != -1) {
            sb.append((char) c);
        }
        String s = sb.toString();
        s = extractLogMessages(s);
        s = extractJsState(sessionId, uniqueId, s);
        return s;
    }

    private String extractLogMessages(String s) {
        String logMessages = grepStringsStartingWith("logLevel=", s);
        if (logMessages==null) {
            return s;
        }
        logMessages = "\t" + logMessages.replaceAll("\n", "\n\t");  // put a tab in front of all the messages
        logMessages = logMessages.replaceFirst("\t$", "");
        logMessagesBuffer.append(logMessages);
        SeleniumServer.log(logMessages);
        return grepVStringsStartingWith("logLevel=", s);
    }

    private String grepVStringsStartingWith(String pattern, String s) {
        return s.replaceAll(pattern + ".*\n", "");
    }

    private String extractJsState(String sessionId, String uniqueId, String s) {
        String jsInitializers = grepStringsStartingWith("state:", s);
        if (jsInitializers==null) {
            return s;
        }
        for (String jsInitializer : jsInitializers.split("\n")) {
            String jsVarName = extractVarName(jsInitializer);
            InjectionHelper.saveJsStateInitializer(sessionId, uniqueId, jsVarName, jsInitializer);
        }
        return grepVStringsStartingWith("state:", s);
    }

    private String extractVarName(String jsInitializer) {
        int x = jsInitializer.indexOf('=');
        if (x==-1) {
            // apparently a method call, not an assignment
            // for 'browserBot.recordedAlerts.push("lskdjf")',
            // return 'browserBot.recordedAlerts':
            x = jsInitializer.lastIndexOf('(');
            if (x==-1) {
                throw new RuntimeException("expected method call, saw " + jsInitializer);
            }
            x = jsInitializer.lastIndexOf('.', x-1);
            if (x==-1) {
                throw new RuntimeException("expected method call, saw " + jsInitializer);
            }
        }
        return jsInitializer.substring(0, x);
    }

    private String grepStringsStartingWith(String pattern, String s) {
        String[] lines = s.split("\n");
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            if (line.startsWith(pattern)) {
                sb.append(line.substring(pattern.length()))
                .append('\n');
            }
        }
        if (sb.length()==0) {
            return null;
        }
        return sb.toString();
    }

    /** Try to extract the name of the file whose absence caused the exception
     * 
     * @param e - the exception
     * @return the name of the file whose absence caused the exception
     */
    private String extractNameOfFileThatCouldntBeFound(Exception e) {
        String s = e.getMessage();
        if (s==null) {
            return null;
        }
        // will only succeed on Windows -- perhaps I will make it work on other platforms later
        return s.replaceFirst(".*CreateProcess: ", "").replaceFirst(" .*", "");
    }

    private boolean looksLikeBrowserLaunchFailedBecauseFileNotFound(Exception e) {
        String s = e.getMessage();
        // will only succeed on Windows -- perhaps I will make it work on other platforms later
        return (s!=null) && s.matches("java.io.IOException: CreateProcess: .*error=3");
    }

	private void handleCommandRequest(HttpRequest req, HttpResponse res, String cmd, String sessionId) {
        // If this a Driver Client sending a new command...
        res.setContentType("text/plain");
        hackRemoveConnectionCloseHeader(res);

        Vector<String> values = new Vector<String>();

        for (int i = 1; req.getParameter(Integer.toString(i)) != null; i++) {
            values.add(req.getParameter(Integer.toString(i)));
        }
        if (values.size() < 1) {
            values.add("");
        }
        if (values.size() < 2) {
            values.add("");
        }

        String results;
        if (SeleniumServer.isDebugMode()) {
            SeleniumServer.log("queryString = " + req.getQuery());
        }
        results = doCommand(cmd, values, sessionId, res);

        // under some conditions, the results variable will be null
        // (cf http://forums.openqa.org/thread.jspa?threadID=2955&messageID=8085#8085 for an example of this)
        if (results!=null) {
            try {
                res.getOutputStream().write(results.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        req.setHandled(true);
    }

	public String doCommand(String cmd, Vector<String> values, String sessionId, HttpResponse res) {
        String results;
        // handle special commands
        if ("getNewBrowserSession".equals(cmd)) {
            String browserString = values.get(0);
            sessionId = getNewBrowserSession(browserString, values.get(1));
            setDomain(sessionId, values.get(1));
            results = "OK," + sessionId;
        } else if ("getLogMessages".equals(cmd)) {
            results = "OK," + logMessagesBuffer.toString();
            logMessagesBuffer.setLength(0);
        } else if ("testComplete".equals(cmd)) {
            results = endBrowserSession(sessionId, SeleniumServer.reusingBrowserSessions());
        } else if ("shutDown".equals(cmd)) {
            results = null;
            shutDown(res);
        } else if ("isPostSupported".equals(cmd)) {
            // We don't support POST
            results = "OK,false";
        } else if ("setSpeed".equals(cmd)) {
            try {
                SeleneseQueue.setSpeed(Integer.parseInt(values.get(0)));
            }
            catch (NumberFormatException e) {
                return "ERROR: setSlowMode expects a string containing an integer, but saw '" + values.get(0) + "'";
            }
            results = "OK";
        } else if ("getSpeed".equals(cmd)) {
            results = "OK," + SeleneseQueue.getSpeed();
        } else if ("addStaticContent".equals(cmd)) {
            File dir = new File( values.get(0));
            if (dir.exists()) {
                server.addNewStaticContent(dir);
                results = "OK";
            } else {
                results = "ERROR: dir does not exist - " + dir.getAbsolutePath();
            }
        } else if ("runHTMLSuite".equals(cmd)) {
            HTMLLauncher launcher = new HTMLLauncher(server);
            File output = null;
            if (values.size() < 4) {
                results = "ERROR: Not enough arguments (browser, browserURL, suiteURL, multiWindow, [outputFile])";
            } else {
                if (values.size() > 4) {
                    output = new File(values.get(4));
                }
                
                try {
                    results = launcher.runHTMLSuite( values.get(0),  values.get(1),  values.get(2), output, SeleniumServer.getTimeoutInSeconds(), "true".equals(values.get(3)));
                } catch (IOException e) {
                    e.printStackTrace();
                    results = e.toString();
                }
            }
        } else {
            if ("open".equals(cmd)) {
                warnIfApparentDomainChange(sessionId, values.get(0));
            }
            FrameGroupSeleneseQueueSet queue = getQueueSet(sessionId);
            results = queue.doCommand(cmd, values.get(0), values.get(1));
        }
        SeleniumServer.log("Got result: " + results + " on session " + sessionId);
        return results;
    }

    private void shutDown(HttpResponse res) {
        SeleniumServer.log("Shutdown command received");
        
        for (String sessionId : unusedBrowserSessions.values()) {
            endBrowserSession(sessionId, false);
        }
        
        if (res != null) {
            try {
                res.getOutputStream().write("OK".getBytes());
                res.commit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        AsyncExecute.sleepTight(3000);
        System.exit(0);
    }

    private String endBrowserSession(String sessionId, boolean cacheUnused) {
        if (cacheUnused) {
            addUnusedBrowserSession(sessionId);
        }
        else {
            BrowserLauncher launcher = getLauncher(sessionId);
            if (launcher == null) {
                return "ERROR: No launcher found for sessionId " + sessionId;
            } 
            launcher.close();
            clearQueueSet(sessionId);
        }
        return "OK";
    }

    private void warnIfApparentDomainChange(String sessionId, String url) {
        if (url.startsWith("http://")) {
            String urlDomain = url.replaceFirst("^(http://[^/]+, url)/.*", "$1");
            String domain = getDomain(sessionId);
            if (domain==null) {
                setDomain(sessionId, urlDomain);
            }
            else if (!url.startsWith(domain)) {
                System.err.println("warning: you appear to be changing domains from " + domain + " to " + urlDomain + "\n"
                                   + "this may lead to a 'Permission denied' from the browser (unless it is running as *iehta or *chrome,\n"
                                   + "or alternatively the selenium server is running in proxy injection mode)");
            }
        }
    }

    private String getNewBrowserSession(String browserString, String startURL) {
        if (SeleniumServer.getDefaultBrowser()!=null) {
            browserString = SeleniumServer.getDefaultBrowser(); 
        }
        if (SeleniumServer.isProxyInjectionMode() && browserString.equals("*iexplore")) {
            SeleniumServer.log("WARNING: running in proxy injection mode, but you used a *iexplore browser string; this is " +
                    "almost surely inappropriate, so I'm changing it to *piiexplore...");
            browserString = "*piiexplore";
        }
        else if (SeleniumServer.isProxyInjectionMode() && browserString.equals("*firefox")) {
            SeleniumServer.log("WARNING: running in proxy injection mode, but you used a *firefox browser string; this is " +
                    "almost surely inappropriate, so I'm changing it to *pifirefox...");
            browserString = "*pifirefox";
        }
        if (SeleniumServer.isProxyInjectionMode()) {
            InjectionHelper.init();
        }
        if (browserString == null) {
            throw new IllegalArgumentException("browser string may not be null");
        }
        String sessionId = getUnusedBrowserSession(browserString);
        SeleneseQueue queue;
        if (sessionId != null) {
            setLastSessionId(sessionId); 
            queue = getQueueSet(sessionId).getSeleneseQueue();
        }
        else {
            sessionId = Long.toString(System.currentTimeMillis() % 1000000);
            setLastSessionId(sessionId); 
            BrowserLauncherFactory blf = new BrowserLauncherFactory(server);
            queue = getQueueSet(sessionId).getSeleneseQueue();
            BrowserLauncher launcher = blf.getBrowserLauncher(browserString, sessionId, queue);
            launchers.put(sessionId, launcher);
            sessionIdsToBrowserStrings.put(sessionId, browserString);
            
            queue.setResultExpected(true); // keep initial load result for call to discardCommandResult below
            boolean multiWindow = server.isMultiWindow();
            launcher.launchRemoteSession(startURL, multiWindow);
            queue.discardCommandResult();
        }
        SeleniumServer.log("Allocated session " + sessionId + " for " + startURL);
        queue.doCommand("setContext", sessionId, "");
        return sessionId;
    }

    private String getUnusedBrowserSession(String browserString) {
        return unusedBrowserSessions.remove(browserString);
    }

    private void addUnusedBrowserSession(String sessionId) {
        getQueueSet(sessionId).reset();
        unusedBrowserSessions.put(sessionIdsToBrowserStrings.get(sessionId), sessionId);
    }

    /** Perl and Ruby hang forever when they see "Connection: close" in the HTTP headers.
     * They see that and they think that Jetty will close the socket connection, but
     * Jetty doesn't appear to do that reliably when we're creating a process while
     * handling the HTTP response!  So, removing the "Connection: close" header so that
     * Perl and Ruby think we're morons and hang up on us in disgust.
     * @param res the HTTP response
     */
    private void hackRemoveConnectionCloseHeader(HttpResponse res) {
        // First, if Connection has been added, remove it.
        res.removeField(HttpFields.__Connection);
        // Now, claim that this connection is *actually* persistent
        Field[] fields = HttpConnection.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals("_close")) {
                Field _close = fields[i];
                _close.setAccessible(true);
                try {
                    _close.setBoolean(res.getHttpConnection(), false);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (fields[i].getName().equals("_persistent")) {
                Field _close = fields[i];
                _close.setAccessible(true);
                try {
                    _close.setBoolean(res.getHttpConnection(), true);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Retrieves a launcher for the specified sessionId, or <code>null</code> if there is no such launcher. */
    private BrowserLauncher getLauncher(String sessionId) {
        synchronized (launchers) {
            return launchers.get(sessionId);
        }
    }

    /** Retrieves a FrameGroupSeleneseQueueSet for the specifed sessionId, creating a new one if there isn't one with that sessionId already 
     */
    private FrameGroupSeleneseQueueSet getQueueSet(String sessionId) {
        synchronized (queueSets) {
            FrameGroupSeleneseQueueSet queueSet = queueSets.get(sessionId);
            if (queueSet == null) {
                queueSet = new FrameGroupSeleneseQueueSet(sessionId);
                queueSets.put(sessionId, queueSet);
            }
            return queueSet;
        }
    }

    /** Deletes the specified FrameGroupSeleneseQueueSet */
    public void clearQueueSet(String sessionId) {
        synchronized(queueSets) {
            FrameGroupSeleneseQueueSet queue = queueSets.get(sessionId);
            queue.endOfLife();
            queueSets.remove(sessionId);
        }
    }

    public void registerSeleneseLauncher(String sessionId, BrowserLauncher launcher) {
    	launchers.put(sessionId, launcher);
    }
    
    /** Kills all running browsers */
    public void stopAllBrowsers() {
        for (Iterator i = launchers.values().iterator(); i.hasNext();) {
            BrowserLauncher launcher = (BrowserLauncher) i.next();
            launcher.close();
        }
    }

    /** Sets all the don't-cache headers on the HttpResponse */
    private void setNoCacheHeaders(HttpResponse res) {
        res.setField(HttpFields.__CacheControl, "no-cache");
        res.setField(HttpFields.__Pragma, "no-cache");
        res.setField(HttpFields.__Expires, "-1");
    }

    private void setDomain(String sessionId, String domain) {
        domainsBySessionId.put(sessionId, domain);
    }

    private String getDomain(String sessionId) {
        return domainsBySessionId.get(sessionId);
    }

    public static String getLastSessionId() {
        return lastSessionId;
    }

    private static synchronized void setLastSessionId(String sessionId) {
        SeleniumDriverResourceHandler.lastSessionId = sessionId;
    }
}
