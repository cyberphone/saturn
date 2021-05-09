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

public class PayeeAuthorityDecoder implements BaseProperties {
    
    public PayeeAuthorityDecoder(JSONObjectReader rd, String expectedAuthorityUrl) 
            throws IOException, GeneralSecurityException {
        root = Messages.PAYEE_AUTHORITY.parseBaseMessage(rd);
        payeeAuthorityUrl = rd.getString(PAYEE_AUTHORITY_URL_JSON);
        if (!payeeAuthorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + PAYEE_AUTHORITY_URL_JSON + 
                                  "\" mismatch, read=" + 
                                  payeeAuthorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        payeeCoreProperties = new PayeeCoreProperties(rd);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        expires = rd.getDateTime(EXPIRES_JSON, ISODateTime.COMPLETE);
        expiresInMillis = expires.getTimeInMillis();
        attestationKey = rd.getSignature(ISSUER_SIGNATURE_JSON, 
                    new JSONCryptoHelper.Options()
                        .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                        .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED))
                .getPublicKey();
        rd.checkForUnread();
    }

    long expiresInMillis;

    String payeeAuthorityUrl;
    public String getPayeeAuthorityUrl() {
        return payeeAuthorityUrl;
    }

    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    PayeeCoreProperties payeeCoreProperties;
    public PayeeCoreProperties getPayeeCoreProperties() {
        return payeeCoreProperties;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
     public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    PublicKey attestationKey;
    public PublicKey getAttestationKey() {
        return attestationKey;
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }

    boolean cached;  // Set by ExternalCalls
    public boolean isCached() {
        return cached;
    }
}
