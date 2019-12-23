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

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;
import org.webpki.json.JSONAsymKeyEncrypter;

import org.webpki.util.ISODateTime;

public class AuthorizationResponse implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";

    public static abstract class AccountDataDecoder extends JSONDecoder {

        private static final long serialVersionUID = 1L;

        public String logLine() throws IOException {
            return getWriter().serializeToString(JSONOutputFormats.NORMALIZED);
        }
    }

    public static abstract class AccountDataEncoder {

        protected abstract JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException;
        
        public abstract String getContext();
        
        public abstract String getPartialAccountIdentifier();  // Like ************4567
        
        public String getQualifier() {
            return null;  // Optional
        }
        
        public JSONObjectWriter writeObject() throws IOException {
            return new JSONObjectWriter()
                .setString(JSONDecoderCache.CONTEXT_JSON, getContext())
                .setDynamic((wr) -> {
                    if (getQualifier() != null) {
                        wr.setString(JSONDecoderCache.QUALIFIER_JSON, getQualifier());
                    }
                    return writeObject(wr);
                });
        }
    }

    public AuthorizationResponse(JSONObjectReader rd) throws IOException {
        root = Messages.AUTHORIZATION_RESPONSE.parseBaseMessage(rd);
        authorizationRequest = new AuthorizationRequest(Messages.AUTHORIZATION_REQUEST.getEmbeddedMessage(rd));
        accountReference = rd.getString(ACCOUNT_REFERENCE_JSON);
        encryptedAccountData = 
                rd.getObject(ENCRYPTED_ACCOUNT_DATA_JSON)
                    .getEncryptionObject(new JSONCryptoHelper.Options()).require(true);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AUTHORIZATION_SIGNATURE_JSON, new JSONCryptoHelper.Options());
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
                                          ProviderAuthority.EncryptionParameter encryptionParameter,
                                          AccountDataEncoder accountData,
                                          String referenceId,
                                          String optionalLogData,
                                          ServerX509Signer signer) throws IOException, GeneralSecurityException {
        return Messages.AUTHORIZATION_RESPONSE.createBaseMessage()
            .setObject(Messages.AUTHORIZATION_REQUEST.lowerCamelCase(), authorizationRequest.root)
            .setString(ACCOUNT_REFERENCE_JSON, accountData.getPartialAccountIdentifier())
            .setObject(ENCRYPTED_ACCOUNT_DATA_JSON, 
                       JSONObjectWriter
                           .createEncryptionObject(
                               accountData.writeObject().serializeToBytes(JSONOutputFormats.NORMALIZED),
                               encryptionParameter.getDataEncryptionAlgorithm(),
                               new JSONAsymKeyEncrypter(encryptionParameter.getEncryptionKey(),
                               encryptionParameter.getKeyEncryptionAlgorithm())))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDynamic((wr) -> optionalLogData == null ? wr :  wr.setString(LOG_DATA_JSON, optionalLogData))
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(AUTHORIZATION_SIGNATURE_JSON, signer);
    }

    public AccountDataDecoder getProtectedAccountData(JSONDecoderCache knownAccountTypes, 
                                                      ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
         return (AccountDataDecoder) knownAccountTypes.parse(encryptedAccountData.getDecryptedData(decryptionKeys));
    }
}
