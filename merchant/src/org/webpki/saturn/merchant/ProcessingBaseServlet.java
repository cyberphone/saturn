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
package org.webpki.saturn.merchant;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.webutil.ServletUtil;

import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.ProviderAuthorityDecoder;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet holds core methods used by Saturn and Saturn "Native"   //
//////////////////////////////////////////////////////////////////////////

public abstract class ProcessingBaseServlet extends HttpServlet implements BaseProperties, MerchantSessionProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(ProcessingBaseServlet.class.getCanonicalName());
    
    static String getHybridModeUrl(ProviderAuthorityDecoder providerAuthority) throws IOException {
        JSONObjectReader extensions = providerAuthority.getExtensions();
        if (extensions != null) {
            return extensions.getStringConditional(KnownExtensions.HYBRID_PAYMENT);
        }
        return null;
    }

    abstract boolean processCall(MerchantDescriptor merchant, 
                                 PaymentRequestDecoder paymentRequest, 
                                 PayerAuthorization payerAuthorization,
                                 WalletRequest walletRequest,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean debug, 
                                 DebugData debugData, 
                                 UrlHolder urlHolder) throws Exception;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = new UrlHolder(request);
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                HttpSupport.writeJsonData(response, WalletAlertMessage.encode("The session appears to have timed out."));
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
                if (debugData.softAuthorizationError) {
                    debug = false;
                    debugData = null;
                } else {
                    debugData.walletResponse = walletResponse;
                }
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = 
                    new PayerAuthorization(walletResponse, MerchantService.wellKnownUrl);
            
            // Get merchant
            MerchantDescriptor merchant = MerchantService.getMerchant(session);
            
            // Check that we got a valid payment method
            if (!merchant.paymentMethods.containsKey(
                    payerAuthorization.getPaymentMethod().getPaymentMethodUrl())) {
                throw new IOException("Unexpected: " +
                                      payerAuthorization.getPaymentMethod().getPaymentMethodUrl());
            }
            // Restore WalletRequest
            WalletRequest walletRequest = 
                    (WalletRequest)session.getAttribute(WALLET_REQUEST_SESSION_ATTR);

            // Restore PaymentRequest
            PaymentRequestDecoder paymentRequest =
                    new PaymentRequestDecoder(new JSONObjectReader(walletRequest.paymentRequest));
            
            // The actual processing is here
            if (processCall(merchant,
                            paymentRequest, 
                            payerAuthorization,
                            walletRequest,
                            session,
                            request,
                            response,
                            debug,
                            debugData,
                            urlHolder)) {
           
                // This may be a QR session
                String qrId = (String) session.getAttribute(QR_SESSION_ID_ATTR);
                QRSessions.optionalSessionSetReady(qrId);
    
                logger.info("Successful authorization of request: " + paymentRequest.getReferenceId());
                logger.info("Receipt path=" + walletRequest.receiptUrl);
                /////////////////////////////////////////////////////////////////////////////////////////
                // Normal return                                                                       //
                /////////////////////////////////////////////////////////////////////////////////////////
                response.sendRedirect(qrId == null ? 
                        MerchantService.merchantBaseUrl + "/result" : SATURN_LOCAL_SUCCESS_URI);
            }

        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? 
                    "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, HttpSupport.getStackTrace(e, message));
            JSONObjectWriter userError = 
                    WalletAlertMessage.encode("An unexpected error occurred.<br>" +
                                              "Please try again or contact support.");
            HttpSupport.writeJsonData(response, userError);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
