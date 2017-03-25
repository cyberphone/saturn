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

import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class AuthorizationResponse implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";

    public AuthorizationResponse(JSONObjectReader rd) throws IOException {
        root = Messages.AUTHORIZATION_RESPONSE.parseBaseMessage(rd);
        authorizationRequest = new AuthorizationRequest(Messages.AUTHORIZATION_REQUEST.getEmbeddedMessage(rd));
        accountReference = rd.getString(ACCOUNT_REFERENCE_JSON);
        encryptedAccountData = rd.getObject(ENCRYPTED_ACCOUNT_DATA_JSON).getEncryptionObject().require(true);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;

    Software software;

    GregorianCalendar dateTime;

    String optionalLogData;
    public String getOptionalLogData() {
        return optionalLogData;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    String accountReference;
    public String getAccountReference() {
        return accountReference;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONDecryptionDecoder encryptedAccountData;

    AuthorizationRequest authorizationRequest;
    public AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public static JSONObjectWriter encode(AuthorizationRequest authorizationRequest,
                                          String accountReference,
                                          ProviderAuthority.EncryptionParameter encryptionParameter,
                                          AccountDescriptor accountDescriptor,
                                          CardSpecificData cardSpecificData,
                                          String referenceId,
                                          String optionalLogData,
                                          ServerX509Signer signer) throws IOException, GeneralSecurityException {
        return Messages.AUTHORIZATION_RESPONSE.createBaseMessage()
            .setObject(Messages.AUTHORIZATION_REQUEST.getlowerCamelCase(), authorizationRequest.root)
            .setString(ACCOUNT_REFERENCE_JSON, accountReference)
            .setObject(ENCRYPTED_ACCOUNT_DATA_JSON, 
                       JSONObjectWriter
                           .createEncryptionObject(ProtectedAccountData.encode(accountDescriptor, cardSpecificData)
                                                       .serializeToBytes(JSONOutputFormats.NORMALIZED),
                                                   encryptionParameter.getDataEncryptionAlgorithm(),
                                                   encryptionParameter.getEncryptionKey(),
                                                   encryptionParameter.getKeyEncryptionAlgorithm()))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDynamic((wr) -> optionalLogData == null ? wr :  wr.setString(LOG_DATA_JSON, optionalLogData))
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
