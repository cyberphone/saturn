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
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.Messages;
import org.webpki.saturn.common.PayerAccountTypes;
import org.webpki.saturn.common.ProviderAuthority;
import org.webpki.saturn.common.Expires;
import org.webpki.saturn.common.FinalizeCreditResponse;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.ReserveOrBasicResponse;
import org.webpki.saturn.common.ReserveOrBasicRequest;
import org.webpki.saturn.common.FinalizeRequest;
import org.webpki.saturn.common.PayerAuthorization;
import org.webpki.saturn.common.ProviderUserResponse;
import org.webpki.saturn.common.ServerAsymKeySigner;

//////////////////////////////////////////////////////////////////////////
// This servlet does all Merchant backend payment transaction work      //
//////////////////////////////////////////////////////////////////////////

public class AuthorizationServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;

    private static final String MERCHANT_ACCOUNT_TYPE = "https://swift.com";

    private static final String MERCHANT_ACCOUNT_ID = "IBAN:FR7630004003200001019471656";
    
    static ProviderAuthority payeeProviderAuthority;
    
    synchronized void updatePayeeProviderAuthority(UrlHolder urlHolder) throws IOException {
        payeeProviderAuthority = new ProviderAuthority(getData(urlHolder), urlHolder.getUrl());
        // Verify that the claimed authority belongs to a known payment provider network
        payeeProviderAuthority.getSignatureDecoder().verify(MerchantService.paymentRoot);
    }

    @Override
    void processCall(JSONObjectReader walletResponse,
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
 
        // ugly fix to cope with local installation
        String providerAuthorityUrl = payerAuthorization.getProviderAuthorityUrl();
        if (MerchantService.payeeProviderAuthorityUrl.contains("localhost")) {
            URL url = new URL(MerchantService.payeeProviderAuthorityUrl);
            providerAuthorityUrl = new URL(url.getProtocol(),
                                           url.getHost(), 
                                           url.getPort(),
                                           new URL(providerAuthorityUrl).getFile()).toExternalForm();
        }

        // Lookup of payer's bank.  You would typically cache such information
        urlHolder.setUrl(providerAuthorityUrl);
        ProviderAuthority providerAuthority = new ProviderAuthority(getData(urlHolder), urlHolder.getUrl());
        urlHolder.setUrl(null);
        AccountDescriptor accountDescriptor = null;
        if (!acquirerBased) {
            for (String accountType : providerAuthority.getProviderAccountTypes()) {
                if (accountType.equals(MERCHANT_ACCOUNT_TYPE)) {
                    accountDescriptor = new AccountDescriptor(accountType, MERCHANT_ACCOUNT_ID);
                    break;
                }
            }
            if (accountDescriptor == null) {
                throw new IOException("No matching account type: " + providerAuthority.getProviderAccountTypes());
            }
        }

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter authorizationRequest =
            AuthorizationRequest.encode(MerchantService.payeeProviderAuthorityUrl
                                            .substring(0, MerchantService.payeeProviderAuthorityUrl.lastIndexOf('/')) +
                                            "/payees/" + MerchantService.primaryMerchant.merchantId,
                                         payerAuthorization.getAccountType(),
                                         walletResponse.getObject(ENCRYPTED_AUTHORIZATION_JSON),
                                         request.getRemoteAddr(),
                                         paymentRequest,
                                         accountDescriptor,
                                         MerchantService.getReferenceId(),
                                         MerchantService.paymentNetworks.get(paymentRequest.getPublicKey()).signer);

        if (debug) {
  //          debugData.providerAuthority = payeeProviderAuthority.getRoot();
  //          debugData.basicCredit = basicCredit;
        }

        // Call the payee bank
        urlHolder.setUrl(providerAuthority.getAuthorizationUrl());
        JSONObjectReader resultMessage = postData(urlHolder, authorizationRequest);

        if (debug) {
 //           debugData.reserveOrBasicRequest = reserveOrBasicRequest;
 //           debugData.reserveOrBasicResponse = resultMessage;
        }

        if (resultMessage.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PROVIDER_USER_RESPONSE.toString())) {
            if (debug) {
 //               debugData.softReserveOrBasicError = true;
            }
            // Parse for syntax only
            new ProviderUserResponse(resultMessage);
            returnJsonData(response, new JSONObjectWriter(resultMessage));
            return;
        }
    
        // Additional consistency checking
        ReserveOrBasicResponse reserveOrBasicResponse = new ReserveOrBasicResponse(resultMessage);

        // No error return, then we can verify the response fully
        reserveOrBasicResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);
   
        // Two-phase operation: perform the final step
/*
        if (!basicCredit && session.getAttribute(GAS_STATION_SESSION_ATTR) == null) {
            processFinalize(reserveOrBasicResponse,
                            paymentRequest.getAmount(),  // Just a copy since we don't have a complete scenario                                
                            urlHolder,
                            acquirerBased,
                            debugData);
        }
*/        
        // Create a viewable response
        ResultData resultData = new ResultData();
        resultData.amount = paymentRequest.getAmount();  // Gas Station will upgrade amount
        resultData.referenceId = paymentRequest.getReferenceId();
        resultData.currency = paymentRequest.getCurrency();
        resultData.accountType = reserveOrBasicResponse.getPayerAccountType();
        resultData.accountReference = reserveOrBasicResponse.getFormattedAccountReference();
        session.setAttribute(RESULT_DATA_SESSION_ATTR, resultData);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
