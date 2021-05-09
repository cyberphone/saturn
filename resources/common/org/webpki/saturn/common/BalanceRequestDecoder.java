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

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.util.GregorianCalendar;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;

import org.webpki.util.ISODateTime;

public class BalanceRequestDecoder implements BaseProperties {
    
    public BalanceRequestDecoder(JSONObjectReader rd, 
                                 JSONCryptoHelper.Options signatureOptions) 
            throws IOException, GeneralSecurityException {
        Messages.BALANCE_REQUEST.parseBaseMessage(rd);
        recipientUrl = rd.getString(RECIPIENT_URL_JSON);
        accountId = rd.getString(ACCOUNT_ID_JSON);
        credentialId = rd.getString(CREDENTIAL_ID_JSON);
        currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        publicKey = rd.getSignature(REQUEST_SIGNATURE_JSON, signatureOptions).getPublicKey();
        rd.checkForUnread();
    }

    String recipientUrl;
    public String getRecipientUrl() {
        return recipientUrl;
    }

    String accountId;
    public String getAccountId() {
        return accountId;
    }

    String credentialId;
    public String getCredentialId() {
        return credentialId;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }
}
