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

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.LinkedHashMap;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.AccountDataEncoder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.ProviderUserResponse;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    boolean processCall(MerchantDescriptor merchant,
                        JSONObjectReader walletResponse,
                        PaymentRequest paymentRequest, 
                        PayerAuthorization payerAuthorization,
                        HttpSession session,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        boolean debug, 
                        DebugData debugData, 
                        UrlHolder urlHolder) throws IOException, GeneralSecurityException {
        // Slightly different flows for card- and bank-to-bank authorizations
        PaymentMethods paymentMethodEnum = payerAuthorization.getPaymentMethod();
        boolean cardPayment = paymentMethodEnum.isCardPayment();
        String clientPaymentMethodUrl = paymentMethodEnum.getPaymentMethodUrl();
        String payeeAuthorityUrl = merchant.paymentMethods.get(clientPaymentMethodUrl).authorityUrl;
        
        // Lookup of self (since it is provided by an external party)
        PayeeAuthority payeeAuthority = MerchantService.externalCalls.getPayeeAuthority(urlHolder, payeeAuthorityUrl);

        // Strictly put not entirely necessary (YET...)
        ProviderAuthority ownProviderAuthority = 
            MerchantService.externalCalls.getProviderAuthority(urlHolder,
                    payeeAuthority.getProviderAuthorityUrl());
        
         // Lookup of Payer's bank
        ProviderAuthority providerAuthority = 
            MerchantService.externalCalls.getProviderAuthority(urlHolder, 
                    payerAuthorization.getProviderAuthorityUrl());

        if (debug) {
            debugData.providerAuthority = providerAuthority.getRoot();
            debugData.basicCredit = !cardPayment;
            debugData.acquirerMode = cardPayment;
        }

        if (session.getAttribute(GAS_STATION_SESSION_ATTR) != null &&
                !cardPayment && getHybridModeUrl(providerAuthority) == null) {
            JSONObjectWriter notImplemented = WalletAlertMessage.encode(
                    "This card type doesn't support gas station payments yet" +
                    "...<p>Please select another card.");
            HttpSupport.writeJsonData(response, notImplemented);
            return false;
           
        }
 
        TransactionOperation transactionOperation = new TransactionOperation();
        LinkedHashMap<String,AccountDataEncoder> receiveAccounts = 
                merchant.paymentMethods.get(clientPaymentMethodUrl).receiverAccounts;
        AccountDataEncoder paymentBackendMethodEncoder = null;
        for (String backendDataContext : 
             providerAuthority.getPaymentBackendMethods(clientPaymentMethodUrl)) {
            if (receiveAccounts.containsKey(backendDataContext)) {
                paymentBackendMethodEncoder = receiveAccounts.get(backendDataContext);
                break;
            }
        }
        if (paymentBackendMethodEncoder == null) {
            JSONObjectWriter notSupported = WalletAlertMessage.encode(
                    "This issuer is not yet supported...<p>Please select another card.");
            HttpSupport.writeJsonData(response, notSupported);
            return false;
        }

        // Valid method. Find proper to request key and auhorityUrl
        PaymentMethodDescriptor paymentNetwork = merchant.paymentMethods.get(clientPaymentMethodUrl);

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter authorizationRequest =
            AuthorizationRequest.encode(MerchantService.testMode,
                                        providerAuthority.getServiceUrl(),
                                        payeeAuthorityUrl,
                                        payerAuthorization.getPaymentMethod(),
                                        walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON),
                                        request.getRemoteAddr(),
                                        paymentRequest,
                                        paymentBackendMethodEncoder,
                                        merchant.getReferenceId(),
                                        paymentNetwork.signer);

        // Call Payer bank
        JSONObjectReader resultMessage = MerchantService.externalCalls.postJsonData(urlHolder,
                                                                                    providerAuthority.getServiceUrl(),
                                                                                    authorizationRequest);

        if (debug) {
            debugData.authorizationRequest = new JSONObjectReader(authorizationRequest);
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
        String accountReference = authorizationResponse.getOptionalAccountReference();
        if (accountReference == null) {
            accountReference = "N/A";
        } else if (cardPayment) {
            accountReference = AccountDataEncoder.visualFormattedAccountId(accountReference);
        }
        resultData.accountReference = accountReference;

        // Special treatment of the refund mode
        if (HomeServlet.getOption(session, REFUND_MODE_SESSION_ATTR)) {
            resultData.optionalRefund = authorizationResponse;
        }

        // Two-phase operation: perform the final step
        if (cardPayment && session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            transactionOperation.urlToCall = ownProviderAuthority.getServiceUrl();
            transactionOperation.verifier = MerchantService.acquirerRoot;
            if (debugData != null) {
                debugData.acquirerAuthority = ownProviderAuthority.getRoot();
            }
            resultData.transactionError =
                processTransaction(merchant,
                                   transactionOperation,
                                   // Just a copy since we don't have a complete scenario 
                                   paymentRequest.getAmount(),                                
                                   urlHolder,
                                   debugData);
        }

        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);
        
        // Gas Station: Save reservation part for future fulfillment
        if (session.getAttribute(GAS_STATION_SESSION_ATTR) != null) {
            if (cardPayment) {
                transactionOperation.urlToCall = ownProviderAuthority.getServiceUrl();
                transactionOperation.verifier = MerchantService.acquirerRoot;
            } else {
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

    static TransactionResponse.ERROR processTransaction(MerchantDescriptor merchant,
                                                        TransactionOperation transactionOperation,
                                                        BigDecimal actualAmount,
                                                        UrlHolder urlHolder,
                                                        DebugData debugData) throws IOException {

        JSONObjectWriter transactionRequest =
            TransactionRequest.encode(transactionOperation.authorizationResponse,
                                      transactionOperation.urlToCall,
                                      actualAmount,
                                      merchant.getReferenceId(),
                                      merchant.paymentMethods.get(
                                              transactionOperation.authorizationResponse
                                                  .getAuthorizationRequest()
                                                      .getPaymentMethod()
                                                          .getPaymentMethodUrl()).signer);
        // Acquirer or Hybrid call
        JSONObjectReader response =
            MerchantService.externalCalls.postJsonData(urlHolder, 
                                                       transactionOperation.urlToCall, 
                                                       transactionRequest);

        if (debugData != null) {
            debugData.transactionRequest = new JSONObjectReader(transactionRequest);
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
