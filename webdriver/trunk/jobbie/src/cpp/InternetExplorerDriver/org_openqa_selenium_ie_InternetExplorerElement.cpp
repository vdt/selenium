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

#include "stdafx.h"
#include <ExDisp.h>
#include "utils.h"

using namespace std;

ElementWrapper* getWrapper(JNIEnv *env, jobject obj)
{
	jclass cls = env->GetObjectClass(obj);
	jfieldID fid = env->GetFieldID(cls, "nodePointer", "J");
	jlong value = env->GetLongField(obj, fid);

	return (ElementWrapper *) value;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_click
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	try {
		if (!wrapper->isDisplayed()) {
			throwUnsupportedOperationException(env, L"You may not click on an element that is not displayed");
			cout << "No exception" << endl;
			return;
		}
	} catch (std::wstring& message) {	
		throwRunTimeException(env, L"You may not click on an element that is not displayed. It is possible that the page this element was on is no longer being displayed.");
		return;
	}

	try {
		wrapper->click();
	} catch (std::wstring& message) {
		throwRunTimeException(env, message.c_str());
	}
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getValue
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	LPCWSTR text = wrapper->getValue();
	return lpcw2jstring(env, text);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_doSendKeys
  (JNIEnv *env, jobject obj, jstring newValue)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	wchar_t* converted = (wchar_t*) env->GetStringChars(newValue, NULL);

	try {
		wrapper->sendKeys(converted);
	} catch (std::wstring& message) {
		throwUnsupportedOperationException(env, message.c_str());
	}
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_clear
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	wrapper->clear();
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getText
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	LPCWSTR text = wrapper->getText();
	return lpcw2jstring(env, text);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getElementName
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	LPCWSTR name = wrapper->getElementName();
	return lpcw2jstring(env, name);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getAttribute
  (JNIEnv *env, jobject obj, jstring attributeName)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	const wchar_t* converted = (wchar_t*) env->GetStringChars(attributeName, NULL);
	LPCWSTR text = wrapper->getAttribute(converted);
	return lpcw2jstring(env, text);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jboolean JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_isEnabled
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	return wrapper->isEnabled() ? JNI_TRUE : JNI_FALSE;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jboolean JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_isSelected
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	return wrapper->isSelected() ? JNI_TRUE : JNI_FALSE;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_setSelected
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	try {
		wrapper->setSelected();
	} catch (std::wstring& message) {
		throwUnsupportedOperationException(env, message.c_str());
	}
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_submit
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	try {
		wrapper->submit();
	} catch (std::wstring& message) {
		throwNoSuchElementException(env, message.c_str());
	}
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jboolean JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_toggle
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	try {
		return wrapper->toggle() ? JNI_TRUE : JNI_FALSE;
	} catch (std::wstring& message) {
		throwUnsupportedOperationException(env, message.c_str());
	}
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jboolean JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_isDisplayed
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	return wrapper->isDisplayed() ? JNI_TRUE : JNI_FALSE;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getLocationOnScreenOnceScrolledIntoView
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper *wrapper = getWrapper(env, obj);
	long x = 0, y = 0;
	wrapper->getLocationOnceScrolledIntoView(&x, &y);

	jclass pointClass = env->FindClass("java/awt/Point");
	jmethodID cId = env->GetMethodID(pointClass, "<init>", "(II)V");

	return env->NewObject(pointClass, cId, x, y);
	} 
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getLocation
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	long x, y;
	wrapper->getLocation(&x, &y);

	jclass pointClass = env->FindClass("java/awt/Point");
	jmethodID cId = env->GetMethodID(pointClass, "<init>", "(II)V");

	return env->NewObject(pointClass, cId, x, y);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jobject JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getSize
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	long width = wrapper->getWidth();
	long height = wrapper->getHeight();

	jclass pointClass = env->FindClass("java/awt/Dimension");
	jmethodID cId = env->GetMethodID(pointClass, "<init>", "(II)V");

	return env->NewObject(pointClass, cId, width, height);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT jlong JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getElementNode
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	IHTMLElement* pElem = wrapper->getWrappedElement();

	return (jlong) pElem;
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_releaseElementNode
  (JNIEnv *env, jobject obj, jlong freeMe)
{
//	((IHTMLDOMNode*) freeMe)->Release();
}

JNIEXPORT jlong JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getIePointer
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	return (jlong) wrapper->getParent();
	}
	END_TRY_CATCH_ANY
	return NULL;
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_releaseIePointer
  (JNIEnv *, jobject, jlong)
{
	// No-op, but here to mirror behaviour of accessing other nodes
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getChildrenOfTypeNatively
  (JNIEnv *env, jobject obj, jobject list, jstring tagName)
{
	TRY
	{
	jclass listClass = env->FindClass("java/util/List");
	jmethodID addId = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

	jclass ieeClass = env->FindClass("org/openqa/selenium/ie/InternetExplorerElement");
	jmethodID cId = env->GetMethodID(ieeClass, "<init>", "(J)V");

	const wchar_t* converted = (wchar_t*) env->GetStringChars(tagName, NULL);
	ElementWrapper* wrapper = getWrapper(env, obj);
	const std::vector<ElementWrapper*>* elements = wrapper->getChildrenWithTagName(converted);

	std::vector<ElementWrapper*>::const_iterator end = elements->end();
	std::vector<ElementWrapper*>::const_iterator cur = elements->begin();

	while(cur < end)
	{
		ElementWrapper* wrapper = *cur;
		jobject wrapped = env->NewObject(ieeClass, cId, wrapper);
		env->CallVoidMethod(list, addId, wrapped);
		cur++;
	}
	delete elements;

	}
	END_TRY_CATCH_ANY
}

JNIEXPORT void JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_deleteStoredObject
  (JNIEnv *env, jobject obj)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);
	if(!wrapper) return;
	wrapper->releaseInterface();
	delete wrapper;
	}
	END_TRY_CATCH_ANY
}

JNIEXPORT jstring JNICALL Java_org_openqa_selenium_ie_InternetExplorerElement_getValueOfCssProperty
  (JNIEnv *env, jobject obj, jstring propertyName)
{
	TRY
	{
	ElementWrapper* wrapper = getWrapper(env, obj);

	const wchar_t* converted = (wchar_t*) env->GetStringChars(propertyName, NULL);
	LPCWSTR text = wrapper->getValueOfCssProperty(converted);
	return lpcw2jstring(env, text);
	}
	END_TRY_CATCH_ANY
	return NULL;
}

#ifdef __cplusplus
}
#endif
