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

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.security.interfaces.ECPublicKey;

import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

////////////////////////////////////////////////////////////////////////////////
// This is effectively a "remake" of a subset of JWE.  Why a remake?          //
// Because the encryption system (naturally) borrows heavily from JCS         //
// including public key structures and property naming conventions.           //
//                                                                            //
// The supported algorithms are though JOSE compatible including their names. //
////////////////////////////////////////////////////////////////////////////////

public class EncryptedData {

    public static final String ENCRYPTED_DATA_JSON  = "encryptedData";
    public static final String ENCRYPTED_KEY_JSON   = "encryptedKey";
    public static final String STATIC_KEY_JSON      = "staticKey";
    public static final String EPHEMERAL_KEY_JSON   = "ephemeralKey";
    public static final String IV_JSON              = "iv";
    public static final String TAG_JSON             = "tag";
    public static final String CIPHER_TEXT_JSON     = "cipherText";

    private PublicKey publicKey;

    private ECPublicKey ephemeralPublicKey;  // For ECHD only

    private String dataEncryptionAlgorithm;

    private byte[] iv;

    private byte[] tag;

    private String keyEncryptionAlgorithm;

    private byte[] encryptedKeyData;  // For RSA only

    private byte[] encryptedData;
    
    private byte[] authenticatedData;  // This implementation uses "encryptedKey" which is similar to JWE's protected header
    
    static boolean isRsaKey(String keyEncryptionAlgorithm) {
        return keyEncryptionAlgorithm.contains("RSA");
    }
    
    public static EncryptedData parse(JSONObjectReader rd) throws IOException {
        return new EncryptedData(rd);
    }

    private EncryptedData(JSONObjectReader encryptionObject) throws IOException {
        JSONObjectReader rd = encryptionObject.getObject(ENCRYPTED_DATA_JSON);
        dataEncryptionAlgorithm = rd.getString(JSONSignatureDecoder.ALGORITHM_JSON);
        iv = rd.getBinary(IV_JSON);
        tag = rd.getBinary(TAG_JSON);
        JSONObjectReader encryptedKey = rd.getObject(ENCRYPTED_KEY_JSON);
        authenticatedData = encryptedKey.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        keyEncryptionAlgorithm = encryptedKey.getString(JSONSignatureDecoder.ALGORITHM_JSON);
        if (isRsaKey(keyEncryptionAlgorithm)) {
            publicKey = encryptedKey.getPublicKey(AlgorithmPreferences.JOSE);
            encryptedKeyData = encryptedKey.getBinary(CIPHER_TEXT_JSON);
        } else {
            publicKey = encryptedKey.getObject(STATIC_KEY_JSON).getPublicKey(AlgorithmPreferences.JOSE);
            ephemeralPublicKey = 
                (ECPublicKey) encryptedKey.getObject(EPHEMERAL_KEY_JSON).getPublicKey(AlgorithmPreferences.JOSE);
        }
        encryptedData = rd.getBinary(CIPHER_TEXT_JSON);
    }

    public JSONObjectReader getDecryptedData(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        boolean notFound = true;
        for (DecryptionKeyHolder decryptionKey : decryptionKeys) {
            if (decryptionKey.publicKey.equals(publicKey)) {
                notFound = false;
                if (decryptionKey.keyEncryptionAlgorithm.equals(keyEncryptionAlgorithm)) {
                    return JSONParser.parse(
                        Encryption.contentDecryption(dataEncryptionAlgorithm,
                                                     isRsaKey(keyEncryptionAlgorithm) ?
                                 Encryption.rsaDecryptKey(keyEncryptionAlgorithm,
                                                          encryptedKeyData,
                                                          decryptionKey.privateKey)
                                                     :
                                 Encryption.receiverKeyAgreement(keyEncryptionAlgorithm,
                                                                 dataEncryptionAlgorithm,
                                                                 ephemeralPublicKey,
                                                                 decryptionKey.privateKey),
                                                     encryptedData,
                                                     iv,
                                                     authenticatedData,
                                                     tag));
                }
            }
        }
        throw new IOException(notFound ? "No matching key found" : "No matching key+algorithm found");
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedData,
                                          String dataEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String keyEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        JSONObjectWriter encryptionObject = new JSONObjectWriter();
        JSONObjectWriter encryptedData = encryptionObject.setObject(ENCRYPTED_DATA_JSON);
        JSONObjectWriter encryptedKey = encryptedData.setObject(ENCRYPTED_KEY_JSON)
            .setString(JSONSignatureDecoder.ALGORITHM_JSON, keyEncryptionAlgorithm);
        byte[] dataEncryptionKey = null;
        if (EncryptedData.isRsaKey(keyEncryptionAlgorithm)) {
            encryptedKey.setPublicKey(keyEncryptionKey, AlgorithmPreferences.JOSE);
            dataEncryptionKey = Encryption.generateDataEncryptionKey(dataEncryptionAlgorithm);
            encryptedKey.setBinary(CIPHER_TEXT_JSON,
            Encryption.rsaEncryptKey(keyEncryptionAlgorithm,
                                     dataEncryptionKey,
                                     keyEncryptionKey));
        } else {
            Encryption.EcdhSenderResult result =
                Encryption.senderKeyAgreement(keyEncryptionAlgorithm,
                                              dataEncryptionAlgorithm,
                                              keyEncryptionKey);
            dataEncryptionKey = result.getSharedSecret();
            encryptedKey.setObject(STATIC_KEY_JSON)
                .setPublicKey(keyEncryptionKey, AlgorithmPreferences.JOSE);
            encryptedKey.setObject(EPHEMERAL_KEY_JSON)
                .setPublicKey(result.getEphemeralKey(), AlgorithmPreferences.JOSE);
        }
        Encryption.AuthEncResult result =
            Encryption.contentEncryption(dataEncryptionAlgorithm,
                                         dataEncryptionKey,
                                         unencryptedData.serializeJSONObject(JSONOutputFormats.NORMALIZED),
                                         encryptedKey.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        encryptedData.setString(JSONSignatureDecoder.ALGORITHM_JSON, dataEncryptionAlgorithm)
                     .setBinary(IV_JSON, result.getIv())
                     .setBinary(TAG_JSON, result.getTag())
                     .setBinary(CIPHER_TEXT_JSON, result.getCipherText());
        return encryptionObject;
    }
}
