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

import java.lang.reflect.InvocationTargetException;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.crypto.ContentEncryptionAlgorithms;
import org.webpki.crypto.KeyEncryptionAlgorithms;

import org.webpki.util.ISODateTime;

public class ProviderAuthorityDecoder implements BaseProperties {
    
    public static class EncryptionParameter {
        
        ContentEncryptionAlgorithms contentEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        PublicKey encryptionKey;

        public EncryptionParameter(ContentEncryptionAlgorithms contentEncryptionAlgorithm,
                                   KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                                   PublicKey encryptionKey) {
            this.contentEncryptionAlgorithm = contentEncryptionAlgorithm;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.encryptionKey = encryptionKey;
        }

        public PublicKey getEncryptionKey() {
            return encryptionKey;
        }

        public ContentEncryptionAlgorithms getContentEncryptionAlgorithm() {
            return contentEncryptionAlgorithm;
        }

        public KeyEncryptionAlgorithms getKeyEncryptionAlgorithm() {
            return keyEncryptionAlgorithm;
        }
    }

    public static class PaymentMethodDeclaration {
        String clientPaymentMethod;
        ArrayList<String> backendPaymentMethods = new ArrayList<>();
        
        public PaymentMethodDeclaration(String clientPaymentMethod) {
            this.clientPaymentMethod = clientPaymentMethod;
        }
        
        PaymentMethodDeclaration(String clientPaymentMethod,
                                 String[] backendPaymentMethods) {
            this(clientPaymentMethod);
            for (String backendPaymentData : backendPaymentMethods) {
                this.backendPaymentMethods.add(backendPaymentData);
            }
        }

        public PaymentMethodDeclaration add(
            Class<? extends AccountDataDecoder> pbmd) 
                throws InstantiationException, IllegalAccessException, 
                       IllegalArgumentException, InvocationTargetException, 
                       NoSuchMethodException, SecurityException {
            backendPaymentMethods.add(pbmd.getDeclaredConstructor().newInstance().getContext());
            return this;
        }
        
        public String[] getBackendPaymentDatas() {
            return backendPaymentMethods.toArray(new String[0]);
        }
    }

    public static class PaymentMethodDeclarations {
        LinkedHashMap<String,PaymentMethodDeclaration> paymentMethodDeclarations =
                new LinkedHashMap<>();
        public PaymentMethodDeclarations add(PaymentMethodDeclaration pmb) {
            paymentMethodDeclarations.put(pmb.clientPaymentMethod, pmb);
            return this;
        }

        public JSONObjectWriter toObject() throws IOException {
            JSONObjectWriter pmdObject = new JSONObjectWriter();
            for (String clientPaymentMethod : paymentMethodDeclarations.keySet()) {
                pmdObject.setStringArray(clientPaymentMethod, 
                                         paymentMethodDeclarations.get(clientPaymentMethod)
                                             .getBackendPaymentDatas());
            }
            return pmdObject;
        }
    }

    public static final String[] HTTP_VERSION_SUPPORT = {"HTTP/1.1", "HTTP/2"};
    
    static final HashSet<String> CHECK_HTTP = new HashSet<>();
    
    static {
        for (String httpVersion : HTTP_VERSION_SUPPORT) {
            CHECK_HTTP.add(httpVersion);
        }
    }

    public ProviderAuthorityDecoder(JSONObjectReader rd, String expectedAuthorityUrl)
            throws IOException, GeneralSecurityException {
        root = Messages.PROVIDER_AUTHORITY.parseBaseMessage(rd);
        httpVersions = rd.getStringArray(HTTP_VERSIONS_JSON);
        boolean notFound = true;
        for (String httpVersion : httpVersions) {
            if (CHECK_HTTP.contains(httpVersion)) {
                notFound = false;
                break;
            }
        }
        if (notFound) {
            throw new IOException("\"" + HTTP_VERSIONS_JSON + 
                                 "\" is currently limited to " + 
                                 HTTP_VERSION_SUPPORT);
        }
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        if (!providerAuthorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + PROVIDER_AUTHORITY_URL_JSON +
                                  "\" mismatch, read=" + 
                                  providerAuthorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        commonName = rd.getString(COMMON_NAME_JSON);
        homePage = rd.getString(HOME_PAGE_JSON);
        logotypeUrl = rd.getString(LOGOTYPE_URL_JSON);
        serviceUrl = rd.getString(SERVICE_URL_JSON);
        JSONObjectReader paymentMethodsObject = rd.getObject(SUPPORTED_PAYMENT_METHODS_JSON);
        paymentMethods = new PaymentMethodDeclarations();
        for (String clientPaymentMethod : paymentMethodsObject.getProperties()) {
            paymentMethods.add(new PaymentMethodDeclaration(clientPaymentMethod,
                    paymentMethodsObject.getStringArray(clientPaymentMethod)));
        }
        if (rd.hasProperty(EXTENSIONS_JSON)) {
            optionalExtensions = rd.getObject(EXTENSIONS_JSON);
            if (optionalExtensions.getProperties().length == 0) {
                throw new IOException("Empty \"" + EXTENSIONS_JSON + "\" not allowed");
            }
            rd.scanAway(EXTENSIONS_JSON);
        }

        // Signature profiles tell other parties what kind of signatures that are accepted
        // Additional signature profiles can be introduced without breaking existing applications
        ArrayList<SignatureProfiles> profileArray = new ArrayList<>();
        JSONArrayReader jsonProfileArray = rd.getArray(SIGNATURE_PROFILES_JSON);
        do {
            SignatureProfiles signatureProfile = SignatureProfiles.getProfileFromString(jsonProfileArray.getString());
            if (signatureProfile != null) {
                profileArray.add(signatureProfile);
            }
        } while (jsonProfileArray.hasMore());
        if (profileArray.isEmpty()) {
            throw new IOException("No \"" + SIGNATURE_PROFILES_JSON + "\" were recognized");
        }
        signatureProfiles = profileArray.toArray(new SignatureProfiles[0]);

        // Encryption parameters tell other parties what kind of encryption keys you have
        // Additional algorithms can be introduced without breaking existing applications
        ArrayList<EncryptionParameter> parameterArray = new ArrayList<>();
        JSONArrayReader jsonParameterArray = rd.getArray(ENCRYPTION_PARAMETERS_JSON);
        do {
            JSONObjectReader encryptionParameter = jsonParameterArray.getObject();
            String algorithm = encryptionParameter.getString(DATA_ENCRYPTION_ALGORITHM_JSON);
            boolean notRecognized = true;
            for (ContentEncryptionAlgorithms contentEncryptionAlgorithm : ContentEncryptionAlgorithms.values()) {
                if (contentEncryptionAlgorithm.getJoseAlgorithmId().equals(algorithm)) {
                    algorithm = encryptionParameter.getString(KEY_ENCRYPTION_ALGORITHM_JSON);
                    for (KeyEncryptionAlgorithms keyEncryptionAlgorithm : KeyEncryptionAlgorithms.values()) {
                        if (keyEncryptionAlgorithm.getJoseAlgorithmId().equals(algorithm)) {
                            parameterArray.add(
                                    new EncryptionParameter(contentEncryptionAlgorithm,
                                                            keyEncryptionAlgorithm,
                                                            encryptionParameter.getPublicKey()));
                            notRecognized = false;
                            break;
                        }
                    }
                    break;
                }
            }
            // The parsing is setup to flag unread elements so we must perform a
            // dummy read when we find a parameter object we don't fully understand
            if (notRecognized) {
                encryptionParameter.scanAway(DATA_ENCRYPTION_ALGORITHM_JSON);
                encryptionParameter.scanAway(KEY_ENCRYPTION_ALGORITHM_JSON);
                encryptionParameter.scanAway(JSONCryptoHelper.PUBLIC_KEY_JSON);
            }
        } while (jsonParameterArray.hasMore());
        if (parameterArray.isEmpty()) {
            throw new IOException("No \"" + ENCRYPTION_PARAMETERS_JSON + "\" were recognized");
        }
        encryptionParameters = parameterArray.toArray(new EncryptionParameter[0]);

        // If the following object is defined it means that the bank/provider
        // have outsourced the administration of Merchants to a Hosting
        // facility which it vouches for here
        if (rd.hasProperty(HOSTING_PROVIDERS_JSON)) {
            JSONArrayReader ar = rd.getArray(HOSTING_PROVIDERS_JSON);
            ArrayList<HostingProvider> hostingProviders = new ArrayList<>();
            do {
                hostingProviders.add(new HostingProvider(ar.getObject()));
            } while (ar.hasMore());
            optionalHostingProviders = hostingProviders.toArray(new HostingProvider[0]);
        }

        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        expires = rd.getDateTime(EXPIRES_JSON, ISODateTime.COMPLETE);
        expiresInMillis = expires.getTimeInMillis();
        signatureDecoder = rd.getSignature(ISSUER_SIGNATURE_JSON,
                new JSONCryptoHelper.Options()
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.CERTIFICATE_PATH));
        rd.checkForUnread();
    }

    long expiresInMillis;

    String[] httpVersions;
    public String[] getHttpVersions() {
        return httpVersions;
    }

    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    String commonName;
    public String getCommonName() {
        return commonName;
    }

    String homePage;
    public String getHomePage() {
        return homePage;
    }

    String logotypeUrl;
    public String getLogotypeUrl() {
        return logotypeUrl;
    }

    String serviceUrl;
    public String getServiceUrl() {
        return serviceUrl;
    }

    JSONObjectReader optionalExtensions;
    public JSONObjectReader getExtensions() {
        return optionalExtensions;
    }

    SignatureProfiles[] signatureProfiles;
    public SignatureProfiles[] getSignatureProfiles() {
        return signatureProfiles;
    }

    PaymentMethodDeclarations paymentMethods;
    public String[] getPaymentBackendMethods(String clientPaymentMethod)
            throws IOException {
        PaymentMethodDeclaration pmd = 
                paymentMethods.paymentMethodDeclarations.get(clientPaymentMethod);
        if (pmd == null) {
            throw new IOException("Unknown method: " + clientPaymentMethod);
        }
        return pmd.getBackendPaymentDatas();
    }

    EncryptionParameter[] encryptionParameters;
    public EncryptionParameter[] getEncryptionParameters() {
        return encryptionParameters;
    }

    HostingProvider[] optionalHostingProviders;
    public HostingProvider[] getHostingProviders() {
        return optionalHostingProviders;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
     public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public boolean checkPayeeKey(PayeeAuthorityDecoder payeeAuthority) throws IOException {
        if (optionalHostingProviders == null) {
            // Direct attestation of Payee
            return payeeAuthority.attestationKey.equals(
                    signatureDecoder.getCertificatePath()[0].getPublicKey());
        } else {
            // Indirect attestation of Payee through a designated Hosting provider
            for (HostingProvider hostingProvider : optionalHostingProviders) {
                if (payeeAuthority.payeeAuthorityUrl.startsWith(hostingProvider.hostingUrl)) {
                    return payeeAuthority.attestationKey.equals(hostingProvider.publicKey);
                }
            }
            return false;
        }
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }

    boolean cached;  // Set by ExternalCalls
    public boolean isCached() {
        return cached;
    }
}
