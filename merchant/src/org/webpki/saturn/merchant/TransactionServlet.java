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

import java.math.BigDecimal;

import java.net.URL;

import java.security.GeneralSecurityException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

import org.webpki.util.ArrayUtil;

import org.webpki.webutil.ServletUtil;

import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.ProviderUserResponse;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends HttpServlet implements BaseProperties, MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(TransactionServlet.class.getCanonicalName());
    
    static final int TIMEOUT_FOR_REQUEST = 5000;
    
    static final byte[] NORMAL_RETURN = {'{','}'};

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
    
    JSONObjectReader fetchJSONData(HTTPSWrapper wrap, UrlHolder urlHolder) throws IOException {
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
            logger.info("Call to " + urlHolder.getUrl() + " returned:\n" + result);
        }
        return result;
    }

    JSONObjectReader postData(UrlHolder urlHolder, JSONObjectWriter request) throws IOException {
        if (MerchantService.logging) {
            logger.info("About to call " + urlHolder.getUrl() + " with data:\n" + request);
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(urlHolder.getUrl()), request.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        return fetchJSONData(wrap, urlHolder);
    }

    JSONObjectReader getData(UrlHolder urlHolder) throws IOException {
        if (MerchantService.logging) {
            logger.info("About to call " + urlHolder.getUrl());
        }
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJSONData(wrap, urlHolder);
    }

    // The purpose of this class is to enable URL information in exceptions

    class UrlHolder {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    Authority payeeProviderAuthority;
    
    synchronized void updatePayeeProviderAuthority(UrlHolder urlHolder) throws IOException {
        payeeProviderAuthority = new Authority(getData(urlHolder), urlHolder.getUrl());
        // Verify that the claimed authority belongs to a known payment provider network
        payeeProviderAuthority.getSignatureDecoder().verify(MerchantService.paymentRoot);
    }

    void returnJsonData(HttpServletResponse response, byte[] data) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(data);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = new UrlHolder();
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                returnJsonData(response, NORMAL_RETURN);
                return;
             }

            // Reading the Wallet response
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader walletResponse = JSONParser.parse(ServletUtil.getData(request));
            byte[] requestHash = (byte[]) session.getAttribute(UserPaymentServlet.REQUEST_HASH_SESSION_ATTR);
            if (MerchantService.logging) {
                logger.info("Received from wallet:\n" + walletResponse);
            }

            // Do we have web debug mode?
            DebugData debugData = null;
            boolean debug = UserPaymentServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
            if (debug) {
                debugData = (DebugData) session.getAttribute(DEBUG_DATA_SESSION_ATTR);
                debugData.walletResponse = walletResponse;
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = new PayerAuthorization(walletResponse);

            // Get the original payment request
            PaymentRequest paymentRequest = payerAuthorization.getPaymentRequest();
            if (!ArrayUtil.compare(requestHash, paymentRequest.getRequestHash())) {
                throw new IOException("Incorrect \"" + REQUEST_HASH_JSON + "\"");
            }
           
            // Basic credit is only applicable to account2account operations
            boolean acquirerBased = payerAuthorization.getAccountType().isAcquirerBased();
            boolean basicCredit = !UserPaymentServlet.getOption(session, RESERVE_MODE_SESSION_ATTR) &&
                                  !acquirerBased;

            // Attest the user's encrypted authorization to show "intent"
            JSONObjectWriter reserveOrBasicRequest =
                ReserveOrBasicRequest.encode(basicCredit,
                                             payerAuthorization.getProviderAuthorityUrl(),
                                             payerAuthorization.getAccountType(),
                                             walletResponse.getObject(AUTHORIZATION_DATA_JSON),
                                             request.getRemoteAddr(),
                                             paymentRequest,
                                             MerchantService.acquirerAuthorityUrl, // Card only
                                             Expires.inMinutes(30), // Reserve only
                                             MerchantService.merchantKey);

            // Now we need to find out where to send the request
            urlHolder.setUrl(MerchantService.payeeProviderAuthorityUrl);
            if (payeeProviderAuthority == null) {
               updatePayeeProviderAuthority(urlHolder);
            }

            if (debug) {
                debugData.providerAuthority = payeeProviderAuthority.getRoot();
                debugData.basicCredit = basicCredit;
            }


            // Call the payee bank
            urlHolder.setUrl(payeeProviderAuthority.getTransactionUrl());
            JSONObjectReader resultMessage = postData(urlHolder, reserveOrBasicRequest);

            if (debug) {
                debugData.reserveOrBasicRequest = reserveOrBasicRequest;
                debugData.reserveOrBasicResponse = resultMessage;
            }

            if (resultMessage.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PROVIDER_USER_RESPONSE.toString())) {
                if (debug) {
                    debugData.softReserveOrBasicError = true;
                }
                // Parse for syntax only
                new ProviderUserResponse(resultMessage);
                returnJsonData(response,
                               resultMessage.serializeJSONObject(JSONOutputFormats.NORMALIZED));
                return;
            }
        
            // Additional consistency checking
            ReserveOrBasicResponse reserveOrBasicResponse = new ReserveOrBasicResponse(resultMessage);

            // No error return, then we can verify the response fully
            reserveOrBasicResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
       
            // Create a viewable response
            ResultData resultData = new ResultData();
            resultData.amount = paymentRequest.getAmount();
            resultData.referenceId = paymentRequest.getReferenceId();
            resultData.currency = paymentRequest.getCurrency();
            resultData.accountType = reserveOrBasicResponse.getPayerAccountType();
            resultData.accountReference =  acquirerBased ? // = Card
                    AuthorizationData.formatCardNumber(reserveOrBasicResponse.getAccountReference())
                                                 :
                        reserveOrBasicResponse.getAccountReference();  // Currently "unmoderated" account
            session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);

            // Two-phase operation: perform the final step
            if (!basicCredit) {
                processFinalize(reserveOrBasicResponse,
                                paymentRequest.getAmount(),
                                urlHolder,
                                acquirerBased,
                                debugData);
            }
 
            logger.info("Successful authorization of request: " + paymentRequest.getReferenceId());
            /////////////////////////////////////////////////////////////////////////////////////////
            // Normal return                                                                       //
            /////////////////////////////////////////////////////////////////////////////////////////
            returnJsonData(response, NORMAL_RETURN);

        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            JSONObjectWriter userError = WalletAlertMessage.encode("A technical error occurred.<br>" +
                                                                   "Please try again or contact support.");
            returnJsonData(response, userError.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        }
    }

    void processFinalize(ReserveOrBasicResponse reserveOrBasicResponse,
                         BigDecimal actualAmount,
                         UrlHolder urlHolder,
                         boolean acquirerBased,
                         DebugData debugData)
    throws IOException, GeneralSecurityException {
        if (acquirerBased) {
            // Lookup of configured acquirer authority.  This information is preferably cached
            urlHolder.setUrl(MerchantService.acquirerAuthorityUrl);
            Authority acquirerAuthority = new Authority(getData(urlHolder), MerchantService.acquirerAuthorityUrl);
            urlHolder.setUrl(acquirerAuthority.getTransactionUrl());
            if (debugData != null) {
                debugData.acquirerMode = true;
                debugData.acquirerAuthority = acquirerAuthority.getRoot();
            }
        }

        JSONObjectWriter finalizeRequest = FinalizeRequest.encode(reserveOrBasicResponse,
                                                                  actualAmount,
                                                                  MerchantService.getReferenceId(),
                                                                  MerchantService.merchantKey);
        // Call the payment provider or acquirer
        JSONObjectReader response = postData(urlHolder, finalizeRequest);

        if (debugData != null) {
            debugData.finalizeRequest = finalizeRequest;
            debugData.finalizeResponse = response;
        }
        
        if (acquirerBased) {
            FinalizeCardpayResponse finalizeCardpayResponse = new FinalizeCardpayResponse(response);
            finalizeCardpayResponse.getSignatureDecoder().verify(MerchantService.acquirerRoot);
        } else {
            FinalizeCreditResponse finalizeCreditResponse = new FinalizeCreditResponse(response);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
