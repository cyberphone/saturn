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
package org.webpki.saturn.bank;

import java.io.IOException;

import java.security.GeneralSecurityException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.ProtectedAccountData;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.RefundRequest;
import org.webpki.saturn.common.RefundResponse;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "refund" decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class RefundServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws IOException, GeneralSecurityException {

        // Decode refund request which embeds the authorization response
        RefundRequest refundRequest = new RefundRequest(providerRequest, false);
        
        // Verify that the payee (merchant) is one of our customers
        Payee payee = refundRequest.getPayee();
        PayeeCoreProperties merchantProperties = BankService.merchantAccountDb.get(payee.getId());
        if (merchantProperties == null) {
            throw new IOException("Unknown merchant Id: " + payee.getId());
        }
        if (!merchantProperties.getPublicKey().equals(refundRequest.getPublicKey())) {
            throw new IOException("Non-matching public key for merchant Id: " + payee.getId());
        }

        // Get payer account data.
        ProtectedAccountData protectedAccountData = refundRequest.getProtectedAccountData(BankService.decryptionKeys);

        boolean testMode = refundRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Refunding for Account=" + protectedAccountData + 
                    ", Amount=" + refundRequest.getAmount().toString() +
                    " " + refundRequest.getPaymentRequest().getCurrency().toString());
        String optionalLogData = null;
        if (!testMode) {

            // Here we are supposed to do the actual payment
            optionalLogData = "Bank payment network log data...";
        }

        // It appears that we succeeded
        return RefundResponse.encode(refundRequest,
                                     getReferenceId(),
                                     optionalLogData,
                                     BankService.bankKey);
    }

}
