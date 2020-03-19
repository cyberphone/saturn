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
import java.util.ArrayList;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ArrayUtil;
import org.webpki.util.ISODateTime;

public class AuthorizationRequest implements BaseProperties {
    
    public static final HashAlgorithms DEFAULT_KEY_HASH_ALGORITHM = HashAlgorithms.SHA256;
    
    public AuthorizationRequest(JSONObjectReader rd) throws IOException {
        root = Messages.AUTHORIZATION_REQUEST.parseBaseMessage(rd);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        recepientUrl = rd.getString(RECEPIENT_URL_JSON);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        paymentMethod = PaymentMethods.fromTypeUrl(rd.getString(PAYMENT_METHOD_JSON));
        keyHashAlgorithm = rd.hasProperty(KEY_HASH_ALGORITHM_JSON) ?
                HashAlgorithms.getAlgorithmFromId(rd.getString(KEY_HASH_ALGORITHM_JSON),
                                                  AlgorithmPreferences.JOSE)
                                                                   :
                DEFAULT_KEY_HASH_ALGORITHM;                   
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        encryptedAuthorizationData = PayerAuthorization.getEncryptedAuthorization(rd);
        undecodedAccountData = rd.getObject(PAYEE_RECEIVE_ACCOUNT_JSON);
        rd.scanAway(PAYEE_RECEIVE_ACCOUNT_JSON);  // Read all to not throw on checkForUnread()
        referenceId = rd.getString(REFERENCE_ID_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(REQUEST_SIGNATURE_JSON, 
                new JSONCryptoHelper.Options()
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED)
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN));
        rd.checkForUnread();
    }

    Software software;

    JSONDecryptionDecoder encryptedAuthorizationData;

    JSONObjectReader root;
    
    JSONObjectReader undecodedAccountData;
    
    HashAlgorithms keyHashAlgorithm;

    boolean testMode;
    public boolean getTestMode() {
        return testMode;
    }

    PaymentMethods paymentMethod;
    public PaymentMethods getPaymentMethod() {
        return paymentMethod;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public AccountDataDecoder getPayeeReceiveAccount(JSONDecoderCache knownAccountTypes)
    throws IOException {
        return (AccountDataDecoder) knownAccountTypes.parse(undecodedAccountData);
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    String recepientUrl;
    public String getRecepientUrl() {
        return recepientUrl;
    }

    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public static JSONObjectWriter encode(Boolean testMode,
                                          String recepientUrl,
                                          String authorityUrl,
                                          PaymentMethods paymentMethod,
                                          HashAlgorithms keyHashAlgorithm,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          AccountDataEncoder payeeReceiveAccount,
                                          String referenceId,
                                          ServerAsymKeySigner signer) throws IOException {
        return Messages.AUTHORIZATION_REQUEST.createBaseMessage()
            .setDynamic((wr) -> testMode == null ? wr : wr.setBoolean(TEST_MODE_JSON, testMode))
            .setString(RECEPIENT_URL_JSON, recepientUrl)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(PAYMENT_METHOD_JSON, paymentMethod.getPaymentMethodUrl())
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setDynamic((wr) -> keyHashAlgorithm == DEFAULT_KEY_HASH_ALGORITHM ?
                    wr : wr.setString(KEY_HASH_ALGORITHM_JSON, keyHashAlgorithm.getJoseAlgorithmId()))
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData)
            .setObject(PAYEE_RECEIVE_ACCOUNT_JSON, payeeReceiveAccount.writeObject())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setObject(SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME, PaymentRequest.SOFTWARE_VERSION))
            .setSignature(REQUEST_SIGNATURE_JSON, signer);
    }

    public AuthorizationData getDecryptedAuthorizationData(
            ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys,
            JSONCryptoHelper.Options option)
    throws IOException, GeneralSecurityException {
        AuthorizationData authorizationData =
            new AuthorizationData(JSONParser.parse(encryptedAuthorizationData.getDecryptedData(decryptionKeys)),
                                                   option);
        if (!ArrayUtil.compare(authorizationData.requestHash, 
                               paymentRequest.getRequestHash(authorizationData.requestHashAlgorithm))) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        if (!authorizationData.paymentMethodUrl.equals(paymentMethod.paymentMethodUrl)) {
            throw new IOException("Non-matching \"" + PAYMENT_METHOD_JSON + "\"");
        }
        if (!ArrayUtil.compare(HashSupport.getJwkThumbPrint(signatureDecoder.getPublicKey(),
                                                            keyHashAlgorithm),
                               authorizationData.keyHash)) {
            throw new IOException("Non-matching \"" + KEY_HASH_JSON + "\"");
        }
        return authorizationData;
    }
}
