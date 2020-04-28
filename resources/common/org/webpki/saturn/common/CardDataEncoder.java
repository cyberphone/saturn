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

import java.security.PublicKey;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectWriter;

// This class holds the data associated with a virtual card (modulo the logotype).
// The data is embedded in an SKS (Secure Key Store) extension object belonging to the signature key.

public class CardDataEncoder {
     
    public static JSONObjectWriter encode(String paymentMethod,
                                          String credentialId,
                                          String accountId,
                                          String authorityUrl,
                                          HashAlgorithms reguestHashAlgorithm,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                                          PublicKey encryptionKey,
                                          String optionalKeyId,
                                          byte[] optionalAccountStatusKeyHash) throws IOException {
        return new JSONObjectWriter()
            .setString(CardDataDecoder.VERSION_JSON, CardDataDecoder.ACTUAL_VERSION)
            .setString(BaseProperties.PAYMENT_METHOD_JSON, paymentMethod)
            .setString(BaseProperties.ACCOUNT_ID_JSON, accountId)
            .setString(BaseProperties.CREDENTIAL_ID_JSON, credentialId)
            .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON, authorityUrl)
            .setString(CardDataDecoder.REQUEST_HASH_ALGORITHM_JSON, 
                       reguestHashAlgorithm.getJoseAlgorithmId())
            .setString(BaseProperties.SIGNATURE_ALGORITHM_JSON,
                       signatureAlgorithm.getJoseAlgorithmId())
            .setObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, 
                           dataEncryptionAlgorithm.toString())
                .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                           keyEncryptionAlgorithm.toString())
                .setPublicKey(encryptionKey)
                .setDynamic((wr)-> optionalKeyId == null ?
                        wr : wr.setString(JSONCryptoHelper.KEY_ID_JSON, optionalKeyId)))
            .setDynamic((wr)-> optionalAccountStatusKeyHash == null ?
                    wr : wr.setBinary(CardDataDecoder.ACCOUNT_STATUS_KEY_HASH,
                                      optionalAccountStatusKeyHash));
    }
}
