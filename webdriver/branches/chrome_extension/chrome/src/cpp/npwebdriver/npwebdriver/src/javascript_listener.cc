#include "webdriver/javascript_listener.h"

#include "webdriver/chrome_driver_plugin.h"
#include "webdriver/logging.h"

#include <string>

using namespace std;

namespace webdriver {

JavascriptListener::JavascriptListener(NPP instance) :
    BaseJavascriptListener(instance),
    chrome_driver_plugin_(NULL),
    browser_funcs_(NULL) {
}

bool JavascriptListener::HasMethod(NPIdentifier name) {
  const char *method = browser_funcs_->utf8fromidentifier(name);
  
  WEBDRIVER_LOG("HasMethod on ");
  WEBDRIVER_LOG((char *)method);
  WEBDRIVER_LOG("\n");
  
  if (strcmp(method, "SendHttp") &&
      strcmp(method, "return_send_element_keys") &&
      strcmp(method, "return_click_element")) {
    return false;
  } else {
    return true;
  }
}

bool JavascriptListener::Invoke(NPIdentifier name,
                                const NPVariant *args,
                                uint32_t argCount,
                                NPVariant *result) {
  if (browser_funcs_ == NULL || chrome_driver_plugin_ == NULL) {
    return false;
  }
  //WEBDRIVER_LOG all javascript calls
  JS_WEBDRIVER_LOG(browser_funcs_->utf8fromidentifier(name));
  JS_WEBDRIVER_LOG("(");
  for (uint32_t i = 0; i < argCount; i++) {
    if (args[i].type == NPVariantType_Int32) {
      char buf[1000];
      sprintf(buf, "%d", args[i].value.intValue);
      JS_WEBDRIVER_LOG(buf);
    } else if (args[i].type == NPVariantType_String) {
      JS_WEBDRIVER_LOG("\"");
      JS_WEBDRIVER_LOG((char *)args[i].value.stringValue.UTF8Characters);
      JS_WEBDRIVER_LOG("\"");
    } else if (args[i].type == NPVariantType_Bool) {
      if (args[i].value.boolValue) {
        JS_WEBDRIVER_LOG("true");
      } else {
        JS_WEBDRIVER_LOG("false");
      }
    } else if (args[i].type == NPVariantType_Null) {
      JS_WEBDRIVER_LOG("NULL");
    } else {
      JS_WEBDRIVER_LOG("SOME_OTHER_TYPE");
    }
    if (i != argCount - 1) {
      JS_WEBDRIVER_LOG(", ");
    }
  }
  JS_WEBDRIVER_LOG(")\n");

  const char *method = browser_funcs_->utf8fromidentifier(name);

  if (!strcmp(method, "SendHttp") && argCount == 1 &&
      args[0].type == NPVariantType_String) {
    chrome_driver_plugin_->SendHttp(args[0].value.stringValue.UTF8Characters);
  } else if (!strcmp(method, "return_send_element_keys") && argCount == 2 &&
      args[0].type == NPVariantType_Bool &&
      args[1].type == NPVariantType_String) {
    chrome_driver_plugin_->ReturnSendElementKeys(args[0].value.boolValue,
        (char *)args[1].value.stringValue.UTF8Characters);
  } else if (!strcmp(method, "return_click_element") && argCount == 3 &&
      args[0].type == NPVariantType_Bool &&
      args[1].type == NPVariantType_Int32 &&
      args[2].type == NPVariantType_Int32) {
    chrome_driver_plugin_->ReturnClickElement(args[0].value.boolValue,
                                              args[1].value.intValue,
                                              args[2].value.intValue);
  } else {
    return false;
  }
  return true;
}

void JavascriptListener::set_chrome_driver_plugin(
    ChromeDriverPlugin *chrome_driver_plugin) {
  chrome_driver_plugin_ = chrome_driver_plugin;
}

void JavascriptListener::set_browser_funcs(NPNetscapeFuncs *browser_funcs) {
  browser_funcs_ = browser_funcs;
}

} //namespace webdriver