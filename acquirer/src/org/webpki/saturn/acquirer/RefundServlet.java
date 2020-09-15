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
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.PaymentRequestDecoder;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.RefundRequestDecoder;
import org.webpki.saturn.common.RefundResponseEncoder;

import com.supercard.SupercardAccountDataDecoder;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "refund" decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class RefundServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception {

        // Decode refund request which embeds the authorization response
        RefundRequestDecoder refundRequest = new RefundRequestDecoder(providerRequest, true);
        refundRequest.verifyPayerBank(AcquirerService.paymentRoot);
        
        // Verify that the payee (merchant) is one of our customers
        String payeeAuthorityUrl = refundRequest
                .getAuthorizationResponse()
                    .getAuthorizationRequest()
                        .getPayeeAuthorityUrl();
        PayeeCoreProperties payeeCoreProperties = AcquirerService.payeeAccountDb.get(payeeAuthorityUrl);
        if (payeeCoreProperties == null) {
            throw new IOException("Unknown merchant: " + payeeAuthorityUrl);
        }
        payeeCoreProperties.verify(refundRequest.getSignatureDecoder());

        SupercardAccountDataDecoder accountData = getAccountData(refundRequest.getAuthorizationResponse());
        PaymentRequestDecoder paymentRequest = refundRequest.getPaymentRequest();

        boolean testMode = refundRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Refunding for Account=" + accountData.logLine() +
                    ", Amount=" + refundRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());
        String optionalLogData = null;
        if (!testMode) {

            // Here we are supposed to do the actual payment with respect to databases
        }

        AccountDataDecoder payeeSourceAccount = 
                refundRequest.getPayeeSourceAccount(AcquirerService.payeeAccountTypes);
        // Note: here there MUST be a test that the source account actually belongs to the payee!
        
        IBResponse ibResponse = 
                IBRequest.perform(AcquirerService.payerInterbankUrl,
                                  IBRequest.Operations.CREDIT_CARD_REFUND,
                                  accountData.getAccountId(),
                                  null,
                                  refundRequest.getAmount(),
                                  paymentRequest.getCurrency().toString(),
                                  paymentRequest.getPayeeCommonName(),
                                  paymentRequest.getReferenceId(),
                                  payeeSourceAccount.getAccountId(),
                                  testMode,
                                  AcquirerService.acquirerKey);
        // It appears that we succeeded
        return RefundResponseEncoder.encode(refundRequest,
                                            getReferenceId(),
                                            optionalLogData,
                                            AcquirerService.acquirerKey);
    }
}
