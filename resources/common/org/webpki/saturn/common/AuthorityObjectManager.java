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

import java.security.GeneralSecurityException;

import java.util.HashMap;
import java.util.Collection;

import java.util.logging.Logger;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;

public class AuthorityObjectManager extends Thread {

    private static final Logger logger = 
            Logger.getLogger(AuthorityObjectManager.class.getCanonicalName());

    HashMap<String,byte[]> payeeAuthorityBlobs = new HashMap<>();

    String providerAuthorityUrl;
    String providerCommonName;
    String providerHomePage;
    String providerLogotypeUrl;
    String serviceUrl;
    JSONObjectReader optionalExtensions;
    ProviderAuthorityDecoder.PaymentMethodDeclarations paymentMethods;
    SignatureProfiles[] signatureProfiles;
    ProviderAuthorityDecoder.EncryptionParameter[] encryptionParameters;
    HostingProvider[] optionalHostingProviders; 

    Collection<PayeeCoreProperties> payees;

    int expiryTimeInSeconds;
    long renewCycle;
    byte[] providerAuthorityBlob;
    ServerX509Signer providerSigner;
    ServerAsymKeySigner attestationSigner;

    boolean logging;

    void update() throws IOException, GeneralSecurityException {
        if (providerSigner != null) {
            synchronized(this) {
                providerAuthorityBlob = 
                        ProviderAuthorityEncoder.encode(providerAuthorityUrl,
                                                        providerCommonName,
                                                        providerHomePage,
                                                        providerLogotypeUrl,
                                                        serviceUrl,
                                                        paymentMethods,
                                                        optionalExtensions,
                                                        signatureProfiles,
                                                        encryptionParameters,
                                                        optionalHostingProviders, 
                                                        TimeUtils.inSeconds(expiryTimeInSeconds),
                                                        providerSigner)
                            .serializeToBytes(JSONOutputFormats.NORMALIZED);
            }
            if (logging) {
                logger.info("Updated \"" + Messages.PROVIDER_AUTHORITY.toString() + "\"");
            }
        }

        if (attestationSigner != null) {
            for (PayeeCoreProperties payeeCoreProperties : payees) {
                synchronized(this) {
                    payeeAuthorityBlobs.put(
                            payeeCoreProperties.urlSafeId, 
                            PayeeAuthorityEncoder.encode(payeeCoreProperties.payeeAuthorityUrl,
                                                         providerAuthorityUrl,
                                                         payeeCoreProperties,
                                                         TimeUtils.inSeconds(expiryTimeInSeconds),
                                                         attestationSigner)
                                .serializeToBytes(JSONOutputFormats.NORMALIZED));
                }
                if (logging) {
                    logger.info("Updated \"" + Messages.PAYEE_AUTHORITY.toString() + 
                                "\" with id: " + payeeCoreProperties.getPayeeId());
                }
            }
        }

    }

    public AuthorityObjectManager(
                String providerAuthorityUrl /* Both */,

                // ProviderAuthority (may be null)
                String providerCommonName,
                String providerHomePage,
                String providerLogotypeUrl,
                String serviceUrl,
                ProviderAuthorityDecoder.PaymentMethodDeclarations paymentMethods,
                JSONObjectReader optionalExtensions,
                SignatureProfiles[] signatureProfiles,
                ProviderAuthorityDecoder.EncryptionParameter[] encryptionParameters,
                HostingProvider[] optionalHostingProviders, 
                ServerX509Signer providerSigner,
                                    
                // PayeeAuthority (may be null)
                Collection<PayeeCoreProperties> payees, // Zero-length list is allowed
                ServerAsymKeySigner attestationSigner,

                int expiryTimeInSeconds /* Both */,
                boolean logging /* Both */) throws IOException, GeneralSecurityException {
        this.providerAuthorityUrl = providerAuthorityUrl;
        this.providerCommonName = providerCommonName;
        this.providerHomePage = providerHomePage;
        this.providerLogotypeUrl = providerLogotypeUrl;
        this.serviceUrl = serviceUrl;
        this.paymentMethods = paymentMethods;
        this.optionalExtensions = optionalExtensions;
        this.signatureProfiles = signatureProfiles;
        this.encryptionParameters = encryptionParameters;
        this.optionalHostingProviders = optionalHostingProviders; 

        this.payees = payees;
        
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

    public synchronized void updateProviderSigner(ServerX509Signer providerSigner)
    throws IOException, GeneralSecurityException {
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
