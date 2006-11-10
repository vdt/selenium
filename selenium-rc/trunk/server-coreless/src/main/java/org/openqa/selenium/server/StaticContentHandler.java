package org.openqa.selenium.server;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.util.*;

class StaticContentHandler extends ResourceHandler {
	private static final long serialVersionUID = 8031049889874827358L;
	private final boolean slowResources;
    private List<ResourceLocator> resourceLocators = new ArrayList<ResourceLocator>();
    public static final int SERVER_DELAY = 1000;

    public StaticContentHandler(boolean slowResources) {
        this.slowResources = slowResources;
    }

    public void handle(String pathInContext, String pathParams, HttpRequest httpRequest, HttpResponse httpResponse) throws HttpException, IOException {
        hackRemoveLastModifiedSince(httpRequest);
        setNoCacheHeaders(httpResponse);
        if (pathInContext.equals("/core/SeleneseRunner.html") && SeleniumServer.isProxyInjectionMode()) {
            pathInContext = pathInContext.replaceFirst("/core/SeleneseRunner.html",
                    "/core/InjectedSeleneseRunner.html");
        }
        super.handle(pathInContext, pathParams, httpRequest, httpResponse);
    }
    
    /** DGF Opera just refuses to honor my cache settings.  This will
     * force jetty to return the document anyway.
     */
    private void hackRemoveLastModifiedSince(HttpRequest req) {
        if (null == req.getField(HttpFields.__IfModifiedSince)) {
            return;
        }
        try {
            Field f = HttpMessage.class.getDeclaredField("_header");
            f.setAccessible(true);
            HttpFields header = (HttpFields) f.get(req);
            header.remove(HttpFields.__IfModifiedSince);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** Sets all the don't-cache headers on the HttpResponse */
    private void setNoCacheHeaders(HttpResponse res) {
        res.setField(HttpFields.__CacheControl, "no-cache");
        res.setField(HttpFields.__Pragma, "no-cache");
        res.setField(HttpFields.__Expires, HttpFields.__01Jan1970);
    }


    protected Resource getResource(final String pathInContext) throws IOException {
        delayIfNeed(pathInContext);
        for (int i = 0; i < resourceLocators.size(); i++) {
            ResourceLocator resourceLocator = resourceLocators.get(i);
            Resource resource = resourceLocator.getResource(getHttpContext(), pathInContext);
            if (resource.exists()) return resource;
        }
        return Resource.newResource("MISSING RESOURCE");
    }

    private void delayIfNeed(String pathInContext) {
        if (slowResources) {
            pause(SERVER_DELAY);
            if (pathInContext != null && pathInContext.endsWith("slow.html")) {
                pause(SERVER_DELAY);
            }
        }
    }

    private void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }


    public void addStaticContent(ResourceLocator locator) {
        resourceLocators.add(locator);
    }

    public void sendData(HttpRequest request,
                         HttpResponse response,
                         String pathInContext,
                         Resource resource,
                         boolean writeHeaders) throws IOException {
        if (!SeleniumServer.isProxyInjectionMode()) {
            super.sendData(request, response, pathInContext, resource, writeHeaders);
            return;
        }
        ResourceCache.ResourceMetaData metaData = (ResourceCache.ResourceMetaData) resource.getAssociate();
        String mimeType = metaData.getMimeType();
        response.setContentType(mimeType);
        if (resource.length() != -1) {
            response.setField(HttpFields.__ContentLength, metaData.getLength());
        }
        InjectionHelper.injectJavaScript(request, response, resource.getInputStream(), response.getOutputStream());
        request.setHandled(true);
    }
}