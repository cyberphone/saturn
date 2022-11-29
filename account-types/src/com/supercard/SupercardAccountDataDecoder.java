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
package com.supercard;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

import org.webpki.saturn.common.AccountDataDecoder;
import org.webpki.saturn.common.BaseProperties;

import org.webpki.util.ISODateTime;

public final class SupercardAccountDataDecoder extends AccountDataDecoder {

    static final String CONTEXT = "https://supercard.com/saturn/v3#account";

    static final String CARD_NUMBER_JSON   = "cardNumber";    // PAN
    static final String CARD_HOLDER_JSON   = "cardHolder";    // Name

    String cardHolder;                   // Name
    public String getCardHolder() {
        return cardHolder;
    }

    GregorianCalendar expirationDate;    // Card expiration date
    public GregorianCalendar getExpirationDate() {
        return expirationDate;
    }

    @Override
    protected void readJSONData(JSONObjectReader rd) throws IOException {
        readOptionalNonce(rd);
        accountId = rd.getString(CARD_NUMBER_JSON);
        cardHolder = rd.getString(CARD_HOLDER_JSON);
        expirationDate = rd.getDateTime(BaseProperties.EXPIRES_JSON, ISODateTime.COMPLETE);
    }

    @Override
    protected byte[] getAccountObject() throws IOException {
        return getWriter().serializeToBytes(JSONOutputFormats.CANONICALIZED);
    }

    @Override
    public String getContext() {
        return CONTEXT;
    }

    @Override
    protected SupercardAccountDataEncoder createEncoder() {
        return new SupercardAccountDataEncoder();
    }
}
