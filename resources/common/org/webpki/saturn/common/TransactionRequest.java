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
import java.util.Arrays;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class TransactionRequest implements BaseProperties {
    
    public TransactionRequest(JSONObjectReader rd) throws IOException {
        Messages.parseBaseMessage(Messages.TRANSACTION_REQUEST, root = rd);
        reserveOrBasicRequest = new ReserveOrBasicRequest(rd.getObject(EMBEDDED_JSON));

        // Strictly not necessary but it could hold "config" data
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);

        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;
    
    Software software;
    
    GregorianCalendar dateTime;

    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    ReserveOrBasicRequest reserveOrBasicRequest;
    public ReserveOrBasicRequest getReserveOrBasicRequest() {
        return reserveOrBasicRequest;
    }

    public static JSONObjectWriter encode(ReserveOrBasicRequest reserveOrBasicRequest,
                                          String authorityUrl,
                                          String referenceId,
                                          ServerX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.TRANSACTION_REQUEST)
            .setObject(EMBEDDED_JSON, reserveOrBasicRequest.root)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setObject(SOFTWARE_JSON, Software.encode(AuthorizationResponse.SOFTWARE_NAME, AuthorizationResponse.SOFTWARE_VERSION))
            .setSignature(signer);
    }

    void compareCertificates(JSONSignatureDecoder signatureDecoder) throws IOException {
        if (!Arrays.equals(this.signatureDecoder.getCertificatePath(), signatureDecoder.getCertificatePath())) {
            throw new IOException("Outer and inner certificates differ");
        }
    }
}
