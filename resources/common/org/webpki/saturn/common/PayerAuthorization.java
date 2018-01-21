/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.json.encryption.DataEncryptionAlgorithms;
import org.webpki.json.encryption.KeyEncryptionAlgorithms;

public class PayerAuthorization implements BaseProperties {
    
    public PayerAuthorization(JSONObjectReader rd) throws IOException {
        Messages.PAYER_AUTHORIZATION.parseBaseMessage(rd);
        // Only syntax checking for intermediaries
        rd.getObject(ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject().require(true);
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        paymentMethod = PaymentMethods.fromTypeUri(rd.getString(PAYMENT_METHOD_JSON));
        rd.checkForUnread();
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
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String optionalKeyId,
                                          KeyEncryptionAlgorithms keyEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        return Messages.PAYER_AUTHORIZATION.createBaseMessage()
            .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
            .setString(PAYMENT_METHOD_JSON, paymentMethod)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, 
                       JSONObjectWriter
                          .createEncryptionObject(unencryptedAuthorizationData.serializeToBytes(JSONOutputFormats.NORMALIZED),
                                                  dataEncryptionAlgorithm,
                                                  keyEncryptionKey,
                                                  optionalKeyId,
                                                  keyEncryptionAlgorithm));
    }
}
