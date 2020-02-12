/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.common;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.webutil.ServletUtil;

public class HttpSupport {

    private HttpSupport() {}
    
    public static final String HTTP_PRAGMA              = "Pragma";
    public static final String HTTP_EXPIRES             = "Expires";
    public static final String HTTP_ACCEPT_HEADER       = "Accept";
    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    
    public static final String HTML_CONTENT_TYPE        = "text/html";

    public static void writeData(HttpServletResponse response, byte[] data, String contentType) throws IOException {
        response.setContentType(contentType);
        response.setHeader(HTTP_PRAGMA, "No-Cache");
        response.setDateHeader(HTTP_EXPIRES, 0);
        // Chunked data seems unnecessary here
        response.setContentLength(data.length);
        ServletOutputStream serverOutputStream = response.getOutputStream();
        serverOutputStream.write(data);
        serverOutputStream.flush();
    }
    
    public static void writeHtml(HttpServletResponse response, StringBuilder html) throws IOException {
        writeData(response, html.toString().getBytes("utf-8"), HTML_CONTENT_TYPE + "; charset=\"utf-8\"");
    }

    public static void writeJsonData(HttpServletResponse response, JSONObjectWriter json) throws IOException {
        writeData(response, json.serializeToBytes(JSONOutputFormats.NORMALIZED), BaseProperties.JSON_CONTENT_TYPE);
    }

    public static JSONObjectReader readJsonData(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (!contentType.equals(BaseProperties.JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + 
                                  BaseProperties.JSON_CONTENT_TYPE + "\" , found: " + contentType);
        }
        String accept = request.getHeader(HTTP_ACCEPT_HEADER);
        if (accept != null && !accept.equals(BaseProperties.JSON_CONTENT_TYPE)) {
            throw new IOException("Accept must be \"" + 
                    BaseProperties.JSON_CONTENT_TYPE + "\" , found: " + accept);
        }
        return JSONParser.parse(ServletUtil.getData(request));
    }
    
    public static String getStackTrace(Exception e, String message) {
        StringBuilder error = new StringBuilder()
                .append(e.getClass().getName())
                .append(": ")
                .append(message);
        StackTraceElement[] st = e.getStackTrace();
        int length = st.length;
        if (length > 20) {
            length = 20;
        }
        for (int i = 0; i < length; i++) {
            String entry = st[i].toString();
            error.append("\n  at " + entry);
            if (entry.contains(".HttpServlet")) {
                break;
            }
        }
        return error.toString();
    }
}
