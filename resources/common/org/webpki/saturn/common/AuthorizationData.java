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

import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import java.security.PublicKey;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.AsymKeySignerInterface;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONAsymKeySigner;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.json.encryption.DataEncryptionAlgorithms;

public class AuthorizationData implements BaseProperties {

    public static final String SOFTWARE_ID      = "WebPKI.org - Wallet";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          AccountDescriptor account,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          ResponseToChallenge[] optionalChallengeResults,
                                          String referenceId,
                                          GregorianCalendar timeStamp,
                                          JSONAsymKeySigner signer) throws IOException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                .setString(JSONSignatureDecoder.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                .setBinary(JSONSignatureDecoder.VALUE_JSON, paymentRequest.getRequestHash()))
            .setString(DOMAIN_NAME_JSON, domainName)
            .setObject(ACCOUNT_JSON, account.writeObject())
            .setObject(ENCRYPTION_PARAMETERS_JSON, 
                       new JSONObjectWriter()
                .setString(JSONSignatureDecoder.ALGORITHM_JSON, dataEncryptionAlgorithm.toString())
                .setBinary(KEY_JSON, dataEncryptionKey));
        if (optionalChallengeResults != null && optionalChallengeResults.length > 0) {
            JSONArrayWriter aw = wr.setArray(RESPONSE_TO_CHALLENGE_JSON);
            for (ResponseToChallenge challengeResult : optionalChallengeResults) {
                aw.setObject(challengeResult.writeObject());
            }
        }
        if (referenceId != null) {
            wr.setString(REFERENCE_ID_JSON, referenceId);
        }
        return wr.setDateTime(TIME_STAMP_JSON, timeStamp, false)
                 .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
                 .setSignature (signer);
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          AccountDescriptor account,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm,
                                          ResponseToChallenge[] optionalChallengeResults,
                                          String referenceId,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          AsymKeySignerInterface signer) throws IOException {
        return encode(paymentRequest,
                      domainName,
                      account,
                      dataEncryptionKey,
                      dataEncryptionAlgorithm,
                      optionalChallengeResults,
                      referenceId,
                      new GregorianCalendar(),
                      new JSONAsymKeySigner(signer)
                          .setSignatureAlgorithm(signatureAlgorithm)
                          .setAlgorithmPreferences(AlgorithmPreferences.JOSE));
    }

    public static String formatCardNumber(String accountId) {
        StringBuffer s = new StringBuffer();
        int q = 0;
        for (char c : accountId.toCharArray()) {
            if (q != 0 && q % 4 == 0) {
                s.append(' ');
            }
            s.append(c);
            q++;
        }
        return s.toString();
    }

    public AuthorizationData(JSONObjectReader rd) throws IOException {
        requestHash = RequestHash.parse(rd);
        domainName = rd.getString(DOMAIN_NAME_JSON);
        account = new AccountDescriptor(rd.getObject(ACCOUNT_JSON));
        JSONObjectReader encryptionParameters = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
        dataEncryptionAlgorithm = DataEncryptionAlgorithms
            .getAlgorithmFromString(encryptionParameters.getString(JSONSignatureDecoder.ALGORITHM_JSON));
        dataEncryptionKey = encryptionParameters.getBinary(KEY_JSON);
        if (rd.hasProperty(RESPONSE_TO_CHALLENGE_JSON)) {
            LinkedHashMap<String,ResponseToChallenge> results = new LinkedHashMap<String,ResponseToChallenge>();
            JSONArrayReader ar = rd.getArray(RESPONSE_TO_CHALLENGE_JSON);
             do {
                 ResponseToChallenge challengeResult = new ResponseToChallenge(ar.getObject());
                if (results.put(challengeResult.getId(), challengeResult) != null) {
                    throw new IOException("Duplicate: " + challengeResult.getId());
                }
            } while (ar.hasMore());
            optionalChallengeResults = results.values().toArray(new ResponseToChallenge[0]);
        }
        referenceId = rd.getStringConditional(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        publicKey = rd.getSignature(AlgorithmPreferences.JOSE).getPublicKey();
        rd.checkForUnread();
    }

    DataEncryptionAlgorithms dataEncryptionAlgorithm;
    public DataEncryptionAlgorithms getDataEncryptionAlgorithm() {
        return dataEncryptionAlgorithm;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    byte[] dataEncryptionKey;
    public byte[] getDataEncryptionKey() {
        return dataEncryptionKey;
    }

    ResponseToChallenge[] optionalChallengeResults;
    public ResponseToChallenge[] getOptionalChallengeResults() {
        return optionalChallengeResults;
    }

    byte[] requestHash;
    public byte[] getRequestHash() {
        return requestHash;
    }

    String domainName;
    public String getDomainName() {
        return domainName;
    }

    AccountDescriptor account;
    public AccountDescriptor getAccount() {
        return account;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
