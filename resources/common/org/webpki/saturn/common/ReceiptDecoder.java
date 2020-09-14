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

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class ReceiptDecoder implements BaseProperties {
    
    public final static int PENDING     = 0;
    public final static int AVAILABLE   = 1;
    public final static int FAILED      = 2;
    
    public ReceiptDecoder(String receiptUrl,
                          String clientPaymentMethodUrl, 
                          String providerAuthorityUrl,
                          String payeeAuthorityUrl,
                          PaymentRequestDecoder paymentRequest) throws IOException {
        receiptDocument = Messages.RECEIPT.createBaseMessage()
                .setString(RECEIPT_URL_JSON, receiptUrl)
                .setString(PAYMENT_METHOD_JSON, clientPaymentMethodUrl)
                .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
                .setString(PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
                .setMoney(AMOUNT_JSON, 
                          paymentRequest.amount,
                          paymentRequest.currency.getDecimals())
                .setString(CURRENCY_JSON, paymentRequest.currency.toString())
                .setDateTime(TIME_STAMP_JSON, 
                             paymentRequest.timeStamp, 
                             ISODateTime.UTC_NO_SUBSECONDS);
    }

    JSONObjectWriter receiptDocument;
    public JSONObjectWriter getReceiptDocument() {
        return receiptDocument;
    }
}
