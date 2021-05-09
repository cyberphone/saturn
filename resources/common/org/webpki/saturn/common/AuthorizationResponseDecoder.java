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

import java.math.BigDecimal;

import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ISODateTime;

public class AuthorizationResponseDecoder extends ProviderResponseDecoder {
    
    public AuthorizationResponseDecoder(JSONObjectReader rd) throws IOException,
                                                                    GeneralSecurityException {
        root = Messages.AUTHORIZATION_RESPONSE.parseBaseMessage(rd);
        authorizationRequest = new AuthorizationRequestDecoder(
                Messages.AUTHORIZATION_REQUEST.getEmbeddedMessage(rd));
        optionalAccountReference = rd.getStringConditional(ACCOUNT_REFERENCE_JSON);
        encryptedAccountData = 
                rd.getObject(ENCRYPTED_ACCOUNT_DATA_JSON)
                    .getEncryptionObject(new JSONCryptoHelper.Options()
                        .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                        .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.REQUIRED));
        referenceId = rd.getString(REFERENCE_ID_JSON);
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AUTHORIZATION_SIGNATURE_JSON, 
                new JSONCryptoHelper.Options()
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.CERTIFICATE_PATH));
        rd.checkForUnread();
    }

    JSONObjectReader root;

    Software software;

    GregorianCalendar timeStamp;

    String optionalLogData;
    public String getOptionalLogData() {
        return optionalLogData;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    String optionalAccountReference;
    public String getOptionalAccountReference() {
        return optionalAccountReference;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONDecryptionDecoder encryptedAccountData;

    AuthorizationRequestDecoder authorizationRequest;
    public AuthorizationRequestDecoder getAuthorizationRequest() {
        return authorizationRequest;
    }

    public AccountDataDecoder getProtectedAccountData(
            JSONDecoderCache knownAccountTypes, 
            ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        return (AccountDataDecoder) knownAccountTypes.parse(
                 encryptedAccountData.getDecryptedData(decryptionKeys));
    }

    @Override
    public BigDecimal getAmount() {
        return authorizationRequest.paymentRequest.amount;
    }

    @Override
    public AuthorizationResponseDecoder getAuthorizationResponse() {
        return this;
    }

    @Override
    JSONObjectReader getRoot() {
        return root;
    }

    @Override
    public GregorianCalendar getProviderTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getProviderReferenceId() {
        return referenceId;
    }

    @Override
    public String getPayeeRequestId() {
        return authorizationRequest.paymentRequest.referenceId;
    }
}
