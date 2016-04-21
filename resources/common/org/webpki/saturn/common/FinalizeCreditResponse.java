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

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

public class FinalizeCreditResponse implements BaseProperties {
    
    public FinalizeCreditResponse(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        Messages.parseBaseMessage(Messages.FINALIZE_CREDIT_RESPONSE, rd);
        finalizeTransactionResponse = new FinalizeTransactionResponse(rd.getObject(EMBEDDED_JSON));
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        finalizeTransactionResponse
            .finalizeTransactionRequest
                .finalizeRequest
                    .reserveOrBasicResponse
                        .transactionResponse
                            .transactionRequest
            .compareCertificates(signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE));
        rd.checkForUnread();
    }

    FinalizeTransactionResponse finalizeTransactionResponse;
    public FinalizeTransactionResponse getFinalizeTransactionResponse() throws IOException {
        return finalizeTransactionResponse;
    }
    
    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    public static JSONObjectWriter encode(FinalizeTransactionResponse finalizeTransactionResponse,
                                          String referenceId, 
                                          ServerX509Signer signer)
    throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.FINALIZE_CREDIT_RESPONSE)
            .setObject(EMBEDDED_JSON, finalizeTransactionResponse.root)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setSignature(signer);
    }
}
