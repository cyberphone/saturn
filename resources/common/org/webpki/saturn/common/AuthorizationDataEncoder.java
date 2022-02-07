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

import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.ContentEncryptionAlgorithms;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSigner;

import org.webpki.util.ISODateTime;

public class AuthorizationDataEncoder implements BaseProperties {

    public static JSONObjectWriter encode(PaymentRequestDecoder paymentRequest,
                                          HashAlgorithms requestHashAlgorithm,
                                          String payeeAuthorityUrl,
                                          String payeeHost,
                                          String paymentMethodUrl,
                                          String credentialId,
                                          String accountId,
                                          byte[] dataEncryptionKey,
                                          ContentEncryptionAlgorithms contentEncryptionAlgorithm,
                                          UserResponseItem[] optionalUserResponseItems,
                                          UserAuthorizationMethods userAuthorizationMethod,
                                          GregorianCalendar timeStamp,
                                          String applicationName,
                                          String applicationVersion,
                                          ClientPlatform clientPlatform,
                                          JSONSigner signer) throws IOException,
                                                                    GeneralSecurityException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                .setString(JSONCryptoHelper.ALGORITHM_JSON, 
                           requestHashAlgorithm.getJoseAlgorithmId())
                .setBinary(JSONCryptoHelper.VALUE_JSON, 
                           paymentRequest.getRequestHash(requestHashAlgorithm)))
            .setString(PAYEE_AUTHORITY_URL_JSON, payeeAuthorityUrl)
            .setString(PAYEE_HOST_JSON, payeeHost)
            .setString(PAYMENT_METHOD_JSON, paymentMethodUrl)
            .setString(CREDENTIAL_ID_JSON, credentialId)
            .setString(ACCOUNT_ID_JSON, accountId)
            .setObject(ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                .setString(JSONCryptoHelper.ALGORITHM_JSON, 
                           contentEncryptionAlgorithm.getJoseAlgorithmId())
                .setBinary(ENCRYPTION_KEY_JSON, dataEncryptionKey));
        if (optionalUserResponseItems != null && optionalUserResponseItems.length > 0) {
            JSONArrayWriter aw = wr.setArray(USER_RESPONSE_ITEMS_JSON);
            for (UserResponseItem challengeResult : optionalUserResponseItems) {
                aw.setObject(challengeResult.writeObject());
            }
        }
        return wr.setString(USER_AUTHORIZATION_METHOD_JSON, userAuthorizationMethod.toString())
                 .setDateTime(TIME_STAMP_JSON, timeStamp, ISODateTime.LOCAL_NO_SUBSECONDS)
                 .setDynamic((wr2) -> Software.encode(wr2, applicationName, applicationVersion))
                 .setObject(PLATFORM_JSON, new JSONObjectWriter()
                     .setString(NAME_JSON, clientPlatform.name)
                     .setString(VERSION_JSON, clientPlatform.version)
                     .setString(VENDOR_JSON, clientPlatform.vendor))
                 .setSignature(AUTHORIZATION_SIGNATURE_JSON, signer);
    }
}
