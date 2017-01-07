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

import java.util.Date;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AccountDescriptor;
import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.CardPaymentRequest;
import org.webpki.saturn.common.CardPaymentResponse;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.ProviderUserResponse;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;

    private static final String MERCHANT_ACCOUNT_TYPE = "https://swift.com";

    private static final String MERCHANT_ACCOUNT_ID = "IBAN:FR7630004003200001019471656";
    
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
        // Slightly different flows for card- and bank-to-bank authorizations
        boolean cardPayment = payerAuthorization.getAccountType().isCardPayment();
        
        if (!cardPayment && session.getAttribute(GAS_STATION_SESSION_ATTR) != null) {
            JSONObjectWriter notImplemented = WalletAlertMessage.encode(
                    "This card type doesn't support gas station payments yet...<p>Please select another card.");
            returnJsonData(response, notImplemented);
            return false;
           
        }
 
        // ugly fix to cope with local installation
        String providerAuthorityUrl = payerAuthorization.getProviderAuthorityUrl();
        if (MerchantService.payeeProviderAuthorityUrl.contains("localhost")) {
            URL url = new URL(MerchantService.payeeProviderAuthorityUrl);
            providerAuthorityUrl = new URL(url.getProtocol(),
                                           url.getHost(), 
                                           url.getPort(),
                                           new URL(providerAuthorityUrl).getFile()).toExternalForm();
        }

        // Lookup of Payer's bank
        urlHolder.setUrl(providerAuthorityUrl);
        ProviderAuthority providerAuthority = getProviderAuthority(urlHolder);
        urlHolder.setUrl(null);

        if (debug) {
            debugData.providerAuthority = providerAuthority.getRoot();
            debugData.basicCredit = !cardPayment;
            debugData.acquirerMode = cardPayment;
        }

        AccountDescriptor accountDescriptor = null;
        if (!cardPayment) {
            for (String accountType : providerAuthority.getProviderAccountTypes(true)) {
                if (accountType.equals(MERCHANT_ACCOUNT_TYPE)) {
                    accountDescriptor = new AccountDescriptor(accountType, MERCHANT_ACCOUNT_ID);
                    break;
                }
            }
            if (accountDescriptor == null) {
                throw new IOException("No matching account type: " + providerAuthority.getProviderAccountTypes(true));
            }
        }

        // Card payments are actually reservations and they are not indefinite
        Date expires = cardPayment ? Expires.inMinutes(10) : null;

        String payeeAuthorityUrl = cardPayment ? MerchantService.payeeAcquirerAuthorityUrl : MerchantService.payeeProviderAuthorityUrl;

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter authorizationRequest =
            AuthorizationRequest.encode(MerchantService.testMode,
                                        payeeAuthorityUrl,
                                        payerAuthorization.getAccountType(),
                                        walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON),
                                        request.getRemoteAddr(),
                                        paymentRequest,
                                        accountDescriptor,
                                        MerchantService.getReferenceId(),
                                        expires,
                                        MerchantService.paymentNetworks.get(paymentRequest.getPublicKey()).signer);

        // Call Payer bank
        urlHolder.setUrl(providerAuthority.getServiceUrl());
        JSONObjectReader resultMessage = postData(urlHolder, authorizationRequest);
        urlHolder.setUrl(null);

        if (debug) {
            debugData.authorizationRequest = makeReader(authorizationRequest);
            urlHolder.setUrl(payeeAuthorityUrl);
            PayeeAuthority payeeAuthority = getPayeeAuthority(urlHolder);
            debugData.payeeAuthority = payeeAuthority.getRoot();
            urlHolder.setUrl(payeeAuthority.getProviderAuthorityUrl());
            debugData.payeeProviderAuthority = getProviderAuthority(urlHolder).getRoot();
            urlHolder.setUrl(null);
            debugData.authorizationResponse = resultMessage;
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
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(resultMessage);

        // No error return, then we can verify the response fully
        authorizationResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
   
        // Two-phase operation: perform the final step
        if (cardPayment && session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            processCardPayment(authorizationResponse,
                               paymentRequest.getAmount(),  // Just a copy since we don't have a complete scenario                                
                               urlHolder,
                               debugData);
        }

        // Create a viewable response
        ResultData resultData = new ResultData();
        resultData.amount = paymentRequest.getAmount();  // Gas Station will upgrade amount
        resultData.referenceId = paymentRequest.getReferenceId();
        resultData.currency = paymentRequest.getCurrency();
        resultData.accountType = authorizationResponse.getAuthorizationRequest().getPayerAccountType();
        String accountReference = authorizationResponse.getAccountReference();
        if (cardPayment) {
            accountReference = AuthorizationData.formatCardNumber(accountReference);
        }
        resultData.accountReference = accountReference;
        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);
        
        // Gas Station: Save reservation part for future fulfillment
        if (session.getAttribute(GAS_STATION_SESSION_ATTR) != null) {
            session.setAttribute(GAS_STATION_RES_SESSION_ATTR, authorizationResponse);
        }
        return true;
    }

    static void processCardPayment(AuthorizationResponse authorizationResponse,
                                   BigDecimal actualAmount,
                                   UrlHolder urlHolder,
                                   DebugData debugData) throws IOException {
        // Lookup of acquirer authority
        urlHolder.setUrl(MerchantService.payeeAcquirerAuthorityUrl);
        urlHolder.setUrl(getPayeeAuthority(urlHolder).getProviderAuthorityUrl());
        ProviderAuthority acquirerAuthority = getProviderAuthority(urlHolder);
        urlHolder.setUrl(acquirerAuthority.getServiceUrl());
        if (debugData != null) {
            debugData.acquirerAuthority = acquirerAuthority.getRoot();
        }

        JSONObjectWriter cardPaymentRequest =
            CardPaymentRequest.encode(authorizationResponse,
                                      actualAmount,
                                      MerchantService.getReferenceId(),
                                      MerchantService.paymentNetworks.get(authorizationResponse
                                                                              .getAuthorizationRequest()
                                                                                  .getPublicKey()).signer);
        // Call the Acquirer
        JSONObjectReader response = postData(urlHolder, cardPaymentRequest);

        if (debugData != null) {
            debugData.cardPaymentRequest = makeReader(cardPaymentRequest);
            debugData.cardPaymentResponse = response;
        }
        
        CardPaymentResponse cardPaymentResponse = new CardPaymentResponse(response);
        cardPaymentResponse.getSignatureDecoder().verify(MerchantService.acquirerRoot);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
