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
import org.webpki.saturn.common.Payee;
import org.webpki.saturn.common.PayeeCoreProperties;
import org.webpki.saturn.common.UrlHolder;
import org.webpki.saturn.common.RefundRequest;
import org.webpki.saturn.common.RefundResponse;

////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn "refund" decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class RefundServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, JSONObjectReader providerRequest) throws Exception {

        // Decode refund request which embeds the authorization response
        RefundRequest refundRequest = new RefundRequest(providerRequest, true);
        refundRequest.verifyPayerBank(AcquirerService.paymentRoot);
        
        // Verify that the payee (merchant) is one of our customers
        Payee payee = refundRequest.getPayee();
        PayeeCoreProperties payeeCoreProperties = AcquirerService.merchantAccountDb.get(payee.getId());
        if (payeeCoreProperties == null) {
            throw new IOException("Unknown merchant Id: " + payee.getId());
        }
        payeeCoreProperties.verify(payee, refundRequest.getSignatureDecoder());

        AuthorizationResponse.AccountDataDecoder accountData = getAccountData(refundRequest.getAuthorizationResponse());

        boolean testMode = refundRequest.getTestMode();
        logger.info((testMode ? "TEST ONLY: ":"") +
                    "Refunding for Account=" + accountData.logLine() +
                    ", Amount=" + refundRequest.getAmount().toString() +
                    " " + refundRequest.getPaymentRequest().getCurrency().toString());
        String optionalLogData = null;
        if (!testMode) {

            // Here we are supposed to do the actual payment
            optionalLogData = "Card payment network log data...";
        }

        // It appears that we succeeded
        return RefundResponse.encode(refundRequest,
                                     getReferenceId(),
                                     optionalLogData,
                                     AcquirerService.acquirerKey);
    }
}
