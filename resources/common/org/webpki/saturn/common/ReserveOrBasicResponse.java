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

import java.security.PublicKey;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Signer;

public class ReserveOrBasicResponse implements BaseProperties {

    static Messages matching(TransactionResponse transactionResponse) {
        switch (transactionResponse.transactionRequest.reserveOrBasicRequest.message) {
            case BASIC_CREDIT_REQUEST:
                return Messages.BASIC_CREDIT_RESPONSE;

            case RESERVE_CREDIT_REQUEST:
                return Messages.RESERVE_CREDIT_RESPONSE;

            default:
                return Messages.RESERVE_CARDPAY_RESPONSE;
        }
    }

    public static JSONObjectWriter encode(TransactionResponse transactionResponse,
                                          JSONX509Signer signer) throws IOException {
        return Messages.createBaseMessage(matching(transactionResponse))
            .setObject(EMBEDDED_JSON, transactionResponse.root)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setSignature (signer);
    }

    JSONObjectReader root;

    GregorianCalendar timeStamp;

    Messages message;

    public ReserveOrBasicResponse(JSONObjectReader rd) throws IOException {
        transactionResponse = new TransactionResponse(rd.getObject(EMBEDDED_JSON));
        message = Messages.parseBaseMessage(matching(transactionResponse), root = rd);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        transactionResponse
            .transactionRequest
                .compareCertificates(signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE));
        rd.checkForUnread();
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    TransactionResponse transactionResponse;
    public TransactionResponse getTransactionResponse() {
        return transactionResponse;
    }

    // Convenience methods
    public PayerAccountTypes getPayerAccountType() {
        return transactionResponse.transactionRequest.reserveOrBasicRequest.accountType;
    }

    // Convenience methods
    public PublicKey getPublicKey() {
        return transactionResponse.transactionRequest.reserveOrBasicRequest.outerPublicKey;
    }

    public String getAccountReference() {
        return transactionResponse.accountReference;
    }

    public String getFormattedAccountReference() {
        return transactionResponse.transactionRequest.reserveOrBasicRequest.accountType.cardPayment ?
            AuthorizationData.formatCardNumber(transactionResponse.accountReference) : transactionResponse.accountReference;
    }
}
