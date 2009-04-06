/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

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

// Copyright 2008 Google Inc.  All Rights Reserved.

package org.openqa.selenium.remote.server.handler;

import org.openqa.selenium.remote.server.DriverSessions;
import org.openqa.selenium.remote.server.rest.ResultType;

public class SwitchToFrame extends WebDriverHandler {

  private String id;

  public SwitchToFrame(DriverSessions sessions) {
    super(sessions);
  }

  public void setId(String id) {
    this.id = id;
  }

  public ResultType call() throws Exception {
    if (id == null) {
      getDriver().switchTo().defaultContent();
    } else {
      getDriver().switchTo().frame(id);
    }

    return ResultType.SUCCESS;
  }
}
