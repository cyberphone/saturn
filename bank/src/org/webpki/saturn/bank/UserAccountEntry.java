/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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
package org.webpki.saturn.bank;

import java.io.IOException;

import java.math.BigDecimal;

import java.security.PublicKey;

import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONObjectReader;

import org.webpki.json.DataEncryptionAlgorithms;
import org.webpki.json.KeyEncryptionAlgorithms;

import org.webpki.saturn.common.TemporaryCardDBDecoder;

public class UserAccountEntry {
    PublicKey publicKey;
    String paymentMethod;
    String accountId;
    BigDecimal balance;
    String providerAuthorityUrl;
    AsymSignatureAlgorithms signatureAlgorithm;
    String optionalKeyId;
    PublicKey encryptionKey;
    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    KeyEncryptionAlgorithms keyEncryptionAlgorithm;
    
    RiskBasedAuthentication riskBasedAuthentication = new RiskBasedAuthentication();

    public UserAccountEntry(JSONObjectReader rd) throws IOException {
        TemporaryCardDBDecoder temp = new TemporaryCardDBDecoder(rd);
        publicKey = temp.cardPublicKey;
        paymentMethod = temp.coreCardData.getPaymentMethod();
        accountId = temp.coreCardData.getAccountId();
        balance = temp.coreCardData.getTempBalanceFix();
        providerAuthorityUrl = temp.coreCardData.getAuthorityUrl();
        signatureAlgorithm = temp.coreCardData.getSignatureAlgorithm();
        optionalKeyId = temp.coreCardData.getOptionalKeyId();
        encryptionKey = temp.coreCardData.getEncryptionKey();
        dataEncryptionAlgorithm = temp.coreCardData.getDataEncryptionAlgorithm();
        keyEncryptionAlgorithm = temp.coreCardData.getKeyEncryptionAlgorithm();
        rd.checkForUnread();
    }

    public String getAccountId() {
        return accountId;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return optionalKeyId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public RiskBasedAuthentication getRiskBasedAuthentication() {
        return riskBasedAuthentication;
    }
}
