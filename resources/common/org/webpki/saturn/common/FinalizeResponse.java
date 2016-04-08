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

public class FinalizeResponse implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";

    public FinalizeResponse(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        Messages.parseBaseMessage(Messages.FINALIZE_CREDIT_RESPONSE, rd);
        if (rd.hasProperty(ERROR_CODE_JSON)) {
            errorReturn = new ErrorReturn(rd);
            return;
        }
        requestHash = RequestHash.parse(rd);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    ErrorReturn errorReturn;
    public boolean success() {
        return errorReturn == null;
    }

    public ErrorReturn getErrorReturn() {
        return errorReturn;
    }

    byte[] requestHash;
    public byte[] getRequestHash() throws IOException {
        return requestHash;
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

    Software software;
    public Software getSoftware() {
        return software;
    }

    public static JSONObjectWriter encode(ErrorReturn errorReturn)
    throws IOException, GeneralSecurityException {
        return errorReturn.write(Messages.createBaseMessage(Messages.FINALIZE_CARDPAY_RESPONSE));
    }

    public static JSONObjectWriter encode(FinalizeRequest finalizeRequest,
                                          String referenceId, 
                                          ServerX509Signer signer)
    throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.FINALIZE_CREDIT_RESPONSE)
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                .setString(JSONSignatureDecoder.ALGORITHM_JSON, RequestHash.JOSE_SHA_256_ALG_ID)
                .setBinary(JSONSignatureDecoder.VALUE_JSON, 
                           RequestHash.getRequestHash(new JSONObjectWriter(finalizeRequest.root))))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
