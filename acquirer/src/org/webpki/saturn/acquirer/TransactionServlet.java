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
package org.webpki.saturn.acquirer;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.TransactionRequest;
import org.webpki.saturn.common.TransactionResponse;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.Payee;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the core Acquirer (Card-Processor) Saturn basic mode payment authorization servlet //
////////////////////////////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception {

        // Decode and finalize the cardpay request
        TransactionRequest transactionRequest = new TransactionRequest(providerRequest, true);
        PaymentRequest paymentRequest = transactionRequest.getPaymentRequest();

        // Verify that the payer's (user) bank is known
        transactionRequest.verifyPayerBank(AcquirerService.paymentRoot);

        // Verify that the payee (merchant) is one of our customers
        Payee payee = transactionRequest.getPayee();
        PayeeCoreProperties payeeCoreProperties = AcquirerService.merchantAccountDb.get(payee.getId());
        if (payeeCoreProperties == null) {
            throw new IOException("Unknown merchant Id: " + payee.getId());
        }
        payeeCoreProperties.verify(payee, transactionRequest.getSignatureDecoder());
        payeeCoreProperties.verify(payee, paymentRequest.getSignatureDecoder());

        AuthorizationResponse.AccountDataDecoder accountData = getAccountData(transactionRequest.getAuthorizationResponse());
        boolean testMode = transactionRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Acquiring for Account=" + accountData.logLine() + 
                    ", Amount=" + transactionRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());
        
        String optionalLogData = null;
        TransactionResponse.ERROR transactionError = null;
        if (!testMode) {

            // Here we are supposed to talk to the card payment network....
            optionalLogData = "Card payment network log data...";

        }

        // It appears that we succeeded
        AcquirerService.transactionCount++;
        return TransactionResponse.encode(transactionRequest,
                                          transactionError,
                                          getReferenceId(),
                                          optionalLogData,
                                          AcquirerService.acquirerKey);
        
    }
}
