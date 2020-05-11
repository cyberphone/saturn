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

import java.util.GregorianCalendar;

import org.webpki.json.JSONCryptoHelper;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

import org.webpki.util.ISODateTime;

public class RefundResponse implements BaseProperties {
    
    public RefundResponse(JSONObjectReader rd) throws IOException {
        root = Messages.REFUND_RESPONSE.parseBaseMessage(rd);
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON, ISODateTime.COMPLETE);
        software = new Software(rd);
        refundRequest = new RefundRequest(Messages.REFUND_REQUEST.getEmbeddedMessage(rd), null);
        signatureDecoder = rd.getSignature(AUTHORIZATION_SIGNATURE_JSON,
                new JSONCryptoHelper.Options()
                    .setKeyIdOption(JSONCryptoHelper.KEY_ID_OPTIONS.FORBIDDEN)
                    .setPublicKeyOption(JSONCryptoHelper.PUBLIC_KEY_OPTIONS.CERTIFICATE_PATH));
        rd.checkForUnread();
    }

    JSONObjectReader root;

    Software software;

    GregorianCalendar dateTime;

    String optionalLogData;
    public String getOptionalLogData() {
        return optionalLogData;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }


    RefundRequest refundRequest;
    public RefundRequest getRefundRequest() {
        return refundRequest;
    }

    public static JSONObjectWriter encode(RefundRequest refundRequest,
                                          String referenceId,
                                          String optionalLogData,
                                          ServerX509Signer signer) throws IOException {
        return Messages.REFUND_RESPONSE.createBaseMessage()
            .setDynamic((wr) -> optionalLogData == null ? wr : wr.setString(LOG_DATA_JSON, optionalLogData))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), ISODateTime.UTC_NO_SUBSECONDS)
            .setDynamic((wr) -> Software.encode(wr,
                                                TransactionResponse.SOFTWARE_NAME,
                                                TransactionResponse.SOFTWARE_VERSION))
            .setObject(Messages.REFUND_REQUEST.lowerCamelCase(), refundRequest.root)
            .setSignature(AUTHORIZATION_SIGNATURE_JSON, signer);
    }
}
