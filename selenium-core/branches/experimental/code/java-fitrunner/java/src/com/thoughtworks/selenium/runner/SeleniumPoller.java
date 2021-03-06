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
package com.thoughtworks.selenium.runner;

import java.io.File;
import java.io.IOException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author Darren Cotterill
 * @author Ajit George
 */
public class SeleniumPoller {

    private final int _maxPollAttempts;
    private final String _applicationURL;
    private PollResult _lastPollResult;
    private final File _outputFile;

    public SeleniumPoller(String applicationURL, int maxPollAttempts, File outputFile) {
        _applicationURL = applicationURL;
        _maxPollAttempts = maxPollAttempts;
        _outputFile = outputFile;
    }

    public boolean poll() {
        pollForResults();
        return _lastPollResult.isTestsPassed();
    }
    
    private void pollForResults() {
        new UrlPoller(_applicationURL, _maxPollAttempts).poll(new ResponseEvaluator() {
            public boolean evaluate(GetMethod get) {
                if (get.getStatusCode() != HttpStatus.SC_OK) {
                    return false;
                }
                
                try {
                    _lastPollResult = new PollResult(get.getResponseBodyAsString());
                } catch (IOException e) {
                    System.err.println(e);
                }
                
                return _lastPollResult.isTestResultPosted();
            }
        });
    }
    
    private class PollResult {
        private final String _result;
        
        public PollResult(String result) {
            _result = result;
            if (isTestResultPosted()) {
                new ResultWriter(_outputFile).write(_result);
            }
        }
        
        public boolean isTestResultPosted() {
            return _result.indexOf("<body>") != -1;
        }
        
        public boolean isTestsPassed() {
            return _result.indexOf("passed") != -1;
        }
    }
}
