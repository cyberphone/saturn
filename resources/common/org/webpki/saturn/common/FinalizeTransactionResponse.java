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
import org.webpki.json.JSONSignatureTypes;

public class FinalizeTransactionResponse implements BaseProperties {

    public FinalizeTransactionResponse(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        Messages.parseBaseMessage(Messages.FINALIZE_TRANSACTION_RESPONSE, root = rd);
        finalizeTransactionRequest = new FinalizeTransactionRequest(rd.getObject(EMBEDDED_JSON));
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;
    
    Software software;
    
    GregorianCalendar dateTime;

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    AccountDescriptor[] accountDescriptors;
    public AccountDescriptor[] getPayeeAccountDescriptors() {
        return accountDescriptors;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    FinalizeTransactionRequest finalizeTransactionRequest;
    public FinalizeTransactionRequest getFinalizeTransactionRequest() {
        return finalizeTransactionRequest;
    }

    public static JSONObjectWriter encode(FinalizeTransactionRequest finalizeTransactionRequest,
                                          String referenceId,
                                          ServerX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.FINALIZE_TRANSACTION_RESPONSE)
            .setObject(EMBEDDED_JSON, finalizeTransactionRequest.root)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setSignature(signer);
    }
}
