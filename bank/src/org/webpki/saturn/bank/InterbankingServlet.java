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

import java.sql.Connection;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.TransactionTypes;
import org.webpki.saturn.common.UrlHolder;

/////////////////////////////////////////////////////////////////////////////////
// This is a servlet that serves a hypothetical interbanking payment network.  //
// It is entirely independent of Saturn but there is some minor code reuse.    //
/////////////////////////////////////////////////////////////////////////////////

public class InterbankingServlet extends ProcessingBaseServlet {
  
    private static final long serialVersionUID = 1L;
    
    private static int creditTransferId = 100000;


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
                int transactionId = 
                        DataBaseOperations.externalWithDraw(ibRequest.getAmount(),
                                                            ibRequest.getAccount(),
                                                            TransactionTypes.TRANSACT,
                                                            ibRequest.getPayeeAccount(),
                                                            ibRequest.getPayeeName(),
                                                            ibRequest.getPayeeReference(),
                                                            decodeReferenceId(ibRequest
                                                                    .getTransactionReference()),
                                                            true,
                                                            connection);
                try {
                    IBResponse ibResponse1 = 
                            IBRequest.perform(BankService.payeeInterbankUrl,
                                              IBRequest.Operations.CREDIT_TRANSFER,
                                              ibRequest.getAccount(), 
                                              null,
                                              ibRequest.getAmount(),
                                              ibRequest.getCurrency(),
                                              ibRequest.getPayeeName(),
                                              ibRequest.getPayeeReference(),
                                              ibRequest.getPayeeAccount(),
                                              ibRequest.getTestMode(), 
                                              BankService.bankKey);
                    ibResponse = IBResponse.encode(formatReferenceId(transactionId), 
                                                   ibRequest.getTestMode());
                } catch (Exception e) {
                    DataBaseOperations.nullifyTransaction(transactionId, connection);
                    throw e;
                }
                break;

            case CREDIT_CARD_REFUND:
            case REVERSE_CREDIT_TRANSFER:
                ibRequest.verifyCallerAuthenticity(
                        ibRequest.getOperation() == IBRequest.Operations.CREDIT_CARD_REFUND ?
                                BankService.acquirerRoot : BankService.paymentRoot);
                // The following call will throw exceptions on errors
                CreditAccount creditAccount = new CreditAccount(ibRequest.getAmount(),
                                                                ibRequest.getAccount(),
                                                                ibRequest.getPayeeAccount(),
                                                                ibRequest.getPayeeName(),
                                                                ibRequest.getPayeeReference(),
                                                                connection);
                try {
                    ibResponse = 
                            IBResponse.encode(formatReferenceId(creditAccount.getTransactionId()), 
                                              ibRequest.getTestMode());
                } catch (Exception e) {
                    DataBaseOperations.nullifyTransaction(creditAccount.getTransactionId(), connection);
                    throw e;
                }
                break;

            case CREDIT_TRANSFER:
                // The demo does nothing DB-wise in the payee bank for credit transfers
                ibRequest.verifyCallerAuthenticity(BankService.paymentRoot);
                ibResponse = IBResponse.encode("CT" + creditTransferId++, 
                                               ibRequest.getTestMode());
                BankService.successfulTransactions++;
                break;

            default:
                throw new IOException("Not implemented: " + ibRequest.getOperation().toString());
        }
        return ibResponse;
    }
}
