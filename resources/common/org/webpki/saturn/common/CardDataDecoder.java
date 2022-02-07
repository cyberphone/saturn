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

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.ContentEncryptionAlgorithms;
import org.webpki.crypto.KeyEncryptionAlgorithms;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;


// This class holds the data associated with a virtual card (modulo the logotype).
// The data is embedded in an SKS (Secure Key Store) extension object belonging to the signature key.

public class CardDataDecoder implements BaseProperties {

    // Since deployed card data should preferably remain useful even if the Saturn protocol
    // changes, multiple versions may need to be supported by client software.
    static final String VERSION_JSON   = "version";
    static final String ACTUAL_VERSION = "5";
    
    // SKS specific solution for linking related keys
    static final String ACCOUNT_STATUS_KEY_HASH_JSON = "accountStatusKeyHash";
    
    public CardDataDecoder(String expectedVersion, byte[] cardDataBlob) throws IOException,
                                                                               GeneralSecurityException {
        JSONObjectReader rd = JSONParser.parse(cardDataBlob);
        version = rd.getString(VERSION_JSON);
        if (version.equals(expectedVersion)) {
            recognized = true;
            paymentMethod= rd.getString(PAYMENT_METHOD_JSON);
            accountId = rd.getString(ACCOUNT_ID_JSON);
            currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
            credentialId = rd.getString(CREDENTIAL_ID_JSON);
            authorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
            requestHashAlgorithm = CryptoUtils.getHashAlgorithm(rd, REQUEST_HASH_ALGORITHM_JSON);
            signatureAlgorithm = 
                    CryptoUtils.getSignatureAlgorithm(rd, SIGNATURE_ALGORITHM_JSON);
            JSONObjectReader ep = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
            contentEncryptionAlgorithm = ContentEncryptionAlgorithms
                    .getAlgorithmFromId(ep.getString(DATA_ENCRYPTION_ALGORITHM_JSON));
            keyEncryptionAlgorithm = KeyEncryptionAlgorithms
                    .getAlgorithmFromId(ep.getString(KEY_ENCRYPTION_ALGORITHM_JSON));
            encryptionKey = ep.getPublicKey();
            optionalKeyId = ep.getStringConditional(JSONCryptoHelper.KEY_ID_JSON);
            optionalAccountStatusKeyHash = rd.getBinaryConditional(ACCOUNT_STATUS_KEY_HASH_JSON);
            rd.checkForUnread();
        }
    }

    public CardDataDecoder(byte[] cardDataBlob) throws IOException, GeneralSecurityException {
        this(ACTUAL_VERSION, cardDataBlob);
    }

    Currencies currency;
    public Currencies getCurrency() {
        return currency;
    }

    boolean recognized;
    public boolean isRecognized() {
        return recognized;
    }

    String version;
    public String getVersion() {
        return version;
    }

    String paymentMethod;
    public String getPaymentMethod() {
        return paymentMethod;
    }

    String accountId;
    public String getAccountId() {
        return accountId;
    }

    String credentialId;
    public String getCredentialId() {
        return credentialId;
    }

    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    HashAlgorithms requestHashAlgorithm;
    public HashAlgorithms getRequestHashAlgorithm() {
        return requestHashAlgorithm;
    }
    AsymSignatureAlgorithms signatureAlgorithm;
    public AsymSignatureAlgorithms getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    ContentEncryptionAlgorithms contentEncryptionAlgorithm;
    public ContentEncryptionAlgorithms getContentEncryptionAlgorithm() {
        return contentEncryptionAlgorithm;
    }
    
    KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    public KeyEncryptionAlgorithms getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    PublicKey encryptionKey;
    public PublicKey getEncryptionKey() {
        return encryptionKey;
    }

    String optionalKeyId;
    public String getOptionalKeyId() {
        return optionalKeyId;
    }

    byte[] optionalAccountStatusKeyHash;
    public byte[] getOptionalAccountStatusKeyHash() {
        return optionalAccountStatusKeyHash;
    }
}
