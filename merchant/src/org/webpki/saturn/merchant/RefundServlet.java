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

import org.webpki.saturn.common.AuthorizationRequestDecoder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.KnownExtensions;
import org.webpki.saturn.common.PayeeAuthorityDecoder;
import org.webpki.saturn.common.ProviderAuthorityDecoder;
import org.webpki.saturn.common.RefundRequestEncoder;
import org.webpki.saturn.common.RefundResponseDecoder;
import org.webpki.saturn.common.UrlHolder;

//////////////////////////////////////////////////////////////////////////
// This servlet initiates a refund and shows the result.                //
// Note: authorization of refunds are supposed to be carried out by the //
// Payee. This would typically involve user authentication and logging. //
//////////////////////////////////////////////////////////////////////////

public class RefundServlet extends HttpServlet implements MerchantSessionProperties {

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
            MerchantDescriptor merchant = MerchantService.getMerchant(session);

            boolean debug = HomeServlet.getOption(session, DEBUG_MODE_SESSION_ATTR);
            
            AuthorizationRequestDecoder authorizationRequest = 
                    resultData.authorization.getAuthorizationResponse().getAuthorizationRequest();

            logger.info("Trying to refund Amount=" + resultData.authorization.getAmount().toString() +
                          " " + resultData.authorization.getCurrency().toString() + 
                        ", Account=" + resultData.authorization.getAccountReference() + 
                        ", Method=" + authorizationRequest.getPaymentMethod().getCommonName());

            PayeeAuthorityDecoder payeeAuthority = MerchantService.externalCalls.getPayeeAuthority(
                    urlHolder, authorizationRequest.getPayeeAuthorityUrl());
             ProviderAuthorityDecoder providerAuthority = MerchantService.externalCalls
                     .getProviderAuthority(urlHolder, payeeAuthority.getProviderAuthorityUrl());

            String refundUrl = providerAuthority.getExtensions() == null ? null :
                providerAuthority.getExtensions()
                    .getStringConditional(KnownExtensions.REFUND_REQUEST);
            if (refundUrl == null) {
                ErrorServlet.systemFail(response, 
                                        "Selected payment method doesn't support refund!");
                return;
            }
            
            // We do the assumption here that SEPA is always useful for receiving and sending money
            String context = new org.payments.sepa.SEPAAccountDataDecoder().getContext();
            PaymentMethodDescriptor pmd = merchant.paymentMethods.get(
                    authorizationRequest.getPaymentMethod().getPaymentMethodUrl());
            JSONObjectWriter refundRequestData =
                RefundRequestEncoder.encode(resultData.authorization.getAuthorizationResponse(),
                                     refundUrl,
                                     resultData.authorization.getAmount(),
                                     pmd.sourceAccounts.get(context),
                                     resultData.authorization.getPayeeReferenceId() + ".2",
                                     pmd.signer);
            JSONObjectReader refundResponseData =
                MerchantService.externalCalls.postJsonData(urlHolder, refundUrl, refundRequestData);

            if (debug) {
                DebugData debugData = (DebugData) session.getAttribute(DEBUG_DATA_SESSION_ATTR);
                debugData.refundRequest = new JSONObjectReader(refundRequestData);
                debugData.refundResponse = refundResponseData;
            }

            RefundResponseDecoder refundResponse = new RefundResponseDecoder(refundResponseData);
            refundResponse.getSignatureDecoder().verify(
               authorizationRequest.getPaymentMethod().isCardPayment() ? 
                       MerchantService.acquirerRoot : MerchantService.paymentRoot);

            HTML.refundResultPage(response,
                                  debug,
                                  resultData);
        } catch (Exception e) {
            String message = (urlHolder.getUrl() == null ? "" : "URL=" + urlHolder.getUrl() + "\n") + e.getMessage();
            logger.log(Level.SEVERE, HttpSupport.getStackTrace(e, message));
            ErrorServlet.systemFail(response, message);
        }
    }
}
