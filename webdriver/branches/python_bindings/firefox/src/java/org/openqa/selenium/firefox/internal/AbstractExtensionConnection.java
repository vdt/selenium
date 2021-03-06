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

package org.openqa.selenium.firefox.internal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.Command;
import org.openqa.selenium.firefox.ExtensionConnection;
import org.openqa.selenium.firefox.NotConnectedException;
import org.openqa.selenium.firefox.Response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public abstract class AbstractExtensionConnection implements ExtensionConnection {
    private Socket socket;
    protected SocketAddress address;
    private OutputStreamWriter out;
    private BufferedInputStream in;

    protected void setAddress(String host, int port) {
        InetAddress addr;

        if ("localhost".equals(host)) {
            addr = obtainLoopbackAddress();
        } else {
            try {
                addr = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new WebDriverException(e);
            }
        }

        address = new InetSocketAddress(addr, port);
    }

    private InetAddress obtainLoopbackAddress() {
        InetAddress localIp4 = null;
        InetAddress localIp6 = null;

        try {
            Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allInterfaces.hasMoreElements()) {
                NetworkInterface iface = allInterfaces.nextElement();
                Enumeration<InetAddress> allAddresses = iface.getInetAddresses();
                while (allAddresses.hasMoreElements()) {
                    InetAddress addr = allAddresses.nextElement();
                    if (addr.isLoopbackAddress()) {
                        if (addr instanceof Inet4Address && localIp4 == null)
                            localIp4 = addr;
                        else if (addr instanceof Inet6Address && localIp6 == null)
                            localIp6 = addr;
                    }
                }
            }
        } catch (SocketException e) {
            throw new WebDriverException(e);
        }

        // Firefox binds to the IP4 address by preference
        if (localIp4 != null)
            return localIp4;

        if (localIp6 != null)
            return localIp6;

        // Nothing found. Grab the first address we can find
        NetworkInterface firstInterface;
        try {
            firstInterface = NetworkInterface.getNetworkInterfaces().nextElement();
        } catch (SocketException e) {
            throw new WebDriverException(e);
        }
        InetAddress firstAddress = null;
        if (firstInterface != null) {
            firstAddress = firstInterface.getInetAddresses().nextElement();
        }

        if (firstAddress != null)
            return firstAddress;

        throw new WebDriverException("Unable to find loopback address for localhost");
    }

    protected void connectToBrowser(long timeToWaitInMilliSeconds) throws IOException {
        long waitUntil = System.currentTimeMillis() + timeToWaitInMilliSeconds;
        while (!isConnected() && waitUntil > System.currentTimeMillis()) {
            try {
                connect();
            } catch (ConnectException e) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ie) {
                    throw new WebDriverException(ie);
                }
            }
        }

        if (!isConnected()) {
          throw new NotConnectedException(socket, timeToWaitInMilliSeconds);
        }
    }

    private void connect() throws IOException {
        socket = new Socket();

        socket.connect(address);
        in = new BufferedInputStream(socket.getInputStream());
        out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public Response sendMessageAndWaitForResponse(Class<? extends RuntimeException> throwOnFailure,
                                                  Command command) {
        String converted = convert(command);

        StringBuilder message = new StringBuilder("Length: ");
        message.append(converted.length()).append("\n\n");
        message.append(converted).append("\n");

        try {
            out.write(message.toString());
            out.flush();
        } catch (IOException e) {
            throw new WebDriverException(e);
        }

        return waitForResponseFor(command.getCommandName());
    }

    private String convert(Command command) {
        JSONObject json = new JSONObject();
        try {
            json.put("commandName", command.getCommandName());
            json.put("context", String.valueOf(command.getContext()));
            json.put("elementId", command.getElementId());

            JSONArray params = new JSONArray();
            for (Object o : command.getParameters()) {
                params.put(o);
            }

            json.put("parameters", params);
        } catch (JSONException e) {
            throw new WebDriverException(e);
        }

      try {
        // Force encoding as UTF-8.
        byte[] bytes = json.toString().getBytes("UTF-8");
        return new String(bytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // If UTF-8 is missing from Java, we've got problems
        throw new IllegalStateException("Cannot convert string to UTF-8");
      }
    }

    private Response waitForResponseFor(String command) {
        try {
            return readLoop(command);
        } catch (IOException e) {
            throw new WebDriverException(e);
        }
    }

    private Response readLoop(String command) throws IOException {
        Response response = nextResponse();

        if (command.equals(response.getCommand()))
            return response;
        throw new WebDriverException("Expected response to " + command + " but actually got: " + response.getCommand() + " (" + response.getCommand() + ")");
    }

    private Response nextResponse() throws IOException {
        String line = readLine();

        // Expected input will be of the form:
        // Header: Value
        // \n
        // JSON object
        //
        // The only expected header is "Length"

        // Read headers
        int count = 0;
        String[] parts = line.split(":", 2);
        if ("Length".equals(parts[0])) {
            count = Integer.parseInt(parts[1].trim());
        }

        // Wait for the blank line
        while (line.length() != 0) {
            line = readLine();
        }

        // Read the rest of the response.
        byte[] remaining = new byte[count];
        for (int i = 0; i < count; i++) {
            remaining[i] = (byte) in.read();
        }

        return new Response(new String(remaining, "UTF-8"));
    }

    private String readLine() throws IOException {
        int size = 4096;
        int growBy = 1024;
        byte[] raw = new byte[size];
        int count = 0;

        for (;;) {
            int b = in.read();

            if (b == -1 || (char) b == '\n')
                break;
            raw[count++] = (byte) b;
            if (count == size) {
                size += growBy;
                byte[] temp = new byte[size];
                System.arraycopy(raw, 0, temp, 0, count);
                raw = temp;
            }
        }

        return new String(raw, 0, count, "UTF-8");
    }
}
