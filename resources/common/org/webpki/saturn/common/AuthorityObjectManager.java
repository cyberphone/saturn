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

import java.util.LinkedHashMap;

import java.util.logging.Logger;

import org.webpki.json.JSONOutputFormats;

public class AuthorityObjectManager extends Thread {

    private static final Logger logger = Logger.getLogger(AuthorityObjectManager.class.getCanonicalName());

    LinkedHashMap<String,byte[]> payeeAuthorityBlobs = new LinkedHashMap<String,byte[]>();

    String providerAuthorityUrl;
    String authorizeUrl;
    String transactionUrl;
    String[] optionalProviderAccountTypes;
    PublicKey optionalEncryptionKey;
    
    LinkedHashMap<String,PayeeCoreProperties> payees;
    String payeeBaseAuthorityUrl;

    int expiryTimeInSeconds;
    long renewCycle;
    byte[] providerAuthorityBlob;
    ServerX509Signer providerSigner;


    void update() throws IOException {
        synchronized(this) {
            providerAuthorityBlob = ProviderAuthority.encode(providerAuthorityUrl,
                                                             authorizeUrl,
                                                             transactionUrl,
                                                             optionalProviderAccountTypes,
                                                             optionalEncryptionKey,
                                                             Expires.inSeconds(expiryTimeInSeconds),
                                                             providerSigner).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT);
        }
        logger.info("Updated \"" + Messages.PROVIDER_AUTHORITY.toString() + "\"");

        for (String id : payees.keySet()) {
            PayeeCoreProperties payeeCoreProperties = payees.get(id);
            synchronized(this) {
                payeeAuthorityBlobs.put(id, 
                                        PayeeAuthority.encode(payeeBaseAuthorityUrl + id,
                                                              providerAuthorityUrl,
                                                              new Payee(payeeCoreProperties.getCommonName(), id),
                                                              payeeCoreProperties.getPublicKey(),
                                                              Expires.inSeconds(expiryTimeInSeconds),
                                                              providerSigner).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT));
            }
            logger.info("Updated \"" + Messages.PAYEE_AUTHORITY.toString() + "\" with id:" + id);
        }

    }

    public AuthorityObjectManager(String providerAuthorityUrl,
                                    String authorizeUrl,
                                    String transactionUrl,
                                    String[] optionalProviderAccountTypes,
                                    PublicKey optionalEncryptionKey,
                                    
                                    LinkedHashMap<String,PayeeCoreProperties> payees, // Zero-length list is allowed
                                    String payeeBaseAuthorityUrl,

                                    int expiryTimeInSeconds,
                                    ServerX509Signer providerSigner) throws IOException {
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.authorizeUrl = authorizeUrl;
        this.transactionUrl = transactionUrl;
        this.optionalProviderAccountTypes = optionalProviderAccountTypes;
        this.optionalEncryptionKey = optionalEncryptionKey;

        this.payees = payees;
        this.payeeBaseAuthorityUrl = payeeBaseAuthorityUrl;
        
        this.expiryTimeInSeconds = expiryTimeInSeconds;
        this.renewCycle = expiryTimeInSeconds * 500;
        this.providerSigner = providerSigner;
        update();
        start();
    }

    public synchronized byte[] getProviderAuthorityBlob() {
        return providerAuthorityBlob;
    }

    public synchronized byte[] getPayeeAuthorityBlob(String id) {
        return payeeAuthorityBlobs.get(id);
    }

    @Override
    public void run() {
        while (true) {
            try {
                sleep(renewCycle);
                update();
            } catch (Exception e) {
                break;
            }
        }
    }
}
