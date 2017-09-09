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

import java.util.TreeMap;
import java.util.SortedMap;

import java.util.logging.Logger;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

public class AuthorityObjectManager extends Thread {

    private static final Logger logger = Logger.getLogger(AuthorityObjectManager.class.getCanonicalName());

    TreeMap<String,byte[]> payeeAuthorityBlobs = new TreeMap<String,byte[]>();

    String providerAuthorityUrl;
    String providerHomePage;
    String serviceUrl;
    JSONObjectReader optionalExtensions;
    String[] optionalProviderAccountTypes;
    SignatureProfiles[] signatureProfiles;
    ProviderAuthority.EncryptionParameter[] encryptionParameters;
    HostingProvider optionalHostingProvider; 

    SortedMap<String,PayeeCoreProperties> payees;
    String payeeBaseAuthorityUrl;

    int expiryTimeInSeconds;
    long renewCycle;
    byte[] providerAuthorityBlob;
    ServerX509Signer providerSigner;
    ServerAsymKeySigner attestationSigner;

    boolean logging;

    
    void update() throws IOException {
        if (providerSigner != null) {
            synchronized(this) {
                providerAuthorityBlob = ProviderAuthority.encode(providerAuthorityUrl,
                                                                 providerHomePage,
                                                                 serviceUrl,
                                                                 optionalExtensions,
                                                                 optionalProviderAccountTypes,
                                                                 signatureProfiles,
                                                                 encryptionParameters,
                                                                 optionalHostingProvider, 
                                                                 TimeUtil.inSeconds(expiryTimeInSeconds),
                                                                 providerSigner).serializeToBytes(JSONOutputFormats.NORMALIZED);
            }
            if (logging) {
                logger.info("Updated \"" + Messages.PROVIDER_AUTHORITY.toString() + "\"");
            }
        }

        if (attestationSigner != null) {
            for (String id : payees.keySet()) {
                PayeeCoreProperties payeeCoreProperties = payees.get(id);
                synchronized(this) {
                    payeeAuthorityBlobs.put(id, 
                                            PayeeAuthority.encode(payeeBaseAuthorityUrl + id,
                                                                  providerAuthorityUrl,
                                                                  payeeCoreProperties,
                                                                  TimeUtil.inSeconds(expiryTimeInSeconds),
                                                                  attestationSigner).serializeToBytes(JSONOutputFormats.NORMALIZED));
                }
                if (logging) {
                    logger.info("Updated \"" + Messages.PAYEE_AUTHORITY.toString() + "\" with id:" + id);
                }
            }
        }

    }

    public AuthorityObjectManager(String providerAuthorityUrl /* Both */,

                                  // ProviderAuthority (may be null)
                                  String providerHomePage,
                                  String serviceUrl,
                                  JSONObjectReader optionalExtensions,
                                  String[] optionalProviderAccountTypes,
                                  SignatureProfiles[] signatureProfiles,
                                  ProviderAuthority.EncryptionParameter[] encryptionParameters,
                                  HostingProvider optionalHostingProvider, 
                                  ServerX509Signer providerSigner,
                                    
                                  // PayeeAuthority (may be null)
                                  SortedMap<String,PayeeCoreProperties> payees, // Zero-length list is allowed
                                  String payeeBaseAuthorityUrl,
                                  ServerAsymKeySigner attestationSigner,

                                  int expiryTimeInSeconds /* Both */,
                                  boolean logging /* Both */) throws IOException {
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.providerHomePage = providerHomePage;
        this.serviceUrl = serviceUrl;
        this.optionalExtensions = optionalExtensions;
        this.optionalProviderAccountTypes = optionalProviderAccountTypes;
        this.signatureProfiles = signatureProfiles;
        this.encryptionParameters = encryptionParameters;
        this.optionalHostingProvider = optionalHostingProvider; 

        this.payees = payees;
        this.payeeBaseAuthorityUrl = payeeBaseAuthorityUrl;
        
        this.expiryTimeInSeconds = expiryTimeInSeconds;
        this.renewCycle = expiryTimeInSeconds * 500;
        this.providerSigner = providerSigner;
        this.attestationSigner = attestationSigner;
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
