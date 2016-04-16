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
import org.webpki.saturn.common.CertificatePathCompare;
import org.webpki.saturn.common.ErrorReturn;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.Authority;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.FinalizeResponse;
import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.RequestHash;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
import org.webpki.saturn.common.UserMessageResponse;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(TransactionServlet.class.getCanonicalName());
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

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
    
    JSONObjectReader fetchJSONData(HTTPSWrapper wrap) throws IOException {
        if (wrap.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("HTTP error " + wrap.getResponseCode() + " " + wrap.getResponseMessage() + ": " +
                                  (wrap.getData() == null ? "No other information available" : wrap.getDataUTF8()));
        }
        // We expect JSON, yes
        if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getRawContentType());
        }
        return JSONParser.parse(wrap.getData());        
    }

    JSONObjectReader postData(URLHolder urlHolder, byte[] data) throws IOException {
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(false);
        wrap.makePostRequest(portFilter(urlHolder.getUrl()), data);
        return fetchJSONData(wrap);
    }

    JSONObjectReader getData(URLHolder urlHolder) throws IOException {
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setRequireSuccess(false);
        wrap.makeGetRequest(portFilter(urlHolder.getUrl()));
        return fetchJSONData(wrap);
    }

    // The purpose of this class is to enable URL information in exceptions

    class URLHolder {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    Authority payeeProviderAuthority;
    
    synchronized void updatePayeeProviderAuthority(URLHolder urlHolder) throws IOException {
        JSONObjectReader resultMessage = getData(urlHolder);
        logger.info("Returned from payee provider [" + urlHolder.getUrl() + "]:\n" + resultMessage);
        payeeProviderAuthority = new Authority(resultMessage, urlHolder.getUrl());
        // Verify that the claimed authority belongs to a known payment provider network
        payeeProviderAuthority.getSignatureDecoder().verify(MerchantService.paymentRoot);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        URLHolder urlHolder = new URLHolder();
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                ErrorServlet.sessionTimeout(response);
                return;
             }

            // Reading the wallet response which is FORM POSTed
            byte[] requestHash = (byte[]) session.getAttribute(UserPaymentServlet.REQUEST_HASH_SESSION_ATTR);
            request.setCharacterEncoding("UTF-8");
            JSONObjectReader userAuthorization = JSONParser.parse(request.getParameter(UserPaymentServlet.AUTHDATA_FORM_ATTR));
            logger.info("Received from wallet:\n" + userAuthorization);

            // Do we have web debug mode?
            DebugData debugData = null;
            boolean debug = UserPaymentServlet.getOption(session, HomeServlet.DEBUG_MODE_SESSION_ATTR);
            if (debug) {
                debugData = (DebugData) session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR);
                debugData.WalletInitialized = request.getParameter(UserPaymentServlet.INITMSG_FORM_ATTR).getBytes("UTF-8");
                debugData.walletResponse = userAuthorization.serializeJSONObject(JSONOutputFormats.NORMALIZED);
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = new PayerAuthorization(userAuthorization);

            // Get the original payment request
            PaymentRequest paymentRequest = payerAuthorization.getPaymentRequest();
            if (!ArrayUtil.compare(requestHash, paymentRequest.getRequestHash())) {
                throw new IOException("Incorrect \"" + REQUEST_HASH_JSON + "\"");
            }
           
            // Basic credit is only applicable to account2account operations
            boolean acquirerBased = payerAuthorization.getAccountType().isAcquirerBased();
            boolean basicCredit = !UserPaymentServlet.getOption(session, HomeServlet.RESERVE_MODE_SESSION_ATTR) &&
                                  !acquirerBased;

            // Attest the user's encrypted authorization to show "intent"
            JSONObjectWriter providerRequest =
                ReserveOrBasicRequest.encode(basicCredit,
                                             payerAuthorization.getProviderAuthorityUrl(),
                                             payerAuthorization.getAccountType(),
                                             userAuthorization.getObject(AUTHORIZATION_DATA_JSON),
                                             request.getRemoteAddr(),
                                             paymentRequest,
                                             basicCredit ? null : Expires.inMinutes(30),
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

            urlHolder.setUrl(payeeProviderAuthority.getTransactionUrl());
            logger.info("About to send to payee provider [" + urlHolder.getUrl() + "]:\n" + providerRequest);

            // Call the payee bank
            byte[] providerRequestBlob = providerRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
            JSONObjectReader resultMessage = postData(urlHolder, providerRequestBlob);
            logger.info("Returned from payee provider [" + urlHolder.getUrl() + "]:\n" + resultMessage);

            if (debug) {
                debugData.reserveOrBasicRequest = providerRequestBlob;
                debugData.reserveOrBasicResponse = resultMessage;
            }

            if (resultMessage.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.USER_MESSAGE_RESPONSE.toString())) {
                if (debug) {
                    debugData.softReserveOrBasicError = true;
                }
                UserMessageResponse userMessageResponse = new UserMessageResponse(resultMessage);
                HTML.paymentError(response, debug, userMessageResponse.getText());
                return;
            }
        
            // Additional consistency checking
            ReserveOrBasicResponse reserveOrBasicResponse = new ReserveOrBasicResponse(resultMessage);
/*
            if (reserveOrBasicResponse.isBasicCredit() != basicCredit) {
                throw new IOException("Response basic/reserve mode doesn't match request");
            }
*/
            // In addition to hard errors, there are a few "normal" errors which preferably would
            // be dealt with in more user-oriented fashion.
/*
            if (!reserveOrBasicResponse.success()) {
                if (debug) {
                    debugData.softReserveOrBasicError = true;
                }
                HTML.paymentError(response, debug, reserveOrBasicResponse.getErrorReturn());
                return;
            }

            if (!reserveOrBasicResponse.getAccountType().equals(payerAuthorization.getAccountType().getTypeUri())) {
                throw new IOException("Response account type doesn't match request");
            }
*/
            // No error return, then we can verify the response fully
            reserveOrBasicResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
            
            TransactionResponse transactionResponse = reserveOrBasicResponse.getTransactionResponse();
            TransactionRequest transactionRequest = transactionResponse.getTransactionRequest();
            ReserveOrBasicRequest reserveOrBasicRequest = transactionRequest.getReserveOrBasicRequest();
            if (!ArrayUtil.compare(reserveOrBasicRequest.getPaymentRequest().getRequestHash(), requestHash)) {
                throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
            }
            /*

            if (!reserveOrBasicResponse.isBasicCredit()) {
                // Two-phase operation: perform the final step
                ErrorReturn errorReturn = processFinalize (reserveOrBasicResponse, urlHolder, debugData);

                // Is there a soft error?  Return it to the user
                if (errorReturn != null) {
                    if (debug) {
                        debugData.softFinalizeError = true;
                    }
                    HTML.paymentError(response, debug, errorReturn);
                    return;
                }
            }
*/            

            logger.info("Successful authorization of request: " + paymentRequest.getReferenceId());
            HTML.resultPage(response,
                            debug,
                            paymentRequest,
                            reserveOrBasicRequest.getPayerAccountType(),
                            acquirerBased ? // = Card
                                AuthorizationData.formatCardNumber(transactionResponse.getAccountReference())
                                                          :
                                transactionResponse.getAccountReference());  // Currently "unmoderated" account

        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            ErrorServlet.systemFail(response, message);
        }
    }

    ErrorReturn processFinalize(ReserveOrBasicResponse bankResponse, URLHolder urlHolder, DebugData debugData)
    throws IOException, GeneralSecurityException {
/*
        String target = "provider";
        Authority acquirerAuthority = null;
        if (!bankResponse.isAccount2Account()) {
            target = "acquirer";
            // Lookup indicated acquirer authority
            urlHolder.setUrl(MerchantService.acquirerAuthorityUrl);
            acquirerAuthority = new Authority(getData(urlHolder), MerchantService.acquirerAuthorityUrl);
            urlHolder.setUrl(acquirerAuthority.getTransactionUrl());
            if (debugData != null) {
                debugData.acquirerMode = true;
                debugData.acquirerAuthority = acquirerAuthority.getRoot();
            }
        }

        JSONObjectWriter finalizeRequest = FinalizeRequest.encode(bankResponse,
                                                                  bankResponse.getPaymentRequest().getAmount(),
                                                                  UserPaymentServlet.getReferenceId(),
                                                                  MerchantService.merchantKey);
        logger.info("About to send to " + target + " [" + urlHolder.getUrl() + "]:\n" + finalizeRequest);

        // Call the payment provider or acquirer
        byte[] sentFinalize = finalizeRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        JSONObjectReader response = postData(urlHolder, sentFinalize);
        logger.info("Received from " + target + " [" + urlHolder.getUrl() + "]:\n" + response);

        if (debugData != null) {
            debugData.finalizeRequest = sentFinalize;
            debugData.finalizeResponse = response;
        }
        
        FinalizeResponse finalizeResponse = new FinalizeResponse(response);

        // Is there a soft error?  Then there is no more to do
        if (!finalizeResponse.success()) {
            return finalizeResponse.getErrorReturn();
        }

        // Check signature origins
        CertificatePathCompare.compareCertificatePaths(bankResponse.isAccount2Account() ?
                                                bankResponse.getSignatureDecoder().getCertificatePath()
                                                                                        :
                                                acquirerAuthority.getSignatureDecoder().getCertificatePath(),                                         
                                                      finalizeResponse.getSignatureDecoder().getCertificatePath());

        if (!ArrayUtil.compare(RequestHash.getRequestHash(sentFinalize), finalizeResponse.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
        }
*/
        return null;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
