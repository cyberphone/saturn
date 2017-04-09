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
package org.webpki.saturn.merchant;

import java.io.IOException;
import java.net.URL;

import java.security.GeneralSecurityException;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

import org.webpki.webutil.ServletUtil;

import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet holds core methods used by Saturn and Saturn "Native"   //
//////////////////////////////////////////////////////////////////////////

public abstract class ProcessingBaseServlet extends HttpServlet implements BaseProperties, MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ProcessingBaseServlet.class.getCanonicalName());
    
    static Map<String,ProviderAuthority> providerAuthorityObjects = Collections.synchronizedMap(new LinkedHashMap<String,ProviderAuthority>());

    static Map<String,PayeeAuthority> payeeAuthorityObjects = Collections.synchronizedMap(new LinkedHashMap<String,PayeeAuthority>());

    static final int TIMEOUT_FOR_REQUEST = 5000;

    static JSONObjectReader makeReader(JSONObjectWriter writer) throws IOException {
        return JSONParser.parse(writer.serializeToString(JSONOutputFormats.NORMALIZED));
    }

    static String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (MerchantService.serverPortMapping == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       MerchantService.serverPortMapping,
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
        if (MerchantService.logging) {
            logger.info("Call to " + urlHolder.getUrl() + urlHolder.getCallerAddress() +
                        "returned:\n" + result);
        }
        return result;
    }

    static JSONObjectReader postData(UrlHolder urlHolder, JSONObjectWriter request) throws IOException {
        if (MerchantService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.getCallerAddress() +
                        "with data:\n" + request);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader(HttpSupport.HTTP_CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
        wrap.setHeader(HttpSupport.HTTP_ACCEPT_HEADER, JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(urlHolder.getUrl()), request.serializeToBytes(JSONOutputFormats.NORMALIZED));
        return fetchJSONData(wrap, urlHolder);
    }

    static JSONObjectReader getData(UrlHolder urlHolder) throws IOException {
        if (MerchantService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + urlHolder.getCallerAddress());
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader(HttpSupport.HTTP_ACCEPT_HEADER, JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJSONData(wrap, urlHolder);
    }

    static void returnJsonData(HttpServletResponse response, JSONObjectWriter returnData) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        byte[] data = returnData.serializeToBytes(JSONOutputFormats.NORMALIZED);
        // Chunked data seems unnecessary here
        response.setContentLength(data.length);
        ServletOutputStream serverOutputStream = response.getOutputStream();
        serverOutputStream.write(data);
        serverOutputStream.flush();
    }

    static ProviderAuthority getProviderAuthority(UrlHolder urlHolder) throws IOException {
        ProviderAuthority providerAuthority = providerAuthorityObjects.get(urlHolder.getUrl());
        if (providerAuthority == null || providerAuthority.getExpires().before(new GregorianCalendar())) {
            providerAuthority = new ProviderAuthority(getData(urlHolder), urlHolder.getUrl());
            providerAuthorityObjects.put(urlHolder.getUrl(), providerAuthority);
            if (MerchantService.logging) {
                logger.info("Updated cache " + urlHolder.getUrl());
            }
        } else {
            if (MerchantService.logging) {
                logger.info("Fetched from cache " + urlHolder.getUrl());
            }
        }
        return providerAuthority;
    }

    static PayeeAuthority getPayeeAuthority(UrlHolder urlHolder) throws IOException {
        PayeeAuthority payeeAuthority = payeeAuthorityObjects.get(urlHolder.getUrl());
        if (payeeAuthority == null || payeeAuthority.getExpires().before(new GregorianCalendar())) {
            payeeAuthority = new PayeeAuthority(getData(urlHolder), urlHolder.getUrl());
            payeeAuthorityObjects.put(urlHolder.getUrl(), payeeAuthority);
            if (MerchantService.logging) {
                logger.info("Updated cache " + urlHolder.getUrl());
            }
        } else {
            if (MerchantService.logging) {
                logger.info("Fetched from cache " + urlHolder.getUrl());
            }
        }
        return payeeAuthority;
    }
    
    static String getHybridModeUrl(ProviderAuthority providerAuthority) throws IOException {
        JSONObjectReader extensions = providerAuthority.getExtensions();
        if (extensions != null) {
            return extensions.getStringConditional(KnownExtensions.HYBRID_PAYMENT);
        }
        return null;
    }

    abstract boolean processCall(JSONObjectReader walletResponse,
                                 PaymentRequest paymentRequest, 
                                 PayerAuthorization payerAuthorization,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean debug, 
                                 DebugData debugData, 
                                 UrlHolder urlHolder) throws IOException, GeneralSecurityException;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = new UrlHolder(request);
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                returnJsonData(response, WalletAlertMessage.encode("The session appears to have timed out."));
                return;
            }
            
            MerchantService.slowOperationSimulator();

            // Reading the Wallet response
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader walletResponse = JSONParser.parse(ServletUtil.getData(request));
            if (MerchantService.logging) {
                logger.info("Received from wallet:\n" + walletResponse);
            }

            // Do we have web debug mode?
            DebugData debugData = null;
            boolean debug = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
            if (debug) {
                debugData = (DebugData) session.getAttribute(DEBUG_DATA_SESSION_ATTR);
                if (debugData.softReserveOrBasicError) {
                    debug = false;
                    debugData = null;
                } else {
                    debugData.walletResponse = walletResponse;
                }
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = new PayerAuthorization(walletResponse);

            @SuppressWarnings("unchecked")
            JSONObjectWriter rawPaymentRequest = ((LinkedHashMap<String,JSONObjectWriter>) session.getAttribute(WALLET_REQUEST_SESSION_ATTR))
                .get(payerAuthorization.getAccountType().getTypeUri());
            if (rawPaymentRequest == null) {
                throw new IOException("Missing: " + payerAuthorization.getAccountType().getTypeUri());
            }
            PaymentRequest paymentRequest =
                new PaymentRequest(JSONParser.parse(rawPaymentRequest.serializeToString(JSONOutputFormats.NORMALIZED)));
            
            // The actual processing is here
            if (processCall(walletResponse,
                            paymentRequest, 
                            payerAuthorization,
                            session,
                            request,
                            response,
                            debug,
                            debugData,
                            urlHolder)) {
           
                // This may be a QR session
                QRSessions.optionalSessionSetReady((String) session.getAttribute(QR_SESSION_ID_ATTR));
    
                logger.info("Successful authorization of request: " + paymentRequest.getReferenceId());
                /////////////////////////////////////////////////////////////////////////////////////////
                // Normal return                                                                       //
                /////////////////////////////////////////////////////////////////////////////////////////
                returnJsonData(response, Messages.PAYMENT_CLIENT_SUCCESS.createBaseMessage());
            }

        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            JSONObjectWriter userError = WalletAlertMessage.encode("An unexpected error occurred.<br>" +
                                                                   "Please try again or contact support.");
            returnJsonData(response, userError);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
