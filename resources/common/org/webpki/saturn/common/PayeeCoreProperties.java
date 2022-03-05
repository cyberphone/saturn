
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

import java.net.URLEncoder;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

import java.util.ArrayList;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64URL;

public class PayeeCoreProperties implements BaseProperties {
    
    // Only for "init" not part of Saturn vocabulary
    static final String PAYEE_ACCOUNTS_JSON = "payeeAccounts";

    ArrayList<byte[]> optionalAccountHashes;
    SignatureParameter[] signatureParameters;
    String payeeId;
    String commonName;
    String homePage;
    String logotypeUrl;
    String urlSafeId;
    String payeeAuthorityUrl;

    private HashAlgorithms accountHashAlgorithm;

    public static class SignatureParameter {
        
        AsymSignatureAlgorithms signatureAlgorithm;
        PublicKey publicKey;

        public SignatureParameter(AsymSignatureAlgorithms signatureAlgorithm, PublicKey publicKey) {
            this.signatureAlgorithm = signatureAlgorithm;
            this.publicKey = publicKey;
        }
    }
    
    

    public PayeeCoreProperties(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        payeeId = rd.getString(LOCAL_PAYEE_ID_JSON);
        commonName = rd.getString(COMMON_NAME_JSON);
        homePage = rd.getString(HOME_PAGE_JSON);
        logotypeUrl = rd.getString(LOGOTYPE_URL_JSON);
        if (rd.hasProperty(ACCOUNT_VERIFIER_JSON)) {
            optionalAccountHashes = new ArrayList<>();
            JSONObjectReader accountVerifier = rd.getObject(ACCOUNT_VERIFIER_JSON);
            accountHashAlgorithm = 
                    CryptoUtils.getHashAlgorithm(accountVerifier, JSONCryptoHelper.ALGORITHM_JSON);
            JSONArrayReader accountHashes = accountVerifier.getArray(HASHED_PAYEE_ACCOUNTS_JSON);
            do {
                optionalAccountHashes.add(accountHashes.getBinary());
            } while (accountHashes.hasMore());
        }
        ArrayList<SignatureParameter> parameterArray = new ArrayList<>();
        JSONArrayReader jsonParameterArray = rd.getArray(SIGNATURE_PARAMETERS_JSON);
        do {
            JSONObjectReader signatureParameter = jsonParameterArray.getObject();
            parameterArray.add(
                    new SignatureParameter(
                            CryptoUtils.getSignatureAlgorithm(signatureParameter,
                                                              JSONCryptoHelper.ALGORITHM_JSON),
                    signatureParameter.getPublicKey()));
        } while (jsonParameterArray.hasMore());
        signatureParameters = parameterArray.toArray(new SignatureParameter[0]);
    }
    
    public static String createUrlSafeId(String payeeId) throws IOException {
        if (URLEncoder.encode(payeeId, "utf-8").equals(payeeId)) {
            return payeeId;
        }
        return Base64URL.encode(payeeId.getBytes("utf-8"));
    }

    public static PayeeCoreProperties init(JSONObjectReader rd,
                                           String payeeBaseAuthorityUrl,
                                           HashAlgorithms accountHashAlgorithm,
                                           JSONDecoderCache knownPaymentMethods)
            throws IOException, GeneralSecurityException {
        ArrayList<byte[]> optionalAccountHashes = new ArrayList<>();
        JSONArrayReader payeeAccounts = rd.getArray(PAYEE_ACCOUNTS_JSON);
        do {
            AccountDataDecoder paymentMethodDecoder =
                (AccountDataDecoder)knownPaymentMethods.parse(payeeAccounts.getObject());
            byte[] accountHash = paymentMethodDecoder.getAccountHash(accountHashAlgorithm);
            if (accountHash != null) {
                optionalAccountHashes.add(accountHash);
            }
        } while (payeeAccounts.hasMore());
        PayeeCoreProperties payeeCoreProperties = new PayeeCoreProperties(rd);
        payeeCoreProperties.accountHashAlgorithm = accountHashAlgorithm;
        payeeCoreProperties.optionalAccountHashes = 
                optionalAccountHashes.isEmpty() ? null : optionalAccountHashes;
        String urlSafeId = createUrlSafeId(payeeCoreProperties.payeeId);
        payeeCoreProperties.urlSafeId = urlSafeId;
        payeeCoreProperties.payeeAuthorityUrl = payeeBaseAuthorityUrl + urlSafeId;
        return payeeCoreProperties;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getLogotypeUrl() {
        return logotypeUrl;
    }

    public String getHomePage() {
        return homePage;
    }

    public SignatureParameter[] getSignatureParameters() {
        return signatureParameters;
    }

    public ArrayList<byte[]> getAccountHashes() {
        return optionalAccountHashes;
    }

    public String getPayeeAuthorityUrl() {
        return payeeAuthorityUrl;
    }

    public JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        wr.setString(LOCAL_PAYEE_ID_JSON, payeeId)
          .setString(COMMON_NAME_JSON, commonName)
          .setString(HOME_PAGE_JSON, homePage)
          .setString(LOGOTYPE_URL_JSON, logotypeUrl);
        if (optionalAccountHashes != null) {
            wr.setObject(ACCOUNT_VERIFIER_JSON)
                  .setString(JSONCryptoHelper.ALGORITHM_JSON,
                             accountHashAlgorithm.getJoseAlgorithmId())
                  .setBinaryArray(HASHED_PAYEE_ACCOUNTS_JSON, optionalAccountHashes);
        }
        JSONArrayWriter jsonArray = wr.setArray(SIGNATURE_PARAMETERS_JSON);
        for (SignatureParameter signatureParameter : signatureParameters) {
            jsonArray.setObject().setString(JSONCryptoHelper.ALGORITHM_JSON,
                                            signatureParameter
                                                .signatureAlgorithm
                                                    .getJoseAlgorithmId())
                                 .setPublicKey(signatureParameter.publicKey);
        }
        return wr;
    }

    public void verify(JSONSignatureDecoder signatureDecoder) throws IOException {
        boolean publicKeyMismatch = true;
        for (SignatureParameter signatureParameter : signatureParameters) {
            if (signatureParameter.publicKey.equals(signatureDecoder.getPublicKey())) {
                publicKeyMismatch = false;
                if (signatureParameter.signatureAlgorithm == 
                        (AsymSignatureAlgorithms)signatureDecoder.getAlgorithm()) {
                    return;
                }
            }
        }
        throw new IOException((publicKeyMismatch ? "Public key" : "Signature algorithm") + 
                              " mismatch for payee: " + payeeId);
    }

    public void verifyAccount(AccountDataDecoder backendAccountDataDecoder)
    throws IOException, GeneralSecurityException {
        byte[] accountHash = backendAccountDataDecoder.getAccountHash(accountHashAlgorithm);
        if (getAccountHashes() == null) {
            if (accountHash != null) {
                throw new IOException("Missing \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        } else {
            if (accountHash == null) {
                throw new IOException("Missing verifiable payee account");
            }
            boolean notFound = true;
            for (byte[] hash : getAccountHashes()) {
                if (ArrayUtil.compare(accountHash, hash)) {
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                throw new IOException("Payee account does not match \"" + ACCOUNT_VERIFIER_JSON + 
                                      "\" in \"" + Messages.PAYEE_AUTHORITY.toString() + "\"");
            }
        }
    }
}
