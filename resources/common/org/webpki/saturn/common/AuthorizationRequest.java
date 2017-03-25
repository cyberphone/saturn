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
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DecryptionKeyHolder;

import org.webpki.util.ArrayUtil;

public class AuthorizationRequest implements BaseProperties {
    
    public AuthorizationRequest(JSONObjectReader rd) throws IOException {
        root = Messages.AUTHORIZATION_REQUEST.parseBaseMessage(rd);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        recepientUrl = rd.getString(RECEPIENT_URL_JSON);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        payerAccountType = PayerAccountTypes.fromTypeUri(rd.getString(ACCOUNT_TYPE_JSON));
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        encryptedAuthorizationData = rd.getObject(ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject().require(true);
        if (rd.hasProperty(PAYEE_ACCOUNT_JSON)) {
            optionalPayeeAccount = new AccountDescriptor(rd.getObject(PAYEE_ACCOUNT_JSON));
        }
        if (rd.hasProperty(ADDITIONAL_PAYEE_DATA_JSON)) {
            additionalPayeeData = rd.getObject(ADDITIONAL_PAYEE_DATA_JSON);
            rd.scanAway(ADDITIONAL_PAYEE_DATA_JSON);
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }


    Software software;

    JSONDecryptionDecoder encryptedAuthorizationData;

    JSONObjectReader root;

    boolean testMode;
    public boolean getTestMode() {
        return testMode;
    }

    PayerAccountTypes payerAccountType;
    public PayerAccountTypes getPayerAccountType() {
        return payerAccountType;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    AccountDescriptor optionalPayeeAccount;
    public AccountDescriptor getAccountDescriptor() {
        return optionalPayeeAccount;
    }

    JSONObjectReader additionalPayeeData;
    public JSONObjectReader getAdditionalPayeeData() {
        return additionalPayeeData;
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
                                          PayerAccountTypes payerAccountType,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          AccountDescriptor optionalPayeeAccount,
                                          JSONObjectReader additionalPayeeData,
                                          String referenceId,
                                          ServerAsymKeySigner signer) throws IOException {
        return Messages.AUTHORIZATION_REQUEST.createBaseMessage()
            .setDynamic((wr) -> testMode == null ? wr : wr.setBoolean(TEST_MODE_JSON, testMode))
            .setString(RECEPIENT_URL_JSON, recepientUrl)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(ACCOUNT_TYPE_JSON, payerAccountType.getTypeUri())
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData)
            .setDynamic((wr) -> optionalPayeeAccount == null ? wr 
                      : wr.setObject(PAYEE_ACCOUNT_JSON, optionalPayeeAccount.writeObject()))
            .setDynamic((wr) -> additionalPayeeData == null ? wr 
                      : wr.setObject(ADDITIONAL_PAYEE_DATA_JSON, additionalPayeeData))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setObject(SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME, PaymentRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }

    public AuthorizationData getDecryptedAuthorizationData(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        AuthorizationData authorizationData =
            new AuthorizationData(JSONParser.parse(encryptedAuthorizationData.getDecryptedData(decryptionKeys)));
        if (!ArrayUtil.compare(authorizationData.requestHash, paymentRequest.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        if (!authorizationData.account.typeUri.equals(payerAccountType.typeUri)) {
            throw new IOException("Non-matching account \"" + TYPE_JSON + "\" value");
        }
        return authorizationData;
    }
}
