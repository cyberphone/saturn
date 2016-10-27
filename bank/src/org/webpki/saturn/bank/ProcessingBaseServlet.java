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
package org.webpki.saturn.bank;

import java.io.IOException;
import java.io.PrintWriter;

import java.math.BigDecimal;

import java.net.URL;

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

import org.webpki.net.HTTPSWrapper;

import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.PaymentRequest;

import org.webpki.webutil.ServletUtil;

//////////////////////////////////////////////////////////////////////////
// This is the core Payment Provider (Bank) processing servlet //
//////////////////////////////////////////////////////////////////////////

public abstract class ProcessingBaseServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ProcessingBaseServlet.class.getCanonicalName());
    
    static final int TIMEOUT_FOR_REQUEST = 5000;
    
    // Just a few demo values
    
    static final BigDecimal DEMO_ACCOUNT_LIMIT     = new BigDecimal("1000000.00");
    static final BigDecimal DEMO_RBA_LIMIT         = new BigDecimal("100000.00");
    static final String RBA_PARM_MOTHER            = "mother";
    

    static String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (BankService.serverPortMapping == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       BankService.serverPortMapping,
                       url2.getFile()).toExternalForm(); 
    }

    static JSONObjectReader fetchJSONData(HTTPSWrapper wrap, UrlHolder urlHolder) throws IOException {
        if (wrap.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("HTTP error " + wrap.getResponseCode() + " " + wrap.getResponseMessage() + ": " +
                                  (wrap.getData() == null ? "No other information available" : wrap.getDataUTF8()));
        }
        // We expect JSON, yes
        if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getRawContentType());
        }
        JSONObjectReader result = JSONParser.parse(wrap.getData());
        if (BankService.logging) {
            logger.info("Call to " + urlHolder.getUrl() + urlHolder.callerAddress +
                        "returned:\n" + result);
        }
        return result;
    }

    static JSONObjectReader postData(UrlHolder urlHolder, JSONObjectWriter request) throws IOException {
        if (BankService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.callerAddress +
                        "with data:\n" + request);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(urlHolder.getUrl()), request.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        return fetchJSONData(wrap, urlHolder);
    }

    static JSONObjectReader getData(UrlHolder urlHolder) throws IOException {
        if (BankService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.callerAddress);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJSONData(wrap, urlHolder);
    }

    // The purpose of this class is to enable URL information in exceptions

    class UrlHolder {
        String remoteAddress;
        String contextPath;
        String callerAddress;
        HttpServletRequest request;
        
        UrlHolder(HttpServletRequest request) {
            this.remoteAddress = request.getRemoteAddr();
            this.contextPath = request.getContextPath();
            callerAddress = " [Origin=" + remoteAddress + ", Context=" + contextPath + "] ";
            this.request = request;
        }

        private String url;

        String getUrl() {
            return url;
        }

        void setUrl(String url) {
            this.url = url;
        }
    }
    
    static String getReferenceId() {
        return "#" + (BankService.referenceId++);
    }
    
    static ProviderAuthority getAuthority(UrlHolder urlHolder) throws IOException {
        return new ProviderAuthority(getData(urlHolder), urlHolder.getUrl());
    }
    
    static String amountInHtml(PaymentRequest paymentRequest, BigDecimal amount) throws IOException {
        return "<span style=\"font-weight:bold;white-space:nowrap\">" + 
               paymentRequest.getCurrency().amountToDisplayString(amount, true) +
               "</span>";
    }

    abstract JSONObjectWriter processCall(JSONObjectReader providerRequest, UrlHolder urlHolder) 
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
            if (BankService.logging) {
                logger.info("Call from" + urlHolder.callerAddress + "with data:\n" + providerRequest);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // We rationalize here by using a single end-point for all requests                    //
            /////////////////////////////////////////////////////////////////////////////////////////
            JSONObjectWriter providerResponse = processCall(providerRequest, urlHolder); 
            if (BankService.logging) {
                logger.info("Responded to caller"  + urlHolder.callerAddress + "with data:\n" + providerResponse);
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
            String message = (urlHolder == null ? "" : "From" + urlHolder.callerAddress +
                              (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl()) + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
        }
    }
}
