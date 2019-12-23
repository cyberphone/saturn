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

import java.security.PublicKey;

import java.util.ArrayList;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;

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
    String payeeHomePage;
    String payeeCommonName;
    String urlSafeId;
    String payeeAuthorityUrl;

    public static class SignatureParameter {
        
        AsymSignatureAlgorithms signatureAlgorithm;
        PublicKey publicKey;

        public SignatureParameter(AsymSignatureAlgorithms signatureAlgorithm, PublicKey publicKey) {
            this.signatureAlgorithm = signatureAlgorithm;
            this.publicKey = publicKey;
        }
    }

    public PayeeCoreProperties(JSONObjectReader rd) throws IOException {
        payeeId = rd.getString(LOCAL_PAYEE_ID_JSON);
        payeeCommonName = rd.getString(COMMON_NAME_JSON);
        payeeHomePage = rd.getString(HOME_PAGE_JSON);
        if (rd.hasProperty(ACCOUNT_VERIFIER_JSON)) {
            optionalAccountHashes = new ArrayList<byte[]>();
            JSONObjectReader accountVerifier = rd.getObject(ACCOUNT_VERIFIER_JSON);
            if (!accountVerifier.getString(JSONCryptoHelper.ALGORITHM_JSON).equals(RequestHash.JOSE_SHA_256_ALG_ID)) {
                throw new IOException("Unexpected hash algorithm");
            }
            JSONArrayReader accountHashes = accountVerifier.getArray(HASHED_PAYEE_ACCOUNTS_JSON);
            do {
                optionalAccountHashes.add(accountHashes.getBinary());
            } while (accountHashes.hasMore());
        }
        ArrayList<SignatureParameter> parameterArray = new ArrayList<SignatureParameter>();
        JSONArrayReader jsonParameterArray = rd.getArray(SIGNATURE_PARAMETERS_JSON);
        do {
            JSONObjectReader signatureParameter = jsonParameterArray.getObject();
            parameterArray.add(
                new SignatureParameter(
                    AsymSignatureAlgorithms
                        .getAlgorithmFromId(signatureParameter.getString(JSONCryptoHelper.ALGORITHM_JSON),
                                            AlgorithmPreferences.JOSE),
                    signatureParameter.getPublicKey()));
        } while (jsonParameterArray.hasMore());
        signatureParameters = parameterArray.toArray(new SignatureParameter[0]);
    }

    public static PayeeCoreProperties init(JSONObjectReader rd,
                                           String payeeBaseAuthorityUrl,

                                           // Only of interest if addVerifier = true
                                           JSONDecoderCache knownPaymentMethods,

                                           boolean addVerifier) throws IOException {
        ArrayList<byte[]> optionalAccountHashes = new ArrayList<byte[]>();
        if (addVerifier) {
            JSONArrayReader payeeAccounts = rd.getArray(PAYEE_ACCOUNTS_JSON);
            do {
                AuthorizationRequest.PaymentBackendMethodDecoder paymentMethodDecoder =
                    (AuthorizationRequest
                            .PaymentBackendMethodDecoder)knownPaymentMethods
                                .parse(payeeAccounts.getObject());
                byte[] accountHash = paymentMethodDecoder.getAccountHash();
                if (accountHash != null) {
                    optionalAccountHashes.add(accountHash);
                }
            } while (payeeAccounts.hasMore());
        }
        PayeeCoreProperties payeeCoreProperties = new PayeeCoreProperties(rd);
        payeeCoreProperties.optionalAccountHashes = 
                optionalAccountHashes.isEmpty() ? null : optionalAccountHashes;
        String urlSafeId = payeeCoreProperties.payeeId;
        if (!URLEncoder.encode(urlSafeId, "utf-8").equals(urlSafeId)) {
            urlSafeId = Base64URL.encode(urlSafeId.getBytes("utf-8"));
        }
        payeeCoreProperties.urlSafeId = urlSafeId;
        payeeCoreProperties.payeeAuthorityUrl = payeeBaseAuthorityUrl + urlSafeId;
        return payeeCoreProperties;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public String getCommonName() {
        return payeeCommonName;
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
          .setString(COMMON_NAME_JSON, payeeCommonName)
          .setString(HOME_PAGE_JSON, payeeHomePage);
        if (optionalAccountHashes != null) {
            wr.setObject(ACCOUNT_VERIFIER_JSON)
                  .setString(JSONCryptoHelper.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                  .setBinaryArray(HASHED_PAYEE_ACCOUNTS_JSON, optionalAccountHashes);
        }
        JSONArrayWriter jsonArray = wr.setArray(SIGNATURE_PARAMETERS_JSON);
        for (SignatureParameter signatureParameter : signatureParameters) {
            jsonArray.setObject().setString(JSONCryptoHelper.ALGORITHM_JSON,
                                            signatureParameter
                                                .signatureAlgorithm
                                                    .getAlgorithmId(AlgorithmPreferences.JOSE))
                                 .setPublicKey(signatureParameter.publicKey, 
                                               AlgorithmPreferences.JOSE);
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

    public void verifyAccount(
            AuthorizationRequest.PaymentBackendMethodDecoder paymentMethodSpecific)
    throws IOException {
        byte[] accountHash = paymentMethodSpecific.getAccountHash();
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
