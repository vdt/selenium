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

#ifndef interactions_h
#define interactions_h

#include <wchar.h>

#ifdef _WIN32
#include "stdafx.h"
#define EXPORT __declspec(dllexport)
#else
#define EXPORT 
#endif

#ifdef __cplusplus
extern "C" {
#endif

// Keyboard interactions
EXPORT void sendKeys(void* windowHandle, const wchar_t* value, int timePerKey);

#ifdef __cplusplus
}
#endif
#endif
