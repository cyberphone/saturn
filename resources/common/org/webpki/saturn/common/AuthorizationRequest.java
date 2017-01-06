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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONParser;

import org.webpki.json.encryption.DecryptionKeyHolder;

import org.webpki.util.ArrayUtil;

public class AuthorizationRequest implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Payee";
    public static final String SOFTWARE_VERSION = "1.00";

    public AuthorizationRequest(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.AUTHORIZATION_REQUEST, root = rd);
        testMode = rd.getBooleanConditional(TEST_MODE_JSON);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        payerAccountType = PayerAccountTypes.fromTypeUri(rd.getString(ACCOUNT_TYPE_JSON));
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        encryptedAuthorizationData = rd.getObject(ENCRYPTED_AUTHORIZATION_JSON).getEncryptionObject().require(true);
        if (rd.hasProperty(PAYEE_ACCOUNT_JSON)) {
            payeeAccount = new AccountDescriptor(rd.getObject(PAYEE_ACCOUNT_JSON));
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        if (rd.hasProperty(EXPIRES_JSON)) {
            expires = rd.getDateTime(EXPIRES_JSON);
        }
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        publicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        comparePublicKeys (publicKey, paymentRequest);
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

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    AccountDescriptor payeeAccount;
    public AccountDescriptor getAccountDescriptor() {
        return payeeAccount;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
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
                                          String authorityUrl,
                                          PayerAccountTypes payerAccountType,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          AccountDescriptor payeeAccount,
                                          String referenceId,
                                          Date expires,
                                          ServerAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(Messages.AUTHORIZATION_REQUEST);
        if (testMode != null) {
            wr.setBoolean(TEST_MODE_JSON, testMode);
        }
        wr.setString(AUTHORITY_URL_JSON, authorityUrl)
          .setString(ACCOUNT_TYPE_JSON, payerAccountType.getTypeUri())
          .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
          .setObject(ENCRYPTED_AUTHORIZATION_JSON, encryptedAuthorizationData);
        if (payeeAccount != null) {
            wr.setObject(PAYEE_ACCOUNT_JSON, payeeAccount.writeObject());
        }
        wr.setString(REFERENCE_ID_JSON, referenceId)
          .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress);
        if (expires != null) {
            wr.setDateTime(EXPIRES_JSON, expires, true);
        }
        return wr
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }

    public static void comparePublicKeys(PublicKey publicKey, PaymentRequest paymentRequest) throws IOException {
        if (!publicKey.equals(paymentRequest.getPublicKey())) {
            throw new IOException("Outer and inner public keys differ");
        }
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
