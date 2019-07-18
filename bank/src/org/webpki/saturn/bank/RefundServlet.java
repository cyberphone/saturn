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

import java.sql.Connection;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.AuthorizationResponse;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.PaymentRequest;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.RefundRequest;
import org.webpki.saturn.common.RefundResponse;

import com.supercard.SupercardAccountDataDecoder;

import org.payments.sepa.SEPAAccountDataDecoder;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "refund" decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class RefundServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, 
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {

        // Decode refund request which embeds the authorization response
        RefundRequest refundRequest = new RefundRequest(providerRequest, false);
        refundRequest.verifyPayerBank(BankService.paymentRoot);
        
        // Verify that the payee (merchant) is one of our customers
        Payee payee = refundRequest.getPayee();
        PayeeCoreProperties payeeCoreProperties = BankService.merchantAccountDb.get(payee.getId());
        if (payeeCoreProperties == null) {
            throw new IOException("Unknown merchant Id: " + payee.getId());
        }
        payeeCoreProperties.verify(payee, refundRequest.getSignatureDecoder());

        // Get payer account data.
        AuthorizationResponse.AccountDataDecoder accountData = 
            refundRequest.getAuthorizationResponse().getProtectedAccountData(BankService.knownAccountTypes,
                                                                             BankService.decryptionKeys);
        String account = accountData instanceof SupercardAccountDataDecoder ?
                ((SupercardAccountDataDecoder)accountData).getCardNumber()  :
                ((SEPAAccountDataDecoder)accountData).geyPayerIban();
        PaymentRequest paymentRequest = refundRequest.getPaymentRequest();
        boolean testMode = refundRequest.getTestMode();
        String optionalLogData = null;
        if (!testMode) {

            // Here we are supposed to do the actual payment with respect to databases
        }
        IBResponse ibResponse = 
                IBRequest.perform(BankService.payerInterbankUrl,
                                  IBRequest.Operations.REVERSE_CREDIT_TRANSFER,
                                  account,
                                  null,
                                  refundRequest.getAmount(),
                                  paymentRequest.getCurrency().toString(),
                                  paymentRequest.getPayee().getCommonName(),
                                  paymentRequest.getReferenceId(),
                                  refundRequest.getPayeeSourceAccount(),
                                  testMode,
                                  BankService.bankKey);
        String transactionId = ibResponse.getOurReference();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Refunding for Account=" + accountData.logLine() +
                    ", Amount=" + refundRequest.getAmount().toString() +
                    " " + paymentRequest.getCurrency().toString());


        // It appears that we succeeded
        return RefundResponse.encode(refundRequest,
                                     transactionId,
                                     optionalLogData,
                                     BankService.bankKey);
    }
}
