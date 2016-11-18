/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.acquirer;

import java.io.IOException;

import java.io.PrintWriter;

import java.security.GeneralSecurityException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.BaseProperties;

import org.webpki.webutil.ServletUtil;

////////////////////////////////////////////////////////////////////////////
// This is the core Acquirer (Card-Processor) payment transaction servlet //
////////////////////////////////////////////////////////////////////////////

public abstract class ProcessingBaseServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ProcessingBaseServlet.class.getCanonicalName());
    
    static int referenceId = 194006;

    static String getReferenceId() {
        return "#" + (referenceId++);
    }

    abstract JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest)
    throws IOException, GeneralSecurityException;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = null;
        try {
            urlHolder = new UrlHolder(request);

            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader providerRequest = JSONParser.parse(ServletUtil.getData(request));
            if (AcquirerService.logging) {
                logger.info("Call from" + urlHolder.getCallerAddress() + "with data:\n" + providerRequest);
            }
            
            JSONObjectWriter providerResponse = processCall(urlHolder, providerRequest);

            if (AcquirerService.logging) {
                logger.info("Responded to caller"  + urlHolder.getCallerAddress() + "with data:\n" + providerResponse);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            response.setContentType(JSON_CONTENT_TYPE);
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().write(providerResponse.serializeJSONObject(JSONOutputFormats.NORMALIZED));

        } catch (Exception e) {
            /////////////////////////////////////////////////////////////////////////////////////////
            // Hard error return. Note that we return a clear-text message in the response body.   //
            // Having specific error message syntax for hard errors only complicates things since  //
            // there will always be the dreadful "internal server error" to deal with as well as   //
            // general connectivity problems.                                                      //
            /////////////////////////////////////////////////////////////////////////////////////////
            String message = (urlHolder == null ? "" : "Source" + urlHolder.getCallerAddress()) + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
        }
    }
}
