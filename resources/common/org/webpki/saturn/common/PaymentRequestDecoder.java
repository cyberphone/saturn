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

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.GregorianCalendar;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class PaymentRequestDecoder implements BaseProperties {
    
    public PaymentRequestDecoder(JSONObjectReader rd) throws IOException {
        root = rd;
        payeeCommonName = rd.getString(COMMON_NAME_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        amount = rd.getMoney(AMOUNT_JSON, currency.getDecimals());
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        expires = rd.getDateTime(EXPIRES_JSON, ISODateTime.COMPLETE);
        if (rd.hasProperty(NON_DIRECT_PAYMENT_JSON)) {
            nonDirectPayment = new NonDirectPaymentDecoder(rd.getObject(NON_DIRECT_PAYMENT_JSON),
                                                           timeStamp);
        }
        rd.checkForUnread();
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    String payeeCommonName;
    public String getPayeeCommonName() {
        return payeeCommonName;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    NonDirectPaymentDecoder nonDirectPayment;
    public NonDirectPaymentDecoder getNonDirectPayment() {
        return nonDirectPayment;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }
    
    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    JSONObjectReader root;

    public byte[] getRequestHash(HashAlgorithms requestHashAlgorithm)
            throws IOException, GeneralSecurityException {
        return CryptoUtils.getJsonHash(new JSONObjectWriter(root), requestHashAlgorithm);
    }
}
