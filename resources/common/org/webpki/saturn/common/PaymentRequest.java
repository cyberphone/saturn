/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

import java.math.BigDecimal;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONSignatureDecoder;

public class PaymentRequest implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Payee";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(Payee payee,
                                          BigDecimal amount,
                                          Currencies currency,
                                          NonDirectPayments optionalNonDirectPayment,
                                          String referenceId,
                                          GregorianCalendar timeStamp,
                                          GregorianCalendar expires,
                                          JSONAsymKeySigner signer) throws IOException {
        return new JSONObjectWriter()
            .setObject(PAYEE_JSON, payee.writeObject())
            .setBigDecimal(AMOUNT_JSON, amount, currency.getDecimals())
            .setString(CURRENCY_JSON, currency.toString())
            .setDynamic((wr) -> optionalNonDirectPayment == null ?
                    wr : wr.setString(NON_DIRECT_PAYMENT_JSON, optionalNonDirectPayment.toString()))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, timeStamp, true)
            .setDateTime(EXPIRES_JSON, expires, true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }

    public PaymentRequest(JSONObjectReader rd) throws IOException {
        root = rd;
        payee = new Payee(rd.getObject(PAYEE_JSON));
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        if (rd.hasProperty(NON_DIRECT_PAYMENT_JSON)) {
            nonDirectPayment = NonDirectPayments.fromType(rd.getString(NON_DIRECT_PAYMENT_JSON));
        }
        amount = rd.getBigDecimal(AMOUNT_JSON, currency.getDecimals());
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(new JSONSignatureDecoder.Options());
        rd.checkForUnread();
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    Payee payee;
    public Payee getPayee() {
        return payee;
    }

    BigDecimal amount;
    public BigDecimal getAmount() {
        return amount;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    NonDirectPayments nonDirectPayment;
    public NonDirectPayments getNonDirectPayment() {
        return nonDirectPayment;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }
    
    GregorianCalendar dateTime;
    public GregorianCalendar getDateTime() {
        return dateTime;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONObjectReader root;

    public byte[] getRequestHash() throws IOException {
        return RequestHash.getRequestHash(new JSONObjectWriter(root));
    }

    public void consistencyCheck(PaymentRequest paymentRequest) throws IOException {
        if (paymentRequest.currency != currency ||
            !paymentRequest.amount.equals(amount) ||
            paymentRequest.nonDirectPayment != nonDirectPayment ||
            !paymentRequest.referenceId.equals(referenceId)) {
            throw new IOException("Inconsistent \"" + PAYMENT_REQUEST_JSON + "\" objects");
        }
    }
}
