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

import java.util.LinkedHashMap;

import java.util.logging.Logger;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

public class AuthorityObjectManager extends Thread {

    private static final Logger logger = Logger.getLogger(AuthorityObjectManager.class.getCanonicalName());

    LinkedHashMap<String,byte[]> payeeAuthorityBlobs = new LinkedHashMap<String,byte[]>();

    String providerAuthorityUrl;
    String serviceUrl;
    JSONObjectReader optionalExtensions;
    String[] optionalProviderAccountTypes;
    String[] signatureProfiles;
    ProviderAuthority.EncryptionParameter[] encryptionParameters;
    
    LinkedHashMap<String,PayeeCoreProperties> payees;
    String payeeBaseAuthorityUrl;

    int expiryTimeInSeconds;
    long renewCycle;
    byte[] providerAuthorityBlob;
    ServerX509Signer providerSigner;

    boolean logging;

    
    void update() throws IOException {
        synchronized(this) {
            providerAuthorityBlob = ProviderAuthority.encode(providerAuthorityUrl,
                                                             serviceUrl,
                                                             optionalExtensions,
                                                             optionalProviderAccountTypes,
                                                             signatureProfiles,
                                                             encryptionParameters,
                                                             Expires.inSeconds(expiryTimeInSeconds),
                                                             providerSigner).serializeToBytes(JSONOutputFormats.PRETTY_PRINT);
        }
        if (logging) {
            logger.info("Updated \"" + Messages.PROVIDER_AUTHORITY.toString() + "\"");
        }

        for (String id : payees.keySet()) {
            PayeeCoreProperties payeeCoreProperties = payees.get(id);
            synchronized(this) {
                payeeAuthorityBlobs.put(id, 
                                        PayeeAuthority.encode(payeeBaseAuthorityUrl + id,
                                                              providerAuthorityUrl,
                                                              payeeCoreProperties,
                                                              Expires.inSeconds(expiryTimeInSeconds),
                                                              providerSigner).serializeToBytes(JSONOutputFormats.PRETTY_PRINT));
            }
            if (logging) {
                logger.info("Updated \"" + Messages.PAYEE_AUTHORITY.toString() + "\" with id:" + id);
            }
        }

    }

    public AuthorityObjectManager(String providerAuthorityUrl,
                                  String serviceUrl,
                                  JSONObjectReader optionalExtensions,
                                  String[] optionalProviderAccountTypes,
                                  String[] signatureProfiles,
                                  ProviderAuthority.EncryptionParameter[] encryptionParameters,
                                    
                                  LinkedHashMap<String,PayeeCoreProperties> payees, // Zero-length list is allowed
                                  String payeeBaseAuthorityUrl,

                                  int expiryTimeInSeconds,
                                  ServerX509Signer providerSigner,
                                  
                                  boolean logging) throws IOException {
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.serviceUrl = serviceUrl;
        this.optionalExtensions = optionalExtensions;
        this.optionalProviderAccountTypes = optionalProviderAccountTypes;
        this.signatureProfiles = signatureProfiles;
        this.encryptionParameters = encryptionParameters;

        this.payees = payees;
        this.payeeBaseAuthorityUrl = payeeBaseAuthorityUrl;
        
        this.expiryTimeInSeconds = expiryTimeInSeconds;
        this.renewCycle = expiryTimeInSeconds * 500;
        this.providerSigner = providerSigner;
        this.logging = logging;
        update();
        start();
    }

    public synchronized byte[] getProviderAuthorityBlob() {
        return providerAuthorityBlob;
    }

    public synchronized byte[] getPayeeAuthorityBlob(String id) {
        return payeeAuthorityBlobs.get(id);
    }

    public synchronized void updateProviderSigner(ServerX509Signer providerSigner) throws IOException {
        this.providerSigner = providerSigner;
        update();
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
