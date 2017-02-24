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

import org.webpki.saturn.common.AuthorizationData;
import org.webpki.saturn.common.AuthorizationRequest;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.CardPaymentRequest;
import org.webpki.saturn.common.CardPaymentResponse;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "hybrid" mode decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class HybridPaymentServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws IOException, GeneralSecurityException {

        // Decode and finalize the cardpay request which in hybrid mode actually is account-2-account
        CardPaymentRequest cardPaymentRequest = new CardPaymentRequest(providerRequest, false);
        
        // Get account data.  Note: this may also be derived from a transaction DB
        AuthorizationRequest authorizationRequest = cardPaymentRequest.getAuthorizationResponse().getAuthorizationRequest();
        AuthorizationData authorizationData = authorizationRequest.getDecryptedAuthorizationData(BankService.decryptionKeys);

        boolean testMode = cardPaymentRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Charging for AccountID=" + authorizationData.getAccount().getId() + 
                    ", Amount=" + cardPaymentRequest.getAmount().toString() +
                    " " + cardPaymentRequest.getPaymentRequest().getCurrency().toString());
        String optionalLogData = null;
        if (!testMode) {

            // Here we are supposed to do the actual payment
            optionalLogData = "Bank payment network log data...";
        }

        // It appears that we succeeded
        return CardPaymentResponse.encode(cardPaymentRequest,
                                          getReferenceId(),
                                          optionalLogData,
                                          BankService.bankKey);
    }

}
