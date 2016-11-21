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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.ProviderUserResponse;
import org.webpki.saturn.common.UrlHolder;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    @Override
    boolean processCall(JSONObjectReader walletResponse,
                        PaymentRequest paymentRequest, 
                        PayerAuthorization payerAuthorization,
                        HttpSession session,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        boolean debug, 
                        DebugData debugData, 
                        UrlHolder urlHolder) throws IOException, GeneralSecurityException {
        // Basic credit is only applicable to account2account operations
        boolean acquirerBased = payerAuthorization.getAccountType().isCardPayment();
        urlHolder.setUrl(acquirerBased ? MerchantService.payeeAcquirerAuthorityUrl : MerchantService.payeeProviderAuthorityUrl);
        urlHolder.setUrl(getPayeeAuthority(urlHolder).getProviderAuthorityUrl());
        ProviderAuthority payeeProviderAuthority = getProviderAuthority(urlHolder);
        urlHolder.setUrl(null);

        boolean basicCredit = !HomeServlet.getOption(session, RESERVE_MODE_SESSION_ATTR) && !acquirerBased;

        // ugly fix to cope with local installation
        String payerProviderAuthorityUrl = payerAuthorization.getProviderAuthorityUrl();
        if (MerchantService.payeeProviderAuthorityUrl.contains("localhost")) {
            URL url = new URL(MerchantService.payeeProviderAuthorityUrl);
            payerProviderAuthorityUrl = new URL(url.getProtocol(),
                                                url.getHost(), 
                                                url.getPort(),
                                                new URL(payerProviderAuthorityUrl).getFile()).toExternalForm();
        }

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter reserveOrBasicRequest =
            ReserveOrBasicRequest.encode(basicCredit,
                                         payerProviderAuthorityUrl,
                                         payerAuthorization.getAccountType(),
                                         walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON),
                                         request.getRemoteAddr(),
                                         paymentRequest,
// TODO
                                         null, // Card only
                                         acquirerBased ? null : new AccountDescriptor(SATURN_WEB_PAY_CONTEXT_URI, "3407766"),
                                         Expires.inMinutes(30), // Reserve only
                                         MerchantService.paymentNetworks.get(paymentRequest.getPublicKey()).signer);

        if (debug) {
            debugData.providerAuthority = payeeProviderAuthority.getRoot();
            debugData.basicCredit = basicCredit;
            debugData.nativeMode = true;
        }

        // Call the payee bank
        urlHolder.setUrl(payeeProviderAuthority.getExtendedServiceUrl());
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
            returnJsonData(response, new JSONObjectWriter(resultMessage));
            return false;
        }
    
        // Additional consistency checking
        ReserveOrBasicResponse reserveOrBasicResponse = new ReserveOrBasicResponse(resultMessage);

        // No error return, then we can verify the response fully
        reserveOrBasicResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
   
        // Two-phase operation: perform the final step
        if (!basicCredit && session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            processFinalize(reserveOrBasicResponse,
                            paymentRequest.getAmount(),  // Just a copy since we don't have a complete scenario                                
                            urlHolder,
                            acquirerBased,
                            debugData);
        }
        
        // Create a viewable response
        ResultData resultData = new ResultData();
        resultData.amount = paymentRequest.getAmount();  // Gas Station will upgrade amount
        resultData.referenceId = paymentRequest.getReferenceId();
        resultData.currency = paymentRequest.getCurrency();
        resultData.accountType = reserveOrBasicResponse.getPayerAccountType();
        resultData.accountReference = reserveOrBasicResponse.getFormattedAccountReference();
        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);
        return true;
    }

    static void processFinalize(ReserveOrBasicResponse reserveOrBasicResponse,
                                BigDecimal actualAmount,
                                UrlHolder urlHolder,
                                boolean acquirerBased,
                                DebugData debugData)
    throws IOException, GeneralSecurityException {
        if (acquirerBased) {
            // Lookup of configured acquirer authority.  This information is preferably cached
// TODO
            urlHolder.setUrl(null);
            ProviderAuthority acquirerAuthority = new ProviderAuthority(getData(urlHolder), null);
            urlHolder.setUrl(acquirerAuthority.getExtendedServiceUrl());
            if (debugData != null) {
                debugData.acquirerMode = true;
                debugData.acquirerAuthority = acquirerAuthority.getRoot();
            }
        } else if (urlHolder.getUrl() == null) {
// TODO
            urlHolder.setUrl(null/* payeeProviderAuthority.getExtendedServiceUrl() */);
        }

        JSONObjectWriter finalizeRequest =
            FinalizeRequest.encode(reserveOrBasicResponse,
                                   actualAmount,
                                   MerchantService.getReferenceId(),
                                   MerchantService.paymentNetworks.get(reserveOrBasicResponse.getPublicKey()).signer);
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

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
