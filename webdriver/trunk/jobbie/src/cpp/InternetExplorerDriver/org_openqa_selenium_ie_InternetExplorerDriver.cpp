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

// This is the main DLL file.

#include "stdafx.h"
#include "utils.h"
#include "webdriver.h"

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif

InternetExplorerDriver* g_pStillOpenedIE = NULL;

InternetExplorerDriver* createIE(JNIEnv *env, jobject& obj)
{
	TRY
	{
	InternetExplorerDriver* wrapper = NULL;
	wrapper = new InternetExplorerDriver();
	g_pStillOpenedIE = wrapper;

	jclass cls = env->GetObjectClass(obj);
	jfieldID fid = env->GetFieldID(cls, "iePointer", "J");
	env->SetLongField(obj, fid, (jlong) wrapper);
	return wrapper;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

InternetExplorerDriver* getIe(JNIEnv *env, jobject obj)
{
	TRY
	{
	jclass cls = env->GetObjectClass(obj);
	jfieldID fid = env->GetFieldID(cls, "iePointer", "J");
	jlong value = env->GetLongField(obj, fid);

	return (InternetExplorerDriver *) value;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

WebDriver* getDriver(JNIEnv *env, jobject obj) 
{
	InternetExplorerDriver* raw = getIe(env, obj);
	WebDriver* toReturn;
	nastyBridgingFunction(raw, &toReturn);
	return toReturn;
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doExecuteScript
  (JNIEnv *env, jobject obj, jstring script, jobjectArray args)
{
	TRY
	{
	InternetExplorerDriver* wrapper = getIe(env, obj);

	// Convert the args into something we can use elsewhere.
	jclass numberClazz = env->FindClass("java/lang/Number");
	jclass booleanClazz = env->FindClass("java/lang/Boolean");
	jclass stringClazz = env->FindClass("java/lang/String");
	jclass elementClazz = env->FindClass("org/openqa/selenium/ie/InternetExplorerElement");

	jmethodID longValue = env->GetMethodID(numberClazz, "longValue", "()J");
	jmethodID booleanValue = env->GetMethodID(booleanClazz, "booleanValue", "()Z");
	jfieldID elementPointer = env->GetFieldID(elementClazz, "nodePointer", "J");

	jsize length = env->GetArrayLength(args);

	SAFEARRAYBOUND bounds;
	bounds.cElements = length;
	bounds.lLbound = 0;
	SAFEARRAY* convertedItems = SafeArrayCreate(VT_VARIANT, 1, &bounds);
	
	LONG index[1];
	for (jsize i = 0; i < length; i++) {
		index[0] = i;
		CComVariant dest;

		jobject arrayObject = env->GetObjectArrayElement(args, i);
		jclass objClazz = env->GetObjectClass(arrayObject);
		
		if (env->IsInstanceOf(arrayObject, numberClazz)) {
			jlong value = env->CallLongMethod(arrayObject, longValue);

			dest.vt = VT_I4;
			dest.lVal = (LONG) value;
		} else if (env->IsInstanceOf(arrayObject, stringClazz)) {
			wchar_t *converted = (wchar_t *)env->GetStringChars((jstring) arrayObject, 0);
			std::wstring value(converted);
			env->ReleaseStringChars((jstring) arrayObject, (jchar*) converted);

			dest.vt = VT_BSTR;
			dest.bstrVal = SysAllocString(value.c_str());
		} else if (env->IsInstanceOf(arrayObject, booleanClazz)) {
			bool value = env->CallBooleanMethod(arrayObject, booleanValue) == JNI_TRUE;

			dest.vt = VT_BOOL;
			dest.boolVal = value;
		} else if (env->IsInstanceOf(arrayObject, elementClazz)) {
			ElementWrapper* element = (ElementWrapper*) env->GetLongField(arrayObject, elementPointer);
			
			dest.vt = VT_DISPATCH;
			dest.pdispVal = element->getWrappedElement();
		}

		SafeArrayPutElement(convertedItems, &i, &dest);
	}

	const wchar_t* converted = (wchar_t *)env->GetStringChars(script, 0);
	CComVariant& result = wrapper->executeScript(converted, convertedItems);
	env->ReleaseStringChars(script, (jchar*) converted);

	// TODO (simon): Does this clear everything properly?
	SafeArrayDestroy(convertedItems);

	if (result.vt == VT_BSTR) {
		return bstr2jstring(env, result.bstrVal);
	} else if (result.vt == VT_DISPATCH) {
		// Attempt to create a new webelement
		IHTMLElement *node = (IHTMLElement*) result.pdispVal;
		if (!node) {
			cerr << L"Cannot convert response to element. Attempting to convert to string" << endl;
			return lpcw2jstring(env, comvariant2cw(result));
		}

		ElementWrapper* element = new ElementWrapper(wrapper, node);

		jclass clazz = env->FindClass("org/openqa/selenium/ie/InternetExplorerElement");
		jmethodID cId = env->GetMethodID(clazz, "<init>", "(J)V");

		return env->NewObject(clazz, cId, (jlong) element);
	} else if (result.vt == VT_BOOL) {
		jclass clazz = env->FindClass("java/lang/Boolean");
		jmethodID cId = env->GetMethodID(clazz, "<init>", "(Z)V");

		return env->NewObject(clazz, cId, (jboolean) (result.boolVal == VARIANT_TRUE));
	} else if (result.vt == VT_I4) {
		jclass clazz = env->FindClass("java/lang/Long");
		jmethodID cId = env->GetMethodID(clazz, "<init>", "(J)V");
		return env->NewObject(clazz, cId, (jlong) result.lVal);
	} else if (result.vt == VT_I8) {
		jclass clazz = env->FindClass("java/lang/Long");
		jmethodID cId = env->GetMethodID(clazz, "<init>", "(J)V");
		return env->NewObject(clazz, cId, (jlong) result.dblVal);
	} else if (result.vt == VT_USERDEFINED) {
		jclass newExcCls;
		env->ExceptionDescribe();
		env->ExceptionClear();
		newExcCls = env->FindClass("java/lang/RuntimeException");
		jmethodID cId = env->GetMethodID(newExcCls, "<init>", "(Ljava/lang/String;)V");

		jstring message = bstr2jstring(env, result.bstrVal);

		jobject exception;
		if (message) {
			exception = env->NewObject(newExcCls, cId, message);
		} else {
			cout << "Falling back" << endl;
			exception = env->NewObject(newExcCls, cId, (jstring) "Cannot extract cause of error");
		}

		env->Throw((jthrowable) exception);
		return NULL;
	}

	cerr << "Unknown variant type. Will attempt to coerce to string: " << result.vt << endl;
	return lpcw2jstring(env, comvariant2cw(result));
	}
	END_TRY_CATCH_ANY
	return NULL;
	
	return NULL;
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getPageSource
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);
	StringWrapper* wrapper;
	wdGetPageSource(driver, &wrapper);
	nastyBridgingFunction2(driver);

	return convertToJString(env, wrapper);
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_close
(JNIEnv *env, jobject obj) 
{
	WebDriver* driver = getDriver(env, obj);
	wdClose(driver);
	nastyBridgingFunction2(driver);

	g_pStillOpenedIE = NULL;

	return NULL;
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_startComNatively
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	}
	END_TRY_CATCH_ANY
}


JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_openIe
  (JNIEnv *env, jobject obj)
{
	TRY
	{
		if(g_pStillOpenedIE)
		{
			g_pStillOpenedIE->close();
			g_pStillOpenedIE = NULL;
		}
	createIE(env, obj);
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_setVisible
  (JNIEnv *env, jobject obj, jboolean isVisible)
{
	WebDriver* driver = getDriver(env, obj);
	wdSetVisible(driver, isVisible == JNI_TRUE ? 1 : 0);
	nastyBridgingFunction2(driver);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_waitForLoadToComplete
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);

	TRY
	{
		wdWaitForLoadToComplete(driver);
		nastyBridgingFunction2(driver);
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getCurrentUrl
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);
	StringWrapper* wrapper;
	wdGetCurrentUrl(driver, &wrapper);
	nastyBridgingFunction2(driver);

	return convertToJString(env, wrapper);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_get
  (JNIEnv *env, jobject obj, jstring url)
{
	WebDriver* driver = getDriver(env, obj);
	const wchar_t* converted = (wchar_t *)env->GetStringChars(url, 0);
	wdGet(driver, converted);
	nastyBridgingFunction2(driver);
	env->ReleaseStringChars(url, (jchar*) converted);
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_getTitle
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);
	StringWrapper* wrapper;
	wdGetTitle(driver, &wrapper);
	nastyBridgingFunction2(driver);

	return convertToJString(env, wrapper);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_deleteStoredObject
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	InternetExplorerDriver* ie = getIe(env, obj);
	if (ie)
	{
		delete ie;
	}
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_setFrameIndex
  (JNIEnv *env, jobject obj, jstring pathToFrame)
{
	WebDriver *driver = getDriver(env, obj);
	const wchar_t* path = (wchar_t *)env->GetStringChars(pathToFrame, 0);

	int result = wdSwitchToFrame(driver, path);
	env->ReleaseStringChars(pathToFrame, (jchar*) path);
	nastyBridgingFunction2(driver);

	if (result != SUCCESS) {
		std::wstring msg(L"Cannot locate frame using path: ");
		msg += path;
		throwNoSuchFrameException(env, msg.c_str());
	}
}


JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_goBack
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);
	wdGoBack(driver);
	nastyBridgingFunction2(driver);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_goForward
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);
	wdGoForward(driver);
	nastyBridgingFunction2(driver);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doAddCookie
  (JNIEnv *env, jobject obj, jstring cookieString)
{
	WebDriver* driver = getDriver(env, obj);

	const wchar_t* converted = (wchar_t *)env->GetStringChars(cookieString, 0);
	wdAddCookie(driver, converted);
	env->ReleaseStringChars(cookieString, (jchar*) converted);

	nastyBridgingFunction2(driver);
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doGetCookies
  (JNIEnv *env, jobject obj)
{
	WebDriver* driver = getDriver(env, obj);

	StringWrapper* wrapper;
	wdGetCookies(driver, &wrapper);

	nastyBridgingFunction2(driver);
	return convertToJString(env, wrapper);
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doSetMouseSpeed
  (JNIEnv *env, jobject obj, jint speed) 
{
	TRY
	{
	InternetExplorerDriver* ie = getIe(env, obj);
	ie->setSpeed((int) speed);
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerDriver_doSwitchToActiveElement
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	InternetExplorerDriver* ie = getIe(env, obj);
	ElementWrapper* element = ie->getActiveElement();

	if (!element)
		return NULL;

    jclass clazz = env->FindClass("org/openqa/selenium/ie/InternetExplorerElement");
	jmethodID cId = env->GetMethodID(clazz, "<init>", "(J)V");

	return env->NewObject(clazz, cId, (jlong) element);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

#ifdef __cplusplus
}
#endif
