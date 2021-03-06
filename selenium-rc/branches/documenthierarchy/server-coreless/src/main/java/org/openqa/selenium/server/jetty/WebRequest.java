package org.openqa.selenium.server.jetty;

import java.io.InputStream;
import java.util.Map;

import org.mortbay.http.HttpFields;

public interface WebRequest {
	String getPath();
	String getRequestURL();
	int getPort();
	String getScheme();
	String getHost();
	String getQuery();
	String getMethod();
	InputStream getInputStream();
	void setHandled(boolean handled);
	Map getParameterStringArrayMap();
	void setField(String field, String value);
	String getField(String name);
}
