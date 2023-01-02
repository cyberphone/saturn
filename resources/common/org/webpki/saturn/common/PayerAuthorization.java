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
import java.security.PublicKey;

import org.webpki.json.JSONAsymKeyEncrypter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.net.HTTPSWrapper;

import org.webpki.crypto.ContentEncryptionAlgorithms;
import org.webpki.crypto.KeyEncryptionAlgorithms;

public class PayerAuthorization implements BaseProperties {
    
    static final JSONCryptoHelper.Options options =
        new JSONCryptoHelper.Options()
                .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.KEY_ID_OR_PUBLIC_KEY);
    
    static JSONDecryptionDecoder getEncryptedAuthorization(JSONObjectReader rd) 
    throws IOException, GeneralSecurityException {
        return rd.getObject(ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject(options);
    }

    public PayerAuthorization(JSONObjectReader rd, String wellKnownUrl) throws IOException, GeneralSecurityException {
        Messages.PAYER_AUTHORIZATION.parseBaseMessage(rd);
        encryptedAuthorization = rd.getObject(ENCRYPTED_AUTHORIZATION_JSON);
        // Only syntax checking for intermediaries
        encryptedAuthorization.getEncryptionObject(options);
        String urlOrHost = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        if (urlOrHost.startsWith("https://")) {
            providerAuthorityUrl = urlOrHost;
        } else {
            HTTPSWrapper wrap = new HTTPSWrapper();
            wrap.makeGetRequest("https://" + urlOrHost + "/.well-known/" + wellKnownUrl);
            providerAuthorityUrl = wrap.getDataUTF8();
        }
        paymentMethod = PaymentMethods.fromTypeUrl(rd.getString(PAYMENT_METHOD_JSON));
        rd.checkForUnread();
    }

    JSONObjectReader encryptedAuthorization;
    public JSONObjectReader getEncryptedAuthorization() {
        return encryptedAuthorization;
    }

    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    PaymentMethods paymentMethod;
    public PaymentMethods getPaymentMethod() {
        return paymentMethod;
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedAuthorizationData,
                                          String providerAuthorityUrl,
                                          String paymentMethod,
                                          ContentEncryptionAlgorithms contentEncryptionAlgorithm,
                                          KeyEncryptionAlgorithms keyEncryptionAlgorithm,
                                          PublicKey encryptionKey,
                                          String optionalKeyId)
    throws IOException, GeneralSecurityException {
        JSONAsymKeyEncrypter encrypter = new JSONAsymKeyEncrypter(encryptionKey, 
                                                                  keyEncryptionAlgorithm);
        if (optionalKeyId != null) {
            encrypter.setKeyId(optionalKeyId).setOutputPublicKeyInfo(false);
        }
        return Messages.PAYER_AUTHORIZATION.createBaseMessage()
            .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
            .setString(PAYMENT_METHOD_JSON, paymentMethod)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, 
                       JSONObjectWriter
                          .createEncryptionObject(
                              unencryptedAuthorizationData.serializeToBytes(
                                      JSONOutputFormats.NORMALIZED),
                              contentEncryptionAlgorithm,
                              encrypter));
    }
}
