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

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import java.security.PublicKey;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.DataEncryptionAlgorithms;

import org.webpki.util.ISODateTime;

public class AuthorizationDataDecoder implements BaseProperties {

    public AuthorizationDataDecoder(JSONObjectReader rd, 
                             JSONCryptoHelper.Options signatureOptions) throws IOException {
        JSONObjectReader requestHashObject = rd.getObject(REQUEST_HASH_JSON);
        requestHashAlgorithm = CryptoUtils.getHashAlgorithm(requestHashObject, 
                                                            JSONCryptoHelper.ALGORITHM_JSON);
        requestHash = requestHashObject.getBinary(VALUE_JSON);

        JSONObjectReader keyHashObject = rd.getObject(KEY_HASH_JSON);
        keyHashAlgorithm = CryptoUtils.getHashAlgorithm(keyHashObject, 
                                                        JSONCryptoHelper.ALGORITHM_JSON);
        keyHash = keyHashObject.getBinary(VALUE_JSON);

        domainName = rd.getString(DOMAIN_NAME_JSON);

        paymentMethodUrl = rd.getString(PAYMENT_METHOD_JSON);
        credentialId = rd.getString(CREDENTIAL_ID_JSON);
        accountId = rd.getString(ACCOUNT_ID_JSON);

        JSONObjectReader encryptionParameters = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
        dataEncryptionAlgorithm = DataEncryptionAlgorithms
            .getAlgorithmFromId(encryptionParameters.getString(JSONCryptoHelper.ALGORITHM_JSON));
        dataEncryptionKey = encryptionParameters.getBinary(KEY_JSON);

        if (rd.hasProperty(USER_RESPONSE_ITEMS_JSON)) {
            JSONArrayReader ar = rd.getArray(USER_RESPONSE_ITEMS_JSON);
             do {
                 UserResponseItem challengeResult = new UserResponseItem(ar.getObject());
                if (optionalUserResponseItems.put(challengeResult.getName(), challengeResult) != null) {
                    throw new IOException("Duplicate: " + challengeResult.getName());
                }
            } while (ar.hasMore());
        }

        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        publicKey = rd.getSignature(AUTHORIZATION_SIGNATURE_JSON, signatureOptions).getPublicKey();
        rd.checkForUnread();
    }

    HashAlgorithms requestHashAlgorithm;
    public HashAlgorithms getRequestHashAlgorithm() {
        return requestHashAlgorithm;
    }

    byte[] requestHash;
    public byte[] getRequestHash() {
        return requestHash;
    }

    HashAlgorithms keyHashAlgorithm;
    public HashAlgorithms getKeyHashAlgorithm() {
        return keyHashAlgorithm;
    }

    byte[] keyHash;
    public byte[] getKeyHash() {
        return keyHash;
    }

    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    public DataEncryptionAlgorithms getDataEncryptionAlgorithm() {
        return dataEncryptionAlgorithm;
    }

    byte[] dataEncryptionKey;
    public byte[] getDataEncryptionKey() {
        return dataEncryptionKey;
    }

    LinkedHashMap<String,UserResponseItem> optionalUserResponseItems = new LinkedHashMap<>();
    public LinkedHashMap<String,UserResponseItem> getUserResponseItems() {
        return optionalUserResponseItems;
    }

    String domainName;
    public String getDomainName() {
        return domainName;
    }

    String paymentMethodUrl;
    public String getPaymentMethodUrl() {
        return paymentMethodUrl;
    }

    String credentialId;
    public String getCredentialId() {
        return credentialId;
    }

    String accountId;
    public String getAccountId() {
        return accountId;
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