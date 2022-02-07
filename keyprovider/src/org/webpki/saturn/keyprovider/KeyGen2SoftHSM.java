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

package org.webpki.saturn.keyprovider;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.cert.X509Certificate;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import java.security.spec.ECGenParameterSpec;

import java.util.LinkedHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.HmacAlgorithms;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.SignatureWrapper;

import org.webpki.keygen2.ServerCryptoInterface;

import org.webpki.sks.SecureKeyStore;

import org.webpki.util.ArrayUtil;

import org.webpki.saturn.common.KeyStoreEnumerator;

public class KeyGen2SoftHSM implements ServerCryptoInterface {

    ////////////////////////////////////////////////////////////////////////////////////////
    // Private and secret keys would in a HSM implementation be represented as handles
    ////////////////////////////////////////////////////////////////////////////////////////
    LinkedHashMap<PublicKey,PrivateKey> keyManagementKeys = new LinkedHashMap<>();
    
    public KeyGen2SoftHSM(KeyStoreEnumerator keyStoreEnumerator) throws IOException {
        keyManagementKeys.put(keyStoreEnumerator.getPublicKey(), 
                              keyStoreEnumerator.getPrivateKey());
    }
    
    ECPrivateKey serverEcPrivateKey;
    
    byte[] sessionKey;
  
    @Override
    public ECPublicKey generateEphemeralKey(KeyAlgorithms ephemeralKeyAlgorithm)
    throws IOException, GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec eccgen = new ECGenParameterSpec(ephemeralKeyAlgorithm.getJceName());
        generator.initialize (eccgen, new SecureRandom());
        KeyPair kp = generator.generateKeyPair();
        serverEcPrivateKey = (ECPrivateKey) kp.getPrivate();
        return (ECPublicKey) kp.getPublic();
    }
  
    @Override
    public void generateAndVerifySessionKey(ECPublicKey clientEphemeralKey,
                                            byte[] kdfData,
                                            byte[] attestationArguments,
                                            X509Certificate deviceCertificate,
                                            byte[] sessionAttestation)
            throws IOException, GeneralSecurityException {
        // SP800-56A C(2, 0, ECC CDH)
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(serverEcPrivateKey);
        keyAgreement.doPhase(clientEphemeralKey, true);
        byte[] Z = keyAgreement.generateSecret();
  
        // The custom KDF
        Mac mac = Mac.getInstance(HmacAlgorithms.HMAC_SHA256.getJceName ());
        mac.init (new SecretKeySpec(Z, "RAW"));
        sessionKey = mac.doFinal(kdfData);

        // E2ES mode only
            PublicKey devicePublicKey = deviceCertificate.getPublicKey();
  
            // Verify that attestation was signed by the device key
        if (!new SignatureWrapper(devicePublicKey instanceof RSAPublicKey ?
                                        AsymSignatureAlgorithms.RSA_SHA256 
                                                                          :
                                        AsymSignatureAlgorithms.ECDSA_SHA256, 
                                  devicePublicKey)
                  .update(attestationArguments)
                  .verify(sessionAttestation)) {
            throw new IOException("Verify provisioning signature failed");
        }
    }
  
    @Override
    public byte[] mac(byte[] data, byte[] keyModifier) throws IOException, GeneralSecurityException {
        Mac mac = Mac.getInstance(HmacAlgorithms.HMAC_SHA256.getJceName ());
        mac.init(new SecretKeySpec(ArrayUtil.add(sessionKey, keyModifier), "RAW"));
        return mac.doFinal(data);
    }
  
    @Override
    public byte[] encrypt(byte[] data) throws IOException, GeneralSecurityException {
        byte[] key = mac(SecureKeyStore.KDF_ENCRYPTION_KEY, new byte[0]);
        Cipher crypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        crypt.init(Cipher.ENCRYPT_MODE,
                   new SecretKeySpec(key, "AES"), 
                   new IvParameterSpec(iv));
        return ArrayUtil.add(iv, crypt.doFinal(data));
    }
  
    @Override
    public byte[] generateNonce() throws IOException {
        byte[] rnd = new byte[32];
        new SecureRandom().nextBytes(rnd);
        return rnd;
    }
  
    @Override
    public byte[] generateKeyManagementAuthorization(PublicKey keyManagementKey, byte[] data)
            throws IOException, GeneralSecurityException {
        return new SignatureWrapper(keyManagementKey instanceof RSAPublicKey ?
                                           AsymSignatureAlgorithms.RSA_SHA256 
                                                                             :
                                           AsymSignatureAlgorithms.ECDSA_SHA256,
                                    keyManagementKeys.get(keyManagementKey))
            .update(data)
            .sign();
    }
  
    @Override
    public PublicKey[] enumerateKeyManagementKeys() throws IOException {
        return keyManagementKeys.keySet().toArray(new PublicKey[0]);
    }
}
