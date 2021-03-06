/*
 * Copyright 2005 Shinya Kasatani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function Debugger(editor) {
	this.log = new Log("Debugger");
	this.editor = editor;
	var self = this;
	
	this.init = function() {
		if (this.runner != null) {
			// already initialized
			return;
		}
		
		this.log.debug("init");
		this.paused = false;
		this.runner = new Object();
		this.runner.editor = this.editor;

		this.editor.testCaseListeners.push(function(testCase) { self.runner.testCase = testCase; });
		this.runner.testCase = this.editor.testCase;
		
		const subScriptLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
	    .getService(Components.interfaces.mozIJSSubScriptLoader);
		//subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium/selenium-logging.js', this.runner);

		subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium/scripts/selenium-api.js', this.runner);
		subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium/scripts/selenium-commandhandlers.js', this.runner);
		subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium/scripts/selenium-executionloop.js', this.runner);
		subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium/scripts/selenium-browserbot.js', this.runner);
		if (this.editor.options.userExtensionsURL) {
			try {
				ExtensionsLoader.loadSubScript(subScriptLoader, this.editor.options.userExtensionsURL, this.runner);
			} catch (error) {
				this.log.error("error loading user-extensions.js: " + error);
			}
		}
		subScriptLoader.loadSubScript('chrome://selenium-ide/content/selenium-runner.js', this.runner);

        this.editor.infoPanel.logView.setLog(this.runner.LOG);
        
		this.runner.getInterval = function() {
			if (self.runner.testCase.debugContext.currentCommand().breakpoint) {
				self.paused = true;
				return -1;
			} else if (self.paused) {
				return -1;
			} else {
				return document.getElementById("runInterval").selectedItem.value;
			}
		}
	}
}

Debugger.prototype.start = function() {
	document.getElementById("record-button").checked = false;
	this.editor.toggleRecordingEnabled(false);

	this.log.debug("start");

	this.init();
	this.paused = false;
	this.runner.start(this.editor.getBaseURL());
};

Debugger.prototype.executeCommand = function(command) {
	document.getElementById("record-button").checked = false;
	this.editor.toggleRecordingEnabled(false);

	this.init();
	this.runner.executeCommand(this.editor.getBaseURL(), command);
};

Debugger.prototype.pause = function() {
	this.log.debug("pause");
	this.paused = true;
}

Debugger.prototype.doContinue = function(pause) {
	document.getElementById("record-button").checked = false;
	this.editor.toggleRecordingEnabled(false);

	this.log.debug("doContinue: pause=" + pause);
	this.init();
	if (!pause) this.paused = false;
	if (this.runner.resume) {
		// Selenium 0.7
		this.runner.resume();
	} else {
		// Selenium 0.6
		this.runner.continueCurrentTest();
	}
};

Debugger.prototype.showElement = function(locator) {
	this.init();
	this.runner.showElement(locator);
}
