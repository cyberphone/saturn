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
package org.webpki.saturn.bank;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.Connection;

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AuthorizationDataDecoder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.UserChallengeItem;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.ProviderUserResponseEncoder;
import org.webpki.saturn.common.UrlHolder;

//////////////////////////////////////////////////////////////////////////
// This is the core Payment Provider (Bank) processing servlet          //
//////////////////////////////////////////////////////////////////////////

public abstract class ProcessingBaseServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ProcessingBaseServlet.class.getCanonicalName());
    
    static final long MAX_CLIENT_CLOCK_SKEW        = 5 * 60 * 1000;
    static final long MAX_CLIENT_AUTH_AGE          = 20 * 60 * 1000;
    
    // Just a few demo values

    // Standard limit requiring RBA
    static final BigDecimal DEMO_RBA_LIMIT          = new BigDecimal("1000.00");
    
    // Clear text UI test (3 cars + 5 ice-cream)
    static final BigDecimal DEMO_RBA_LIMIT_CT       = new BigDecimal("1668.00");

    // 1 car + 7 ice-creams
    static final BigDecimal DEMO_FINGER_PRINT_TEST  = new BigDecimal("575.25");

    static final String RBA_PARM_MOTHER             = "mother";
    static final String MOTHER_NAME                 = "smith";
    
    static String formatReferenceId(int referenceId) {
        return String.format("#%010d", referenceId);
    }
    
    static int decodeReferenceId(String referenceId) throws IOException {
        int r = Integer.valueOf(referenceId.substring(1));
        if (!formatReferenceId(r).equals(referenceId)) {
            throw new IOException("Bad referenceId: " + referenceId);
        }
        return r;
    }

    static String amountInHtml(PaymentRequestDecoder paymentRequest, BigDecimal amount) 
    throws IOException {
        return "<span style=\"font-weight:bold;white-space:nowrap\">" + 
               paymentRequest.getCurrency().amountToDisplayString(amount, true) +
               "</span>";
    }

    static JSONObjectWriter createProviderUserResponse(
            String text,
            UserChallengeItem[] optionalUserChallengeItems,
            AuthorizationDataDecoder authorizationData)
    throws IOException, GeneralSecurityException {
        return ProviderUserResponseEncoder.encode(BankService.bankCommonName,
                                                  text,
                                                  optionalUserChallengeItems,
                                                  authorizationData.getDataEncryptionKey(),
                                                  authorizationData.getContentEncryptionAlgorithm());
    }

    abstract JSONObjectWriter processCall(UrlHolder urlHolder, 
                                          JSONObjectReader providerRequest,
                                          Connection connection) throws Exception;
    

    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        UrlHolder urlHolder = null;
        Connection connection = null;
        try {
// TODO Here there should be a generic input/output cache to provide idempotent operation
// because you don't want a retried request to pass the transaction mechanism.

            urlHolder = new UrlHolder(request);

            /////////////////////////////////////////////////////////////////////////////////////////
            // Must be tagged as JSON content and parse as well                                    //
            /////////////////////////////////////////////////////////////////////////////////////////
            JSONObjectReader providerRequest = HttpSupport.readJsonData(request);

            /////////////////////////////////////////////////////////////////////////////////////////
            // First control passed...                                                             //
            /////////////////////////////////////////////////////////////////////////////////////////
            if (BankService.logging) {
                logger.info("Call from" + 
                            urlHolder.getCallerAddress() + 
                            "with data:\n" +
                            providerRequest);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // Each method has its own servlet in this setup but that is just an option            //
            /////////////////////////////////////////////////////////////////////////////////////////
            if (BankService.jdbcDataSource != null) {
                connection = BankService.jdbcDataSource.getConnection();
            }
            JSONObjectWriter providerResponse = processCall(urlHolder, 
                                                            providerRequest,
                                                            connection);
            if (connection != null) {
                connection.close();
                connection = null;
            }

            if (BankService.logging) {
                logger.info("Responded to caller"  + urlHolder.getCallerAddress() + 
                            "with data:\n" + providerResponse);
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
            BankService.rejectedTransactions++;
            String message = (urlHolder == null ? "" : "From" + urlHolder.getCallerAddress() +
                              (urlHolder.getUrl() == null ? 
                                      "" : "URL=" + urlHolder.getUrl()) + "\n") + e.getMessage();
            if (!(e instanceof NormalException)) {
                logger.log(Level.SEVERE, HttpSupport.getStackTrace(e, message));
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception sql) {}
            }
        }
    }
}
