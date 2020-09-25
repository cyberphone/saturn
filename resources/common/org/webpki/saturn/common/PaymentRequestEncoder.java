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

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class PaymentRequestEncoder implements BaseProperties {
    
    public static JSONObjectWriter encode(String payeeCommonName,
                                          BigDecimal amount,
                                          Currencies currency,
                                          NonDirectPaymentEncoder optionalNonDirectPayment,
                                          String referenceId,
                                          GregorianCalendar timeStamp,
                                          GregorianCalendar expires) throws IOException {
        return new JSONObjectWriter()
            .setString(COMMON_NAME_JSON, payeeCommonName)
            .setMoney(AMOUNT_JSON, amount, currency.getDecimals())
            .setString(CURRENCY_JSON, currency.toString())
            .setDynamic((wr) -> optionalNonDirectPayment == null ?
                    wr : wr.setObject(NON_DIRECT_PAYMENT_JSON, optionalNonDirectPayment.root))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, timeStamp, ISODateTime.UTC_NO_SUBSECONDS)
            .setDateTime(EXPIRES_JSON, expires, ISODateTime.UTC_NO_SUBSECONDS);
    }
}
