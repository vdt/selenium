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

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.thoughtworks.selenium.SeleneseCommand;

/**
 * An RMI object that can handle Selenese commands.
 * 
 * @author Paul Hammant
 * @version $Revision$
 */
public interface RemoteSeleneseHandler extends Remote {

    /** Retrieve the first Selenese command from the queue */
    SeleneseCommand handleStart() throws RemoteException;
    /** Handle the previous Selenese response and retrieve the next Selenese command from the queue */
    SeleneseCommand handleCommandResult(String commandResult) throws RemoteException;

}
