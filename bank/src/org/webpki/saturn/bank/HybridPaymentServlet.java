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
package org.webpki.saturn.bank;

import io.interbanking.IBRequest;
import io.interbanking.IBResponse;

import java.io.IOException;

import java.util.Arrays;

import java.sql.Connection;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONCryptoHelper;

import org.webpki.saturn.common.AccountDataDecoder;
import org.webpki.saturn.common.AuthorizationDataDecoder;
import org.webpki.saturn.common.AuthorizationRequestDecoder;
import org.webpki.saturn.common.AuthorizationResponseDecoder;
import org.webpki.saturn.common.PayeeAuthorityDecoder;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.TransactionRequestDecoder;
import org.webpki.saturn.common.TransactionTypes;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.TransactionResponseDecoder;
import org.webpki.saturn.common.TransactionResponseEncoder;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "hybrid" mode decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class HybridPaymentServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, 
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {

        // Decode and finalize the "emulated" card pay request which in 
        // hybrid mode actually is account-2-account
        TransactionRequestDecoder transactionRequest =
                new TransactionRequestDecoder(providerRequest, false);
 
        // Verify that it was actually we who created the original response
        AuthorizationResponseDecoder authorizationResponse = 
                transactionRequest.getAuthorizationResponse();
        if (!Arrays.equals(BankService.bankCertificatePath,
                           authorizationResponse.getSignatureDecoder().getCertificatePath())) {
            throw new IOException("\"" + JSONCryptoHelper.CERTIFICATE_PATH_JSON + "\" mismatch");
        }

        // Although we have already verified the Payee (merchant) during the authorization phase
        // we should do it for this round as well...
        AuthorizationRequestDecoder authorizationRequest = 
                authorizationResponse.getAuthorizationRequest();
        PayeeAuthorityDecoder payeeAuthority =
                BankService.externalCalls.getPayeeAuthority(
                        urlHolder,
                        authorizationRequest.getPayeeAuthorityUrl());
            payeeAuthority.getPayeeCoreProperties()
                .verify(transactionRequest.getSignatureDecoder());

        // Get the payment method (we already know that it is OK since it was dealt with in
        // the initial call).
        AccountDataDecoder payeeReceiveAccount =
            authorizationRequest.getPayeeReceiveAccount(BankService.knownPayeeMethods);
        
        // Get payer account data.  Note: transaction request contains ALL required data, the
        // backend system only needs to understand the concept of reserving funds and supply
        // an identifier to the requesting party which is subsequently referred to here.
        PaymentRequestDecoder paymentRequest = authorizationRequest.getPaymentRequest();
        AuthorizationDataDecoder authorizationData = authorizationRequest
                .getDecryptedAuthorizationData(BankService.decryptionKeys,
                                               BankService.AUTHORIZATION_SIGNATURE_POLICY);

        boolean testMode = transactionRequest.getTestMode();
        String optionalLogData = null;
        TransactionResponseDecoder.ERROR transactionError = null;
        int transactionId;
        if (testMode) {
            transactionId = BankService.testReferenceId++;
        } else {
            // The following call will throw exceptions on errors
            transactionId = 
                    DataBaseOperations.externalWithDraw(transactionRequest.getAmount(),
                                                        authorizationData.getAccountId(),
                                                        TransactionTypes.TRANSACT,
                                                        payeeReceiveAccount.getAccountId(),
                                                        paymentRequest.getPayeeCommonName(),
                                                        paymentRequest.getReferenceId(),
                                                        decodeReferenceId(transactionRequest
                                                                .getAuthorizationResponse()
                                                                    .getReferenceId()),
                                                        true,
                                                        connection);
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
                                  paymentRequest.getPayeeCommonName(),
                                  paymentRequest.getReferenceId(),
                                  payeeReceiveAccount.getAccountId(),
                                  testMode, 
                                  BankService.bankKey);
            optionalLogData = ibResponse.getOurReference();
        } catch (Exception e) {
            DataBaseOperations.nullifyTransaction(transactionId, connection);
            throw e;
        }
        // It appears that we succeeded
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Charging for Account ID=" + authorizationData.getAccountId() + 
                    ", Amount=" + transactionRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());
        return TransactionResponseEncoder.encode(transactionRequest,
                                                 transactionError,
                                                 formatReferenceId(transactionId),
                                                 optionalLogData,
                                                 BankService.bankKey);
    }
}
