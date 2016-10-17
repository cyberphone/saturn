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

    PAYMENT_CLIENT_IS_READY       ("PaymentClientIsReady"),          // PaymentClient to payee Web page message
    PAYMENT_CLIENT_REQUEST        ("PaymentClientRequest"),          // Payee payment request + other data
    PAYMENT_CLIENT_ALERT          ("PaymentClientAlert"),            // Payee to PaymentClient message
    PAYMENT_CLIENT_SUCCESS        ("PaymentClientSuccess"),          // Payee to PaymentClient message
    PAYER_AUTHORIZATION           ("PayerAuthorization"),            // Created by the PaymentClient
    
    PROVIDER_USER_RESPONSE        ("ProviderUserResponse"),          // May replace any RESPONSE message

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // One-step payment operation in Account2Account mode
    ///////////////////////////////////////////////////////////////////////////////////////////////
    BASIC_CREDIT_REQUEST          ("BasicCreditRequest",             // Payee request to Payee provider
                                   false, true),
    TRANSACTION_REQUEST           ("TransactionRequest"),            // Payee provider to Payer provider
    TRANSACTION_RESPONSE          ("TransactionResponse"),           // Payer provider response
    BASIC_CREDIT_RESPONSE         ("BasicCreditResponse",            // Payee provider response
                                   false, true),
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Two-step payment operation in the Account2Account mode
    //
    // First step - Payee to Payee provider
    RESERVE_CREDIT_REQUEST        ("ReserveCreditRequest",           // Reserve debit funds at provider
                                   false, false),
    // TRANSACTION_REQUEST/TRANSACTION_RESPONSE pair like in the one-step mode
    RESERVE_CREDIT_RESPONSE       ("ReserveCreditResponse",          // Provider response to request
                                   false, false),
    //
    // Second step - Payee to Payee provider
    FINALIZE_CREDIT_REQUEST       ("FinalizeCreditRequest",          // Perform the actual payment operation
                                   false, false),
    FINALIZE_TRANSACTION_REQUEST  ("FinalizeTransactionRequest",     // Payee provider to Payer provider
                                   false, false),
    FINALIZE_TRANSACTION_RESPONSE ("FinalizeTransactionResponse",    // Payer provider response
                                   false, false),
    FINALIZE_CREDIT_RESPONSE      ("FinalizeCreditResponse",         // Provider response
                                   false, false),
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Two-step payment operation in the Card payment mode
    //
    // First step - Payee to Payee provider.  Note that Payee provider may be = Acquirer
    RESERVE_CARDPAY_REQUEST       ("ReserveCardpayRequest",          // Reserve funds at provider
                                   true, false),
    // TRANSACTION_REQUEST/TRANSACTION_RESPONSE pair like in the one-step mode
    RESERVE_CARDPAY_RESPONSE      ("ReserveCardpayResponse",         // Provider response to request
                                   true, false),
    //
    // Second step - Payee to Acquirer
    FINALIZE_CARDPAY_REQUEST      ("FinalizeCardpayRequest",         // Perform the actual payment operation
                                   true, false),
    FINALIZE_CARDPAY_RESPONSE     ("FinalizeCardpayResponse",        // Acquirer response
                                   true, false),
    ///////////////////////////////////////////////////////////////////////////////////////////////

    PROVIDER_AUTHORITY            ("ProviderAuthority"),             // Published provider entity data
    
    PAYEE_AUTHORITY               ("PayeeAuthority");                // Published Payee entity data

    String qualifier;
    
    Boolean cardPayment;
    Boolean basicCredit;

    Messages(String qualifier, Boolean cardPayment, Boolean basicCredit) {
        this.qualifier = qualifier;
        this.cardPayment = cardPayment;
        this.basicCredit = basicCredit;
    }

    Messages(String qualifier) {
        this(qualifier, null, null);
    }

    @Override
    public String toString() {
        return qualifier;
    }

    public boolean isBasicCredit() {
        return basicCredit;
    }

    public boolean isCardPayment() {
        return cardPayment;
    }

    public static JSONObjectWriter createBaseMessage(Messages message) throws IOException {
        return new JSONObjectWriter()
          .setString(JSONDecoderCache.CONTEXT_JSON, BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)  
          .setString(JSONDecoderCache.QUALIFIER_JSON, message.toString());
    }

    public static Messages parseBaseMessage(Messages expectedMessage,
                                            JSONObjectReader requestObject) throws IOException {
        return parseBaseMessage(new Messages[]{expectedMessage}, requestObject);
    }

    public static Messages parseBaseMessage(Messages[] expectedMessages,
                                            JSONObjectReader requestObject) throws IOException {
        if (!requestObject.getString(JSONDecoderCache.CONTEXT_JSON).equals(BaseProperties.SATURN_WEB_PAY_CONTEXT_URI)) {
            throw new IOException("Unknown context: " + requestObject.getString(JSONDecoderCache.CONTEXT_JSON));
        }
        String qualifier = requestObject.getString(JSONDecoderCache.QUALIFIER_JSON);
        for (Messages message : expectedMessages) {
            if (qualifier.equals(message.toString())) {
                return message;
            }
        }
        throw new IOException("Unexpected qualifier: " + qualifier);
    }
}
