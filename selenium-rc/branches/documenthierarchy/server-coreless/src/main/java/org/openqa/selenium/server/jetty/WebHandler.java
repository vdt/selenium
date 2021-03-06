package org.openqa.selenium.server.jetty;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mortbay.http.HttpFields;
import org.openqa.selenium.server.jetty.Handler.Method;
import org.openqa.selenium.server.jetty5.Jetty5StaticContentHandler;

public class WebHandler {
	private static Logger logger = Logger
	.getLogger(WebHandler.class);
	
	private static boolean handlingEnabled = true;
	
	public void handleHandler(Handler handler, String pathInContext, WebRequest webRequest, WebResponse webResponse) throws IOException {
		String queryString = webRequest.getQuery();
		
		String contextPath = pathInContext;
		
		// Must be parameter string array map
		Map parameterMap = webRequest.getParameterStringArrayMap();
		OutputStream responseOutputStream = webResponse.getOutputStream();
		String methodString = webRequest.getMethod();
		boolean requestWasHandled = false;

		// Get the method; null if it isn't supported.
		Method method = Method.getMethod(methodString);

		if (method != null) {
			// Create a new byte array output stream
			// The handler should not be able to flush the stream, thus a
			// separate output stream is passed to it
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
			
			requestWasHandled = handler.handle(contextPath,
					queryString, parameterMap, method, webRequest, webResponse, /*requestInputStream,*/
					bufferedOutputStream);

			if (requestWasHandled) {
				// Flush the finished output stream from the handler
				bufferedOutputStream.flush();
				
				//Writer out = new OutputStreamWriter(responseOutputStream, "UTF-8");
				try {
					// Write the output stream from the handler to the response output stream
					responseOutputStream.write(byteArrayOutputStream.toByteArray());
				}
				catch (SocketException e) {
//					handlingEnabled = false;
//					logger.debug("Socket Exception Thread " + Thread.currentThread().getName());
//					throw e;
				}

			}
		}

		webRequest.setHandled(requestWasHandled);
		
		webResponse.setCharacterEncoding("UTF-8");
		
		// Flush the response output stream
		responseOutputStream.flush();
		
		if (requestWasHandled) {
			if (webResponse.getStatus() != HttpServletResponse.SC_NOT_MODIFIED) {
			webResponse.setStatus(HttpServletResponse.SC_OK);
			}
		} else if (method == null) {
			webResponse.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		} else {
			webResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
