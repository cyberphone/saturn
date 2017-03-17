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

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

public class ProviderAuthority implements BaseProperties {
    
    public static class EncryptionParameter {
        
        DataEncryptionAlgorithms dataEncryptionAlgorithm;
        KeyEncryptionAlgorithms keyEncryptionAlgorithm;
        PublicKey encryptionKey;

        public EncryptionParameter(DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                   KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                                   PublicKey encryptionKey) {
            this.dataEncryptionAlgorithm = dataEncryptionAlgorithm;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.encryptionKey = encryptionKey;
        }

        public PublicKey getEncryptionKey() {
            return encryptionKey;
        }

        public DataEncryptionAlgorithms getDataEncryptionAlgorithm() {
            return dataEncryptionAlgorithm;
        }

        public KeyEncryptionAlgorithms getKeyEncryptionAlgorithm() {
            return keyEncryptionAlgorithm;
        }
    }

    public static final String HTTP_VERSION_SUPPORT = "HTTP/1.1";

    public static JSONObjectWriter encode(String authorityUrl,
                                          String serviceUrl,
                                          JSONObjectReader optionalExtensions,
                                          String[] optionalProviderAccountTypes,
                                          String[] signatureProfiles,
                                          EncryptionParameter[] encryptionParameters,
                                          GregorianCalendar expires,
                                          ServerX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.PROVIDER_AUTHORITY)
            .setString(HTTP_VERSION_JSON, HTTP_VERSION_SUPPORT)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(SERVICE_URL_JSON, serviceUrl)
            .setDynamic((wr) -> optionalExtensions == null ? wr : wr.setObject(EXTENSIONS_JSON, optionalExtensions))
            .setDynamic((wr) -> optionalProviderAccountTypes == null ?
                    wr : wr.setStringArray(PROVIDER_ACCOUNT_TYPES_JSON, optionalProviderAccountTypes))
            .setStringArray(SIGNATURE_PROFILES_JSON, signatureProfiles)
            .setDynamic((wr) -> {
                JSONArrayWriter jsonArray = wr.setArray(ENCRYPTION_PARAMETERS_JSON);
                for (EncryptionParameter encryptionParameter : encryptionParameters) {
                    jsonArray.setObject()
                        .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON,
                                   encryptionParameter.dataEncryptionAlgorithm.toString())
                        .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON,
                                   encryptionParameter.keyEncryptionAlgorithm.toString())
                    .setPublicKey(encryptionParameter.encryptionKey, AlgorithmPreferences.JOSE);
                }
                return wr;
            })
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setDateTime(BaseProperties.EXPIRES_JSON, expires, true)
            .setSignature(signer);
    }

    public ProviderAuthority(JSONObjectReader rd, String expectedAuthorityUrl) throws IOException {
        Messages.parseBaseMessage(Messages.PROVIDER_AUTHORITY, root = rd);
        httpVersion = rd.getString(HTTP_VERSION_JSON);
        if (!httpVersion.equals(HTTP_VERSION_SUPPORT)) {
            throw new IOException("\"" + HTTP_VERSION_JSON + "\" is currently limited to " + HTTP_VERSION_SUPPORT);
        }
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        if (!authorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + AUTHORITY_URL_JSON + "\" mismatch, read=" + authorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        serviceUrl = rd.getString(SERVICE_URL_JSON);
        if (rd.hasProperty(EXTENSIONS_JSON)) {
            optionalExtensions = rd.getObject(EXTENSIONS_JSON);
            if (optionalExtensions.getProperties().length == 0) {
                throw new IOException("Empty \"" + EXTENSIONS_JSON + "\" not allowed");
            }
            rd.scanAway(EXTENSIONS_JSON);
        }
        optionalProviderAccountTypes = rd.getStringArrayConditional(PROVIDER_ACCOUNT_TYPES_JSON);
        signatureProfiles = rd.getStringArray(SIGNATURE_PROFILES_JSON);
        Vector<EncryptionParameter> parameterArray = new Vector<EncryptionParameter>();
        JSONArrayReader jsonParameterArray = rd.getArray(ENCRYPTION_PARAMETERS_JSON);
        do {
            JSONObjectReader encryptionParameter = jsonParameterArray.getObject();
            parameterArray.add(
                new EncryptionParameter(DataEncryptionAlgorithms
                                            .getAlgorithmFromString(encryptionParameter
                                                 .getString(DATA_ENCRYPTION_ALGORITHM_JSON)),
                                        KeyEncryptionAlgorithms
                                            .getAlgorithmFromString(encryptionParameter
                                                 .getString(KEY_ENCRYPTION_ALGORITHM_JSON)),
                                        encryptionParameter.getPublicKey()));
        } while (jsonParameterArray.hasMore());
        encryptionParameters = parameterArray.toArray(new EncryptionParameter[0]);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    String httpVersion;
    public String getHttpVersion() {
        return httpVersion;
    }

    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String serviceUrl;
    public String getServiceUrl() {
        return serviceUrl;
    }

    JSONObjectReader optionalExtensions;
    public JSONObjectReader getExtensions() {
        return optionalExtensions;
    }

    String[] signatureProfiles;
    public String[] getSignatureProfiles() {
        return signatureProfiles;
    }

    String[] optionalProviderAccountTypes;
    public String[] getProviderAccountTypes(boolean required) throws IOException {
        if (required && optionalProviderAccountTypes == null) {
            throw new IOException("Expected \"" + PROVIDER_ACCOUNT_TYPES_JSON + "\" missing in: " + authorityUrl);
        }
        return optionalProviderAccountTypes;
    }

    EncryptionParameter[] encryptionParameters;
    public EncryptionParameter[] getEncryptionParameters() {
        return encryptionParameters;
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

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }

    public static void compareCertificates(JSONSignatureDecoder arg1, JSONSignatureDecoder arg2) throws IOException {
        if (!Arrays.equals(arg1.getCertificatePath(), arg2.getCertificatePath())) {
            throw new IOException("\"" + JSONSignatureDecoder.CERTIFICATE_PATH_JSON + "\" mismatch");
        }
    }

    public void compareIssuers(PayeeAuthority payeeAuthority) throws IOException {
        compareCertificates(signatureDecoder, payeeAuthority.getSignatureDecoder());
    }
}
