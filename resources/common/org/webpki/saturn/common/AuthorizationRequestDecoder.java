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

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ArrayUtil;
import org.webpki.util.ISODateTime;

public class AuthorizationRequestDecoder implements BaseProperties {
    
    static final JSONCryptoHelper.Options signatureOptions = new JSONCryptoHelper.Options()
            .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED)
            .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN);
    
    public AuthorizationRequestDecoder(JSONObjectReader rd) throws IOException,
                                                                   GeneralSecurityException {
        root = Messages.AUTHORIZATION_REQUEST.parseBaseMessage(rd);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        recipientUrl = rd.getString(RECIPIENT_URL_JSON);
        payeeAuthorityUrl = rd.getString(PAYEE_AUTHORITY_URL_JSON);
        paymentMethod = PaymentMethods.fromTypeUrl(rd.getString(PAYMENT_METHOD_JSON));
        paymentRequest = new PaymentRequestDecoder(rd.getObject(PAYMENT_REQUEST_JSON));
        encryptedAuthorizationData = PayerAuthorization.getEncryptedAuthorization(rd);
        undecodedAccountData = rd.getObject(PAYEE_RECEIVE_ACCOUNT_JSON);
        rd.scanAway(PAYEE_RECEIVE_ACCOUNT_JSON);  // Read all to not throw on checkForUnread()
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(REQUEST_SIGNATURE_JSON, signatureOptions);
        rd.checkForUnread();
    }

    Software software;

    JSONDecryptionDecoder encryptedAuthorizationData;

    JSONObjectReader root;
    
    JSONObjectReader undecodedAccountData;
    
    boolean testMode;
    public boolean getTestMode() {
        return testMode;
    }

    PaymentMethods paymentMethod;
    public PaymentMethods getPaymentMethod() {
        return paymentMethod;
    }

    public byte[] getHashOfAuthorization(HashAlgorithms hashAlgorithm) 
            throws IOException, GeneralSecurityException {
System.out.println("Auth" + encryptedAuthorizationData.getEncryptionObject());
        return hashAlgorithm.digest(encryptedAuthorizationData
                .getEncryptionObject().serializeToBytes(JSONOutputFormats.CANONICALIZED));
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public AccountDataDecoder getPayeeReceiveAccount(JSONDecoderCache knownAccountTypes)
    throws IOException, GeneralSecurityException {
        return (AccountDataDecoder) knownAccountTypes.parse(undecodedAccountData);
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    String recipientUrl;
    public String getRecipientUrl() {
        return recipientUrl;
    }

    String payeeAuthorityUrl;
    public String getPayeeAuthorityUrl() {
        return payeeAuthorityUrl;
    }

    // Note: we reuse the referenceId of PaymentRequest
    // since these objects are "inseparable" anyway
    public String getReferenceId() {
        return paymentRequest.referenceId;
    }

    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    PaymentRequestDecoder paymentRequest;
    public PaymentRequestDecoder getPaymentRequest() {
        return paymentRequest;
    }

    public AuthorizationDataDecoder getDecryptedAuthorizationData(
            ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys,
            JSONCryptoHelper.Options option)
    throws IOException, GeneralSecurityException {
        AuthorizationDataDecoder authorizationData =
            new AuthorizationDataDecoder(
                    JSONParser.parse(encryptedAuthorizationData.getDecryptedData(decryptionKeys)),
                                     option);
        if (!ArrayUtil.compare(authorizationData.requestHash, 
                               paymentRequest.getRequestHash(authorizationData.requestHashAlgorithm))) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
        }
        if (!authorizationData.paymentMethodUrl.equals(paymentMethod.paymentMethodUrl)) {
            throw new IOException("Non-matching \"" + PAYMENT_METHOD_JSON + "\"");
        }
        if (!authorizationData.payeeAuthorityUrl.equals(payeeAuthorityUrl)) {
            throw new IOException("Non-matching \"" + PAYEE_AUTHORITY_URL_JSON + "\"");
        }
        return authorizationData;
    }
}
