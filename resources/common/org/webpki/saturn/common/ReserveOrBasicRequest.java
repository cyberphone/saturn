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

import org.webpki.util.ArrayUtil;

public class ReserveOrBasicRequest implements BaseProperties {
    
    static final Messages[] valid = {Messages.BASIC_CREDIT_REQUEST,
                                     Messages.RESERVE_CREDIT_REQUEST,
                                     Messages.RESERVE_CARDPAY_REQUEST};
    
    public ReserveOrBasicRequest(JSONObjectReader rd) throws IOException {
        message = Messages.parseBaseMessage(valid, root = rd);
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        accountType = PayerAccountTypes.fromTypeUri(rd.getString(ACCOUNT_TYPE_JSON));
        encryptedAuthorizationData = EncryptedData.parse(rd.getObject(AUTHORIZATION_DATA_JSON), false);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        if (message.isCardPayment()) {
            acquirerAuthorityUrl = rd.getString(ACQUIRER_AUTHORITY_URL_JSON);
        }
        if (!message.isBasicCredit()) {
            expires = rd.getDateTime(EXPIRES_JSON);
        }
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        outerPublicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        rd.checkForUnread();
    }


    GregorianCalendar dateTime;

    PublicKey outerPublicKey;

    EncryptedData encryptedAuthorizationData;

    JSONObjectReader root;

    PayerAccountTypes accountType;
    public PayerAccountTypes getPayerAccountType() {
        return accountType;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    Messages message;
    public Messages getMessage() {
        return message;
    }

    String acquirerAuthorityUrl;
    public String getAcquirerAuthorityUrl() {
        return acquirerAuthorityUrl;
    }

    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }
 
    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public static JSONObjectWriter encode(boolean basicCredit,
                                          String providerAuthorityUrl,
                                          PayerAccountTypes accountType,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          String acquirerAuthorityUrl,
                                          Date expires,
                                          ServerAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = Messages.createBaseMessage(basicCredit ? Messages.BASIC_CREDIT_REQUEST : 
            accountType.isCardPayment() ? Messages.RESERVE_CARDPAY_REQUEST : Messages.RESERVE_CREDIT_REQUEST)
            .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl)
            .setString(ACCOUNT_TYPE_JSON, accountType.getTypeUri())
            .setObject(AUTHORIZATION_DATA_JSON, encryptedAuthorizationData)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root);
        if (accountType.isCardPayment()) {
            wr.setString(ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl);
        }
        if (!basicCredit) {
            wr.setDateTime(EXPIRES_JSON, expires, true);
        }
        return wr.setDateTime(TIME_STAMP_JSON, new Date(), true)
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
            new AuthorizationData(encryptedAuthorizationData.getDecryptedData(decryptionKeys));
        comparePublicKeys (outerPublicKey, paymentRequest);
        if (!ArrayUtil.compare(authorizationData.getRequestHash(), paymentRequest.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        return authorizationData;
    }
}
