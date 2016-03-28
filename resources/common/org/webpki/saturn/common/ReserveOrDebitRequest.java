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

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ArrayUtil;

public class ReserveOrDebitRequest implements BaseProperties {
    
    public ReserveOrDebitRequest(JSONObjectReader rd) throws IOException {
        directDebit = rd.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.DIRECT_DEBIT_REQUEST.toString());
        Messages.parseBaseMessage(directDebit ?
                Messages.DIRECT_DEBIT_REQUEST : Messages.RESERVE_FUNDS_REQUEST, rd);
        accountType = PayerAccountTypes.fromTypeUri(rd.getString(ACCOUNT_TYPE_JSON));
        encryptedAuthorizationData = EncryptedData.parse(rd.getObject(AUTHORIZATION_DATA_JSON));
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        if (!directDebit) {
            expires = rd.getDateTime(EXPIRES_JSON);
        }
        Vector<AccountDescriptor> accounts = new Vector<AccountDescriptor> ();
        if (!directDebit && rd.hasProperty(ACQUIRER_AUTHORITY_URL_JSON)) {
            acquirerAuthorityUrl = rd.getString(ACQUIRER_AUTHORITY_URL_JSON);
        } else {
            JSONArrayReader ar = rd.getArray(PAYEE_ACCOUNTS_JSON);
            do {
                accounts.add(new AccountDescriptor(ar.getObject()));
            } while (ar.hasMore());
        }
        this.accounts = accounts.toArray(new AccountDescriptor[0]);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        outerPublicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        rd.checkForUnread();
    }

    PayerAccountTypes accountType;
    
    GregorianCalendar dateTime;

    Software software;
    
    PublicKey outerPublicKey;
    
    EncryptedData encryptedAuthorizationData;

    AccountDescriptor[] accounts;
    public AccountDescriptor[] getPayeeAccountDescriptors() {
        return accounts;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    boolean directDebit;
    public boolean isDirectDebit() {
        return directDebit;
    }

    String acquirerAuthorityUrl;
    public String getAcquirerAuthorityUrl() {
        return acquirerAuthorityUrl;
    }
 
    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public static JSONObjectWriter encode(boolean directDebit,
                                          PayerAccountTypes accountType,
                                          JSONObjectReader encryptedAuthorizationData,
                                          String clientIpAddress,
                                          PaymentRequest paymentRequest,
                                          String acquirerAuthorityUrl,
                                          AccountDescriptor[] accounts,
                                          Date expires,
                                          ServerAsymKeySigner signer)
        throws IOException, GeneralSecurityException {
        JSONObjectWriter wr = Messages.createBaseMessage(directDebit ?
                                       Messages.DIRECT_DEBIT_REQUEST : Messages.RESERVE_FUNDS_REQUEST)
            .setString(ACCOUNT_TYPE_JSON, accountType.getTypeUri())
            .setObject(AUTHORIZATION_DATA_JSON, encryptedAuthorizationData)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root);
        if (directDebit || acquirerAuthorityUrl == null) {
            JSONArrayWriter aw = wr.setArray(PAYEE_ACCOUNTS_JSON);
            for (AccountDescriptor account : accounts) {
                aw.setObject(account.writeObject());
            }
        } else {
            zeroTest(PAYEE_ACCOUNTS_JSON, accounts);
            wr.setString(ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl);
        }
        if (directDebit) {
            zeroTest(EXPIRES_JSON, expires);
            zeroTest(ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl);
        } else {
            wr.setDateTime(EXPIRES_JSON, expires, true);
        }
        wr.setDateTime(TIME_STAMP_JSON, new Date(), true)
          .setObject(SOFTWARE_JSON, Software.encode(PaymentRequest.SOFTWARE_NAME,
                                                    PaymentRequest.SOFTWARE_VERSION))
          .setSignature(signer);
        return wr;
    }

    static void zeroTest(String name, Object object) throws IOException {
        if (object != null) {
            throw new IOException("Argument error, parameter \"" + name + "\" must be \"null\"");
        }
    }

    public static void comparePublicKeys(PublicKey publicKey, PaymentRequest paymentRequest) throws IOException {
        if (!publicKey.equals(paymentRequest.getPublicKey())) {
            throw new IOException("Outer and inner public key differ");
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
