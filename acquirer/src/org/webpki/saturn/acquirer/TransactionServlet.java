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
package org.webpki.saturn.acquirer;

import java.io.IOException;

import java.security.GeneralSecurityException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.FinalizeCardpayResponse;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.FinalizeRequest;

/////////////////////////////////////////////////////////////////////////////////////////////////
// This is the core Acquirer (Card-Processor) Saturn "native" mode payment transaction servlet //
/////////////////////////////////////////////////////////////////////////////////////////////////

public class TransactionServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws IOException, GeneralSecurityException {

        // Decode the finalize cardpay request
        FinalizeRequest finalizeRequest = new FinalizeRequest(providerRequest);

        // Verify that the merchant's bank is known
        finalizeRequest.verifyMerchantBank(AcquirerService.paymentRoot);

        // Verify that the merchant is one of our customers
        Payee payee = finalizeRequest.getPayee();
        PayeeCoreProperties merchantProperties = AcquirerService.merchantAccountDb.get(payee.getId());
        if (merchantProperties == null) {
            throw new IOException("Unknown merchant Id: " + payee.getId());
        }
        if (!merchantProperties.getPublicKey().equals(finalizeRequest.getPublicKey())) {
            throw new IOException("Non-matching public key for merchant Id: " + payee.getId());
        }

        logger.info("Card data: " + finalizeRequest.getProtectedAccountData(AcquirerService.decryptionKeys, true));

        // Here we are supposed to talk to the card payment network....

        // It appears that we succeeded
        return FinalizeCardpayResponse.encode(finalizeRequest,
                                              getReferenceId(),
                                              AcquirerService.acquirerKey);
    }
}
