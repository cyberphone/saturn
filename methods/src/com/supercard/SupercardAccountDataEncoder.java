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
package com.supercard;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectWriter;

import org.webpki.saturn.common.BaseProperties;
import org.webpki.saturn.common.AuthorizationResponse;

public final class SupercardAccountDataEncoder extends AuthorizationResponse.AccountDataEncoder {

    static final String ACCOUNT_DATA       = "https://supercard.com/saturn/v3#ad";

    static final String CARD_NUMBER_JSON   = "cardNumber";    // PAN
    static final String CARD_HOLDER_JSON   = "cardHolder";    // Name
    static final String SECURITY_CODE_JSON = "securityCode";  // CCV

    String cardNumber;                   // PAN
    String cardHolder;                   // Name
    GregorianCalendar expirationDate;    // Card expiration date
    String securityCode;                 // CCV or similar

    public SupercardAccountDataEncoder(String cardNumber,
                                       String cardHolder,
                                       GregorianCalendar expirationDate,
                                       String securityCode) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expirationDate = expirationDate;
        this.securityCode = securityCode;
    }

    @Override
    protected JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        return wr.setString(CARD_NUMBER_JSON, cardNumber)
                 .setString(CARD_HOLDER_JSON, cardHolder)
                 .setDateTime(BaseProperties.EXPIRES_JSON, expirationDate, true)
                 .setString(SECURITY_CODE_JSON, securityCode);
    }

    @Override
    public String getContext() {
        return ACCOUNT_DATA;
    }
}
