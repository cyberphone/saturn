/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.bank;

import java.io.IOException;

import java.security.PublicKey;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

import org.webpki.saturn.common.BaseProperties;

public class UserAccountEntry implements BaseProperties {
    PublicKey publicKey;
    String paymentMethod;
    String accountId;
    boolean cardFormatAccountId;
    String providerAuthorityUrl;
    AsymSignatureAlgorithms signatureAlgorithm;
    String optionalKeyId;
    PublicKey encryptionKey;
    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    
    RiskBasedAuthentication riskBasedAuthentication = new RiskBasedAuthentication();

    public UserAccountEntry(JSONObjectReader rd) throws IOException {
        paymentMethod = rd.getString(PAYMENT_METHOD_JSON);
        accountId = rd.getString(ACCOUNT_ID_JSON);
        cardFormatAccountId = rd.getBoolean(CARD_FORMAT_ACCOUNT_ID_JSON);
        signatureAlgorithm = AsymSignatureAlgorithms
            .getAlgorithmFromId(rd.getString(SIGNATURE_ALGORITHM_JSON), AlgorithmPreferences.JOSE);
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        publicKey = rd.getPublicKey();
        JSONObjectReader encryptionParameters = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
        if (encryptionParameters.hasProperty(JSONSignatureDecoder.KEY_ID_JSON)) {
            optionalKeyId = encryptionParameters.getString(JSONSignatureDecoder.KEY_ID_JSON);
        }
        encryptionKey = encryptionParameters.getPublicKey();
        dataEncryptionAlgorithm = DataEncryptionAlgorithms
            .getAlgorithmFromId(encryptionParameters.getString(DATA_ENCRYPTION_ALGORITHM_JSON));
        keyEncryptionAlgorithm = KeyEncryptionAlgorithms
            .getAlgorithmFromId(encryptionParameters.getString(KEY_ENCRYPTION_ALGORITHM_JSON));
        rd.checkForUnread();
    }

    public String getAccountId() {
         return accountId;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return optionalKeyId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public RiskBasedAuthentication getRiskBasedAuthentication() {
        return riskBasedAuthentication;
    }
}
