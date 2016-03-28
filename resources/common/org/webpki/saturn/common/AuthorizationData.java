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

import java.util.Date;
import java.util.GregorianCalendar;

import java.security.PublicKey;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.AsymKeySignerInterface;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONSignatureDecoder;

public class AuthorizationData implements BaseProperties {

    public static final String SOFTWARE_ID      = "WebPKI.org - Wallet";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          AccountDescriptor accountDescriptor,
                                          Date timeStamp,
                                          JSONAsymKeySigner signer) throws IOException {
        return new JSONObjectWriter()
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                .setString(JSONSignatureDecoder.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                .setBinary(JSONSignatureDecoder.VALUE_JSON, paymentRequest.getRequestHash()))
            .setString(DOMAIN_NAME_JSON, domainName)
            .setObject(PAYER_ACCOUNT_JSON, accountDescriptor.writeObject())
            .setDateTime(TIME_STAMP_JSON, timeStamp, false)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
            .setSignature (signer);
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          AccountDescriptor accountDescriptor,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          AsymKeySignerInterface signer) throws IOException {
        return encode(paymentRequest,
                      domainName,
                      accountDescriptor,
                      new Date(),
                      new JSONAsymKeySigner(signer)
                          .setSignatureAlgorithm(signatureAlgorithm)
                          .setAlgorithmPreferences(AlgorithmPreferences.JOSE));
    }

    public static String formatCardNumber(String accountId) {
        StringBuffer s = new StringBuffer();
        int q = 0;
        for (char c : accountId.toCharArray()) {
            if (q != 0 && q % 4 == 0) {
                s.append(' ');
            }
            s.append(c);
            q++;
        }
        return s.toString();
    }

    public AuthorizationData(JSONObjectReader rd) throws IOException {
        requestHash = RequestHash.parse(rd);
        domainName = rd.getString(DOMAIN_NAME_JSON);
        accountDescriptor = new AccountDescriptor(rd.getObject(PAYER_ACCOUNT_JSON));
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        publicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        rd.checkForUnread();
    }

    byte[] requestHash;
    public byte[] getRequestHash() {
        return requestHash;
    }

    String domainName;
    public String getDomainName() {
        return domainName;
    }

    AccountDescriptor accountDescriptor;
    public AccountDescriptor getAccountDescriptor() {
        return accountDescriptor;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
