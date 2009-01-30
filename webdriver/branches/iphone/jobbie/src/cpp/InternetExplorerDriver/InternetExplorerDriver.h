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

#ifndef InternetExplorerDriver_h
#define InternetExplorerDriver_h

#include "ElementWrapper.h"
#include "IEThread.h"

class InternetExplorerDriver
{
public:
	InternetExplorerDriver();
	InternetExplorerDriver(InternetExplorerDriver* other);
	~InternetExplorerDriver();

	void close();

	IeThread* ThreadFactory();

	bool getVisible();
	void setVisible(bool isShown);

	LPCWSTR getCurrentUrl();

	LPCWSTR getPageSource();
	LPCWSTR getTitle();
	void get(const wchar_t* url);
	void goForward();
	void goBack();
	void GetIe(IWebBrowser2 **ppv);

	void setSpeed(int speed);
	int getSpeed();

	bool sendThreadMsg(UINT msg, DataMarshaller& data);
	DataMarshaller& prepareCmData();
	DataMarshaller& prepareCmData(LPCWSTR str);
	DataMarshaller& prepareCmData(int v);
	DataMarshaller& prepareCmData(IHTMLElement *pElem, LPCWSTR str);

	ElementWrapper* getActiveElement();

	ElementWrapper* selectElementByXPath(IHTMLElement *p, const wchar_t *xpath);
	std::vector<ElementWrapper*>* selectElementsByXPath(IHTMLElement *p, const wchar_t *xpath);
	ElementWrapper* selectElementById(IHTMLElement *p, const wchar_t *elementId);
	std::vector<ElementWrapper*>* selectElementsById(IHTMLElement *p, const wchar_t *elementId);
	ElementWrapper* selectElementByLink(IHTMLElement *p, const wchar_t *elementLink);
	std::vector<ElementWrapper*>* selectElementsByLink(IHTMLElement *p, const wchar_t *elementLink);
	ElementWrapper* selectElementByName(IHTMLElement *p, const wchar_t *elementName);
	std::vector<ElementWrapper*>* selectElementsByName(IHTMLElement *p, const wchar_t *elementName);
	ElementWrapper* selectElementByTagName(IHTMLElement *p, const wchar_t *elementName);
	std::vector<ElementWrapper*>* selectElementsByTagName(IHTMLElement *p, const wchar_t *elementName);
	ElementWrapper* selectElementByClassName(IHTMLElement *p, const wchar_t *elementClassName);
	std::vector<ElementWrapper*>* selectElementsByClassName(IHTMLElement *p, const wchar_t *elementClassName);

	void waitForNavigateToFinish();
	bool switchToFrame(LPCWSTR pathToFrame);

	LPCWSTR getCookies();
	void addCookie(const wchar_t *cookieString);

	IeThread* p_IEthread;

	CComVariant& executeScript(const wchar_t *script, SAFEARRAY* args, bool tryAgain = true);

private:

	int speed;
	bool closeCalled;

	DataMarshaller& commandData() {return p_IEthread->getCmdData();}
};

#endif