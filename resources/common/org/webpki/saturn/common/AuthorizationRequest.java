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
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ArrayUtil;
import org.webpki.util.ISODateTime;

public class AuthorizationRequest implements BaseProperties {
    
    public static abstract class BackendPaymentDataDecoder extends JSONDecoder {

        private static final long serialVersionUID = 1L;
        
        private byte[] optionalNonce;

        public final String logLine() throws IOException {
            return getWriter().serializeToString(JSONOutputFormats.NORMALIZED);
        }

        public final byte[] getAccountHash() throws IOException {
            return optionalNonce == null ? null : HashAlgorithms.SHA256.digest(getAccountObject());
        }

        // All invariant backend payment data (minimally: account number + context)
        // returned as a canonical binary
        protected abstract byte[] getAccountObject() throws IOException;

        // Account number
        public abstract String getPayeeAccount();

        // Must be called in every BackendPaymentDataDecoder.readJSONData()
        protected final void readOptionalNonce(JSONObjectReader rd) throws IOException {
            optionalNonce = rd.getBinaryConditional(NONCE_JSON);
        }
        
        protected abstract BackendPaymentDataEncoder createEncoder();
    }

    public static abstract class BackendPaymentDataEncoder {
        
        private BackendPaymentDataDecoder backendPaymentDataDecoder;

        public final String getContext() {
            return backendPaymentDataDecoder.getContext();
        }

        public final String getQualifier() {
            return backendPaymentDataDecoder.getQualifier();  // Optional
        }

        public final JSONObjectWriter writeObject() throws IOException {
            return backendPaymentDataDecoder.getWriter();
        }

        public final static BackendPaymentDataEncoder create(BackendPaymentDataDecoder backendPaymentDataDecoder) {
            BackendPaymentDataEncoder backendPaymentDataEncoder = backendPaymentDataDecoder.createEncoder();
            backendPaymentDataEncoder.backendPaymentDataDecoder = backendPaymentDataDecoder;
            return backendPaymentDataEncoder;
        }
    }

    public AuthorizationRequest(JSONObjectReader rd) throws IOException {
        root = Messages.AUTHORIZATION_REQUEST.parseBaseMessage(rd);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        recepientUrl = rd.getString(RECEPIENT_URL_JSON);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        paymentMethod = PaymentMethods.fromTypeUrl(rd.getString(PAYMENT_METHOD_JSON));
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        encryptedAuthorizationData = 
                rd.getObject(ENCRYPTED_AUTHORIZATION_JSON)
                    .getEncryptionObject(new JSONCryptoHelper.Options()).require(true);
        undecodedPaymentMethodSpecific = rd.getObject(BACKEND_PAYMENT_DATA_JSON);
        rd.scanAway(BACKEND_PAYMENT_DATA_JSON);  // Read all to not throw on checkForUnread()
        referenceId = rd.getString(REFERENCE_ID_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(REQUEST_SIGNATURE_JSON, new JSONCryptoHelper.Options());
        rd.checkForUnread();
    }

    Software software;

    JSONDecryptionDecoder encryptedAuthorizationData;

    JSONObjectReader root;
    
    JSONObjectReader undecodedPaymentMethodSpecific;

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

    public BackendPaymentDataDecoder getBackendPaymentData(
                                  JSONDecoderCache knownPaymentMethods) throws IOException {
        return (BackendPaymentDataDecoder) knownPaymentMethods
                .parse(undecodedPaymentMethodSpecific.clone()); // Clone => Fresh read
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
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          BackendPaymentDataEncoder paymentMethodSpecific,
                                          String referenceId,
                                          ServerAsymKeySigner signer) throws IOException {
        return Messages.AUTHORIZATION_REQUEST.createBaseMessage()
            .setDynamic((wr) -> testMode == null ? wr : wr.setBoolean(TEST_MODE_JSON, testMode))
            .setString(RECEPIENT_URL_JSON, recepientUrl)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(PAYMENT_METHOD_JSON, paymentMethod.getPaymentMethodUrl())
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData)
            .setObject(BACKEND_PAYMENT_DATA_JSON, paymentMethodSpecific.writeObject())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setObject(SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME, PaymentRequest.SOFTWARE_VERSION))
            .setSignature(REQUEST_SIGNATURE_JSON, signer);
    }

    public AuthorizationData getDecryptedAuthorizationData(
            ArrayList<JSONDecryptionDecoder.DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        AuthorizationData authorizationData =
            new AuthorizationData(JSONParser.parse(encryptedAuthorizationData.getDecryptedData(decryptionKeys)));
        if (!ArrayUtil.compare(authorizationData.requestHash, paymentRequest.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        if (!authorizationData.paymentMethodUrl.equals(paymentMethod.paymentMethodUrl)) {
            throw new IOException("Non-matching \"" + PAYMENT_METHOD_JSON + "\"");
        }
        return authorizationData;
    }
}
