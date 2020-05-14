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

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.Connection;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BalanceRequestDecoder;
import org.webpki.saturn.common.BalanceResponseEncoder;
import org.webpki.saturn.common.UrlHolder;


////////////////////////////////////////////////////////////////////////////////////////////////
// This is the Saturn balance request decoder servlet                                           //
////////////////////////////////////////////////////////////////////////////////////////////////

public class BalanceRequestServlet extends ProcessingBaseServlet {

    private static final long serialVersionUID = 1L;
    
    JSONObjectWriter processCall(UrlHolder urlHolder, 
                                 JSONObjectReader providerRequest,
                                 Connection connection) throws Exception {

        // Decode the balance request
        BalanceRequestDecoder balanceRequest = 
                new BalanceRequestDecoder(providerRequest,
                                          BankService.AUTHORIZATION_SIGNATURE_POLICY);
 
        // The request parsed and the signature was (technically) correct, continue
        BigDecimal balance = 
                DataBaseOperations.requestAccountBalance(balanceRequest.getCredentialId(),
                                                         balanceRequest.getAccountId(), 
                                                         balanceRequest.getPublicKey(),
                                                         balanceRequest.getCurrency(), 
                                                         connection);
        
        // Specific tests
        if (balanceRequest.getAccountId().equals(BankService.balanceFailTest)) {
            throw new IOException("Programmed fail for:" + BankService.balanceFailTest);
        }
        if (BankService.balanceSlowTest) {
            Thread.sleep(30000);
        }

        // We did it, now return the result to the "wallet"
        return BalanceResponseEncoder.encode(balanceRequest.getAccountId(), 
                                             balance, 
                                             balanceRequest.getCurrency());
    }
}
