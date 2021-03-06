/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.
Portions copyright 2007 ThoughtWorks, Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

const charset = "UTF-8";
const CI = Components.interfaces;

function StaleElementError() {};

function SocketListener(server, transport)
{
    this.outstream = transport.openOutputStream(Components.interfaces.nsITransport.OPEN_BLOCKING, 0, 0);

    this.stream = transport.openInputStream(0, 0, 0);
    var cin = Components.classes["@mozilla.org/intl/converter-input-stream;1"].createInstance(CI.nsIConverterInputStream);
    cin.init(this.stream, charset, 0, 0x0000);

    this.inputStream = cin;

    var pump = Components.classes["@mozilla.org/network/input-stream-pump;1"].createInstance(CI.nsIInputStreamPump);
    pump.init(this.stream, -1, -1, 0, 0, false);
    pump.asyncRead(this, null);

    this.linesLeft = "";
    this.data = "";
    this.command = "";
    this.step = 0;
    this.readLength = false;
    this.wm = Utils.getService("@mozilla.org/appshell/window-mediator;1", "nsIWindowMediator");
    this.server = server;
}

SocketListener.prototype.onStartRequest = function(request, context)
{
};

SocketListener.prototype.onStopRequest = function(request, context, status)
{
};

SocketListener.prototype.onDataAvailable = function(request, context, inputStream, offset, count)
{
    var incoming = {};
    var read = this.inputStream.readString(count, incoming);

    var lines = incoming.value.split('\n');
    for (var j = 0; j < lines.length; j++) {
        if (this.isReadingHeaders()) {
            var head = lines[j].split(": ", 2);
            if (head[0] == "Content-Length") {
                this.linesLeft = head[1] - 0;
                this.readLength = true;
            } else if (lines[j].length == 0 && this.readLength) {
                this.step++;
            }
        } else {
            this.data += lines[j];
            this.linesLeft -= read;

            if (this.linesLeft <= 0) {
                this.executeCommand();
                j++;  // Consume the empty line
            }
        }
    }

    if (this.linesLeft <= 0 && this.data) {
        this.executeCommand();
    }
};

SocketListener.prototype.executeCommand = function() {
    var fxbrowser;
    var self = this;

    try {
      var command = JSON.parse(this.data);
    } catch (e) {
      Utils.dump(e);
      Utils.dump(this.data);

      // Something has gone seriously wrong. Quit the browser
      this.quit({});      
    }

    var sendBack = {
        commandName : command ? command.commandName : "Unknown command",
        isError : false,
        response : "",
        elementId : command.elementId
    };

    var statusBarLabel = null;

    var respond = {
        send : function() {
            // Indicate, that we are no longer executing a command ...
            if (statusBarLabel) {
                statusBarLabel.style.color = "black";
            }

            sendBack.context = "" + sendBack.context;
            var remainder = JSON.stringify(sendBack);

            var converter = Utils.newInstance("@mozilla.org/intl/scriptableunicodeconverter", "nsIScriptableUnicodeConverter");
            converter.charset = charset;

            var data = converter.convertToByteArray(remainder, {});
            var header = "Length: " + data.length + "\n\n";
            self.outstream.write(header, header.length);
            self.outstream.flush();

            var stream = converter.convertToInputStream(remainder);
            self.outstream.writeFrom(stream, data.length);
            self.outstream.flush();
            stream.close();
        },

        setField : function(name, value) { sendBack[name] = value; },

        set commandName(name) { sendBack.commandName = name; },
        get commandName()     { return sendBack.commandName; },
        set elementId(id)     { sendBack.elementId = id; },
        get elementId()       { return sendBack.elementId; },
        set isError(error)    { sendBack.isError = error; },
        get isError()         { return sendBack.isError; },
        set response(res)     { sendBack.response = res; },
        get response()        { return sendBack.response; },
        set context(c)        { sendBack.context = c; },
        get context()         { return sendBack.context; }
    };

    respond.context = command.context;

    if (!this.driverPrototype)
        this.driverPrototype = FirefoxDriver.prototype;

    // These are used to locate a new driver, and so not having one is a fine thing to do
    if (command.commandName == "findActiveDriver" ||
        command.commandName == "switchToWindow" ||
        command.commandName == "getAllWindowHandles" ||
        command.commandName == "quit") {

        this.data = "";
        this.linesLeft = 0;                                                                                   
        this.step = 0;
        this.readLength = false;

      try {
        respond.commandName = command.commandName;
        this[command.commandName](respond, command.parameters);
      } catch (e) {
        var obj = {
          fileName : e.fileName,
          lineNumber : e.lineNumber,
          message : e.message,
          name : e.name,
          stack : e.stack
        };
        var message = "Exception caught by driver: " + info.command.commandName
            + "(" + info.command.parameters + ")\n" + e;
        Utils.dumpn(message);
        Utils.dump(e);
        respond.isError = true;
        respond.context = info.driver.context;
        respond.response = obj;
        respond.send();
      }
    } else if (this.driverPrototype[command.commandName]) {
        var driver;

        var res = command.context.split(" ", 2);
        var context = new Context(res[0], res[1]);

        command.context = command.context.toString();

        var allWindows = this.wm.getEnumerator(null);
        var win;
        while (allWindows.hasMoreElements()) {
            win = allWindows.getNext();
            if (win["fxdriver"] && win.fxdriver.id == context.windowId) {
                fxbrowser = win.getBrowser();
                driver = win.fxdriver;
                break;
            }
        }

        if (!fxbrowser) {
            Utils.dumpn("Unable to find browser with id " + context.windowId + "\n");
            respond.isError = true;
            respond.response = "Unable to find browser with id " + context.windowId;
            respond.send();
            return;
        }
        if (!driver) {
            Utils.dumpn("Unable to find the driver\n");
            respond.isError = true;
            respond.response = "Unable to find the driver";
            respond.send();
            return;
        }

        driver.context = context;
        driver.context.fxbrowser = fxbrowser;

        // Determine whether or not we need to care about frames.
        var frames = fxbrowser.contentWindow.frames;
        if ("?" == driver.context.frameId) {
            if (frames && frames.length) {
                if ("FRAME" == frames[0].frameElement.tagName) {
                    driver.context.frameId = 0;
                } else {
                    driver.context.frameId = undefined;
                }
            } else {
                driver.context.frameId = undefined;
            }
        }
        if (driver.context.frameId !== undefined) {
            driver.context.frame = Utils.findFrame(fxbrowser, driver.context.frameId);
        }

        // Indicate, that we are about to execute a command ...
        statusBarLabel = win.document.getElementById("fxdriver-label");
        if (statusBarLabel) {
            statusBarLabel.style.color = "red";
        }

        var activeWindow = driver.context.frame || fxbrowser.contentWindow;
        var webNav = activeWindow.QueryInterface(CI.nsIInterfaceRequestor).getInterface(CI.nsIWebNavigation);
        var loadGroup = webNav.QueryInterface(CI.nsIInterfaceRequestor).getInterface(CI.nsILoadGroup);

        var info = {
            webProgress: loadGroup,
            command: command,
            driver: driver,
            onBlank: false
        };

        this.data = "";
        this.linesLeft = 0;
        this.step = 0;
        this.readLength = 0;

        var wait = function(info) {
            if (info.webProgress.isPending()) {
                info.driver.window.setTimeout(wait, 100, info);
            } else {
                // Ugh! New windows open on "about:blank" before going to their destination
                // URL. This check attempts to tell the difference between a newly opened
                // window and someone actually wanting to do something on about:blank.
                if (info.driver.window.location == "about:blank" && !info.onBlank) {
                  info.onBlank = true;
                  info.driver.window.setTimeout(wait, 100, info);
                } else {
                  try {
                      respond.commandName = info.command.commandName;
                      info.driver[info.command.commandName](respond, info.command.parameters);
                  } catch (e) {
                      if (e instanceof StaleElementError) {
                        respond.isError = true;
                        respond.context  = info.driver.context;
                        respond.response = "element is obsolete";
                        respond.send();
                      } else {
                        var obj = {
                          fileName : e.fileName,
                          lineNumber : e.lineNumber,
                          message : e.message,
                          name : e.name,
                          stack : e.stack
                        };
                        var message = "Exception caught by driver: " + info.command.commandName
                            + "(" + info.command.parameters + ")\n" + e;
                        Utils.dumpn(message);
                        Utils.dump(e);
                        respond.isError = true;
                        respond.context = info.driver.context;
                        respond.response = obj;
                        respond.send();
                      }
                  }
                }
            }
        }
        driver.window.setTimeout(wait, 0, info);
    } else {
        Utils.dumpn("Unrecognised command: " + this.command + "\n");
        this.linesLeft = 0;
        this.step = 0;
        this.readLength = false;
        respond.isError = true;
        respond.response = "Unrecognised command: " + command.commandName;
        // Context was already set.
        respond.send();
    }
};

SocketListener.prototype.isReadingHeaders = function() {
    return this.step == 0;
};

SocketListener.prototype.isReadingLineCount = function() {
    return this.step == 1;
};

SocketListener.prototype.switchToWindow = function(respond, windowId) {
    var lookFor = windowId[0];
    var wm = Utils.getService("@mozilla.org/appshell/window-mediator;1", "nsIWindowMediator");
    var allWindows = wm.getEnumerator(null);

    var matches = function(win, lookFor) {
      if (win.closed)
        return false;

      if (win.content && win.content.name == lookFor)
        return true;

      if (win.top && win.top.fxdriver && win.top.fxdriver.id == lookFor)
        return true;

      return false;
    }

    Utils.dumpn("Looking for: " + windowId);

    while (allWindows.hasMoreElements()) {
        var win = allWindows.getNext();
        if (matches(win, lookFor)) {
            win.focus();
            var driver = win.top.fxdriver;
            if (!driver) {
                respond.isError = true;
                respond.response = "No driver found attached to top window!";
                respond.send();
            }

            respond.response = new Context(win.fxdriver.id).toString();
            respond.send();
            return;
        }
    }

    respond.context = this.context;
    respond.isError = true;
    respond.response = "No window found";
    respond.send();
};

SocketListener.prototype.getAllWindowHandles = function(respond) {
  var wm = Utils.getService("@mozilla.org/appshell/window-mediator;1", "nsIWindowMediator");
  var allWindows = wm.getEnumerator("navigator:browser");

  var res = "";
  var index = -1;
  while (allWindows.hasMoreElements()) {
    var win = allWindows.getNext();

    if (win.top.fxdriver && !win.top.closed) {
      res += win.top.fxdriver.id + ","
    }
  }

  respond.response = res;
  respond.send();
}

SocketListener.prototype.findActiveDriver = function(respond) {
    var win = this.wm.getMostRecentWindow("navigator:browser");
    var driver = win.fxdriver;

    if (!driver) {
        respond.isError = true;
        respond.response = "No drivers associated with the window\n";
    }

    respond.context = this.context;
    respond.response = driver.id;

    respond.send();
};

SocketListener.prototype.quit = function(respond) {
  var appService = Utils.getService("@mozilla.org/toolkit/app-startup;1", "nsIAppStartup");
  appService.quit(CI.nsIAppStartup.eForceQuit);
};
