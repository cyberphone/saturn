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

import org.webpki.saturn.common.AuthorizationRequestEncoder;
import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.AccountDataEncoder;
import org.webpki.saturn.common.HttpSupport;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PayeeAuthorityDecoder;
import org.webpki.saturn.common.ProviderAuthorityDecoder;
import org.webpki.saturn.common.ProviderUserResponseDecoder;
import org.webpki.saturn.common.ServerAsymKeySigner;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.PaymentMethods;
import org.webpki.saturn.common.TransactionResponseDecoder;
import org.webpki.saturn.common.TransactionRequestEncoder;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.WalletAlertMessage;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    boolean processCall(MerchantDescriptor merchantDescriptor,
                        PaymentRequestDecoder paymentRequest, 
                        PayerAuthorization payerAuthorization,
                        WalletRequest walletRequest,
                        HttpSession session,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        boolean debug, 
                        DebugData debugData, 
                        UrlHolder urlHolder) throws Exception {
        // Slightly different flows for card- and bank-to-bank authorizations
        PaymentMethods paymentMethodEnum = payerAuthorization.getPaymentMethod();
        boolean cardPayment = paymentMethodEnum.isCardPayment();
        String clientPaymentMethodUrl = paymentMethodEnum.getPaymentMethodUrl();
        String payeeAuthorityUrl = 
                merchantDescriptor.paymentMethods.get(clientPaymentMethodUrl).authorityUrl;
        
        // Lookup of self (since it is provided by an external party)
        PayeeAuthorityDecoder payeeAuthority =
                MerchantService.externalCalls.getPayeeAuthority(urlHolder, payeeAuthorityUrl);

        // Strictly put not entirely necessary (YET...)
        ProviderAuthorityDecoder ownProviderAuthority = 
            MerchantService.externalCalls.getProviderAuthority(urlHolder,
                    payeeAuthority.getProviderAuthorityUrl());
        
         // Lookup of Payer's bank
        ProviderAuthorityDecoder providerAuthority = 
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
                merchantDescriptor.paymentMethods.get(clientPaymentMethodUrl).receiverAccounts;
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
        PaymentMethodDescriptor paymentNetwork = merchantDescriptor.paymentMethods.get(clientPaymentMethodUrl);
        
        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter authorizationRequest =
            AuthorizationRequestEncoder.encode(
                                        MerchantService.testMode,
                                        providerAuthority.getServiceUrl(),
                                        payeeAuthorityUrl,
                                        payerAuthorization.getPaymentMethod(),
                                        payerAuthorization.getEncryptedAuthorization(),
                                        request.getRemoteAddr(),
                                        paymentRequest,
                                        paymentBackendMethodEncoder,
                                        paymentNetwork.signer);

        // Call Payer bank
        JSONObjectReader resultMessage = 
                MerchantService.externalCalls.postJsonData(urlHolder,
                                                           providerAuthority.getServiceUrl(),
                                                           authorizationRequest);
        // Receipt handling
        if (walletRequest.receiptUrl != null) {
/*
             DataBaseOperations.updateReceiptInformation(walletRequest.orderId,
                     new ReceiptEncoder(walletRequest.receiptUrl,
                                        clientPaymentMethodUrl,
                                        payerAuthorization.getProviderAuthorityUrl(),
                                        payeeAuthorityUrl,
                                        paymentRequest));
*/
        }

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
            new ProviderUserResponseDecoder(resultMessage);
            HttpSupport.writeJsonData(response, new JSONObjectWriter(resultMessage));
            return false;
        }
    
        // Additional consistency checking
        AuthorizationResponseDecoder authorizationResponse = new AuthorizationResponseDecoder(resultMessage);

        // No error return, then we can verify the response fully
        authorizationResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
        transactionOperation.authorizationResponse = authorizationResponse;
   
        // Create a viewable response
        ResultData resultData = new ResultData();
        resultData.authorization = authorizationResponse;
        resultData.walletRequest = walletRequest;
        resultData.providerCommonName = providerAuthority.getCommonName();
        resultData.providerAuthorityUrl = payerAuthorization.getProviderAuthorityUrl();
        String accountReference = authorizationResponse.getOptionalAccountReference();
        if (accountReference == null) {
            accountReference = "N/A";
        } else if (cardPayment) {
            accountReference = AccountDataEncoder.visualFormattedAccountId(accountReference);
        }

        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);

        // Avoid the special treatments for gas station payments
        if (session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {

            // Card operation: perform the final step
            if (cardPayment) {
                transactionOperation.urlToCall = ownProviderAuthority.getServiceUrl();
                transactionOperation.verifier = MerchantService.acquirerRoot;
                if (debugData != null) {
                    debugData.acquirerAuthority = ownProviderAuthority.getRoot();
                }
                processTransaction(merchantDescriptor,
                                   transactionOperation,
                                   // Just a copy in this simple scenario 
                                   paymentRequest.getAmount(),                                
                                   urlHolder,
                                   resultData,
                                   debugData);
            } else {
                DataBaseOperations.saveTransaction(resultData, merchantDescriptor, paymentNetwork.signer);
            }

        } else {
 
            // Gas Station: Save reservation part for future fulfillment
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

    static void processTransaction(MerchantDescriptor merchantDescriptor,
                                   TransactionOperation transactionOperation,
                                   BigDecimal actualAmount,
                                   UrlHolder urlHolder,
                                   ResultData resultData, 
                                   DebugData debugData)
            throws IOException, GeneralSecurityException {

        ServerAsymKeySigner signer = merchantDescriptor.paymentMethods.get(
            transactionOperation.authorizationResponse
                .getAuthorizationRequest().getPaymentMethod().getPaymentMethodUrl()).signer;
        JSONObjectWriter transactionRequest =
            TransactionRequestEncoder.encode(transactionOperation.authorizationResponse,
                                             transactionOperation.urlToCall,
                                             actualAmount,
                                             transactionOperation.authorizationResponse
                                                 .getAuthorizationRequest()
                                                     .getPaymentRequest().getReferenceId() + ".1",
                                             signer);
        // Acquirer or Hybrid call
        JSONObjectReader response =
            MerchantService.externalCalls.postJsonData(urlHolder, 
                                                       transactionOperation.urlToCall, 
                                                       transactionRequest);

        if (debugData != null) {
            debugData.transactionRequest = new JSONObjectReader(transactionRequest);
            debugData.transactionResponse = response;
        }
        
        TransactionResponseDecoder transactionResponse = new TransactionResponseDecoder(response);
        transactionResponse.getSignatureDecoder().verify(transactionOperation.verifier);
        
        resultData.authorization = transactionResponse;
        resultData.transactionError = transactionResponse.getTransactionError();
        DataBaseOperations.saveTransaction(resultData,
                                           merchantDescriptor,
                                           signer);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
