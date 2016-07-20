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

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONOutputFormats;

public class PayerAuthorization implements BaseProperties {
    
    static JSONDecryptionDecoder getEncryptionObject(JSONObjectReader rd, boolean sharedSecret) throws IOException {
        JSONDecryptionDecoder decryptionDecoder = rd.getEncryptionObject();
        if (sharedSecret != decryptionDecoder.isSharedSecret()) {
            throw new IOException("Unexpected type of encryption object");
        }
        return decryptionDecoder;
    }
    
    public PayerAuthorization(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.PAYER_AUTHORIZATION, rd);
        // Only syntax checking for intermediaries
        getEncryptionObject(rd.getObject(ENCRYPTED_AUTHORIZATION_JSON), false);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        accountType = PayerAccountTypes.fromTypeUri(rd.getString(ACCOUNT_TYPE_JSON));
        rd.checkForUnread();
    }
    
    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    PayerAccountTypes accountType;
    public PayerAccountTypes getAccountType() {
        return accountType;
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          JSONObjectWriter unencryptedAuthorizationData,
                                          String providerAuthorityUrl,
                                          String accountType,
                                          String dataEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String keyEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.PAYER_AUTHORIZATION)
            .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
            .setString(ACCOUNT_TYPE_JSON, accountType)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, 
                       new JSONObjectWriter()
                          .setEncryptionObject(unencryptedAuthorizationData.serializeJSONObject(JSONOutputFormats.NORMALIZED),
                                               dataEncryptionAlgorithm,
                                               keyEncryptionKey,
                                               keyEncryptionAlgorithm));
    }
}
