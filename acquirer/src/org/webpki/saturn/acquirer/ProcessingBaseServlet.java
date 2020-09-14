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

import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.AccountDataDecoder;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.HttpSupport;

import com.supercard.SupercardAccountDataDecoder;

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

    static SupercardAccountDataDecoder getAccountData(AuthorizationResponseDecoder authorizationResponse)
    throws IOException, GeneralSecurityException {
        AccountDataDecoder accountData = 
                authorizationResponse.getProtectedAccountData(AcquirerService.clientAccountTypes,
                                                              AcquirerService.decryptionKeys);
        return (SupercardAccountDataDecoder)accountData;
    }

    abstract JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = null;
        try {
            urlHolder = new UrlHolder(request);

            /////////////////////////////////////////////////////////////////////////////////////////
            // Must be tagged as JSON content and parse as well                                    //
            /////////////////////////////////////////////////////////////////////////////////////////
            JSONObjectReader providerRequest = HttpSupport.readJsonData(request);

            /////////////////////////////////////////////////////////////////////////////////////////
            // First control passed...                                                             //
            /////////////////////////////////////////////////////////////////////////////////////////
             if (AcquirerService.logging) {
                logger.info("Call from" + urlHolder.getCallerAddress() + "with data:\n" + providerRequest);
            }
            
            /////////////////////////////////////////////////////////////////////////////////////////
            // Each method has its own servlet in this setup but that is just an option            //
            /////////////////////////////////////////////////////////////////////////////////////////
            JSONObjectWriter providerResponse = processCall(urlHolder, providerRequest);

            if (AcquirerService.logging) {
                logger.info("Responded to caller"  + urlHolder.getCallerAddress() + "with data:\n" + providerResponse);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            HttpSupport.writeJsonData(response, providerResponse);

        } catch (Exception e) {
            /////////////////////////////////////////////////////////////////////////////////////////
            // Hard error return. Note that we return a clear-text message in the response body.   //
            // Having specific error message syntax for hard errors only complicates things since  //
            // there will always be the dreadful "internal server error" to deal with as well as   //
            // general connectivity problems.                                                      //
            /////////////////////////////////////////////////////////////////////////////////////////
            String message = (urlHolder == null ? "" : "Source" + urlHolder.getCallerAddress()) + e.getMessage();
            logger.log(Level.SEVERE, HttpSupport.getStackTrace(e, message));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
        }
    }
}
