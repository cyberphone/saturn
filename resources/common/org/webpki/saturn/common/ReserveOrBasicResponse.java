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

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;
import org.webpki.json.JSONX509Signer;

public class ReserveOrBasicResponse implements BaseProperties {

    static Messages matching(Messages message) {
        switch (message) {
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
        return Messages.createBaseMessage(matching(transactionResponse.embeddedRequest.embeddedRequest.message))
            .setObject(EMBEDDED_REQUEST_JSON, transactionResponse.root)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(TransactionRequest.SOFTWARE_NAME,
                                                      TransactionRequest.SOFTWARE_VERSION))
            .setSignature (signer);
    }

    JSONObjectReader root;
    
    GregorianCalendar timeStamp;
    
    public ReserveOrBasicResponse(JSONObjectReader rd) throws IOException {
        transactionResponse = new TransactionResponse(rd.getObject(EMBEDDED_REQUEST_JSON));
        Messages.parseBaseMessage(matching(transactionResponse.embeddedRequest.embeddedRequest.message), root = rd);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    TransactionResponse transactionResponse;
    public TransactionResponse getTransactionResponse() {
        return transactionResponse;
    }
    
    Software software;
    public Software getSoftware() {
        return software;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }
}
