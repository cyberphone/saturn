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

import java.security.PublicKey;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

public class UserAccountEntry implements BaseProperties {
    PublicKey publicKey;
    String type;
    String id;
    boolean cardFormatAccountId;
    String providerAuthorityUrl;
    AsymSignatureAlgorithms signatureAlgorithm;
    String optionalKeyId;
    PublicKey encryptionKey;
    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    
    RiskBasedAuthentication riskBasedAuthentication = new RiskBasedAuthentication();

    public UserAccountEntry(JSONObjectReader rd) throws IOException {
        type = rd.getObject(ACCOUNT_JSON).getString(TYPE_JSON);
        id = rd.getObject(ACCOUNT_JSON).getString(ID_JSON);
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

    public String getId() {
         return id;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return optionalKeyId;
    }

    public String getType() {
        return type;
    }

    public RiskBasedAuthentication getRiskBasedAuthentication() {
        return riskBasedAuthentication;
    }
}
