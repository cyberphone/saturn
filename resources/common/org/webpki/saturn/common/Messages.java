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
package org.webpki.saturn.common;

import java.io.IOException;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public enum Messages {

    PAYMENT_CLIENT_REQUEST        ("PaymentClientRequest"),          // Payee payment request + other data
    PAYMENT_CLIENT_ALERT          ("PaymentClientAlert"),            // Payee to PaymentClient message
    PAYER_AUTHORIZATION           ("PayerAuthorization"),            // Created by the PaymentClient
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Saturn core
    ///////////////////////////////////////////////////////////////////////////////////////////////
    AUTHORIZATION_REQUEST         ("AuthorizationRequest"),          // Payee to Payer provider
    AUTHORIZATION_RESPONSE        ("AuthorizationResponse"),         // Response to the former
    PROVIDER_USER_RESPONSE        ("ProviderUserResponse"),          // May replace the former

    TRANSACTION_REQUEST           ("TransactionRequest"),            // Payee to Acquirer or Payer provider
    TRANSACTION_RESPONSE          ("TransactionResponse"),           // Response to the former

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Saturn optional
    ///////////////////////////////////////////////////////////////////////////////////////////////
    REFUND_REQUEST                ("RefundRequest"),                 // Payee to Acquirer or Payee provider
    REFUND_RESPONSE               ("RefundResponse"),                // Response to the former
    
    BALANCE_REQUEST               ("BalanceRequest"),                // Created by the PaymentClient
    BALANCE_RESPONSE              ("BalanceResponse"),               // Response by Payer provider

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Saturn authority lookup response messages
    ///////////////////////////////////////////////////////////////////////////////////////////////
    PROVIDER_AUTHORITY            ("ProviderAuthority"),             // Published provider entity data
    PAYEE_AUTHORITY               ("PayeeAuthority"),                // Published Payee entity data

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Saturn Receipt messages
    ///////////////////////////////////////////////////////////////////////////////////////////////
    RECEIPT                       ("Receipt");                       // "Published" receipt

    String qualifier;

    Messages(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public String toString() {
        return qualifier;
    }

    public JSONObjectWriter createBaseMessage() throws IOException {
        return new JSONObjectWriter()
          .setString(JSONDecoderCache.CONTEXT_JSON, BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)  
          .setString(JSONDecoderCache.QUALIFIER_JSON, qualifier);
    }

    public JSONObjectReader parseBaseMessage(JSONObjectReader requestObject) throws IOException {
        if (!requestObject.getString(JSONDecoderCache.CONTEXT_JSON).equals(BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)) {
            throw new IOException("Unknown context: " + requestObject.getString(JSONDecoderCache.CONTEXT_JSON));
        }
        String qualifier = requestObject.getString(JSONDecoderCache.QUALIFIER_JSON);
        if (!qualifier.equals(this.qualifier)) {
            throw new IOException("Unexpected qualifier: " + qualifier);
        }
        return requestObject;
    }

    public String lowerCamelCase() {
        char[] lowerCamelCasedMessage = qualifier.toCharArray();
        lowerCamelCasedMessage[0] = Character.toLowerCase(lowerCamelCasedMessage[0]);
        return String.valueOf(lowerCamelCasedMessage);
    }

    public JSONObjectReader getEmbeddedMessage(JSONObjectReader reader) throws IOException {
        return reader.getObject(lowerCamelCase());
    }
}
