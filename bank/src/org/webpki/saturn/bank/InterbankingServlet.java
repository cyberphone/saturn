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

import org.webpki.saturn.common.UrlHolder;

/////////////////////////////////////////////////////////////////////////////////
// This is a servlet that serves a hypothetical interbanking payment network.  //
// It is entirely independent of Saturn but there is some minor code reuse.    //
/////////////////////////////////////////////////////////////////////////////////

public class InterbankingServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;


    @Override
    JSONObjectWriter processCall(UrlHolder urlHolder,
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {
        IBRequest ibRequest = new IBRequest(providerRequest);
        JSONObjectWriter ibResponse;
        switch (ibRequest.getOperation()) {

            case CREDIT_CARD_TRANSACT:
                ibRequest.verifyCallerAuthenticity(BankService.acquirerRoot);
                // The following call will throw exceptions on errors
                WithDrawFromAccount wdfa = 
                        new WithDrawFromAccount(ibRequest.getAmount(),
                                                ibRequest.getAccount(),
                                                TransactionTypes.TRANSACT,
                                                ibRequest.getMerchantName(),
                                                ibRequest.getMerchantRef(),
                                                decodeReferenceId(ibRequest.getTransactionReference()),
                                                true,
                                                connection);
                ibResponse = IBResponse.encode(formatReferenceId(wdfa.transactionId), 
                                               ibRequest.getTestMode());
                break;

            case CARD_REFUND:
            case ACCOUNT_REFUND:
                ibRequest.verifyCallerAuthenticity(
                        ibRequest.getOperation() == IBRequest.Operations.CARD_REFUND ?
                                BankService.acquirerRoot : BankService.paymentRoot);
                // The following call will throw exceptions on errors
                RefundAccount rfa = new RefundAccount(ibRequest.getAmount(),
                                                      ibRequest.getAccount(),
                                                      ibRequest.getMerchantName(),
                                                      ibRequest.getMerchantRef(),
                                                      connection);
                ibResponse = IBResponse.encode(formatReferenceId(rfa.transactionId), 
                                               ibRequest.getTestMode());
                break;

            default:
                throw new IOException("Not implemented: " + ibRequest.getOperation().toString());
        }
        return ibResponse;
    }
}
