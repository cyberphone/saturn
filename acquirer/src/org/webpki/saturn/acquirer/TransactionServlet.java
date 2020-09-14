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
package org.webpki.saturn.acquirer;

import io.interbanking.IBRequest;
import io.interbanking.IBResponse;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AccountDataDecoder;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.TransactionRequestDecoder;
import org.webpki.saturn.common.TransactionResponseDecoder;
import org.webpki.saturn.common.TransactionResponseEncoder;
import org.webpki.saturn.common.PayeeCoreProperties;

import com.supercard.SupercardAccountDataDecoder;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the core Acquirer (Card-Processor) Saturn basic mode payment authorization servlet //
////////////////////////////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception {

        // Decode and finalize the cardpay request
        TransactionRequestDecoder transactionRequest = new TransactionRequestDecoder(providerRequest, true);
        PaymentRequestDecoder paymentRequest = transactionRequest.getPaymentRequest();
        
        // Verify that we understand the payee payment method
        AccountDataDecoder payeeReceiveAccount = transactionRequest
                .getAuthorizationResponse()
                    .getAuthorizationRequest()
                        .getPayeeReceiveAccount(AcquirerService.payeeAccountTypes);

        // Verify that the payer's (user) bank is known
        transactionRequest.verifyPayerBank(AcquirerService.paymentRoot);

        // Verify that the payee (merchant) is one of our customers
        String payeeAuthorityUrl = transactionRequest
                .getAuthorizationResponse()
                    .getAuthorizationRequest()
                        .getPayeeAuthorityUrl();
        PayeeCoreProperties payeeCoreProperties = AcquirerService.payeeAccountDb.get(payeeAuthorityUrl);
        if (payeeCoreProperties == null) {
            throw new IOException("Unknown merchant: " + payeeAuthorityUrl);
        }
        payeeCoreProperties.verify(transactionRequest.getSignatureDecoder());

        SupercardAccountDataDecoder accountData = getAccountData(transactionRequest.getAuthorizationResponse());
        boolean testMode = transactionRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Acquiring for Account=" + accountData.logLine() + 
                    ", Amount=" + transactionRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());
        
        String optionalLogData = null;
        TransactionResponseDecoder.ERROR transactionError = null;
        
        // Here we are supposed to talk to the card payment network....
        IBResponse ibResponse = 
                IBRequest.perform(AcquirerService.payerInterbankUrl,
                                  IBRequest.Operations.CREDIT_CARD_TRANSACT,
                                  accountData.getAccountId(),
                                  transactionRequest.getAuthorizationResponse().getReferenceId(),
                                  transactionRequest.getAmount(),
                                  paymentRequest.getCurrency().toString(),
                                  paymentRequest.getPayeeCommonName(),
                                  paymentRequest.getReferenceId(),
                                  payeeReceiveAccount.getAccountId(),
                                  testMode,
                                  AcquirerService.acquirerKey);
        if (!testMode) {
            optionalLogData = "Payer interbanking ref: " + ibResponse.getOurReference();
        }

        // It appears that we succeeded
        AcquirerService.transactionCount++;
        return TransactionResponseEncoder.encode(transactionRequest,
                                                 transactionError,
                                                 getReferenceId(),
                                                 optionalLogData,
                                                 AcquirerService.acquirerKey);
        
    }
}
