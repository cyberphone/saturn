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

import java.util.GregorianCalendar;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

public class ProviderAuthorityEncoder implements BaseProperties {
    
    public static JSONObjectWriter encode(
                    String providerAuthorityUrl,
                    String commonName,
                    String homePage,
                    String providerLogotypeUrl,
                    String serviceUrl,
                    ProviderAuthorityDecoder.PaymentMethodDeclarations paymentMethods,
                    JSONObjectReader optionalExtensions,
                    SignatureProfiles[] signatureProfiles,
                    ProviderAuthorityDecoder.EncryptionParameter[] encryptionParameters,
                    HostingProvider[] optionalHostingProviders,
                    GregorianCalendar expires,
                    ServerX509Signer issuerSigner) throws IOException, GeneralSecurityException {
        return Messages.PROVIDER_AUTHORITY.createBaseMessage()
            .setStringArray(HTTP_VERSIONS_JSON, ProviderAuthorityDecoder.HTTP_VERSION_SUPPORT)
            .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
            .setString(COMMON_NAME_JSON, commonName)
            .setString(HOME_PAGE_JSON, homePage)
            .setString(LOGOTYPE_URL_JSON, providerLogotypeUrl)
            .setString(SERVICE_URL_JSON, serviceUrl)
            .setObject(SUPPORTED_PAYMENT_METHODS_JSON, paymentMethods.toObject())
            .setDynamic((wr) -> optionalExtensions == null ? wr : 
                wr.setObject(EXTENSIONS_JSON, optionalExtensions))
            .setDynamic((wr) -> {
                JSONArrayWriter jsonArray = wr.setArray(SIGNATURE_PROFILES_JSON);
                for (SignatureProfiles signatureProfile : signatureProfiles) {
                    jsonArray.setString(signatureProfile.getId());
                }
                return wr;
            })
            .setDynamic((wr) -> {
                JSONArrayWriter jsonArray = wr.setArray(ENCRYPTION_PARAMETERS_JSON);
                for (ProviderAuthorityDecoder.EncryptionParameter encryptionParameter
                        : encryptionParameters) {
                    jsonArray.setObject()
                        .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON,
                                   encryptionParameter.contentEncryptionAlgorithm.getJoseAlgorithmId())
                        .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON,
                                   encryptionParameter.keyEncryptionAlgorithm.getJoseAlgorithmId())
                        .setPublicKey(encryptionParameter.encryptionKey);
                }
                return wr;
            })
            .setDynamic((wr) -> {
                    if (optionalHostingProviders == null) {
                        return wr;
                    } else {
                        JSONArrayWriter aw = wr.setArray(HOSTING_PROVIDERS_JSON);
                        for (HostingProvider hostingProvider : optionalHostingProviders) {
                            aw.setObject(hostingProvider.writeObject());
                        }
                        return wr;
                    }
                })
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setDateTime(BaseProperties.EXPIRES_JSON, expires, ISODateTime.UTC_NO_SUBSECONDS)
            .setSignature(ISSUER_SIGNATURE_JSON, issuerSigner);
    }
}
