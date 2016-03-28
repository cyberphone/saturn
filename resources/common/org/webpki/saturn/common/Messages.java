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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public enum Messages {

    WALLET_INITIALIZED        ("WalletInitialized"),       // Wallet to payee web page message
    WALLET_REQUEST            ("WalletRequest"),           // Payee payment request + other data
    PAYER_AUTHORIZATION       ("PayerAuthorization"),      // Created by the Wallet

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // One-step payment operation in Account2Account mode
    DIRECT_DEBIT_REQUEST      ("DirectDebitRequest"),      // Payee request to provider
    DIRECT_DEBIT_RESPONSE     ("DirectDebitResponse"),     // Provider response to the above
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Two-step payment operation in Account2Account or Acquirer mode
    //
    // First step - Payee to Provider
    RESERVE_FUNDS_REQUEST     ("ReserveFundsRequest"),     // Reserve funds at provider
    RESERVE_FUNDS_RESPONSE    ("ReserveFundsResponse"),    // Provider response to request
    //
    // Second step - Payee to Provider (Account2Account mode) or Acquirer (Acquirer mode)
    FINALIZE_REQUEST          ("FinalizeRequest"),         // Perform the actual payment operation
    FINALIZE_RESPONSE         ("FinalizeResponse"),        // Provider or Acquirer response to request
    ///////////////////////////////////////////////////////////////////////////////////////////////

    AUTHORITY                 ("Authority");               // Published entity data

    String qualifier;

    Messages(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public String toString() {
        return qualifier;
    }

    public static JSONObjectWriter createBaseMessage(Messages message) throws IOException {
        return new JSONObjectWriter()
           .setString(JSONDecoderCache.CONTEXT_JSON, BaseProperties.W2NB_WEB_PAY_CONTEXT_URI)
           .setString(JSONDecoderCache.QUALIFIER_JSON, message.toString());
    }

    public static JSONObjectReader parseBaseMessage(Messages expected_message,
                                                    JSONObjectReader request_object) throws IOException {
        if (!request_object.getString(JSONDecoderCache.CONTEXT_JSON).equals(BaseProperties.W2NB_WEB_PAY_CONTEXT_URI)) {
            throw new IOException("Unknown context: " + request_object.getString(JSONDecoderCache.CONTEXT_JSON));
        }
        if (!request_object.getString(JSONDecoderCache.QUALIFIER_JSON).equals(expected_message.toString())) {
            throw new IOException("Unexpected qualifier: " + request_object.getString(JSONDecoderCache.QUALIFIER_JSON));
        }
        return request_object;
    }
}
