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
package org.webpki.saturn.bank;

import io.interbanking.IBRequest;
import io.interbanking.IBResponse;

import java.io.IOException;

import java.util.Arrays;

import java.sql.Connection;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONCryptoHelper;

import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.PayeeAuthority;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "hybrid" mode decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class HybridPaymentServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, 
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {

        // Decode and finalize the cardpay request which in hybrid mode actually is account-2-account
        TransactionRequest transactionRequest = new TransactionRequest(providerRequest, false);
 
        // Verify that it was actually we who created the original response
        AuthorizationResponse authorizationResponse = transactionRequest.getAuthorizationResponse();
        if (!Arrays.equals(BankService.bankCertificatePath,
                           authorizationResponse.getSignatureDecoder().getCertificatePath())) {
            throw new IOException("\"" + JSONCryptoHelper.CERTIFICATE_PATH_JSON + "\" mismatch");
        }

        // Although we have already verified the Payee (merchant) during the authorization phase
        // we should do it for this round as well...
        AuthorizationRequest authorizationRequest = authorizationResponse.getAuthorizationRequest();

        // Verify that we understand the payment method
        AuthorizationRequest.PaymentBackendMethodDecoder paymentMethodSpecific =
            authorizationRequest.getPaymentBackendMethodSpecific(BankService.knownPayeeMethods);
        PaymentRequest paymentRequest = authorizationRequest.getPaymentRequest();
        PayeeAuthority payeeAuthority =
            BankService.externalCalls.getPayeeAuthority(urlHolder,
                                                        authorizationRequest.getAuthorityUrl());
        payeeAuthority.getPayeeCoreProperties().verify(paymentRequest.getPayee(), 
                                                       transactionRequest.getSignatureDecoder());
        
        // Get payer account data.  Note: this may also be derived from a transaction DB
        AuthorizationData authorizationData = 
            authorizationRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        boolean testMode = transactionRequest.getTestMode();
        String optionalLogData = null;
        TransactionResponse.ERROR transactionError = null;
        int transactionId;
        if (testMode) {
            transactionId = BankService.testReferenceId++;
        } else {
            // The following call will throw exceptions on errors
            WithDrawFromAccount wdfa = 
                    new WithDrawFromAccount(transactionRequest.getAmount(),
                                            authorizationData.getAccountId(),
                                            TransactionTypes.TRANSACT,
                                            paymentMethodSpecific.getPayeeAccount(),
                                            paymentRequest.getPayee().getCommonName(),
                                            paymentRequest.getReferenceId(),
                                            decodeReferenceId(transactionRequest
                                                    .getAuthorizationResponse().getReferenceId()),
                                            true,
                                            connection);
            transactionId = wdfa.getTransactionId();
        }
        //#################################################
        //# Payment backend networking take place here... #
        //#################################################
        //
        // If Payer and Payee are in the same bank networking is not needed
        // and is replaced by a local database account adjust operations.
        //
        // Note that if backend networking for some reason fails, the transaction
        // must be reversed.
        //
        // If successful one could imagine updating the transaction record with
        // a reference to that part as well.
        try {
            IBResponse ibResponse = 
                IBRequest.perform(BankService.payeeInterbankUrl,
                                  IBRequest.Operations.CREDIT_TRANSFER,
                                  authorizationData.getAccountId(), 
                                  null,
                                  transactionRequest.getAmount(),
                                  paymentRequest.getCurrency().toString(),
                                  paymentRequest.getPayee().getCommonName(),
                                  paymentRequest.getReferenceId(),
                                  paymentMethodSpecific.getPayeeAccount(),
                                  testMode, 
                                  BankService.bankKey);
            optionalLogData = ibResponse.getOurReference();
        } catch (Exception e) {
            new NullifyTransaction(transactionId, connection);
            throw e;
        }
        // It appears that we succeeded
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Charging for Account ID=" + authorizationData.getAccountId() + 
                    ", Amount=" + transactionRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());
        return TransactionResponse.encode(transactionRequest,
                                          transactionError,
                                          formatReferenceId(transactionId),
                                          optionalLogData,
                                          BankService.bankKey);
    }
}
