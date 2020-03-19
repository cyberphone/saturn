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

import java.security.PublicKey;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

// This class holds the data associated with a virtual card (modulo the logotype).
// The data is embedded in an SKS (Secure Key Store) extension object belonging to the signature key.

public class CardDataDecoder {

    // Since deployed card data should preferably remain useful even if the Saturn protocol
    // changes, multiple versions may need to be supported by client software.
    static final String VERSION_JSON   = "version";
    static final String ACTUAL_VERSION = "3";
    
    static final String REQUEST_HASH_ALGORITHM_JSON = "requestHashAlgorithm";
    
    static final String ACCOUNT_STATUS_KEY_HASH     = "accountStatusKeyHash";
    static final String TEMPORARY_BALANCE_FIX       = "temp.bal.fix";
    
    public CardDataDecoder(String expectedVersion, byte[] cardDataBlob) throws IOException {
        JSONObjectReader rd = JSONParser.parse(cardDataBlob);
        version = rd.getString(VERSION_JSON);
        if (version.equals(expectedVersion)) {
            recognized = true;
            paymentMethod= rd.getString(BaseProperties.PAYMENT_METHOD_JSON);
            accountId = rd.getString(BaseProperties.ACCOUNT_ID_JSON);
            credentialId = rd.getString(BaseProperties.CREDENTIAL_ID_JSON);
            authorityUrl = rd.getString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON);
            requestHashAlgorithm = 
                    HashAlgorithms.getAlgorithmFromId(rd.getString(REQUEST_HASH_ALGORITHM_JSON),
                                                      AlgorithmPreferences.JOSE);
            signatureAlgorithm = AsymSignatureAlgorithms
                    .getAlgorithmFromId(rd.getString(BaseProperties.SIGNATURE_ALGORITHM_JSON),
                                                     AlgorithmPreferences.JOSE);
            JSONObjectReader ep = rd.getObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON);
            dataEncryptionAlgorithm = DataEncryptionAlgorithms
                    .getAlgorithmFromId(ep.getString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON));
            keyEncryptionAlgorithm = KeyEncryptionAlgorithms
                    .getAlgorithmFromId(ep.getString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON));
            encryptionKey = ep.getPublicKey();
            optionalKeyId = ep.getStringConditional(JSONCryptoHelper.KEY_ID_JSON);
            optionalAccountStatusKeyHash = rd.getBinaryConditional(ACCOUNT_STATUS_KEY_HASH);
            tempBalanceFix = rd.getMoney(TEMPORARY_BALANCE_FIX, 2);
            rd.checkForUnread();
        }
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

    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    public DataEncryptionAlgorithms getDataEncryptionAlgorithm() {
        return dataEncryptionAlgorithm;
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

    BigDecimal tempBalanceFix;
    public BigDecimal getTempBalanceFix() {
        return tempBalanceFix;
    }    
}
