package org.openqa.selenium.server;

import junit.framework.TestCase;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class StaticContentHandlerTest extends TestCase {
    private StaticContentHandler handler;
    private StaticContentHandler slowHandler;

    public void setUp() throws Exception {
        super.setUp();
        handler = new StaticContentHandler(false);
        slowHandler = new StaticContentHandler(true);
    }

    public void testShouldMakePageNotCachedWhenHandle() throws Exception {
        HttpResponse response = new HttpResponse();
        handler.handle("", "", new HttpRequest(), response);
        assertEquals("-1", response.getField("Expires"));
    }
    
    public void testShouldDelayResourceLoadingIfSetToSlow() throws Exception {
        long start = new Date().getTime();
        slowHandler.getResource("not_exists");
        long end = new Date().getTime();
        assertTrue(end - start >= StaticContentHandler.SERVER_DELAY);
    }

    public void testShouldDoubleDelayWithAPageMarkedAsSlow() throws Exception {
        long start = new Date().getTime();
        slowHandler.getResource("something-really-slow.html");
        long end = new Date().getTime();
        assertTrue(end - start >= 2 * StaticContentHandler.SERVER_DELAY);
    }

    public void testShouldReturnTheFirstResourceLocatedByLocators() throws Exception {
        final File file = File.createTempFile("selenium-test-", "");
        file.deleteOnExit();
        handler.addStaticContent(new ResourceLocator() {
            public Resource getResource(HttpContext context, String pathInContext) throws IOException {
                return Resource.newResource("Missing");
            }
        });
        handler.addStaticContent(new ResourceLocator() {
            public Resource getResource(HttpContext context, String pathInContext) throws IOException {
                return Resource.newResource(file.toURI().toURL());
            }
        });
        assertEquals(file, handler.getResource(file.toURI().toURL().toString()).getFile());
    }

    public void testShouldReturnMissingResourceIfNoResourceLocated() throws Exception {
        Resource resource = handler.getResource("not exists path");
        assertFalse(resource.exists());
    }
}
