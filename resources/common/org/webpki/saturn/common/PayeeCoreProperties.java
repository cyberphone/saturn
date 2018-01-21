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
package org.webpki.saturn.common;

import java.io.IOException;

import java.security.PublicKey;

import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

public class PayeeCoreProperties implements BaseProperties {
    
    static final String PAYEE_ACCOUNTS_JSON = "payeeAccounts";  // Only for "init" not part of Saturn vocabularly

    Vector<byte[]> optionalAccountHashes;
    SignatureParameter[] signatureParameters;
    DecoratedPayee decoratedPayee;

    public static class SignatureParameter {
        
        AsymSignatureAlgorithms signatureAlgorithm;
        PublicKey publicKey;

        public SignatureParameter(AsymSignatureAlgorithms signatureAlgorithm, PublicKey publicKey) {
            this.signatureAlgorithm = signatureAlgorithm;
            this.publicKey = publicKey;
        }
    }

    public PayeeCoreProperties(JSONObjectReader rd) throws IOException {
        decoratedPayee = new DecoratedPayee(rd);
        if (rd.hasProperty(ACCOUNT_VERIFIER_JSON)) {
            optionalAccountHashes = new Vector<byte[]>();
            JSONObjectReader accountVerifier = rd.getObject(ACCOUNT_VERIFIER_JSON);
            if (!accountVerifier.getString(JSONSignatureDecoder.ALGORITHM_JSON).equals(RequestHash.JOSE_SHA_256_ALG_ID)) {
                throw new IOException("Unexpected hash algorithm");
            }
            JSONArrayReader accountHashes = accountVerifier.getArray(HASHED_PAYEE_ACCOUNTS_JSON);
            do {
                optionalAccountHashes.add(accountHashes.getBinary());
            } while (accountHashes.hasMore());
        }
        Vector<SignatureParameter> parameterArray = new Vector<SignatureParameter>();
        JSONArrayReader jsonParameterArray = rd.getArray(SIGNATURE_PARAMETERS_JSON);
        do {
            JSONObjectReader signatureParameter = jsonParameterArray.getObject();
            parameterArray.add(
                new SignatureParameter(
                    AsymSignatureAlgorithms
                        .getAlgorithmFromId(signatureParameter.getString(JSONSignatureDecoder.ALGORITHM_JSON),
                                            AlgorithmPreferences.JOSE),
                    signatureParameter.getPublicKey()));
        } while (jsonParameterArray.hasMore());
        signatureParameters = parameterArray.toArray(new SignatureParameter[0]);
    }

    public static PayeeCoreProperties init(JSONObjectReader rd, 
                                           JSONDecoderCache knownPaymentMethods,
                                           boolean addVerifier) throws IOException {
        JSONArrayReader payeeAccounts = rd.getArray(PAYEE_ACCOUNTS_JSON);
        Vector<byte[]> optionalAccountHashes = new Vector<byte[]>();
        do {
            AuthorizationRequest.PaymentMethodDecoder paymentMethodDecoder =
                    (AuthorizationRequest.PaymentMethodDecoder)knownPaymentMethods.parse(payeeAccounts.getObject());
            byte[] accountHash = paymentMethodDecoder.getAccountHash();
            if (accountHash != null && addVerifier) {
                optionalAccountHashes.add(accountHash);
            }
        } while (payeeAccounts.hasMore());
        PayeeCoreProperties payeeCoreProperties = new PayeeCoreProperties(rd);
        payeeCoreProperties.optionalAccountHashes = optionalAccountHashes.isEmpty() ? null : optionalAccountHashes;
        return payeeCoreProperties;
    }

    public DecoratedPayee getDecoratedPayee() {
        return decoratedPayee;
    }

    public SignatureParameter[] getSignatureParameters() {
        return signatureParameters;
    }

    public Vector<byte[]> getAccountHashes() {
        return optionalAccountHashes;
    }

    public JSONObjectWriter writeObject(JSONObjectWriter wr) throws IOException {
        decoratedPayee.writeObject(wr);
        if (optionalAccountHashes != null) {
            wr.setObject(ACCOUNT_VERIFIER_JSON)
                  .setString(JSONSignatureDecoder.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                  .setBinaryArray(HASHED_PAYEE_ACCOUNTS_JSON, optionalAccountHashes);
        }
        JSONArrayWriter jsonArray = wr.setArray(SIGNATURE_PARAMETERS_JSON);
        for (SignatureParameter signatureParameter : signatureParameters) {
            jsonArray.setObject().setString(JSONSignatureDecoder.ALGORITHM_JSON,
                                            signatureParameter
                                                .signatureAlgorithm
                                                    .getAlgorithmId(AlgorithmPreferences.JOSE))
                                 .setPublicKey(signatureParameter.publicKey, AlgorithmPreferences.JOSE);
        }
        return wr;
    }

    public void verify(Payee payee, JSONSignatureDecoder signatureDecoder) throws IOException {
        if (!this.decoratedPayee.id.equals(payee.id)) {
            throw new IOException("Payee ID mismatch " + this.decoratedPayee.id + " " + payee.id);
        }
        boolean publicKeyMismatch = true;
        for (SignatureParameter signatureParameter : signatureParameters) {
            if (signatureParameter.publicKey.equals(signatureDecoder.getPublicKey())) {
                publicKeyMismatch = false;
                if (signatureParameter.signatureAlgorithm == (AsymSignatureAlgorithms)signatureDecoder.getAlgorithm()) {
                    return;
                }
            }
        }
        throw new IOException((publicKeyMismatch ? "Public key" : "Signature algorithm") + 
                              " mismatch for payee ID: " + payee.id);
    }
}
