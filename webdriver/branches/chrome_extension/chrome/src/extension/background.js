//TODO(danielwh): A nice consistent naming convention
//TODO(danielwh): Factor out error handling (and general responses) to avoid so much duplication

ports = [];

active_port = null;
primary_display_tab_id = null;
session_id_ = null;
context_ = null;
capabilities_ = null;
path_offset_ = 1; //TODO(danielwh): Grab this from the initial URL
has_window_handle = false;
is_loading_page = false;
minimum_element_on_page = -1;
maximum_elemet_on_page = -2;
blocked = false;

chrome.self.onConnect.addListener(function(port) {
  console.log("Connected");
  PushPort(port);
  if (!active_port || port.tab.id == active_port.tab.id) {
    active_port = port; //TODO(danielwh): When SHOULD this actually be true??
  }
  port.onMessage.addListener(parse_port_message);
  port.onDisconnect.addListener(remove_port);
});

function parse_port_message(message) {
  console.log("Received response to: " + message.response);
  switch(message.response) {
  case "title":
    SendValue(message.value);
    break;
  case "inject embed":
    active_port.postMessage({request: "remove embed", uuid: message.uuid, followup: message.followup});
    has_window_handle = true;
    break;
  case "get element":
    if (message.status) {
      minimum_element_on_page = 0;
      maximum_element_on_page = message.maxElement;
      SendValue(message.elements);
    } else {
      SendNotFound({message: "Unable to locate element with " + message.by + " " + message.value, class: "org.openqa.selenium.NoSuchElementException"});
    }
    break;
  case "get element attribute":
    if (message.status) {
      if (message.value != null || message.value == "") {
        SendValue(message.value);
      } else {
        //Attribute not found
        SendNoValue();
      }
    } else {
      //Element not found (or some similar error)
      SendNotFound({message: "An error occured while finding attribute of " + message.attribute + " " + message.value, class: "org.openqa.selenium.NotFoundException"});
    }
    break;
  case "get element value":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "is element selected":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "is element enabled":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "is element displayed":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "tag name":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "get element text":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendStaleElement();
    }
    break;
  case "send element keys":
    if (message.status) {
      document.embeds[0].return_send_element_keys(message.value.join(""));
    } else {
      if (message.reason == "not visible") {
        SendNotVisible();
      } else if (message.reason == "stale element") {
        SendStaleElement();
      }
    }
    break;
  case "send file element keys":
    if (message.status) {
      document.embeds[0].return_file_keys_element(message.value, message.x, message.y);
    } else {
      SendStaleElement();
    }
    break;
  case "clear element":
    if (message.status) {
      SendNoContent();
    } else {
      SendStaleElement();
    }
    break;
  case "click element":
    if (message.status) {
      //document.embeds[0].return_click_element(message.x, message.y);
      SendNoContent();
    } else {
      if (message.reason == "not visible") {
        SendNotVisible();
      } else if (message.reason == "stale element") {
        SendStaleElement();
      }
    }
    break;
  case "submit element":
    if (message.status) {
      SendNoContent();
    } else {
      SendStaleElement();
    }
    break;
  case "select element":
    if (message.status) {
      SendNoContent();
    } else {
      switch (message.reason) {
      case "disabled":
        //Intentional falling through, though it's not really right
      case "unsupported":
        SendNotFound({message: message.message, class: "java.lang.UnsupportedOperationException"});
        break;
      case "not visible":
        SendNotVisible();
        break;
      }
    }
    break;
  case "toggle element":
    if (message.status) {
      SendValue(message.value);
    } else {
      switch (message.reason) {
      case "unsupported":
        SendNotFound({message: message.message, class: "java.lang.UnsupportedOperationException"});
        break;
      case "not visible":
        SendNotVisible();
        break;
      case "stale element":
        SendStaleElement();
        break;
      }
    }
    break;
  case "url":
    SendValue(message.value);
    break;
  case "add cookie":
    if (message.status) {
      SendNoContent();
    } else {
      SendNotFound({message: message.message, class: "org.openqa.selenium.WebDriverException"});
    }
    break;
  case "delete cookie":
    if (message.status) {
      SendNoContent();
    } else {
      SendNotFound({message: message.message, class: "org.openqa.selenium.WebDriverException"});
    }
    break;
  case "get cookies":
    SendValue(message.cookies);
    break;
  case "delete all cookies":
    SendNoContent();
    break;
  case "go back":
    SendNoContent();
    break;
  case "go forward":
    SendNoContent();
    break;
  case "refresh":
    SendNoContent();
    break;
  case "get source":
    SendValue(message.source);
    break;
  case "execute":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendNotFound({message: message.message, class: "org.openqa.selenium.WebDriverException"});
    }
    break;
  case "location":
    if (message.status) {
      SendValue({class: "java.awt.Point", x: message.x, y: message.y});
    } else {
      SendStaleElement();
    }
    break;
  case "size":
    if (message.status) {
      SendValue({class: "java.awt.Dimension", height: message.height, width: message.width});
    } else {
      SendStaleElement();
    }
    break;
  case "select frame":
    if (message.status) {
      SendNoValue();
    } else {
      SendNotFound({message: message.message, class: "org.openqa.selenium.NoSuchFrameException"});
    }
    break;
  case "get element css":
    if (message.status) {
      SendValue(message.value);
    } else {
      SendNotFound({message: message.message, class: "org.openqa.selenium.WebDriverException"});
    }
    break;
  }
}

function HandleGet(uri) {
  var path = uri.split("/").slice(path_offset_);
  if (path.length < 3 || path[0] != "session" || path[1] != session_id_) {
    //TODO(danielwh): Fail with an HTTP message
    console.log("Invalid session setup");
    return;
  }
  //TODO(danielwh): Add check for not having a current page/port
  switch (path.length) {
  case 3:
    SendValue(capabilities_);
    break;
  case 4:
    switch (path[3]) {
    case "title":
      active_port.postMessage({request: "title"});
      break;
    case "url":
      active_port.postMessage({request: "url"});
      break;
    case "cookie":
      active_port.postMessage({request: "get cookies"});
      break;
    case "source":
      active_port.postMessage({request: "get source"});
      break;
    case "window_handle":
      if (active_port) {
        SendValue(active_port.name);
      } else {
        SendNotFound({message: "Can't get window handle without window", class: "org.openqa.selenium.WebDriverException"});
      }
      break;
    case "window_handles":
      get_window_handles();
      break;
    }
    break;
  case 6:
    if (path[3] == "element") {
      var element_id = parseInt(path[4]);
      if (element_id == null) {
        //TODO(danielwh): Fail with an HTTP message
        console.log("Not an integer element id: " + path[4]);
        return;
      } else if (element_id < minimum_element_on_page || element_id > maximum_element_on_page) {
        SendStaleElement();
      }
      switch (path[5]) {
      case "value":
        active_port.postMessage({request: "get element value", "element_id": element_id});
        break;
      case "text":
        active_port.postMessage({request: "get element text", "element_id": element_id});
        break;
      case "selected":
        active_port.postMessage({request: "is element selected", "element_id": element_id});
        break;
      case "enabled":
        active_port.postMessage({request: "is element enabled", "element_id": element_id});
        break;
      case "name":
        active_port.postMessage({request: "tag name", "element_id": element_id});
        break;
      case "location":
        active_port.postMessage({request: "location", "element_id": element_id});
        break;
      case "size":
        active_port.postMessage({request: "size", "element_id": element_id});
        break;
      case "displayed":
        active_port.postMessage({request: "is element displayed", "element_id": element_id});
        break;
      }
    }
    break;
  case 7:
    if (path[3] == "element") {
      var element_id = parseInt(path[4]);
      if (element_id == null) {
        //TODO(danielwh): Fail with an HTTP message
        console.log("Not an integer element id: " + path[4]);
        return;
      }
      switch(path[5]) {
      case "attribute":
        active_port.postMessage({request: "get element attribute", "element_id": element_id, "attribute": path[6]});
        break;
      case "css":
        active_port.postMessage({request: "get element css", element_id: element_id, css: path[6]});
        break;
      }
    }
    break;
  }
}

function HandlePost(uri, post_data, session_id, context) {
  if (!session_id_) {
    session_id_ = session_id;
  }
  if (!context_) {
    context_ = context;
  }
  //TODO(danielwh): Add check for not having a current page/port
  var path = uri.split("/").slice(path_offset_);
  var value = JSON.parse(post_data);
  switch (path.length) {
  case 1:
    if (path[0] == "session") {
      create_session(value);
    }
    break;
  case 4:
    switch (path[3]) {
    case "element":
      active_port.postMessage({request: "get element", value: value});
      break;
    case "elements":
      active_port.postMessage({request: "get elements", value: value});
      break;
    case "cookie":
      var cookie = JSON.parse(post_data)[0];
      if (cookie.class == "org.openqa.selenium.Cookie") {
        active_port.postMessage({request: "add cookie", cookie: cookie});
      } else {
        //TODO(danielwh): Fail somehow
      }
      break;
    case "back":
      active_port.postMessage({request: "go back"});
      break;
    case "forward":
      active_port.postMessage({request: "go forward"});
      break;
    case "refresh":
      active_port.postMessage({request: "refresh"});
      break;
    case "execute":
      active_port.postMessage({request: "execute", command: value});
      break;
    //URL getting is dealt with by a special call to get_url from the plugin,
    //NOT by this switch
    }
    break;
  case 5:
    switch (path[3]) {
    case "window":
      var message = "Could not find window";
      if (value[0] != undefined && value[0] != null && value[0].name != undefined && value[0].name != null) {
        if (SetActivePortByWindowName(value[0].name)) {
          //TODO(danielwh): Focus window and tab
          SendNoContent();
          return;
        } else {
          var message = "Could not find window by " + value[0].name;
        }
      }
      SendNotFound({message: message, class: "org.openqa.selenium.NoSuchWindowException"});
      break;
    case "frame":
      active_port.postMessage({request: "select frame", by: value});
      break;
    }
    break;
  case 6:
    if (path[3] == "element") {
      element_id = parseInt(value[0].id);
      switch (path[5]) {
      case "value":
        if (blocked) {
          return;
        }
        blocked = true;
        var event = {request: "send element keys", "element_id": element_id, "value": value[0].value};
        if (has_window_handle) {
          active_port.postMessage(event);
        } else {
          active_port.postMessage({request: "inject embed", followup: event, session_id: session_id, uuid: g_uuid});
        }
        break;
      case "clear":
        active_port.postMessage({request: "clear element", "element_id": element_id});
        break;
      case "click":
        var event = {request: "click element", "element_id": element_id};
        if (has_window_handle) {
          active_port.postMessage(event);
        } else {
          active_port.postMessage({request: "inject embed", followup: event, session_id: session_id, uuid: g_uuid});
        }
        break;
      case "submit":
        active_port.postMessage({request: "submit element", "element_id": element_id});
        break;
      case "selected":
        active_port.postMessage({request: "select element", "element_id": element_id});
        break;
      case "toggle":
        active_port.postMessage({request: "toggle element", "element_id": element_id});
        break;
       }
     }
     break;
  case 7:
    if (path[3] == "element") {
      switch (path[5]) {
      case "element":
        active_port.postMessage({request: "get element", "value": value});
        break;
      case "elements":
        active_port.postMessage({request: "get elements", "value": value});
        break;
      }
    }
    break;
  }
}

function HandleDelete(uri) {
  //TODO(danielwh): Add check for not having a current page/port
  var path = uri.split("/").slice(path_offset_);
  switch (path.length) {
  case 2:
    if (path[0] == "session" && path[1] == session_id_) {
      SendNoContent();
    }
    break;
  case 4:
    if (path[3] == "cookie") {
      active_port.postMessage({request: "delete all cookies"});
    }
    break;
  case 5:
    if (path[3] == "cookie") {
      active_port.postMessage({request: "delete cookie", name: path[4]});
    }
    break;
  }
}

//TODO(danielwh): Some kind of filtering so that arbitrary HTTP can't be sent by random javascript
function SendHttp(http) {
  console.log("Sending HTTP: " + http);
  document.embeds[0].SendHttp(http);
}

function SendNoContent() {
  SendHttp("HTTP/1.1 204 No Content");
}

function SendNotFound(value) {
  var response_data = '{"error":true,"sessionId":\"' + session_id_ + '\",' +
      '"context":"' + context_ + '","value":' + JSON.stringify(value) + 
      ',"class":"org.openqa.selenium.remote.Response"}';
  var response = "HTTP/1.1 404 Not Found" +
      "\r\nContent-Length: " + response_data.length +
      "\r\nContent-Type: application/json; charset=ISO-8859-1" +
      "\r\n\r\n" + response_data;
  SendHttp(response);
}

function SendValue(value) {
  var response_data = '{"error":false,"sessionId":"' + session_id_ + 
      '","value":' + JSON.stringify(value) + ',"context":"' + context_ + 
      '","class":"org.openqa.selenium.remote.Response"}';
  
  var response = "HTTP/1.1 200 OK" +
      "\r\nContent-Length: " + response_data.length + 
      "\r\nContent-Type: application/json; charset=ISO-8859-1" +
      "\r\n\r\n" + response_data;
  SendHttp(response);
}

function SendNoValue() {
  var response_data = '{"error":false,"sessionId":"' + session_id_ + 
      '","context":"' + context_ + 
      '","class":"org.openqa.selenium.remote.Response"}';
  
  var response = "HTTP/1.1 200 OK" +
      "\r\nContent-Length: " + response_data.length + 
      "\r\nContent-Type: application/json; charset=ISO-8859-1" +
      "\r\n\r\n" + response_data;
  SendHttp(response);
}

function SendStaleElement() {
  SendNotFound({message: "Element is obsolete", class: "org.openqa.selenium.StaleElementReferenceException"});
}

function SendNotVisible() {
  SendNotFound({message: "Element was not visible", class: "org.openqa.selenium.ElementNotVisibleException"});
}

function create_session(request) {
  capabilities_ = request[0];
  //TODO(danielwh): Better check for OS
  //TODO(danielwh): Don't hard code url or port
  if (request[0].platform == "WINDOWS" && request[0].browserName == "chrome") {
    var response = "HTTP/1.1 302 Found" +
    "\r\nLocation: http://localhost:7601/session/" + session_id_ + "/" + context_ +
    "\r\nContent-Length: 0";
    SendHttp(response);
  } else {
    document.embeds[0].deny_session();
  }
}

function get_window_handles() {
  var window_handles = [];
  for (var i = 0; i < ports.length; ++i) {
    window_handles.push(ports[i].name);
  }
  SendValue(window_handles);
}

//XXX(danielwh): This may change to a chrome.tabs.onCreated/onUpdated/onRemoved event listener, if it turns out to conflict with multiple windows
function get_url(url_json, local_session_id, uuid) {
  var url_string = JSON.parse(url_json)[0];
  //Ignore any URL request we get while loading a page,
  //because we should still return a 204 when we cannot find the page.
  if (is_loading_page) {
    return;
  }
  active_port = null;
  has_window_handle = false;
  if (primary_display_tab_id) {
    chrome.tabs.remove(primary_display_tab_id);
  }
  primary_display_tab_id = null;
  session_id = local_session_id;
  g_uuid = uuid;
  has_window_handle = false;
  minimum_element_on_page = -1;
  maximum_element_on_page = -2;
  chrome.tabs.create({url: url_string, selected: true}, get_url_loaded_callback_first_time);
}

function get_url_loaded_callback_first_time(tab) {
  if (tab.status != "complete") {
    is_loading_page = true
    setTimeout("get_url_check_loaded_first_time(" + tab.id + ")", 10);
  } else {
    is_loading_page = false;
    primary_display_tab_id = tab.id;
    SetActivePortByTabId(tab.id);
    //TODO(danielwh): Maybe close other tabs/windows
    SendNoContent();
  }
}

function get_url_check_loaded_first_time(tab_id) {
  chrome.tabs.get(tab_id, get_url_loaded_callback_first_time);
}

function unblock() {
  blocked = false;
}

function SetActivePortByTabId(tab_id) {
  for (var i = 0; i < ports.length; ++i) {
    if (ports[i].tab.id == tab_id) {
      active_port = ports[i];
      break;
    }
  }
}

function SetActivePortByWindowName(name) {
  for (var i = 0; i < ports.length; ++i) {
    if (ports[i].name == name) {
      active_port = ports[i];
      return true;
    }
  }
}

function remove_port(port) {
  //TODO(danielwh): Nice way of removing from array?
  var temp_ports = [];
  for (var i = 0; i < ports.length; ++i) {
    if (ports[i].name != port.name) {
      temp_ports.push(ports[i]);
    }
  }
  ports = temp_ports;
}

function PushPort(port) {
  //It would be nice to only have one port per name, so we enforce this
  remove_port(port);
  ports.push(port);
}
