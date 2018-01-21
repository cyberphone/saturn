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

import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
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

    private static final String MERCHANT_PAYMENT_METHOD = "https://sepa.payments.org";

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
        boolean cardPayment = payerAuthorization.getPaymentMethod().isCardPayment();
        String payeeAuthorityUrl = cardPayment ? MerchantService.payeeAcquirerAuthorityUrl : MerchantService.payeeProviderAuthorityUrl;
        
        // Lookup of self (since it is provided by an external party)
        PayeeAuthority payeeAuthority = MerchantService.externalCalls.getPayeeAuthority(urlHolder, payeeAuthorityUrl);

        // Strictly put not entirely necessary (YET...)
        ProviderAuthority ownProviderAuthority = 
            MerchantService.externalCalls.getProviderAuthority(urlHolder, payeeAuthority.getProviderAuthorityUrl());
        
        // ugly fix to cope with local installation
        String providerAuthorityUrl = payerAuthorization.getProviderAuthorityUrl();
        if (MerchantService.localInstallation) {
            URL url = new URL(MerchantService.payeeProviderAuthorityUrl);
            providerAuthorityUrl = new URL(url.getProtocol(),
                                           url.getHost(), 
                                           url.getPort(),
                                           new URL(providerAuthorityUrl).getFile()).toExternalForm();
        }

        // Lookup of Payer's bank
        ProviderAuthority providerAuthority = 
            MerchantService.externalCalls.getProviderAuthority(urlHolder, providerAuthorityUrl);

        if (debug) {
            debugData.providerAuthority = providerAuthority.getRoot();
            debugData.basicCredit = !cardPayment;
            debugData.acquirerMode = cardPayment;
        }
        

        if (session.getAttribute(GAS_STATION_SESSION_ATTR) != null &&
                !cardPayment && getHybridModeUrl(providerAuthority) == null) {
            JSONObjectWriter notImplemented = WalletAlertMessage.encode(
                    "This card type doesn't support gas station payments yet...<p>Please select another card.");
            HttpSupport.writeJsonData(response, notImplemented);
            return false;
           
        }
 
        AuthorizationRequest.PaymentMethodEncoder paymentMethodEncoder = null;
        TransactionOperation transactionOperation = new TransactionOperation();
        if (cardPayment) {
            transactionOperation.urlToCall = ownProviderAuthority.getServiceUrl();
            transactionOperation.verifier = MerchantService.acquirerRoot;
            if (debugData != null) {
                debugData.acquirerAuthority = ownProviderAuthority.getRoot();
            }
            paymentMethodEncoder = MerchantService.superCardPaymentEncoder;
        } else {
            for (String paymentMethod : providerAuthority.getPaymentMethods()) {
                if (paymentMethod.equals(MERCHANT_PAYMENT_METHOD)) {
                    paymentMethodEncoder = payeeAuthority.getPayeeCoreProperties().getAccountHashes() == null ?
                            MerchantService.sepaPlainAccount : MerchantService.sepaVerifiableAccount;
                    break;
                }
            }
            if (paymentMethodEncoder == null) {
                throw new IOException("No matching account type: " + providerAuthority.getPaymentMethods());
            }
        }

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter authorizationRequest =
            AuthorizationRequest.encode(MerchantService.testMode,
                                        providerAuthority.getServiceUrl(),
                                        payeeAuthorityUrl,
                                        payerAuthorization.getPaymentMethod(),
                                        walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON),
                                        request.getRemoteAddr(),
                                        paymentRequest,
                                        paymentMethodEncoder,
                                        MerchantService.getReferenceId(),
                                        MerchantService.paymentNetworks.get(paymentRequest
                                                                                .getSignatureDecoder()
                                                                                    .getPublicKey())
                                                                                        .signer);

        // Call Payer bank
        JSONObjectReader resultMessage = MerchantService.externalCalls.postJsonData(urlHolder,
                                                                                    providerAuthority.getServiceUrl(),
                                                                                    authorizationRequest);

        if (debug) {
            debugData.authorizationRequest = makeReader(authorizationRequest);
            debugData.payeeAuthority = payeeAuthority.getRoot();
            debugData.payeeProviderAuthority = ownProviderAuthority.getRoot();
            debugData.authorizationResponse = resultMessage;
        }

        if (resultMessage.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PROVIDER_USER_RESPONSE.toString())) {
            if (debug) {
                debugData.softAuthorizationError = true;
            }
            // Parse for syntax only
            new ProviderUserResponse(resultMessage);
            HttpSupport.writeJsonData(response, new JSONObjectWriter(resultMessage));
            return false;
        }
    
        // Additional consistency checking
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(resultMessage);

        // No error return, then we can verify the response fully
        authorizationResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
        transactionOperation.authorizationResponse = authorizationResponse;
   
        // Create a viewable response
        ResultData resultData = new ResultData();
        resultData.amount = paymentRequest.getAmount();  // Gas Station will upgrade amount
        resultData.referenceId = paymentRequest.getReferenceId();
        resultData.currency = paymentRequest.getCurrency();
        resultData.paymentMethod = authorizationResponse.getAuthorizationRequest().getPaymentMethod();
        String accountReference = authorizationResponse.getAccountReference();
        if (cardPayment) {
            accountReference = AuthorizationData.formatCardNumber(accountReference);
        }
        resultData.accountReference = accountReference;

        // Special treatment of the refund mode
        if (HomeServlet.getOption(session, REFUND_MODE_SESSION_ATTR)) {
            resultData.optionalRefund = authorizationResponse;
        }

        // Two-phase operation: perform the final step
        if (cardPayment && session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            resultData.transactionError =
                processTransaction(transactionOperation,
                                   // Just a copy since we don't have a complete scenario 
                                   paymentRequest.getAmount(),                                
                                   urlHolder,
                                   debugData);
        }

        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);
        
        // Gas Station: Save reservation part for future fulfillment
        if (session.getAttribute(GAS_STATION_SESSION_ATTR) != null) {
            if (!cardPayment) {
                transactionOperation.urlToCall = getHybridModeUrl(providerAuthority);
                transactionOperation.verifier = MerchantService.paymentRoot;
                if (debug) {
                    debugData.hybridMode = true;
                }
            }
            session.setAttribute(GAS_STATION_RES_SESSION_ATTR, transactionOperation);
            if (debug) {
                debugData.gasStation = true;
            }
        }
        return true;
    }

    static TransactionResponse.ERROR processTransaction(TransactionOperation transactionOperation,
                                                        BigDecimal actualAmount,
                                                        UrlHolder urlHolder,
                                                        DebugData debugData) throws IOException {

        JSONObjectWriter transactionRequest =
            TransactionRequest.encode(transactionOperation.authorizationResponse,
                                      transactionOperation.urlToCall,
                                      actualAmount,
                                      MerchantService.getReferenceId(),
                                      MerchantService.paymentNetworks.get(transactionOperation.authorizationResponse
                                                                              .getAuthorizationRequest()
                                                                                  .getSignatureDecoder()
                                                                                      .getPublicKey()).signer);
        // Acquirer or Hybrid call
        JSONObjectReader response =
            MerchantService.externalCalls.postJsonData(urlHolder, transactionOperation.urlToCall, transactionRequest);

        if (debugData != null) {
            debugData.transactionRequest = makeReader(transactionRequest);
            debugData.transactionResponse = response;
        }
        
        TransactionResponse transactionResponse = new TransactionResponse(response);
        transactionResponse.getSignatureDecoder().verify(transactionOperation.verifier);

        return transactionResponse.getTransactionError();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
