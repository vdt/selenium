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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.deployable.DeployableFactory;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.tomcat.Tomcat5xContainer;

import com.thoughtworks.selenium.CommandProcessor;

/**
 * <p>Starts a Tomcat server in external JVM by Cargo, which creates a fully separate
 * server.xml instance of Tomcat in the temp directory.</p> 
 * 
 * <p>Note that therefore OutbeddedTomcat does <i>not</i> deploy the specified webapp
 * into the <i>existing</i> Tomcat installation, alongside the current default
 * webapps.  Instead, it starts up its own private version of Tomcat containing
 * only the specific webapps you have deployed.</p>
 * 
 * @author Ben Griffiths
 */
public class OutbeddedTomcat extends ServletContainer {
    private Container container;

    /** Constructs a new Tomcat instance using the jars (but not the settings)
     * found in the specified tomcatHome
     * @param tomcatHome the absolute path of the CATALINA_HOME of Tomcat.  Jars should be in the 
     * ./server/lib directory relative to this path.
     */
    public OutbeddedTomcat(String tomcatHome) {
        container = new Tomcat5xContainer();
        container.setHomeDir(new File(tomcatHome));
        container.setExtraClasspath(calculateCurrentClasspathWithoutJDKClasses());
    }

    private String[] calculateCurrentClasspathWithoutJDKClasses() {
        String classpath =  System.getProperty("java.class.path");
        List l = new ArrayList();
        for (StringTokenizer stringTokenizer = new StringTokenizer(classpath,File.pathSeparator); stringTokenizer.hasMoreTokens();) {
            String s = stringTokenizer.nextToken();
            if (s.indexOf(File.separator+"jre"+File.separator)!=-1) continue;
            if (s.indexOf("xerces")!=-1) continue;
            l.add(s);
        }
        String[] classpathpaths = new String[l.size()];
        classpathpaths = (String[]) l.toArray(classpathpaths);
        return classpathpaths;
    }

    public void installWebApp(File webAppRoot, String contextpath) {
        DeployableFactory fac = container.getDeployableFactory();
        WAR war = fac.createWAR(webAppRoot.getAbsolutePath());
        war.setContext(contextpath);
        container.addDeployable(war);
    }

    public CommandProcessor start() {
        container.start();
        return new CommandBridgeClient(super.buildDriverURL());
    }

    public void stop() {
        container.stop();
    }
}
