/*
 * Copyright (C) 2007 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution. 
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef InspectorController_h
#define InspectorController_h

#include "Console.h"
#include "PlatformString.h"
#include "StringHash.h"
#include "Timer.h"
#if USE(JSC)
#include <JavaScriptCore/JSContextRef.h>
#elif USE(V8)
#include <wtf/RefCounted.h>
#include <v8.h>
#endif
#include <wtf/HashMap.h>
#include <wtf/HashSet.h>
#include <wtf/Vector.h>

#if ENABLE(JAVASCRIPT_DEBUGGER)
#include "JavaScriptDebugListener.h"
#endif

#if USE(JSC)
namespace JSC {
    class Profile;
    class UString;
}
#endif

namespace WebCore {

class CachedResource;
class Database;
class DOMWindow;
class DocumentLoader;
class GraphicsContext;
class HitTestResult;
class InspectorClient;
class JavaScriptCallFrame;
class StorageArea;
class Node;
class Page;
struct ResourceRequest;
class ResourceResponse;
class ResourceError;
class ScriptCallStack;
class ScriptState;
class SharedBuffer;

struct ConsoleMessage;
struct InspectorDatabaseResource;
struct InspectorDOMStorageResource;
struct InspectorResource;

class InspectorController : public RefCounted<InspectorController>
#if ENABLE(JAVASCRIPT_DEBUGGER)
                            , JavaScriptDebugListener
#endif
{
public:

    typedef HashMap<unsigned long, RefPtr<InspectorResource> > ResourcesMap;
    typedef HashMap<RefPtr<Frame>, ResourcesMap*> FrameResourcesMap;
    typedef HashSet<RefPtr<InspectorDatabaseResource> > DatabaseResourcesSet;
    typedef HashSet<RefPtr<InspectorDOMStorageResource> > DOMStorageResourcesSet;

    typedef enum {
        CurrentPanel,
        ConsolePanel,
        DatabasesPanel,
        ElementsPanel,
        ProfilesPanel,
        ResourcesPanel,
        ScriptsPanel
    } SpecialPanels;

    struct Setting {
        enum Type {
            NoType, StringType, StringVectorType, DoubleType, IntegerType, BooleanType
        };

        Setting()
            : m_type(NoType)
        {
        }

        Type type() const { return m_type; }

        String string() const { ASSERT(m_type == StringType); return m_string; }
        const Vector<String>& stringVector() const { ASSERT(m_type == StringVectorType); return m_stringVector; }
        double doubleValue() const { ASSERT(m_type == DoubleType); return m_simpleContent.m_double; }
        long integerValue() const { ASSERT(m_type == IntegerType); return m_simpleContent.m_integer; }
        bool booleanValue() const { ASSERT(m_type == BooleanType); return m_simpleContent.m_boolean; }

        void set(const String& value) { m_type = StringType; m_string = value; }
        void set(const Vector<String>& value) { m_type = StringVectorType; m_stringVector = value; }
        void set(double value) { m_type = DoubleType; m_simpleContent.m_double = value; }
        void set(long value) { m_type = IntegerType; m_simpleContent.m_integer = value; }
        void set(bool value) { m_type = BooleanType; m_simpleContent.m_boolean = value; }

    private:
        Type m_type;

        String m_string;
        Vector<String> m_stringVector;

        union {
            double m_double;
            long m_integer;
            bool m_boolean;
        } m_simpleContent;
    };

    InspectorController(Page*, InspectorClient*);
    ~InspectorController();

    void inspectedPageDestroyed();
    void pageDestroyed() { m_page = 0; }

    bool enabled() const;

    Page* inspectedPage() const { return m_inspectedPage; }

    const Setting& setting(const String& key) const;
    void setSetting(const String& key, const Setting&);

    String localizedStringsURL();
    String hiddenPanels();

    void inspect(Node*);
    void highlight(Node*);
    void hideHighlight();

    void show();
    void showPanel(SpecialPanels);
    void close();

#if USE(JSC)
    bool isRecordingUserInitiatedProfile() const { return m_recordingUserInitiatedProfile; }
    void startUserInitiatedProfilingSoon();
    void startUserInitiatedProfiling(Timer<InspectorController>* = 0);
    void stopUserInitiatedProfiling();

    void enableProfiler(bool skipRecompile = false);
    void disableProfiler();
    bool profilerEnabled() const { return enabled() && m_profilerEnabled; }
#endif

    bool windowVisible();
    void setWindowVisible(bool visible = true, bool attached = false);

    void addMessageToConsole(MessageSource, MessageLevel, ScriptCallStack*);
    void addMessageToConsole(MessageSource, MessageLevel, const String& message, unsigned lineNumber, const String& sourceID);
    void clearConsoleMessages();
    void toggleRecordButton(bool);

#if USE(JSC)
    void addProfile(PassRefPtr<JSC::Profile>, unsigned lineNumber, const JSC::UString& sourceURL);
    void addProfileMessageToConsole(PassRefPtr<JSC::Profile> prpProfile, unsigned lineNumber, const JSC::UString& sourceURL);
    void addScriptProfile(JSC::Profile* profile);
    const ProfilesArray& profiles() const { return m_profiles; }
#endif

    void attachWindow();
    void detachWindow();

    void setAttachedWindow(bool); 
    void setAttachedWindowHeight(unsigned height); 

    void toggleSearchForNodeInPage();
    bool searchingForNodeInPage() { return m_searchingForNode; };
    void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags);
    void handleMousePressOnNode(Node*);

#if USE(JSC)
    JSContextRef scriptContext() const { return m_scriptContext; };
    void setScriptContext(JSContextRef context) { m_scriptContext = context; };
#elif USE(V8)
    void setScriptObject(v8::Handle<v8::Object> newScriptObject);
#endif

    void inspectedWindowScriptObjectCleared(Frame*);
    void windowScriptObjectAvailable();

    void scriptObjectReady();

    void populateScriptObjects();
    void resetScriptObjects();

    void didCommitLoad(DocumentLoader*);
    void frameDetachedFromParent(Frame*);

    void didLoadResourceFromMemoryCache(DocumentLoader*, const CachedResource*);

    void identifierForInitialRequest(unsigned long identifier, DocumentLoader*, const ResourceRequest&);
    void willSendRequest(DocumentLoader*, unsigned long identifier, ResourceRequest&, const ResourceResponse& redirectResponse);
    void didReceiveResponse(DocumentLoader*, unsigned long identifier, const ResourceResponse&);
    void didReceiveContentLength(DocumentLoader*, unsigned long identifier, int lengthReceived);
    void didFinishLoading(DocumentLoader*, unsigned long identifier);
    void didFailLoading(DocumentLoader*, unsigned long identifier, const ResourceError&);
#if USE(JSC)
    void resourceRetrievedByXMLHttpRequest(unsigned long identifier, const JSC::UString& sourceString);
#elif USE(V8)
    void resourceRetrievedByXMLHttpRequest(unsigned long identifier, const String& sourceString);
#endif

#if ENABLE(DATABASE)
    void didOpenDatabase(Database*, const String& domain, const String& name, const String& version);
#endif
#if ENABLE(DOM_STORAGE)
    void didUseDOMStorage(StorageArea* storageArea, bool isLocalStorage, Frame* frame);
#endif

    const ResourcesMap& resources() const { return m_resources; }

    void moveWindowBy(float x, float y) const;
    void closeWindow();

#if ENABLE(JAVASCRIPT_DEBUGGER)
    void enableDebugger();
    void disableDebugger();

    bool debuggerEnabled() const { return m_debuggerEnabled; }

    JavaScriptCallFrame* currentCallFrame() const;

    void addBreakpoint(intptr_t sourceID, unsigned lineNumber);
    void removeBreakpoint(intptr_t sourceID, unsigned lineNumber);

    bool pauseOnExceptions();
    void setPauseOnExceptions(bool pause);

    void pauseInDebugger();
    void resumeDebugger();

    void stepOverStatementInDebugger();
    void stepIntoStatementInDebugger();
    void stepOutOfFunctionInDebugger();
#endif

    void drawNodeHighlight(GraphicsContext&) const;
    
    void count(const String& title, unsigned lineNumber, const String& sourceID);

    void startTiming(const String& title);
    bool stopTiming(const String& title, double& elapsed);

    void startGroup(MessageSource source, ScriptCallStack* callFrame);
    void endGroup(MessageSource source, unsigned lineNumber, const String& sourceURL);

#if USE(V8)
    // InspectorController.idl
    void addSourceToFrame(unsigned long identifier, Node* frame);
    Node* getResourceDocumentNode(unsigned long identifier);
    void highlightDOMNode(Node* node);
    void hideDOMNodeHighlight();
    void loaded();
    void attach();
    void detach();
    // TODO(jackson): search should return an array of JSRanges
    void search(Node* node, const String& query);
    DOMWindow* inspectedWindow();
    String platform() const;
#endif

private:
    void focusNode();

    void addConsoleMessage(ScriptState*, ConsoleMessage*);
    void addScriptConsoleMessage(const ConsoleMessage*);

    void addResource(InspectorResource*);
    void removeResource(InspectorResource*);

#if USE(JSC)
    JSObjectRef addScriptResource(InspectorResource*);
#elif USE(V8)
    void addScriptResource(InspectorResource*);
#endif
    void removeScriptResource(InspectorResource*);

#if USE(JSC)
    JSObjectRef addAndUpdateScriptResource(InspectorResource*);
#elif USE(V8)
    void addAndUpdateScriptResource(InspectorResource*);
#endif
    void updateScriptResourceRequest(InspectorResource*);
    void updateScriptResourceResponse(InspectorResource*);
    void updateScriptResourceType(InspectorResource*);
    void updateScriptResource(InspectorResource*, int length);
    void updateScriptResource(InspectorResource*, bool finished, bool failed = false);
    void updateScriptResource(InspectorResource*, double startTime, double responseReceivedTime, double endTime);

    void pruneResources(ResourcesMap*, DocumentLoader* loaderToKeep = 0);
    void removeAllResources(ResourcesMap* map) { pruneResources(map); }

    // Return true if the inspector should track request/response activity.
    // Chrome's policy is to only log requests if the inspector is already open.
    // This reduces the passive bloat from InspectorController: http://b/1113875
    bool trackResources() const { return m_trackResources; }

    // Start/stop resource tracking.
    void enableTrackResources(bool trackResources);

    bool m_trackResources;

    // Helper function to determine when the script object is initialized
#if USE(JSC)
    inline bool hasScriptObject() const { return m_scriptObject; }
#elif USE(V8)
    inline bool hasScriptObject() { return !m_scriptObject.IsEmpty(); }
#endif

#if ENABLE(DATABASE)
#if USE(JSC)
    JSObjectRef addDatabaseScriptResource(InspectorDatabaseResource*);
#elif USE(V8)
    void addDatabaseScriptResource(InspectorDatabaseResource*);
#endif
    void removeDatabaseScriptResource(InspectorDatabaseResource*);
#endif

#if USE(JSC)
#if ENABLE(DOM_STORAGE)
    JSObjectRef addDOMStorageScriptResource(InspectorDOMStorageResource*);
    void removeDOMStorageScriptResource(InspectorDOMStorageResource*);
#endif

    JSValueRef callSimpleFunction(JSContextRef, JSObjectRef thisObject, const char* functionName) const;
    JSValueRef callFunction(JSContextRef, JSObjectRef thisObject, const char* functionName, size_t argumentCount, const JSValueRef arguments[], JSValueRef& exception) const; 

    bool handleException(JSContextRef, JSValueRef exception, unsigned lineNumber) const; 
#endif

    void showWindow();

#if ENABLE(JAVASCRIPT_DEBUGGER)
    virtual void didParseSource(JSC::ExecState*, const JSC::SourceCode&);
    virtual void failedToParseSource(JSC::ExecState*, const JSC::SourceCode&, int errorLine, const JSC::UString& errorMessage);
    virtual void didPause();
#endif

    Page* m_inspectedPage;
    InspectorClient* m_client;
    Page* m_page;
    RefPtr<Node> m_nodeToFocus;
    RefPtr<InspectorResource> m_mainResource;
    ResourcesMap m_resources;
    HashSet<String> m_knownResources;
    FrameResourcesMap m_frameResources;
    Vector<ConsoleMessage*> m_consoleMessages;
#if USE(JSC)
    ProfilesArray m_profiles;
#endif
    HashMap<String, double> m_times;
    HashMap<String, unsigned> m_counts;
#if ENABLE(DATABASE)
    DatabaseResourcesSet m_databaseResources;
#endif
#if USE(JSC)
#if ENABLE(DOM_STORAGE)
    DOMStorageResourcesSet m_domStorageResources;
#endif
    JSObjectRef m_scriptObject;
    JSObjectRef m_controllerScriptObject;
    JSContextRef m_scriptContext;
#elif USE(V8)
    v8::Persistent<v8::Object> m_scriptObject;
#endif
    bool m_windowVisible;
#if ENABLE(JAVASCRIPT_DEBUGGER)
    bool m_debuggerEnabled;
    bool m_attachDebuggerWhenShown;
#endif
    bool m_profilerEnabled;
    bool m_recordingUserInitiatedProfile;
    SpecialPanels m_showAfterVisible;
    unsigned long m_nextIdentifier;
    RefPtr<Node> m_highlightedNode;
    unsigned m_groupLevel;
    bool m_searchingForNode;
    int m_currentUserInitiatedProfileNumber;
    unsigned m_nextUserInitiatedProfileNumber;
    ConsoleMessage* m_previousMessage;
#if USE(JSC)
    Timer<InspectorController> m_startProfiling;
#endif
};

} // namespace WebCore

#endif // !defined(InspectorController_h)
