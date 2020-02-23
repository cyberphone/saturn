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
import java.io.InputStream;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.util.Enumeration;
import java.util.ArrayList;

import org.webpki.crypto.KeyStoreReader;

public class KeyStoreEnumerator {

    ArrayList<X509Certificate> certificatePath = new ArrayList<>();
    PrivateKey privateKey = null;
    String keyId;
    
    public KeyStoreEnumerator(InputStream is, String password) throws IOException {
        try {
            KeyStore ks = KeyStoreReader.loadKeyStore(is, password);
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                keyId = aliases.nextElement();
                if (ks.isKeyEntry(keyId)) {
                    privateKey = (PrivateKey) ks.getKey(keyId, password.toCharArray());
                    for (Certificate cert : ks.getCertificateChain(keyId)) {
                        certificatePath.add((X509Certificate) cert);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (privateKey == null) {
            throw new IOException("No private key found!");
        }
    }

    public PublicKey getPublicKey() {
        return certificatePath.get(0).getPublicKey();
    }

    public X509Certificate[] getCertificatePath() {
        return certificatePath.toArray(new X509Certificate[0]);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
