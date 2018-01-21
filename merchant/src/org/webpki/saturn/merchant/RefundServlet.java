/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.RefundRequest;
import org.webpki.saturn.common.RefundResponse;

import org.webpki.saturn.common.UrlHolder;

//////////////////////////////////////////////////////////////////////////
// This servlet initiates a refund and shows the result                 //
//////////////////////////////////////////////////////////////////////////

public class RefundServlet extends HttpServlet implements MerchantProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(RefundServlet.class.getCanonicalName());
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UrlHolder urlHolder = new UrlHolder(request);
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                ErrorServlet.sessionTimeout(response);
                return;
            }
            ResultData resultData = (ResultData) session.getAttribute(RESULT_DATA_SESSION_ATTR);
            if (resultData == null) {
                ErrorServlet.systemFail(response, "Missing result data");
                return;
            }

            boolean debug = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
            
            AuthorizationRequest authorizationRequest = resultData.optionalRefund.getAuthorizationRequest();

            logger.info("Trying to refund Amount=" + resultData.amount.toString() +
                          " " + resultData.currency.toString() + 
                        ", Account=" + resultData.accountReference + 
                        ", Method=" + resultData.paymentMethod.getPaymentMethodUri());

            PayeeAuthority payeeAuthority = 
                MerchantService.externalCalls.getPayeeAuthority(urlHolder, authorizationRequest.getAuthorityUrl());
             ProviderAuthority providerAuthority =
                MerchantService.externalCalls.getProviderAuthority(urlHolder, payeeAuthority.getProviderAuthorityUrl());

            String refundUrl = providerAuthority.getExtensions() == null ? null :
                providerAuthority.getExtensions().getStringConditional(KnownExtensions.REFUND_REQUEST);
            if (refundUrl == null) {
                ErrorServlet.systemFail(response, "Selected payment method doesn't support refund!");
                return;
            }
            
            JSONObjectWriter refundRequestData = 
                RefundRequest.encode(resultData.optionalRefund,
                                     refundUrl,
                                     resultData.amount,
                                     MerchantService.getReferenceId(),
                                     MerchantService.paymentNetworks.get(authorizationRequest
                                                                             .getSignatureDecoder()
                                                                                 .getPublicKey()).signer);
        
            JSONObjectReader refundResponseData =
                MerchantService.externalCalls.postJsonData(urlHolder, refundUrl, refundRequestData);

            if (debug) {
                DebugData debugData = (DebugData) session.getAttribute(DEBUG_DATA_SESSION_ATTR);
                debugData.refundRequest = new JSONObjectReader(refundRequestData);
                debugData.refundResponse = refundResponseData;
            }

            RefundResponse refundResponse = new RefundResponse(refundResponseData);
            refundResponse.getSignatureDecoder().verify(
               authorizationRequest.getPaymentMethod().isCardPayment() ? 
                       MerchantService.acquirerRoot : MerchantService.paymentRoot);

            HTML.refundResultPage(response,
                                  debug,
                                  resultData);
        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, message, e);
            ErrorServlet.systemFail(response, message);
        }
    }
}
